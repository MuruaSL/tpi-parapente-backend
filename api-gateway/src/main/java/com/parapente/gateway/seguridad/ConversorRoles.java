package com.parapente.gateway.seguridad;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

/**
 * Convierte los roles de Keycloak (claim realm_access.roles) en authorities ROLE_*.
 * Versión reactiva: el gateway la necesita como Converter hacia Mono.
 */
public class ConversorRoles implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        return Mono.just(new JwtAuthenticationToken(jwt, extraerRoles(jwt)));
    }

    // Keycloak guarda los roles de realm en {"realm_access": {"roles": ["ADMIN", ...]}}
    private Collection<GrantedAuthority> extraerRoles(Jwt jwt) {
        Object acceso = jwt.getClaim("realm_access");
        if (acceso instanceof Map<?, ?> mapa && mapa.get("roles") instanceof Collection<?> roles) {
            return roles.stream()
                    .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
        }
        return List.of();
    }
}
