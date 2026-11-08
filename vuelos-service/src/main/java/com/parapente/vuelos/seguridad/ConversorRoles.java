package com.parapente.vuelos.seguridad;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Keycloak guarda los roles en realm_access.roles; Spring espera authorities "ROLE_X".
 * El nombre del principal queda como el `sub` del token (dueño de las reservas).
 */
public class ConversorRoles implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, roles(jwt), jwt.getSubject());
    }

    private Collection<GrantedAuthority> roles(Jwt jwt) {
        Object realm = jwt.getClaims().get("realm_access");
        if (realm instanceof Map<?, ?> mapa && mapa.get("roles") instanceof List<?> lista) {
            return lista.stream().<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
        }
        return List.of();
    }
}
