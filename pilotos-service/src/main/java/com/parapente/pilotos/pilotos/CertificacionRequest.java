package com.parapente.pilotos.pilotos;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo para habilitar a un piloto en un tipo de vuelo (por su código). */
public record CertificacionRequest(@NotBlank String tipoVuelo) {}
