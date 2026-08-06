package com.cipa.votacao.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationProvider adminAuthenticationProvider;
    private final MesarioAuthenticationProvider mesarioAuthenticationProvider;
    private final CabineAuthenticationProvider cabineAuthenticationProvider;

    @Bean
    @Order(1)
    public SecurityFilterChain mesarioSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/mesario/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/mesario/login").permitAll()
                .anyRequest().hasRole("MESARIO")
            )
            .formLogin(form -> form
                .loginPage("/mesario/login")
                .loginProcessingUrl("/mesario/login")
                .defaultSuccessUrl("/mesario/cabine", true)
                .failureUrl("/mesario/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/mesario/logout")
                .logoutSuccessUrl("/mesario/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
            )
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.migrateSession())
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .authenticationProvider(mesarioAuthenticationProvider);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain cabineSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/cabine/**", "/auth/**", "/votacao/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/cabine/login").permitAll()
                .anyRequest().hasRole("CABINE")
            )
            .formLogin(form -> form
                .loginPage("/cabine/login")
                .loginProcessingUrl("/cabine/login")
                .defaultSuccessUrl("/auth/login", true)
                .failureUrl("/cabine/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/cabine/logout")
                .logoutSuccessUrl("/cabine/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
            )
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.migrateSession())
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .authenticationProvider(cabineAuthenticationProvider);
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/admin/login",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/images/**",
                        "/uploads/**",
                        "/webjars/**",
                        "/error",
                        "/").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().denyAll()
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .defaultSuccessUrl("/admin/dashboard", true)
                .failureUrl("/admin/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.migrateSession())
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .authenticationProvider(adminAuthenticationProvider)
            .securityContext(context -> context.requireExplicitSave(false));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
