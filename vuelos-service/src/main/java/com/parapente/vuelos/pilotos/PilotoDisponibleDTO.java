package com.parapente.vuelos.pilotos;

/** Respuesta de pilotos-service al buscar un piloto libre. */
public record PilotoDisponibleDTO(Long pilotoId, String nombre) {
}
