package com.parapente.vuelos.clima;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** ClimaClient contra un servidor simulado: ok, errores y timeout (todo error = clima no disponible). */
class ClimaClientTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 6, 3, 10, 0);

    MockRestServiceServer server;
    ClimaClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder b = RestClient.builder().baseUrl("http://clima");
        server = MockRestServiceServer.bindTo(b).build();
        cliente = new ClimaClient(b.build());
    }

    @Test
    void veredictoOk() {
        server.expect(method(HttpMethod.GET))
                .andExpect(queryParam("latitud", "-31.4241"))
                .andExpect(queryParam("longitud", "-64.4978"))
                .andExpect(queryParam("fechaHora", "2026-06-03T10:00:00"))
                .andExpect(queryParam("vientoMaxKmh", "25"))
                .andExpect(queryParam("margenKmh", "10"))
                .andRespond(withSuccess("{\"veredicto\":\"CONDICIONAL\",\"vientoKmh\":28.0,\"rafagasKmh\":33.5,"
                        + "\"probLluviaPct\":10,\"motivo\":\"cerca del límite\"}", MediaType.APPLICATION_JSON));
        VeredictoDTO v = cliente.obtenerVeredicto(-31.4241, -64.4978, FECHA, 25, 10);
        assertEquals(Veredicto.CONDICIONAL, v.veredicto());
        assertEquals(33.5, v.rafagasKmh());
        assertEquals("cerca del límite", v.motivo());
    }

    @Test
    void error503Y500SonClimaNoDisponible() {
        server.expect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThrows(ClimaNoDisponibleException.class, () -> cliente.obtenerVeredicto(0, 0, FECHA, 25, 10));
        server.reset();
        server.expect(method(HttpMethod.GET)).andRespond(withServerError());
        assertThrows(ClimaNoDisponibleException.class, () -> cliente.obtenerVeredicto(0, 0, FECHA, 25, 10));
    }

    @Test
    void error422TambienNoConfirma() {
        server.expect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThrows(ClimaNoDisponibleException.class, () -> cliente.obtenerVeredicto(0, 0, FECHA, 25, 10));
    }

    @Test
    void timeoutEsClimaNoDisponible() {
        server.expect(method(HttpMethod.GET)).andRespond(withException(new SocketTimeoutException("timeout")));
        assertThrows(ClimaNoDisponibleException.class, () -> cliente.obtenerVeredicto(0, 0, FECHA, 25, 10));
    }
}
