package com.parapente.clima.errores;

/** Una regla de negocio no se cumple (ej.: fecha fuera de ventana). Se responde 422. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
