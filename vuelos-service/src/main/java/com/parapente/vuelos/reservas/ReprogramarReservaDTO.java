package com.parapente.vuelos.reservas;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de PATCH /reservas/{id}/reprogramacion. */
public record ReprogramarReservaDTO(@NotNull Long turnoId) {
}
