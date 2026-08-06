package com.cipa.votacao.config;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CabineAuthenticationProvider implements AuthenticationProvider {

    private final PasswordEncoder passwordEncoder;

    @Value("${app.cabine.username}")
    private String username;

    @Value("${app.cabine.password}")
    private String password;

    private String passwordHash;

    @PostConstruct
    void inicializarHash() {
        if (StringUtils.hasText(password)) {
            passwordHash = passwordEncoder.encode(password);
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String informedUsername = authentication.getName();
        String informedPassword = authentication.getCredentials().toString();
        if (passwordHash == null
                || !username.equals(informedUsername)
                || !passwordEncoder.matches(informedPassword, passwordHash)) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        return new UsernamePasswordAuthenticationToken(
                informedUsername,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CABINE")));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
