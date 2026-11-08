package com.parapente.pilotos.seguridad;

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
 * Este conversor hace esa traducción (CLIENTE, PILOTO, ADMIN).
 */
public class ConversorRoles implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = roles(jwt).stream()
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private List<String> roles(Jwt jwt) {
        Object realm = jwt.getClaim("realm_access");
        if (realm instanceof Map<?, ?> mapa && mapa.get("roles") instanceof Collection<?> lista) {
            return lista.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
