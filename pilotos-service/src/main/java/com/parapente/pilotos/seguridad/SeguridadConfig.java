package com.parapente.pilotos.seguridad;

import java.io.IOException;
import java.time.Instant;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Servicio stateless: cada request trae su JWT, por eso no hay sesión ni CSRF.
 * Las reglas van de lo más específico a lo más general (la primera que coincide gana).
 */
@Configuration
public class SeguridadConfig {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health", "/api/v1/pilotos/v3/api-docs/**").permitAll()
                // Internos: cualquier token válido. El gateway no los enruta y vuelve a vuelos con el token del usuario (puede ser CLIENTE).
                .requestMatchers("/api/v1/internos/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/pilotos/mi-agenda").hasAnyRole("PILOTO", "ADMIN")
                .requestMatchers("/api/v1/pilotos/**").hasRole("ADMIN")
                .anyRequest().denyAll())
            .oauth2ResourceServer(o -> o
                .jwt(j -> j.jwtAuthenticationConverter(new ConversorRoles()))
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
