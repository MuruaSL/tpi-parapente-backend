package com.parapente.pilotos.asignaciones;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Set;

/** Reglas puras (sin Spring) de asignación de pilotos: R4 certificación y R5 disponibilidad. */
public final class ReglasAsignacion {

    public static final int MAX_VUELOS_POR_DIA = 4;

    private ReglasAsignacion() {}

    /** R4: el piloto debe tener la certificación del tipo de vuelo. */
    public static boolean estaCertificado(Set<String> certificaciones, String tipoVuelo) {
        return certificaciones.contains(tipoVuelo);
    }

    /**
     * R5: libre si no tiene otro vuelo a la misma hora y tiene menos de 4 ese día.
     * "Misma hora" compara la hora de reloj (10:00 y 10:30 chocan).
     */
    public static boolean estaLibre(Collection<LocalDateTime> ocupadas, LocalDateTime nueva) {
        LocalDateTime hora = nueva.truncatedTo(ChronoUnit.HOURS);
        boolean mismaHora = ocupadas.stream().anyMatch(o -> o.truncatedTo(ChronoUnit.HOURS).equals(hora));
        long delDia = ocupadas.stream().filter(o -> o.toLocalDate().equals(nueva.toLocalDate())).count();
        return !mismaHora && delDia < MAX_VUELOS_POR_DIA;
    }
}
