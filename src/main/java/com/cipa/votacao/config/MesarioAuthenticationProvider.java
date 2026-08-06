package com.cipa.votacao.config;

import com.cipa.votacao.entity.Mesario;
import com.cipa.votacao.service.MesarioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MesarioAuthenticationProvider implements AuthenticationProvider {

    private final MesarioService mesarioService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        Mesario mesario = mesarioService.buscarPorUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        if (!mesario.isAtivo()) {
            throw new DisabledException("Mesário inativo");
        }
        if (!mesarioService.validarSenha(password, mesario.getPassword())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MESARIO")));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
