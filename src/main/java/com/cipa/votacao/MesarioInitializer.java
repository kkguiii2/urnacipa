package com.cipa.votacao;

import com.cipa.votacao.entity.Mesario;
import com.cipa.votacao.repository.MesarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class MesarioInitializer implements CommandLineRunner {

    private final MesarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.mesario.username}")
    private String username;

    @Value("${app.mesario.password}")
    private String password;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        if (!StringUtils.hasText(password)) {
            log.warn("Nenhum mesário foi criado: defina MESARIO_PASSWORD no primeiro acesso.");
            return;
        }
        Mesario mesario = new Mesario();
        mesario.setUsername(username);
        mesario.setPassword(passwordEncoder.encode(password));
        mesario.setAtivo(true);
        repository.save(mesario);
        log.info("Conta inicial de mesário criada.");
    }
}
