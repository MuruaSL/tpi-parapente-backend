package com.parapente.vuelos.clima;

/** Respuesta de GET /api/v1/clima/veredicto. */
public record VeredictoDTO(Veredicto veredicto, Double vientoKmh, Double rafagasKmh, Double probLluviaPct,
        String motivo) {
}
