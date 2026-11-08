package com.parapente.vuelos.errores;

/** El recurso pedido no existe (o no le pertenece al usuario: se oculta como 404). */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
