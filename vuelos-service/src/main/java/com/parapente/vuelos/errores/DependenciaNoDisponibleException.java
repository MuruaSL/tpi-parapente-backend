package com.parapente.vuelos.errores;

/** Otro servicio (pilotos) no responde o falló. Se responde 503. */
public class DependenciaNoDisponibleException extends RuntimeException {
    public DependenciaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public DependenciaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
