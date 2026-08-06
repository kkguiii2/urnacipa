package com.cipa.votacao.service;

import com.cipa.votacao.entity.Usuario;
import com.cipa.votacao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public Optional<Usuario> buscarPorMatricula(String matricula) {
        return usuarioRepository.findByMatricula(matricula);
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public boolean existePorMatricula(String matricula) {
        return usuarioRepository.existsByMatricula(matricula);
    }

    public Usuario salvar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public List<Usuario> listarAtivos() {
        return usuarioRepository.findAll().stream()
                .filter(Usuario::isAtivo)
                .toList();
    }

    @Transactional
    public void marcarComoVotou(Long usuarioId) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setVotou(true);
            usuarioRepository.save(usuario);
        }
    }

    public long contarVotos() {
        return usuarioRepository.countByVotouTrue();
    }

    public long contarTotalAtivos() {
        return usuarioRepository.countByAtivoTrue();
    }

    public void excluir(Long id) {
        usuarioRepository.deleteById(id);
    }

    @Transactional
    public void resetarIndicadorVoto() {
        usuarioRepository.resetarIndicadorVoto();
    }

    @Transactional
    public void importarUsuarios(List<Usuario> usuarios) {
        for (Usuario usuario : usuarios) {
            if (!usuarioRepository.existsByMatricula(usuario.getMatricula())) {
                usuarioRepository.save(usuario);
            }
        }
    }
}
