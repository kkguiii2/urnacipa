package com.cipa.votacao.repository;

import com.cipa.votacao.entity.CabineVotacao;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CabineVotacaoRepository extends JpaRepository<CabineVotacao, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CabineVotacao c where c.id = :id")
    Optional<CabineVotacao> findByIdForUpdate(@Param("id") Long id);
}
