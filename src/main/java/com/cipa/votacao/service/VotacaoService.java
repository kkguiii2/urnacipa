package com.cipa.votacao.service;

import com.cipa.votacao.entity.Candidato;
import com.cipa.votacao.entity.ConfiguracaoEleicao;
import com.cipa.votacao.entity.Usuario;
import com.cipa.votacao.entity.Voto;
import com.cipa.votacao.repository.VotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Centraliza as condições para votar e grava o voto junto com a marcação do
 * eleitor dentro da mesma transação.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VotacaoService {

    private final VotoRepository votoRepository;
    private final CandidatoService candidatoService;
    private final ConfiguracaoService configuracaoService;
    private final UsuarioService usuarioService;

    public boolean isVotacaoLiberada() {
        ConfiguracaoEleicao config = configuracaoService.getConfiguracao();
        if (!config.isAberta()) {
            return false;
        }
        return config.isPeriodoVotacao();
    }

    /**
     * Revalida eleição, eleitor e candidato, cria um voto com token único e
     * marca o eleitor como votante.
     *
     * @return o voto salvo, ou vazio quando alguma condição de negócio falha
     */
    @Transactional
    public Optional<Voto> registrarVoto(Long candidatoId, String matricula) {
        // 1. Verifica se a eleição está aberta
        if (!isVotacaoLiberada()) {
            log.warn("Tentativa de voto com eleição fechada. Matrícula: {}", matricula);
            return Optional.empty();
        }

        // 2. Verifica se o usuário existe e ainda NÃO votou (proteção contra voto duplo)
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorMatricula(matricula);
        if (usuarioOpt.isEmpty()) {
            log.warn("Tentativa de voto com matrícula inexistente: {}", matricula);
            return Optional.empty();
        }
        
        Usuario usuario = usuarioOpt.get();
        if (usuario.isVotou()) {
            log.warn("TENTATIVA DE VOTO DUPLO bloqueada! Matrícula: {}", matricula);
            return Optional.empty();
        }

        if (!usuario.isAtivo()) {
            log.warn("Tentativa de voto com usuário inativo. Matrícula: {}", matricula);
            return Optional.empty();
        }

        // 3. Verifica se o candidato existe e está ativo
        Optional<Candidato> candidatoOpt = candidatoService.buscarPorId(candidatoId);
        if (candidatoOpt.isEmpty()) {
            log.warn("Tentativa de voto para candidato inexistente: {}", candidatoId);
            return Optional.empty();
        }

        Candidato candidato = candidatoOpt.get();
        if (!candidato.isAtivo()) {
            log.warn("Tentativa de voto para candidato inativo: {}", candidatoId);
            return Optional.empty();
        }

        // 4. Registra o voto
        Voto voto = new Voto();
        voto.setCandidatoId(candidatoId);
        voto.setToken(UUID.randomUUID().toString());

        Voto salvo = votoRepository.save(voto);

        // 5. Marca o usuário como votou
        usuarioService.marcarComoVotou(usuario.getId());

        log.info("Voto registrado com sucesso — Matrícula: {} | Candidato: {} ({})", 
                matricula, candidato.getNome(), candidato.getNumero());

        return Optional.of(salvo);
    }

    public long contarTotalVotos() {
        return votoRepository.countTotalVotos();
    }

    public List<Object[]> getResultados() {
        return votoRepository.countVotosPorCandidato();
    }

    public boolean podeVotar(String matricula) {
        return !usuarioService.buscarPorMatricula(matricula)
                .map(Usuario::isVotou)
                .orElse(true);
    }
}
