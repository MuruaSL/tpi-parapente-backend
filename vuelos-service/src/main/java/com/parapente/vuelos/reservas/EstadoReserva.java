package com.parapente.vuelos.reservas;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Estados de una reserva y sus transiciones válidas (R8). RECHAZADA, CANCELADA y COMPLETADA son finales.
 */
public enum EstadoReserva {
    PENDIENTE, CONFIRMADA, CONFIRMACION_PENDIENTE, A_REPROGRAMAR, RECHAZADA, CANCELADA, COMPLETADA;

    /** Estados que ocupan cupo y bloquean una nueva reserva del mismo cliente (R3 y R7). */
    public static final List<EstadoReserva> ACTIVAS =
            List.of(PENDIENTE, CONFIRMADA, CONFIRMACION_PENDIENTE, A_REPROGRAMAR);

    /** Estados a los que se puede pasar desde este. */
    public Set<EstadoReserva> destinosValidos() {
        return switch (this) {
            case PENDIENTE -> EnumSet.of(RECHAZADA, CONFIRMADA, CONFIRMACION_PENDIENTE);
            case CONFIRMACION_PENDIENTE -> EnumSet.of(CONFIRMADA, A_REPROGRAMAR, CANCELADA);
            case CONFIRMADA -> EnumSet.of(A_REPROGRAMAR, CANCELADA, COMPLETADA);
            case A_REPROGRAMAR -> EnumSet.of(CONFIRMADA, CANCELADA);
            case RECHAZADA, CANCELADA, COMPLETADA -> EnumSet.noneOf(EstadoReserva.class);
        };
    }

    public boolean puedePasarA(EstadoReserva destino) {
        return destinosValidos().contains(destino);
    }
}
