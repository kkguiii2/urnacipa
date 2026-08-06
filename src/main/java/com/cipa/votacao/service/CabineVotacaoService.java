package com.cipa.votacao.service;

import com.cipa.votacao.entity.CabineStatus;
import com.cipa.votacao.entity.CabineVotacao;
import com.cipa.votacao.entity.ConfiguracaoEleicao;
import com.cipa.votacao.entity.SessaoCabine;
import com.cipa.votacao.entity.Usuario;
import com.cipa.votacao.exception.CabineVotacaoException;
import com.cipa.votacao.repository.CabineVotacaoRepository;
import com.cipa.votacao.repository.SessaoCabineRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CabineVotacaoService {

    public static final long CABINE_UNICA_ID = 1L;

    private final CabineVotacaoRepository cabineRepository;
    private final SessaoCabineRepository sessaoRepository;
    private final UsuarioService usuarioService;
    private final ConfiguracaoService configuracaoService;
    private final ParticipacaoEleicaoService participacaoService;

    @Value("${app.cabine.liberacao-segundos:180}")
    private long liberacaoSegundos;

    @Value("${app.cabine.votacao-segundos:600}")
    private long votacaoSegundos;

    @Value("${app.cabine.max-tentativas:3}")
    private int maxTentativas;

    @Transactional
    public SessaoCabine liberar(String matricula, String mesarioUsername) {
        ConfiguracaoEleicao eleicao = eleicaoAtualAberta();
        Usuario usuario = usuarioService.buscarPorMatricula(matricula)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new CabineVotacaoException("Eleitor não encontrado ou inativo."));

        participacaoService.garantirCadastro(eleicao.getId(), usuario.getId());
        if (participacaoService.jaVotou(eleicao.getId(), usuario.getId())) {
            throw new CabineVotacaoException("Este eleitor já concluiu a votação.");
        }

        CabineVotacao cabine = cabineParaAtualizar();
        Optional<SessaoCabine> atual = sessaoAtual(cabine);
        atual.ifPresent(this::expirarSeNecessario);
        atual.filter(this::ocupaCabine)
                .filter(sessao -> !sessao.getEleicaoId().equals(eleicao.getId()))
                .ifPresent(sessao -> {
                    sessao.setStatus(CabineStatus.CANCELADA);
                    sessaoRepository.save(sessao);
                });
        if (atual.filter(this::ocupaCabine).isPresent()) {
            throw new CabineVotacaoException("A cabine já possui uma liberação ativa.");
        }

        LocalDateTime agora = LocalDateTime.now();
        SessaoCabine sessao = new SessaoCabine();
        sessao.setEleicaoId(eleicao.getId());
        sessao.setUsuarioId(usuario.getId());
        sessao.setMesarioUsername(mesarioUsername);
        sessao.setStatus(CabineStatus.LIBERADA);
        sessao.setLiberadaEm(agora);
        sessao.setExpiraEm(agora.plusSeconds(liberacaoSegundos));
        sessao.setTentativas(0);
        sessao = sessaoRepository.saveAndFlush(sessao);

        cabine.setSessaoAtualId(sessao.getId());
        cabineRepository.save(cabine);
        return sessao;
    }

    @Transactional
    public SessaoCabine identificar(String matricula) {
        CabineVotacao cabine = cabineParaAtualizar();
        SessaoCabine sessao = sessaoAtual(cabine)
                .orElseThrow(() -> erroIdentificacao());
        expirarSeNecessario(sessao);

        if (sessao.getStatus() != CabineStatus.LIBERADA) {
            throw erroIdentificacao();
        }

        Usuario usuario = usuarioService.buscarPorMatricula(matricula).orElse(null);
        if (usuario == null || !usuario.getId().equals(sessao.getUsuarioId())) {
            sessao.setTentativas(sessao.getTentativas() + 1);
            if (sessao.getTentativas() >= maxTentativas) {
                sessao.setStatus(CabineStatus.BLOQUEADA);
            }
            sessaoRepository.save(sessao);
            throw erroIdentificacao();
        }

        if (!usuario.isAtivo()
                || participacaoService.jaVotou(sessao.getEleicaoId(), usuario.getId())) {
            sessao.setStatus(CabineStatus.BLOQUEADA);
            sessaoRepository.save(sessao);
            throw erroIdentificacao();
        }

        LocalDateTime agora = LocalDateTime.now();
        sessao.setStatus(CabineStatus.IDENTIFICADA);
        sessao.setIdentificadaEm(agora);
        sessao.setExpiraEm(agora.plusSeconds(votacaoSegundos));
        return sessaoRepository.save(sessao);
    }

    @Transactional
    public void validarParaVoto(Long sessaoId, Long usuarioId) {
        ConfiguracaoEleicao eleicao = eleicaoAtualAberta();
        CabineVotacao cabine = cabineParaAtualizar();
        SessaoCabine sessao = sessaoAtual(cabine)
                .filter(s -> s.getId().equals(sessaoId))
                .orElseThrow(() -> new CabineVotacaoException("A sessão da cabine não é válida."));
        expirarSeNecessario(sessao);

        if (sessao.getStatus() != CabineStatus.IDENTIFICADA
                || !sessao.getUsuarioId().equals(usuarioId)
                || !sessao.getEleicaoId().equals(eleicao.getId())) {
            throw new CabineVotacaoException("A sessão da cabine não é válida.");
        }
    }

    @Transactional
    public void concluir(Long sessaoId, Long usuarioId) {
        CabineVotacao cabine = cabineParaAtualizar();
        SessaoCabine sessao = sessaoAtual(cabine)
                .filter(s -> s.getId().equals(sessaoId))
                .orElseThrow(() -> new CabineVotacaoException("A sessão da cabine não é válida."));
        if (sessao.getStatus() != CabineStatus.IDENTIFICADA
                || !sessao.getUsuarioId().equals(usuarioId)) {
            throw new CabineVotacaoException("A sessão da cabine não é válida.");
        }
        sessao.setStatus(CabineStatus.CONCLUIDA);
        sessao.setConcluidaEm(LocalDateTime.now());
        sessaoRepository.save(sessao);
    }

    @Transactional
    public void cancelar(String mesarioUsername) {
        CabineVotacao cabine = cabineParaAtualizar();
        SessaoCabine sessao = sessaoAtual(cabine)
                .orElseThrow(() -> new CabineVotacaoException("Não existe liberação para cancelar."));
        expirarSeNecessario(sessao);
        if (!ocupaCabine(sessao)) {
            throw new CabineVotacaoException("Não existe liberação ativa para cancelar.");
        }
        sessao.setStatus(CabineStatus.CANCELADA);
        sessaoRepository.save(sessao);
    }

    @Transactional
    public EstadoCabine obterEstado() {
        CabineVotacao cabine = cabineParaAtualizar();
        Optional<SessaoCabine> sessaoOpt = sessaoAtual(cabine);
        sessaoOpt.ifPresent(this::expirarSeNecessario);
        if (sessaoOpt.isEmpty()) {
            return EstadoCabine.aguardando();
        }
        SessaoCabine sessao = sessaoOpt.get();
        Usuario usuario = ocupaCabine(sessao)
                ? usuarioService.buscarPorId(sessao.getUsuarioId()).orElse(null)
                : null;
        return new EstadoCabine(
                sessao.getId(),
                sessao.getStatus(),
                usuario == null ? null : usuario.getMatricula(),
                usuario == null ? null : usuario.getNome(),
                sessao.getLiberadaEm(),
                sessao.getExpiraEm(),
                sessao.getTentativas());
    }

    @Transactional
    public boolean disponivelParaIdentificacao() {
        return obterEstado().status() == CabineStatus.LIBERADA;
    }

    private ConfiguracaoEleicao eleicaoAtualAberta() {
        ConfiguracaoEleicao eleicao = configuracaoService.getConfiguracao();
        if (!eleicao.isPeriodoVotacao()) {
            throw new CabineVotacaoException("A votação não está disponível no momento.");
        }
        return eleicao;
    }

    private CabineVotacao cabineParaAtualizar() {
        return cabineRepository.findByIdForUpdate(CABINE_UNICA_ID)
                .orElseThrow(() -> new IllegalStateException("Cabine de votação não inicializada."));
    }

    private Optional<SessaoCabine> sessaoAtual(CabineVotacao cabine) {
        if (cabine.getSessaoAtualId() == null) {
            return Optional.empty();
        }
        return sessaoRepository.findById(cabine.getSessaoAtualId());
    }

    private void expirarSeNecessario(SessaoCabine sessao) {
        if (ocupaCabine(sessao) && LocalDateTime.now().isAfter(sessao.getExpiraEm())) {
            sessao.setStatus(CabineStatus.EXPIRADA);
            sessaoRepository.save(sessao);
        }
    }

    private boolean ocupaCabine(SessaoCabine sessao) {
        return sessao.getStatus() == CabineStatus.LIBERADA
                || sessao.getStatus() == CabineStatus.IDENTIFICADA;
    }

    private CabineVotacaoException erroIdentificacao() {
        return new CabineVotacaoException("Matrícula não corresponde à liberação atual.");
    }

    public record EstadoCabine(
            Long sessaoId,
            CabineStatus status,
            String matricula,
            String nome,
            LocalDateTime liberadaEm,
            LocalDateTime expiraEm,
            int tentativas) {

        public static EstadoCabine aguardando() {
            return new EstadoCabine(null, null, null, null, null, null, 0);
        }

        public boolean ativa() {
            return status == CabineStatus.LIBERADA || status == CabineStatus.IDENTIFICADA;
        }
    }
}
