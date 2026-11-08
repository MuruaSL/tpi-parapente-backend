package com.parapente.vuelos.clima;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente HTTP de clima-service. Cualquier falla (503, timeout, 4xx) = clima no disponible. */
public class ClimaClient {

    private static final Logger log = LoggerFactory.getLogger(ClimaClient.class);

    private final RestClient http;

    public ClimaClient(RestClient http) {
        this.http = http;
    }

    public VeredictoDTO obtenerVeredicto(double latitud, double longitud, LocalDateTime fechaHora,
            int vientoMaxKmh, int margenKmh) {
        try {
            VeredictoDTO v = http.get()
                    .uri(b -> b.path("/api/v1/clima/veredicto")
                            .queryParam("latitud", latitud)
                            .queryParam("longitud", longitud)
                            .queryParam("fechaHora", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(fechaHora))
                            .queryParam("vientoMaxKmh", vientoMaxKmh)
                            .queryParam("margenKmh", margenKmh)
                            .build())
                    .retrieve().body(VeredictoDTO.class);
            if (v == null || v.veredicto() == null) {
                throw new ClimaNoDisponibleException("respuesta de clima vacía", null);
            }
            return v;
        } catch (RestClientException e) {
            log.warn("Clima no disponible: {}", e.getMessage());
            throw new ClimaNoDisponibleException("Servicio de clima no disponible", e);
        }
    }
}
