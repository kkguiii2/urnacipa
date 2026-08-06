package com.cipa.votacao.repository;

import com.cipa.votacao.entity.Mesario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MesarioRepository extends JpaRepository<Mesario, Long> {
    Optional<Mesario> findByUsername(String username);
}
