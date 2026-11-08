package com.parapente.clima.veredicto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.parapente.clima.pronostico.PronosticoDTO;
import org.junit.jupiter.api.Test;

class ReglaClimaTest {

    private static String v(double viento, double rafagas, double lluvia, double max, double margen) {
        return ReglaClima.evaluar(new PronosticoDTO(viento, rafagas, lluvia), max, margen).veredicto();
    }

    @Test void apto() { assertEquals("APTO", v(10, 15, 10, 25, 10)); }
    @Test void limiteExactoEsApto() { assertEquals("APTO", v(25, 20, 0, 25, 10)); }
    @Test void condicionalPorViento() { assertEquals("CONDICIONAL", v(28, 20, 0, 25, 10)); }
    @Test void condicionalPorRafagas() { assertEquals("CONDICIONAL", v(10, 30, 0, 25, 10)); }
    @Test void limiteExactoDelMargenEsCondicional() { assertEquals("CONDICIONAL", v(35, 10, 0, 25, 10)); }
    @Test void noAptoPorViento() { assertEquals("NO_APTO", v(36, 10, 0, 25, 10)); }
    @Test void noAptoPorRafagas() { assertEquals("NO_APTO", v(10, 40, 0, 25, 10)); }
    @Test void lluvia59NoCambiaVeredicto() { assertEquals("APTO", v(10, 10, 59, 25, 10)); }
    @Test void lluvia60EsNoApto() { assertEquals("NO_APTO", v(0, 0, 60, 25, 10)); }
    @Test void margenCeroSinTolerancia() { assertEquals("NO_APTO", v(1, 0, 0, 0, 0)); }
}
