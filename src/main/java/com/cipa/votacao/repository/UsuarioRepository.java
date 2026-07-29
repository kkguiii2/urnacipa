package com.cipa.votacao.repository;

import com.cipa.votacao.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByMatricula(String matricula);
    boolean existsByMatricula(String matricula);
    long countByVotouTrue();
    long countByAtivoTrue();
    List<Usuario> findByAtivoTrue();
    List<Usuario> findByMatriculaIn(Collection<String> matriculas);
}
