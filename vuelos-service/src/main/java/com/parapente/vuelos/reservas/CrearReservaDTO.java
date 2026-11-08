package com.parapente.vuelos.reservas;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Cuerpo de POST /reservas. */
public record CrearReservaDTO(@NotNull Long turnoId, @NotNull @Positive Integer pesoKg,
        @NotNull @Positive Integer edad) {
}
