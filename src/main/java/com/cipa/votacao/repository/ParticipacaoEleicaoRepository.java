package com.cipa.votacao.repository;

import com.cipa.votacao.entity.ParticipacaoEleicao;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipacaoEleicaoRepository extends JpaRepository<ParticipacaoEleicao, Long> {

    @Modifying
    @Query(value = """
            insert into participacoes_eleicao (eleicao_id, usuario_id)
            values (:eleicaoId, :usuarioId)
            on conflict (eleicao_id, usuario_id) do nothing
            """, nativeQuery = true)
    int inserirSeAusente(
            @Param("eleicaoId") Long eleicaoId,
            @Param("usuarioId") Long usuarioId);

    Optional<ParticipacaoEleicao> findByEleicaoIdAndUsuarioId(Long eleicaoId, Long usuarioId);

    boolean existsByEleicaoIdAndUsuarioIdAndVotouEmIsNotNull(Long eleicaoId, Long usuarioId);

    long countByEleicaoIdAndVotouEmIsNotNull(Long eleicaoId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ParticipacaoEleicao p
               set p.votouEm = :agora
             where p.eleicaoId = :eleicaoId
               and p.usuarioId = :usuarioId
               and p.votouEm is null
            """)
    int marcarComoVotouSeDisponivel(
            @Param("eleicaoId") Long eleicaoId,
            @Param("usuarioId") Long usuarioId,
            @Param("agora") LocalDateTime agora);
}
