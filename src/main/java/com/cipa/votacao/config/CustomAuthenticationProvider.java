package com.cipa.votacao.config;

import com.cipa.votacao.entity.Admin;
import com.cipa.votacao.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Autentica administradores persistidos, rejeitando contas ausentes, inativas
 * ou com senha incompatível, e concede a autoridade {@code ROLE_ADMIN}.
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final AdminService adminService;

    /**
     * Valida estado e senha da conta antes de produzir a autenticação.
     *
     * @param authentication credenciais submetidas pelo formulário
     * @return token autenticado com {@code ROLE_ADMIN}
     * @throws AuthenticationException quando as credenciais ou a conta são inválidas
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        Admin admin = adminService.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!admin.isAtivo()) {
            throw new DisabledException("Administrador inativo");
        }

        if (!adminService.validatePassword(password, admin.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
