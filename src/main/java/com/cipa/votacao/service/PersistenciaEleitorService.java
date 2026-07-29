package com.cipa.votacao.service;

import com.cipa.votacao.entity.Usuario;
import com.cipa.votacao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Isola a inserção de cada eleitor importado em uma transação independente para
 * que uma falha de linha não marque as demais transações como rollback-only.
 */
@Service
@RequiredArgsConstructor
public class PersistenciaEleitorService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Persiste e força o INSERT de um eleitor novo na transação atual.
     *
     * @throws IllegalArgumentException se o objeto já possuir identificador
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void salvarNovo(Usuario usuario) {
        if (usuario.getId() != null) {
            throw new IllegalArgumentException("Um novo eleitor não pode possuir ID.");
        }
        usuarioRepository.saveAndFlush(usuario);
    }
}
