package com.cipa.votacao.service;

import com.cipa.votacao.entity.ParticipacaoEleicao;
import com.cipa.votacao.repository.ParticipacaoEleicaoRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParticipacaoEleicaoService {

    private final ParticipacaoEleicaoRepository repository;

    @Transactional
    public ParticipacaoEleicao garantirCadastro(Long eleicaoId, Long usuarioId) {
        repository.inserirSeAusente(eleicaoId, usuarioId);
        return repository.findByEleicaoIdAndUsuarioId(eleicaoId, usuarioId)
                .orElseThrow(() -> new IllegalStateException("Não foi possível preparar a participação eleitoral."));
    }

    public boolean jaVotou(Long eleicaoId, Long usuarioId) {
        return repository.existsByEleicaoIdAndUsuarioIdAndVotouEmIsNotNull(eleicaoId, usuarioId);
    }

    @Transactional
    public boolean marcarComoVotou(Long eleicaoId, Long usuarioId) {
        garantirCadastro(eleicaoId, usuarioId);
        return repository.marcarComoVotouSeDisponivel(eleicaoId, usuarioId, LocalDateTime.now()) == 1;
    }

    public long contarVotantes(Long eleicaoId) {
        return repository.countByEleicaoIdAndVotouEmIsNotNull(eleicaoId);
    }
}
