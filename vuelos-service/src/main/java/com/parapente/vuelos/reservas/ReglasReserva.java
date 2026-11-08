package com.parapente.vuelos.reservas;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Reglas de negocio puras (sin Spring ni base) para aceptar una reserva: R1, R2, R3, R7 y R10.
 * Cada método devuelve el motivo del rechazo, o vacío si la regla se cumple.
 */
public final class ReglasReserva {

    /** Días de pronóstico disponibles (R10). */
    public static final int VENTANA_DIAS = 16;

    private ReglasReserva() {
    }

    /** R1: peso dentro de [min, max] (límites incluidos). */
    public static Optional<String> peso(int pesoKg, int min, int max) {
        return pesoKg < min || pesoKg > max ? Optional.of("peso fuera de rango") : Optional.empty();
    }

    /** R2: edad mayor o igual a la mínima. */
    public static Optional<String> edad(int edad, int edadMin) {
        return edad < edadMin ? Optional.of("edad mínima no cumplida") : Optional.empty();
    }

    /** R3: las reservas activas deben ser menos que el cupo. */
    public static Optional<String> cupo(long activas, int cupo) {
        return activas >= cupo ? Optional.of("turno completo") : Optional.empty();
    }

    /** R7: el cliente no puede tener otra reserva activa en el mismo turno. */
    public static Optional<String> noDuplicada(boolean yaTieneActiva) {
        return yaTieneActiva ? Optional.of("ya reservó este turno") : Optional.empty();
    }

    /** R10: turno estrictamente futuro y a lo sumo a 16 días. */
    public static Optional<String> ventana(LocalDateTime turno, LocalDateTime ahora) {
        boolean dentro = turno.isAfter(ahora) && !turno.isAfter(ahora.plusDays(VENTANA_DIAS));
        return dentro ? Optional.empty() : Optional.of("turno fuera de la ventana permitida");
    }

    /** Aplica R1, R2, R3, R7 y R10 en ese orden y devuelve el primer motivo de rechazo. */
    public static Optional<String> evaluar(int pesoKg, int edad, int pesoMin, int pesoMax, int edadMin,
            long activasEnTurno, int cupo, boolean yaTieneActiva, LocalDateTime turno, LocalDateTime ahora) {
        return peso(pesoKg, pesoMin, pesoMax)
                .or(() -> edad(edad, edadMin))
                .or(() -> cupo(activasEnTurno, cupo))
                .or(() -> noDuplicada(yaTieneActiva))
                .or(() -> ventana(turno, ahora));
    }
}
