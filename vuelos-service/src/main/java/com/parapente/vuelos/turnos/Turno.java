package com.parapente.vuelos.turnos;

import java.time.LocalDateTime;

import com.parapente.vuelos.tiposvuelo.TipoVuelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Turno: un tipo de vuelo en un sitio, a una fecha y hora, con cupo limitado. */
@Entity
@Table(name = "turnos")
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // EAGER: open-in-view está apagado y casi siempre se necesitan sitio y tipo junto al turno.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sitio_id")
    private Sitio sitio;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tipo_vuelo_id")
    private TipoVuelo tipoVuelo;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private int cupo;

    protected Turno() {
        // requerido por JPA
    }

    public Turno(Long id, Sitio sitio, TipoVuelo tipoVuelo, LocalDateTime fechaHora, int cupo) {
        this.id = id;
        this.sitio = sitio;
        this.tipoVuelo = tipoVuelo;
        this.fechaHora = fechaHora;
        this.cupo = cupo;
    }

    public Long getId() { return id; }
    public Sitio getSitio() { return sitio; }
    public TipoVuelo getTipoVuelo() { return tipoVuelo; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public int getCupo() { return cupo; }

    public void modificar(LocalDateTime fechaHora, int cupo) {
        this.fechaHora = fechaHora;
        this.cupo = cupo;
    }
}
