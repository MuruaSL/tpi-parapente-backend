package com.parapente.vuelos.reservas;

/** Resultado de la revalidación masiva de clima. */
public record ResumenRevalidacionDTO(int revisadas, int confirmadas, int aReprogramar, int sinCambios) {
}
