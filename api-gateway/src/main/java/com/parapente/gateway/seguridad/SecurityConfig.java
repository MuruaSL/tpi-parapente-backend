package com.parapente.gateway.seguridad;

import java.time.Instant;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import java.nio.charset.StandardCharsets;
import reactor.core.publisher.Mono;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Seguridad del gateway. Spring Cloud Gateway es reactivo (WebFlux/Netty), por eso se usa
 * SecurityWebFilterChain y no el SecurityFilterChain de Spring MVC que usan los servicios.
 * Acá sólo hay autorización gruesa; la fina (dueño de la reserva, etc.) queda en cada servicio.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain cadenaSeguridad(ServerHttpSecurity http) {
        return http
                // API stateless con JWT en el header: no hay sesión ni cookies, CSRF no aplica
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        // Documentación y salud: públicas (los servicios publican su api-docs bajo su prefijo)
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/webjars/**",
                                "/v3/api-docs/**", "/api/v1/*/v3/api-docs/**",
                                "/actuator/health", "/actuator/health/**").permitAll()
                        // Clima sólo para ADMIN; vuelos lo llama directo (no por el gateway) con el token del usuario
                        .pathMatchers("/api/v1/clima/**").hasRole("ADMIN")
                        // El resto de la API: cualquier token válido; el rol exacto lo decide cada servicio
                        .pathMatchers("/api/v1/**").authenticated()
                        .anyExchange().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new ConversorRoles()))
                        // Sin token o token inválido: 401 con el JSON de error común
                        .authenticationEntryPoint((ex, e) -> json(ex, HttpStatus.UNAUTHORIZED, "no autenticado")))
                // Autenticado pero sin el rol: 403 con JSON
                .exceptionHandling(e -> e
                        .accessDeniedHandler((ex, d) -> json(ex, HttpStatus.FORBIDDEN, "acceso denegado")))
                .build();
    }

    /** Escribe el JSON de error {timestamp,status,error} de forma reactiva. */
    static Mono<Void> json(ServerWebExchange ex, HttpStatus status, String error) {
        ex.getResponse().setStatusCode(status);
        ex.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String cuerpo = "{\"timestamp\":\"" + Instant.now() + "\",\"status\":" + status.value()
                + ",\"error\":\"" + error + "\"}";
        DataBuffer buf = ex.getResponse().bufferFactory().wrap(cuerpo.getBytes(StandardCharsets.UTF_8));
        return ex.getResponse().writeWith(Mono.just(buf));
    }
}
