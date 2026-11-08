package com.parapente.clima.seguridad;

import java.io.IOException;
import java.time.Instant;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad stateless con JWT de Keycloak. El veredicto acepta cualquier rol porque
 * vuelos lo llama con el token del usuario (puede ser CLIENTE); el gateway es quien
 * limita /api/v1/clima/** a ADMIN.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filtro(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(new ConversorRoles());

        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health", "/actuator/health/**",
                                 "/api/v1/clima/v3/api-docs", "/api/v1/clima/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o
                .jwt(j -> j.jwtAuthenticationConverter(conversor))
                // Sin token o token inválido: 401 con el JSON de error común
                .authenticationEntryPoint((req, res, ex) -> json(res, 401, "no autenticado")))
            .exceptionHandling(e -> e
                .accessDeniedHandler((req, res, ex) -> json(res, 403, "acceso denegado")));
        return http.build();
    }

    /** Escribe el mismo JSON de error que el GlobalExceptionHandler (Spring Security responde antes que él). */
    private void json(HttpServletResponse res, int status, String error) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":" + status + ",\"error\":\"" + error + "\"}");
    }
}
