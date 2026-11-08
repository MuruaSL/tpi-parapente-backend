package com.parapente.vuelos.seguridad;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Seguridad stateless con JWT de Keycloak. Los permisos por rol van acá (tabla del contrato);
 * que una reserva sea del usuario se decide en ReservaService (ajena = 404).
 */
@Configuration
public class SecurityConfig {

    private static final String V = "/api/v1/vuelos";

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(V + "/v3/api-docs", V + "/v3/api-docs/**", V + "/swagger-ui.html",
                                V + "/swagger-ui/**").permitAll()
                        // Turnos y tipos de vuelo: leer cualquier autenticado, escribir sólo ADMIN
                        .requestMatchers(HttpMethod.GET, V + "/turnos", V + "/turnos/*", V + "/tipos-vuelo")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, V + "/turnos", V + "/tipos-vuelo").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, V + "/turnos/*", V + "/tipos-vuelo/*").hasRole("ADMIN")
                        // Reservas (el orden importa: /mias y /revalidacion antes que /{id})
                        .requestMatchers(HttpMethod.POST, V + "/reservas").hasAnyRole("CLIENTE", "ADMIN")
                        .requestMatchers(HttpMethod.POST, V + "/reservas/revalidacion").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, V + "/reservas/mias").hasAnyRole("CLIENTE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, V + "/reservas").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, V + "/reservas/*").hasAnyRole("CLIENTE", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, V + "/reservas/*/reprogramacion")
                        .hasRole("CLIENTE") // sólo el dueño; ADMIN no reprograma
                        .requestMatchers(HttpMethod.PATCH, V + "/reservas/*/cancelacion")
                        .hasAnyRole("CLIENTE", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, V + "/reservas/*/finalizacion").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(new ConversorRoles()))
                        .authenticationEntryPoint((req, res, e) -> json(res, 401, "No autenticado")))
                .exceptionHandling(e -> e
                        .accessDeniedHandler((req, res, ex) -> json(res, 403, "Acceso denegado")));
        return http.build();
    }

    /** Respuesta de error con el mismo formato JSON que el resto de la API. */
    private void json(HttpServletResponse res, int status, String error) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":" + status
                + ",\"error\":\"" + error + "\"}");
    }
}
