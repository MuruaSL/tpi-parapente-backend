package com.parapente.vuelos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/** Reglas puras R1, R2, R3, R7 y R10: caso normal, límite y rechazo. */
class ReglasReservaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 6, 1, 9, 0);

    @Test
    void r1PesoNormalLimitesYRechazo() {
        assertTrue(ReglasReserva.peso(70, 40, 110).isEmpty());
        assertTrue(ReglasReserva.peso(40, 40, 110).isEmpty()); // límite inferior incluido
        assertTrue(ReglasReserva.peso(110, 40, 110).isEmpty()); // límite superior incluido
        assertEquals(Optional.of("peso fuera de rango"), ReglasReserva.peso(39, 40, 110));
        assertEquals(Optional.of("peso fuera de rango"), ReglasReserva.peso(111, 40, 110));
    }

    @Test
    void r2EdadNormalLimiteYRechazo() {
        assertTrue(ReglasReserva.edad(30, 12).isEmpty());
        assertTrue(ReglasReserva.edad(12, 12).isEmpty());
        assertEquals(Optional.of("edad mínima no cumplida"), ReglasReserva.edad(11, 12));
    }

    @Test
    void r3CupoNormalLimiteYRechazo() {
        assertTrue(ReglasReserva.cupo(0, 10).isEmpty());
        assertTrue(ReglasReserva.cupo(9, 10).isEmpty()); // queda un lugar
        assertEquals(Optional.of("turno completo"), ReglasReserva.cupo(10, 10));
        assertEquals(Optional.of("turno completo"), ReglasReserva.cupo(1, 1));
    }

    @Test
    void r7DobleReserva() {
        assertTrue(ReglasReserva.noDuplicada(false).isEmpty());
        assertEquals(Optional.of("ya reservó este turno"), ReglasReserva.noDuplicada(true));
    }

    @Test
    void r10VentanaNormalLimitesYRechazo() {
        assertTrue(ReglasReserva.ventana(AHORA.plusDays(3), AHORA).isEmpty());
        assertTrue(ReglasReserva.ventana(AHORA.plusDays(16), AHORA).isEmpty()); // límite: exactamente 16 días
        assertTrue(ReglasReserva.ventana(AHORA.plusSeconds(1), AHORA).isPresent() == false);
        String motivo = "turno fuera de la ventana permitida";
        assertEquals(Optional.of(motivo), ReglasReserva.ventana(AHORA, AHORA)); // ahora mismo no es futuro
        assertEquals(Optional.of(motivo), ReglasReserva.ventana(AHORA.minusDays(1), AHORA));
        assertEquals(Optional.of(motivo), ReglasReserva.ventana(AHORA.plusDays(16).plusSeconds(1), AHORA));
    }

    @Test
    void evaluarDevuelveElPrimerRechazoEnOrden() {
        // peso y edad malos: gana R1
        assertEquals(Optional.of("peso fuera de rango"), ReglasReserva.evaluar(10, 5, 40, 110, 12, 0, 10,
                false, AHORA.plusDays(2), AHORA));
        assertTrue(ReglasReserva.evaluar(70, 30, 40, 110, 12, 0, 10, false, AHORA.plusDays(2), AHORA).isEmpty());
    }
}
