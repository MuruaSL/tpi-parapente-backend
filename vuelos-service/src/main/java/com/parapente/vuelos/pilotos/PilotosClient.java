package com.parapente.vuelos.pilotos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.parapente.vuelos.errores.DependenciaNoDisponibleException;

/**
 * Cliente HTTP de los endpoints internos de pilotos-service. Cualquier falla de red/5xx se
 * convierte en {@link DependenciaNoDisponibleException} (503); el 404 de disponibilidad es de negocio.
 */
public class PilotosClient {

    private static final Logger log = LoggerFactory.getLogger(PilotosClient.class);
    private static final String BASE = "/api/v1/internos";

    private final RestClient http;

    public PilotosClient(RestClient http) {
        this.http = http;
    }

    /** Busca un piloto certificado y libre. 404 -> {@link PilotoNoDisponibleException} con el motivo. */
    public PilotoDisponibleDTO buscarDisponible(String tipoVuelo, LocalDateTime fechaHora) {
        try {
            return http.get()
                    .uri(b -> b.path(BASE + "/pilotos/disponibles")
                            .queryParam("tipoVuelo", tipoVuelo)
                            .queryParam("fechaHora", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(fechaHora))
                            .build())
                    .retrieve().body(PilotoDisponibleDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new PilotoNoDisponibleException(motivoDe(e));
        } catch (RestClientException e) {
            throw caida("buscar piloto", e);
        }
    }

    /** Reserva la agenda del piloto para la reserva. Cualquier falla es 503 (el llamador compensa). */
    public void asignar(Long pilotoId, LocalDateTime fechaHora, Long reservaId) {
        try {
            http.post().uri(BASE + "/asignaciones")
                    .body(Map.of("pilotoId", pilotoId,
                            "fechaHora", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(fechaHora),
                            "reservaId", reservaId))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw caida("asignar piloto", e);
        }
    }

    /** Libera la agenda (idempotente en pilotos). */
    public void liberar(Long reservaId) {
        try {
            http.delete().uri(BASE + "/asignaciones/{id}", reservaId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw caida("liberar piloto", e);
        }
    }

    private String motivoDe(HttpClientErrorException e) {
        try {
            Map<?, ?> cuerpo = e.getResponseBodyAs(Map.class);
            if (cuerpo != null && cuerpo.get("error") != null) {
                return cuerpo.get("error").toString();
            }
        } catch (RuntimeException ignorada) {
            // cuerpo ilegible: se usa el motivo genérico
        }
        return "sin piloto disponible";
    }

    private DependenciaNoDisponibleException caida(String operacion, RestClientException e) {
        log.warn("Pilotos no disponible al {}: {}", operacion, e.getMessage());
        return new DependenciaNoDisponibleException("Servicio de pilotos no disponible", e);
    }
}
