package com.parapente.clima.seguridad;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Mapea los roles de realm de Keycloak (realm_access.roles) a authorities ROLE_&lt;rol&gt;. */
public class ConversorRoles implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object realm = jwt.getClaim("realm_access");
        if (realm instanceof Map<?, ?> mapa && mapa.get("roles") instanceof Collection<?> roles) {
            return roles.stream()
                    .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
        }
        return List.of();
    }
}
