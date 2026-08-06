package com.cipa.votacao.service;

import com.cipa.votacao.entity.Candidato;
import com.cipa.votacao.entity.ConfiguracaoEleicao;
import com.cipa.votacao.entity.Usuario;
import com.cipa.votacao.entity.Voto;
import com.cipa.votacao.exception.CabineVotacaoException;
import com.cipa.votacao.repository.VotoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VotacaoService {

    private final VotoRepository votoRepository;
    private final CandidatoService candidatoService;
    private final ConfiguracaoService configuracaoService;
    private final UsuarioService usuarioService;
    private final ParticipacaoEleicaoService participacaoService;
    private final CabineVotacaoService cabineService;

    public boolean isVotacaoLiberada() {
        return configuracaoService.getConfiguracao().isPeriodoVotacao();
    }

    @Transactional
    public Optional<Voto> registrarVoto(Long candidatoId, Long usuarioId, Long sessaoCabineId) {
        ConfiguracaoEleicao eleicao = configuracaoService.getConfiguracao();
        if (!eleicao.isPeriodoVotacao()) {
            return Optional.empty();
        }

        Usuario usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
        if (usuario == null || !usuario.isAtivo()) {
            return Optional.empty();
        }

        try {
            cabineService.validarParaVoto(sessaoCabineId, usuarioId);
        } catch (CabineVotacaoException e) {
            return Optional.empty();
        }

        Candidato candidato = candidatoService.buscarPorId(candidatoId).orElse(null);
        if (candidato == null || !candidato.isAtivo()) {
            return Optional.empty();
        }

        if (!participacaoService.marcarComoVotou(eleicao.getId(), usuarioId)) {
            log.warn("Tentativa de reutilização de uma participação eleitoral bloqueada.");
            return Optional.empty();
        }

        Voto voto = new Voto();
        voto.setEleicaoId(eleicao.getId());
        voto.setCandidatoId(candidatoId);
        voto.setToken(UUID.randomUUID().toString());
        Voto salvo = votoRepository.saveAndFlush(voto);

        usuarioService.marcarComoVotou(usuarioId);
        cabineService.concluir(sessaoCabineId, usuarioId);

        log.info("Voto anônimo registrado com sucesso.");
        return Optional.of(salvo);
    }

    public void validarSessaoCabine(Long sessaoCabineId, Long usuarioId) {
        cabineService.validarParaVoto(sessaoCabineId, usuarioId);
    }

    public long contarTotalVotos() {
        return votoRepository.countTotalVotos(configuracaoService.getConfiguracao().getId());
    }

    public List<Object[]> getResultados() {
        return votoRepository.countVotosPorCandidato(configuracaoService.getConfiguracao().getId());
    }

    public boolean podeVotar(String matricula) {
        ConfiguracaoEleicao eleicao = configuracaoService.getConfiguracao();
        return usuarioService.buscarPorMatricula(matricula)
                .filter(Usuario::isAtivo)
                .map(usuario -> !participacaoService.jaVotou(eleicao.getId(), usuario.getId()))
                .orElse(false);
    }
}
