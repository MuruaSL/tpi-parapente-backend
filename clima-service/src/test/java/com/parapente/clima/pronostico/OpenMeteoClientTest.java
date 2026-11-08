package com.parapente.clima.pronostico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.parapente.clima.errores.ClimaNoDisponibleException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenMeteoClientTest {

    private static final String JSON = """
            {"hourly":{"time":["2030-01-01T09:00","2030-01-01T10:00","2030-01-01T11:00"],
             "wind_speed_10m":[5.0,12.5,20.0],
             "wind_gusts_10m":[8.0,18.0,30.0],
             "precipitation_probability":[0.0,15.0,70.0]}}""";

    private MockRestServiceServer servidor;
    private OpenMeteoClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://open-meteo.test");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new OpenMeteoClient(builder.build());
    }

    @Test
    void tomaLaHoraMasCercana() {
        servidor.expect(requestTo(org.hamcrest.Matchers.containsString("/v1/forecast?latitude=-31.4")))
                .andRespond(withSuccess(JSON, MediaType.APPLICATION_JSON));

        // 10:40 queda más cerca de las 11:00
        PronosticoDTO p = cliente.obtener(-31.4, -64.5, LocalDateTime.parse("2030-01-01T10:40"));

        assertEquals(new PronosticoDTO(20.0, 30.0, 70.0), p);
        servidor.verify();
    }

    @Test
    void error500DaClimaNoDisponible() {
        servidor.expect(requestTo(org.hamcrest.Matchers.containsString("/v1/forecast"))).andRespond(withServerError());
        assertThrows(ClimaNoDisponibleException.class,
                () -> cliente.obtener(0, 0, LocalDateTime.parse("2030-01-01T10:00")));
    }

    @Test
    void timeoutDaClimaNoDisponible() {
        servidor.expect(requestTo(org.hamcrest.Matchers.containsString("/v1/forecast")))
                .andRespond(withException(new SocketTimeoutException("lectura")));
        assertThrows(ClimaNoDisponibleException.class,
                () -> cliente.obtener(0, 0, LocalDateTime.parse("2030-01-01T10:00")));
    }

    @Test
    void respuestaSinDatosDaClimaNoDisponible() {
        servidor.expect(requestTo(org.hamcrest.Matchers.containsString("/v1/forecast")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThrows(ClimaNoDisponibleException.class,
                () -> cliente.obtener(0, 0, LocalDateTime.parse("2030-01-01T10:00")));
    }

    @Test
    void horaIlegibleDaClimaNoDisponible() {
        servidor.expect(requestTo(org.hamcrest.Matchers.containsString("/v1/forecast")))
                .andRespond(withSuccess("""
                        {"hourly":{"time":["basura"],"wind_speed_10m":[1.0],"wind_gusts_10m":[1.0],
                         "precipitation_probability":[1.0]}}""", MediaType.APPLICATION_JSON));
        assertThrows(ClimaNoDisponibleException.class,
                () -> cliente.obtener(0, 0, LocalDateTime.parse("2030-01-01T10:00")));
    }

    @Test
    void fechaFueraDelPronosticoDaClimaNoDisponible() {
        servidor.expect(requestTo(org.hamcrest.Matchers.containsString("/v1/forecast")))
                .andRespond(withSuccess(JSON, MediaType.APPLICATION_JSON));
        // Un día después: la hora más cercana (11:00 del 1/1) está a ~23 h
        assertThrows(ClimaNoDisponibleException.class,
                () -> cliente.obtener(0, 0, LocalDateTime.parse("2030-01-02T10:00")));
    }
}
