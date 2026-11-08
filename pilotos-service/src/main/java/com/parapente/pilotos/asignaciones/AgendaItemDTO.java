package com.parapente.pilotos.asignaciones;

import java.time.LocalDateTime;

/** Un vuelo en la agenda del piloto. El estado de la reserva vive en vuelos, no acá. */
public record AgendaItemDTO(Long reservaId, LocalDateTime fechaHora) {}
