package com.cipa.votacao.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class CabineAuthenticationProviderTest {

    private CabineAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CabineAuthenticationProvider(new BCryptPasswordEncoder());
        ReflectionTestUtils.setField(provider, "username", "urna1");
        ReflectionTestUtils.setField(provider, "password", "senha-forte");
        provider.inicializarHash();
    }

    @Test
    void autenticaSomenteDispositivoConfigurado() {
        var autenticacao = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("urna1", "senha-forte"));

        assertThat(autenticacao.isAuthenticated()).isTrue();
        assertThat(autenticacao.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_CABINE");
        assertThat(autenticacao.getCredentials()).isNull();
    }

    @Test
    void recusaSenhaIncorreta() {
        assertThatThrownBy(() -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("urna1", "incorreta")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
