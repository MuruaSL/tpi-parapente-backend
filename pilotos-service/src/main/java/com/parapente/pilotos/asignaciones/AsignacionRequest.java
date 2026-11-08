package com.parapente.pilotos.asignaciones;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** Cuerpo de POST asignaciones. */
public record AsignacionRequest(@NotNull Long pilotoId, @NotNull LocalDateTime fechaHora, @NotNull Long reservaId) {}
