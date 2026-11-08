package com.parapente.pilotos.pilotos;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de alta/modificación de piloto. "activo" ausente = true (alta) o sin cambio (PUT). */
public record PilotoRequest(@NotBlank String nombre, @NotBlank String keycloakSub, Boolean activo) {}
