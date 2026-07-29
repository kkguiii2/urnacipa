package com.cipa.votacao;

import com.cipa.votacao.entity.Admin;
import com.cipa.votacao.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Cria a primeira conta administrativa somente quando uma senha inicial foi
 * fornecida por configuração externa.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        if (adminRepository.count() == 0) {
            if (!StringUtils.hasText(adminPassword)) {
                log.warn("Nenhum administrador foi criado: defina ADMIN_PASSWORD no primeiro acesso.");
                return;
            }

            Admin admin = new Admin();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setAtivo(true);
            adminRepository.save(admin);
            log.info("Administrador inicial criado com o usuário configurado.");
        }
    }
}
