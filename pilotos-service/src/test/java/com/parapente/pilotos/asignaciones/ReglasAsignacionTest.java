package com.parapente.pilotos.asignaciones;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReglasAsignacionTest {

    private static final LocalDateTime DIEZ = LocalDateTime.of(2030, 1, 10, 10, 0);

    // R4
    @Test void certificado() { assertTrue(ReglasAsignacion.estaCertificado(Set.of("GLOBO", "PRUEBA_APTO"), "GLOBO")); }
    @Test void noCertificado() { assertFalse(ReglasAsignacion.estaCertificado(Set.of("GLOBO"), "PARAPENTE_TANDEM")); }
    @Test void sinCertificaciones() { assertFalse(ReglasAsignacion.estaCertificado(Set.of(), "GLOBO")); }

    // R5
    @Test void libreSinVuelos() { assertTrue(ReglasAsignacion.estaLibre(List.of(), DIEZ)); }

    @Test void rechazaMismaHora() {
        assertFalse(ReglasAsignacion.estaLibre(List.of(DIEZ.plusMinutes(30)), DIEZ));
    }

    @Test void otraHoraEsLibre() {
        assertTrue(ReglasAsignacion.estaLibre(List.of(DIEZ.plusHours(1)), DIEZ));
    }

    @Test void limiteTresVuelosTodaviaLibre() {
        assertTrue(ReglasAsignacion.estaLibre(
                List.of(DIEZ.plusHours(1), DIEZ.plusHours(2), DIEZ.plusHours(3)), DIEZ));
    }

    @Test void cuatroVuelosEnElDiaRechaza() {
        assertFalse(ReglasAsignacion.estaLibre(
                List.of(DIEZ.plusHours(1), DIEZ.plusHours(2), DIEZ.plusHours(3), DIEZ.plusHours(4)), DIEZ));
    }

    @Test void vuelosDeOtroDiaNoCuentan() {
        assertTrue(ReglasAsignacion.estaLibre(
                List.of(DIEZ.plusDays(1), DIEZ.plusDays(1).plusHours(1), DIEZ.plusDays(2), DIEZ.minusDays(1)), DIEZ));
    }
}
