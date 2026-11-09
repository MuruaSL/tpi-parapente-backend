package com.parapente.vuelos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI de este servicio. Swagger UI vive en el gateway y descarga este documento,
 * por eso acá se declara (1) el esquema bearer JWT, para que aparezca el candado Authorize, y
 * (2) el servidor "/", para que "Try it out" llame al gateway y no a la dirección interna de Docker.
 */
@Configuration
public class OpenApiConfig {

    private static final String KEYCLOAK = "http://localhost:8180/realms/parapente/protocol/openid-connect";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .servers(java.util.List.of(new Server().url("/")))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
                        // Login real contra Keycloak (authorization code + PKCE, cliente parapente-swagger)
                        .addSecuritySchemes("keycloak", new SecurityScheme().type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                                        .authorizationUrl(KEYCLOAK + "/auth")
                                        .tokenUrl(KEYCLOAK + "/token")
                                        .scopes(new Scopes().addString("openid", "OpenID"))))))
                .addSecurityItem(new SecurityRequirement().addList("keycloak"))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
