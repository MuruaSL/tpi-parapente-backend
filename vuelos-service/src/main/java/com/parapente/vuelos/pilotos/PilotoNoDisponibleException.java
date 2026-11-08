package com.parapente.vuelos.pilotos;

/** Pilotos respondió 404: "sin piloto certificado" o "sin piloto disponible". El mensaje es el motivo. */
public class PilotoNoDisponibleException extends RuntimeException {
    public PilotoNoDisponibleException(String motivo) {
        super(motivo);
    }
}
