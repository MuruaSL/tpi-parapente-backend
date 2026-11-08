package com.parapente.pilotos.asignaciones;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Ocupa la agenda de un piloto para una reserva (reserva_id es referencia lógica a vuelos_db). */
@Entity
@Table(name = "asignacion")
public class Asignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "piloto_id")
    private Long pilotoId;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    @Column(name = "reserva_id")
    private Long reservaId;

    protected Asignacion() {}

    public Asignacion(Long pilotoId, LocalDateTime fechaHora, Long reservaId) {
        this.pilotoId = pilotoId;
        this.fechaHora = fechaHora;
        this.reservaId = reservaId;
    }

    public Long getId() { return id; }
    public Long getPilotoId() { return pilotoId; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public Long getReservaId() { return reservaId; }
}
