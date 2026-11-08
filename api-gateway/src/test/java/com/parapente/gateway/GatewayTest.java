package com.parapente.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

import com.parapente.gateway.correlacion.CorrelationIdFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.context.ApplicationContext;

/** Tests sin red ni Docker: el contexto no abre sockets ni consulta Keycloak (el JWKS es perezoso). */
@SpringBootTest
@TestPropertySource(properties = {"VUELOS_URL=http://vuelos.test", "PILOTOS_URL=http://pilotos.test",
        "CLIMA_URL=http://clima.test"})
class GatewayTest {

    @Autowired ApplicationContext ctx;
    @Autowired RouteLocator rutas;

    private WebTestClient cliente() {
        return WebTestClient.bindToApplicationContext(ctx).apply(
                org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity())
                .configureClient().build();
    }

    @Test
    void rutasDelContrato() {
        List<Route> lista = rutas.getRoutes().collectList().block();
        assertThat(lista).extracting(Route::getId).containsExactlyInAnyOrder("vuelos", "pilotos", "clima");
        assertThat(lista).extracting(r -> r.getUri().toString())
                .containsExactlyInAnyOrder("http://vuelos.test:80", "http://pilotos.test:80", "http://clima.test:80");
    }

    @Test
    void sinTokenEs401() {
        cliente().get().uri("/api/v1/vuelos/turnos").exchange().expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.status").isEqualTo(401).jsonPath("$.error").exists()
                .jsonPath("$.timestamp").exists();
    }

    @Test
    void climaConRolClienteEs403() {
        cliente().mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE")))
                .get().uri("/api/v1/clima/veredicto").exchange().expectStatus().isForbidden()
                .expectBody().jsonPath("$.status").isEqualTo(403).jsonPath("$.error").exists();
    }

    @Test
    void swaggerYApiDocsSonPublicos() {
        // Sin token no debe dar 401/403 (el contenido puede ser 200 o 404 según el recurso)
        for (String ruta : List.of("/swagger-ui.html", "/actuator/health")) {
            cliente().get().uri(ruta).exchange().expectStatus()
                    .value(s -> assertThat(s).isNotIn(401, 403));
        }
        cliente().get().uri("/swagger-ui.html").exchange().expectStatus().is3xxRedirection();
    }

    @Test
    void correlationIdSeGeneraYSePropaga() {
        var filtro = new CorrelationIdFilter();
        // Sin header: lo genera, lo manda al servicio y lo devuelve en la respuesta
        var ex = org.springframework.mock.web.server.MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/x"));
        var reenviado = new java.util.concurrent.atomic.AtomicReference<String>();
        filtro.filter(ex, e -> { reenviado.set(e.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER));
            return reactor.core.publisher.Mono.empty(); }).block();
        assertThat(reenviado.get()).isNotBlank();
        assertThat(ex.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER)).isEqualTo(reenviado.get());
        // Con header: lo respeta
        var ex2 = org.springframework.mock.web.server.MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/x")
                        .header(CorrelationIdFilter.HEADER, "abc"));
        filtro.filter(ex2, e -> reactor.core.publisher.Mono.empty()).block();
        assertThat(ex2.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER)).isEqualTo("abc");
    }

    @Test
    void correlationIdInvalidoSeReemplaza() {
        var filtro = new CorrelationIdFilter();
        var ex = org.springframework.mock.web.server.MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/x")
                        .header(CorrelationIdFilter.HEADER, "mal\" id con espacios"));
        filtro.filter(ex, e -> reactor.core.publisher.Mono.empty()).block();
        assertThat(ex.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER))
                .matches("^[A-Za-z0-9-]{1,64}$").isNotEqualTo("mal\" id con espacios");
    }

    @Test
    void swaggerDeclaraOauth2YBearer(@Autowired io.swagger.v3.oas.models.OpenAPI api) {
        var esquemas = api.getComponents().getSecuritySchemes();
        assertThat(esquemas).containsKeys("bearer", "keycloak");
        assertThat(esquemas.get("keycloak").getFlows().getAuthorizationCode().getTokenUrl())
                .isEqualTo("http://localhost:8180/realms/parapente/protocol/openid-connect/token");
    }

    @Test
    void swaggerOauthConfigEfectiva(@Autowired org.springframework.core.env.Environment env) {
        assertThat(env.getProperty("springdoc.swagger-ui.oauth.client-id")).isEqualTo("parapente-swagger");
        assertThat(env.getProperty("springdoc.swagger-ui.oauth.use-pkce-with-authorization-code-grant")).isEqualTo("true");
    }
}
