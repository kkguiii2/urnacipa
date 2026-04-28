package com.cipa.votacao.repository;

import com.cipa.votacao.entity.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VotoRepository extends JpaRepository<Voto, Long> {
    boolean existsByToken(String token);
    
    @Query("SELECT v.candidatoId, COUNT(v) FROM Voto v GROUP BY v.candidatoId ORDER BY COUNT(v) DESC")
    List<Object[]> countVotosPorCandidato();
    
    @Query("SELECT COUNT(v) FROM Voto v")
    long countTotalVotos();
}