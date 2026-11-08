package com.parapente.clima.pronostico;

/** Pronóstico de una hora puntual: viento y ráfagas en km/h, probabilidad de lluvia en %. */
public record PronosticoDTO(double vientoKmh, double rafagasKmh, double probLluviaPct) {
}
