package com.cipa.votacao.config;

import com.cipa.votacao.entity.Admin;
import com.cipa.votacao.service.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationProviderTest {

    @Mock
    private AdminService adminService;

    @Test
    void naoAutenticaAdministradorInativo() {
        Admin admin = new Admin(1L, "admin", "hash", false);
        when(adminService.findByUsername("admin")).thenReturn(Optional.of(admin));

        CustomAuthenticationProvider provider = new CustomAuthenticationProvider(adminService);

        assertThatThrownBy(() -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("admin", "senha")))
                .isInstanceOf(DisabledException.class)
                .hasMessage("Administrador inativo");
    }
}
