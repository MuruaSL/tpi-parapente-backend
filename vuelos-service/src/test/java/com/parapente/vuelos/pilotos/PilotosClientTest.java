package com.parapente.vuelos.pilotos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.parapente.vuelos.errores.DependenciaNoDisponibleException;
import com.parapente.vuelos.seguridad.ReenvioCabecerasInterceptor;

/** PilotosClient contra un servidor simulado: ok, 404, 500, timeout y reenvío de cabeceras. */
class PilotosClientTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 6, 3, 10, 0);

    MockRestServiceServer server;
    PilotosClient cliente;

    @BeforeEach
    void preparar() {
        RequestContextHolder.resetRequestAttributes();
        RestClient.Builder b = RestClient.builder().baseUrl("http://pilotos")
                .requestInterceptor(new ReenvioCabecerasInterceptor());
        server = MockRestServiceServer.bindTo(b).build();
        cliente = new PilotosClient(b.build());
    }

    @Test
    void disponibleOk() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://pilotos/api/v1/internos/pilotos/disponibles")))
                .andExpect(queryParam("tipoVuelo", "GLOBO"))
                .andExpect(queryParam("fechaHora", "2026-06-03T10:00:00"))
                .andRespond(withSuccess("{\"pilotoId\":2,\"nombre\":\"Piloto Dos\"}", MediaType.APPLICATION_JSON));
        assertEquals(new PilotoDisponibleDTO(2L, "Piloto Dos"), cliente.buscarDisponible("GLOBO", FECHA));
    }

    @Test
    void disponible404EsDeNegocioConElMotivoDelCuerpo() {
        server.expect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":404,\"error\":\"sin piloto certificado\"}"));
        PilotoNoDisponibleException e = assertThrows(PilotoNoDisponibleException.class,
                () -> cliente.buscarDisponible("SIN_PILOTO", FECHA));
        assertEquals("sin piloto certificado", e.getMessage());
    }

    @Test
    void disponible404SinCuerpoUsaMotivoGenerico() {
        server.expect(method(HttpMethod.GET)).andRespond(withResourceNotFound());
        assertEquals("sin piloto disponible", assertThrows(PilotoNoDisponibleException.class,
                () -> cliente.buscarDisponible("GLOBO", FECHA)).getMessage());
    }

    @Test
    void error500Es503() {
        server.expect(method(HttpMethod.GET)).andRespond(withServerError());
        assertThrows(DependenciaNoDisponibleException.class, () -> cliente.buscarDisponible("GLOBO", FECHA));
    }

    @Test
    void timeoutEs503() {
        server.expect(method(HttpMethod.GET)).andRespond(withException(new SocketTimeoutException("read timed out")));
        assertThrows(DependenciaNoDisponibleException.class, () -> cliente.buscarDisponible("GLOBO", FECHA));
    }

    @Test
    void asignarEnviaElCuerpoDelContrato() {
        server.expect(requestTo("http://pilotos/api/v1/internos/asignaciones"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.pilotoId").value(1))
                .andExpect(jsonPath("$.fechaHora").value("2026-06-03T10:00:00"))
                .andExpect(jsonPath("$.reservaId").value(10))
                .andRespond(withStatus(HttpStatus.CREATED));
        cliente.asignar(1L, FECHA, 10L);
        server.verify();
    }

    @Test
    void asignar422Es503() {
        server.expect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThrows(DependenciaNoDisponibleException.class, () -> cliente.asignar(1L, FECHA, 10L));
    }

    @Test
    void liberarOkYFalla() {
        server.expect(requestTo("http://pilotos/api/v1/internos/asignaciones/10"))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withNoContent());
        cliente.liberar(10L);
        server.verify();

        server.reset();
        server.expect(method(HttpMethod.DELETE)).andRespond(withException(new IOException("sin conexión")));
        assertThrows(DependenciaNoDisponibleException.class, () -> cliente.liberar(10L));
    }

    @Test
    void reenviaAuthorizationYCorrelationId() {
        MockHttpServletRequest entrante = new MockHttpServletRequest();
        entrante.addHeader("Authorization", "Bearer abc");
        entrante.addHeader("X-Correlation-Id", "cid-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(entrante));
        try {
            server.expect(method(HttpMethod.DELETE))
                    .andExpect(header("Authorization", "Bearer abc"))
                    .andExpect(header("X-Correlation-Id", "cid-1"))
                    .andRespond(withNoContent());
            cliente.liberar(10L);
            server.verify();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
