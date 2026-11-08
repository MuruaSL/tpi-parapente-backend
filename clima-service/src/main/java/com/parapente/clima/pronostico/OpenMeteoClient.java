package com.parapente.clima.pronostico;

import com.parapente.clima.errores.ClimaNoDisponibleException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente HTTP de Open-Meteo. Cualquier falla (red, timeout, 4xx/5xx, datos vacíos) se convierte en 503. */
@Component
public class OpenMeteoClient {

    private final RestClient restClient;

    public OpenMeteoClient(RestClient openMeteoRestClient) {
        this.restClient = openMeteoRestClient;
    }

    /** Devuelve el pronóstico de la hora más cercana a {@code fechaHora} (hora local del sitio, timezone=auto). */
    public PronosticoDTO obtener(double latitud, double longitud, LocalDateTime fechaHora) {
        OpenMeteoRespuestaDTO resp;
        try {
            resp = restClient.get()
                    .uri(u -> u.path("/v1/forecast")
                            .queryParam("latitude", latitud)
                            .queryParam("longitude", longitud)
                            .queryParam("hourly", "wind_speed_10m,wind_gusts_10m,precipitation_probability")
                            .queryParam("wind_speed_unit", "kmh")
                            .queryParam("forecast_days", 16)
                            .queryParam("timezone", "auto")
                            .build())
                    .retrieve()
                    .body(OpenMeteoRespuestaDTO.class);
        } catch (RestClientException e) {
            throw new ClimaNoDisponibleException("Servicio de pronóstico no disponible", e);
        }
        return horaMasCercana(resp, fechaHora);
    }

    /** Recorre las horas devueltas y elige la de menor distancia a la pedida. */
    private PronosticoDTO horaMasCercana(OpenMeteoRespuestaDTO resp, LocalDateTime fechaHora) {
        var h = resp == null ? null : resp.hourly();
        if (h == null || h.time() == null || h.time().isEmpty()
                || h.vientoKmh() == null || h.rafagasKmh() == null || h.probLluviaPct() == null) {
            throw new ClimaNoDisponibleException("Pronóstico incompleto", null);
        }
        int mejor = -1;
        long mejorDistancia = Long.MAX_VALUE;
        for (int i = 0; i < h.time().size(); i++) {
            long d;
            try {
                d = Math.abs(Duration.between(LocalDateTime.parse(h.time().get(i)), fechaHora).toSeconds());
            } catch (DateTimeParseException | NullPointerException e) {
                // Un "time" ilegible es una respuesta inválida del proveedor: 503, no 500
                throw new ClimaNoDisponibleException("Pronóstico con fecha inválida", e);
            }
            if (d < mejorDistancia) {
                mejorDistancia = d;
                mejor = i;
            }
        }
        // Si la hora más cercana está a más de 1 h, el pronóstico no cubre esa fecha (no usar un dato de otro día)
        if (mejorDistancia > 3600) {
            throw new ClimaNoDisponibleException("Pronóstico sin datos para esa fecha", null);
        }
        try {
            Double v = h.vientoKmh().get(mejor), r = h.rafagasKmh().get(mejor), p = h.probLluviaPct().get(mejor);
            if (v == null || r == null || p == null) {
                throw new ClimaNoDisponibleException("Pronóstico sin datos para esa hora", null);
            }
            return new PronosticoDTO(v, r, p);
        } catch (IndexOutOfBoundsException e) {
            throw new ClimaNoDisponibleException("Pronóstico incompleto", e);
        }
    }
}
