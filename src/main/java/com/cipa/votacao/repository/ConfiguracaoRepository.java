package com.cipa.votacao.repository;

import com.cipa.votacao.entity.ConfiguracaoEleicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ConfiguracaoRepository extends JpaRepository<ConfiguracaoEleicao, Long> {
    Optional<ConfiguracaoEleicao> findTopByOrderByIdDesc();
}