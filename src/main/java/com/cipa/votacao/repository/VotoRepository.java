package com.cipa.votacao.repository;

import com.cipa.votacao.entity.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VotoRepository extends JpaRepository<Voto, Long> {
    boolean existsByToken(String token);
    boolean existsByCandidatoId(Long candidatoId);
    
    @Query("SELECT v.candidatoId, COUNT(v) FROM Voto v WHERE v.eleicaoId = :eleicaoId "
            + "GROUP BY v.candidatoId ORDER BY COUNT(v) DESC")
    List<Object[]> countVotosPorCandidato(@Param("eleicaoId") Long eleicaoId);
    
    @Query("SELECT COUNT(v) FROM Voto v WHERE v.eleicaoId = :eleicaoId")
    long countTotalVotos(@Param("eleicaoId") Long eleicaoId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Voto v set v.eleicaoId = :eleicaoId where v.eleicaoId is null")
    int associarVotosLegados(@Param("eleicaoId") Long eleicaoId);
}
