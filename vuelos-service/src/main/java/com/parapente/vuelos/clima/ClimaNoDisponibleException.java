package com.parapente.vuelos.clima;

/** clima-service (u Open-Meteo detrás) no respondió: nunca se confirma una reserva sin clima. */
public class ClimaNoDisponibleException extends RuntimeException {
    public ClimaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
