package com.parapente.clima.veredicto;

/** Respuesta del endpoint: veredicto (APTO, CONDICIONAL, NO_APTO) con los datos que lo explican. */
public record VeredictoDTO(String veredicto, double vientoKmh, double rafagasKmh,
                           double probLluviaPct, String motivo) {
}
