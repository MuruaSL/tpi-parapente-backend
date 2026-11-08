package com.parapente.vuelos.reservas;

import com.parapente.vuelos.errores.ReglaNegocioException;

/**
 * La reserva se rechazó y quedó guardada como RECHAZADA. Lleva la reserva para que el 422
 * informe reservaId, estado y motivo.
 */
public class ReservaRechazadaException extends ReglaNegocioException {

    private final transient ReservaDTO reserva;

    public ReservaRechazadaException(ReservaDTO reserva) {
        super("Reserva rechazada: " + reserva.motivo());
        this.reserva = reserva;
    }

    public ReservaDTO getReserva() {
        return reserva;
    }
}
