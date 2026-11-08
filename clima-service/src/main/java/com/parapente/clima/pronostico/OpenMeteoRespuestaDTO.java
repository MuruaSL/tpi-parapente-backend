package com.parapente.clima.pronostico;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Parte de la respuesta de Open-Meteo que nos interesa (bloque "hourly"). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoRespuestaDTO(Hourly hourly) {

    /** Listas paralelas: el índice i de cada lista corresponde a la hora time[i] (hora local del sitio). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hourly(
            List<String> time,
            @JsonProperty("wind_speed_10m") List<Double> vientoKmh,
            @JsonProperty("wind_gusts_10m") List<Double> rafagasKmh,
            @JsonProperty("precipitation_probability") List<Double> probLluviaPct) {
    }
}
