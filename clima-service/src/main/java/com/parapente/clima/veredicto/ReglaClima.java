package com.parapente.clima.veredicto;

import com.parapente.clima.pronostico.PronosticoDTO;

/** Regla R6 pura (sin Spring): decide el veredicto según viento, ráfagas y lluvia. */
public final class ReglaClima {

    /** A partir de este % de probabilidad de lluvia el vuelo es NO_APTO sin importar el viento. */
    public static final double LLUVIA_MAX_PCT = 60;

    private ReglaClima() {
    }

    public static VeredictoDTO evaluar(PronosticoDTO p, double vientoMaxKmh, double margenKmh) {
        // La lluvia manda: se evalúa primero.
        if (p.probLluviaPct() >= LLUVIA_MAX_PCT) {
            return crear("NO_APTO", p, "Probabilidad de lluvia alta (" + p.probLluviaPct() + "%)");
        }
        // Se usa el peor caso entre viento sostenido y ráfagas.
        double v = Math.max(p.vientoKmh(), p.rafagasKmh());
        if (v <= vientoMaxKmh) {
            return crear("APTO", p, "Condiciones dentro de los límites");
        }
        if (v <= vientoMaxKmh + margenKmh) {
            return crear("CONDICIONAL", p, "viento cercano al límite");
        }
        return crear("NO_APTO", p, "Viento excede el límite (" + v + " km/h)");
    }

    private static VeredictoDTO crear(String veredicto, PronosticoDTO p, String motivo) {
        return new VeredictoDTO(veredicto, p.vientoKmh(), p.rafagasKmh(), p.probLluviaPct(), motivo);
    }
}
