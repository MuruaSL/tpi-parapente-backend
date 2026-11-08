package com.parapente.pilotos.errores;

/** Se lanza cuando una regla de negocio impide la operación (responde 422). */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) { super(mensaje); }
}
