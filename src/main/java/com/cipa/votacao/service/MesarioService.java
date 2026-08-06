package com.cipa.votacao.service;

import com.cipa.votacao.entity.Mesario;
import com.cipa.votacao.repository.MesarioRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MesarioService {

    private final MesarioRepository mesarioRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<Mesario> buscarPorUsername(String username) {
        return mesarioRepository.findByUsername(username);
    }

    public boolean validarSenha(String senha, String hash) {
        return passwordEncoder.matches(senha, hash);
    }
}
