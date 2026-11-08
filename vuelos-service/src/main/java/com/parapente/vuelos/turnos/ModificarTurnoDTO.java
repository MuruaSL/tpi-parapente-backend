package com.parapente.vuelos.turnos;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Cuerpo de PUT /turnos/{id}: sólo fecha y cupo son modificables. */
public record ModificarTurnoDTO(@NotNull LocalDateTime fechaHora, @NotNull @Positive Integer cupo) {
}
