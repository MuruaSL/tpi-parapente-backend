package com.parapente.vuelos.errores;

/** Se incumple una regla de negocio (R1 a R10). Se responde 422. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
