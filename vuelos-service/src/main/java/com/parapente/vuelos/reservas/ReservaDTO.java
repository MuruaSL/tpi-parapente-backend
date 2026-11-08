package com.parapente.vuelos.reservas;

import java.time.LocalDateTime;

/** Respuesta pública de una reserva. */
public record ReservaDTO(Long id, Long turnoId, String tipoVuelo, LocalDateTime fechaHoraVuelo,
        EstadoReserva estado, Long pilotoId, String pilotoNombre, String motivo, int pesoKg, int edad,
        LocalDateTime creadaEn) {

    public static ReservaDTO desde(Reserva r) {
        return new ReservaDTO(r.getId(), r.getTurno().getId(), r.getTurno().getTipoVuelo().getCodigo(),
                r.getTurno().getFechaHora(), r.getEstado(), r.getPilotoId(), r.getPilotoNombre(), r.getMotivo(),
                r.getPesoKg(), r.getEdad(), r.getCreadaEn());
    }
}
