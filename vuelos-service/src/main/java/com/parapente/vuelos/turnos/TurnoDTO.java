package com.parapente.vuelos.turnos;

import java.time.LocalDateTime;

/** Turno con su cupo restante (cupo menos reservas activas). */
public record TurnoDTO(Long id, Long sitioId, String sitio, String tipoVuelo, LocalDateTime fechaHora,
        int cupo, long cupoRestante) {

    public static TurnoDTO desde(Turno t, long activas) {
        return new TurnoDTO(t.getId(), t.getSitio().getId(), t.getSitio().getNombre(),
                t.getTipoVuelo().getCodigo(), t.getFechaHora(), t.getCupo(), Math.max(0, t.getCupo() - activas));
    }
}
