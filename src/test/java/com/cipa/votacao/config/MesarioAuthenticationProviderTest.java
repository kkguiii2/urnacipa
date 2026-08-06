package com.cipa.votacao.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.cipa.votacao.entity.Mesario;
import com.cipa.votacao.service.MesarioService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class MesarioAuthenticationProviderTest {

    @Mock private MesarioService mesarioService;
    private MesarioAuthenticationProvider provider;
    private Mesario mesario;

    @BeforeEach
    void setUp() {
        provider = new MesarioAuthenticationProvider(mesarioService);
        mesario = new Mesario(1L, "mesario1", "hash", true);
        when(mesarioService.buscarPorUsername("mesario1")).thenReturn(Optional.of(mesario));
    }

    @Test
    void autenticaComPapelExclusivoDeMesario() {
        when(mesarioService.validarSenha("segredo", "hash")).thenReturn(true);

        var autenticacao = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("mesario1", "segredo"));

        assertThat(autenticacao.isAuthenticated()).isTrue();
        assertThat(autenticacao.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_MESARIO");
        assertThat(autenticacao.getCredentials()).isNull();
    }

    @Test
    void recusaMesarioInativo() {
        mesario.setAtivo(false);

        assertThatThrownBy(() -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("mesario1", "segredo")))
                .isInstanceOf(DisabledException.class);
    }
}
