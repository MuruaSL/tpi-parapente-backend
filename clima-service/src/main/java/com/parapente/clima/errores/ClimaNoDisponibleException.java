package com.parapente.clima.errores;

/** Open-Meteo no respondió bien (caído, timeout, 5xx o datos incompletos). Se responde 503. */
public class ClimaNoDisponibleException extends RuntimeException {
    public ClimaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
