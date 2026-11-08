package com.parapente.vuelos.turnos;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Cuerpo de POST /turnos. */
public record CrearTurnoDTO(@NotNull Long sitioId, @NotNull Long tipoVueloId, @NotNull LocalDateTime fechaHora,
        @NotNull @Positive Integer cupo) {
}
