package com.parapente.gateway.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI agregada: la lista de servicios está en application.yaml (springdoc.swagger-ui.urls).
 * Acá se declara el esquema bearer JWT para que aparezca el botón Authorize (pegar el token de
 * Keycloak) y el flujo con PKCE usa el cliente parapente-swagger configurado en application.yaml.
 */
@Configuration
public class SwaggerConfig {

    private static final String KEYCLOAK = "http://localhost:8180/realms/parapente/protocol/openid-connect";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("TPI Parapente - API Gateway").version("v1"))
                .components(new Components()
                        // Alternativa manual: pegar el token de Keycloak
                        .addSecuritySchemes("bearer",
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
