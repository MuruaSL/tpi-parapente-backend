package com.parapente.vuelos.reservas;

import static com.parapente.vuelos.reservas.EstadoReserva.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import com.parapente.vuelos.errores.ReglaNegocioException;

/** R8: tabla completa de transiciones. */
class EstadoReservaTest {

    @Test
    void transicionesValidasSegunContrato() {
        assertEquals(EnumSet.of(RECHAZADA, CONFIRMADA, CONFIRMACION_PENDIENTE), PENDIENTE.destinosValidos());
        assertEquals(EnumSet.of(CONFIRMADA, A_REPROGRAMAR, CANCELADA), CONFIRMACION_PENDIENTE.destinosValidos());
        assertEquals(EnumSet.of(A_REPROGRAMAR, CANCELADA, COMPLETADA), CONFIRMADA.destinosValidos());
        assertEquals(EnumSet.of(CONFIRMADA, CANCELADA), A_REPROGRAMAR.destinosValidos());
    }

    @Test
    void estadosFinalesNoTienenSalida() {
        for (EstadoReserva final_ : EnumSet.of(RECHAZADA, CANCELADA, COMPLETADA)) {
            assertTrue(final_.destinosValidos().isEmpty());
        }
    }

    @Test
    void transicionInvalidaEs422() {
        Reserva r = new Reserva(null, "sub", 70, 30, LocalDateTime.now());
        assertFalse(PENDIENTE.puedePasarA(COMPLETADA));
        assertThrows(ReglaNegocioException.class, () -> r.cambiarEstado(COMPLETADA, null));
        r.cambiarEstado(RECHAZADA, "x");
        assertThrows(ReglaNegocioException.class, () -> r.cambiarEstado(CONFIRMADA, null));
    }

    @Test
    void activasSonLasQueOcupanCupo() {
        assertEquals(java.util.List.of(PENDIENTE, CONFIRMADA, CONFIRMACION_PENDIENTE, A_REPROGRAMAR), ACTIVAS);
    }
}
