package com.cipa.votacao.repository;

import com.cipa.votacao.entity.Candidato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandidatoRepository extends JpaRepository<Candidato, Long> {
    Optional<Candidato> findByNumero(Integer numero);
    Optional<Candidato> findByNumeroAndAtivoTrue(Integer numero);
    List<Candidato> findByAtivoTrue();
    boolean existsByNumero(Integer numero);
    
    @Query("SELECT c FROM Candidato c WHERE c.ativo = true ORDER BY c.numero")
    List<Candidato> findAllAtivosOrderByNumero();
}