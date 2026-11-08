package com.parapente.vuelos.reservas;

import java.time.LocalDateTime;

import com.parapente.vuelos.errores.ReglaNegocioException;
import com.parapente.vuelos.turnos.Turno;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Reserva de un cliente (identificado por el `sub` de Keycloak) en un turno. Todo cambio de estado
 * pasa por {@link #cambiarEstado}, que aplica R8.
 */
@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "turno_id")
    private Turno turno;

    @Column(name = "cliente_sub", nullable = false)
    private String clienteSub;

    @Column(name = "peso_kg", nullable = false)
    private int pesoKg;

    @Column(nullable = false)
    private int edad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado;

    // Referencia lógica a pilotos_db (sin FK). El nombre se copia para no consultar pilotos al listar.
    @Column(name = "piloto_id")
    private Long pilotoId;

    @Column(name = "piloto_nombre")
    private String pilotoNombre;

    private String motivo;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    protected Reserva() {
        // requerido por JPA
    }

    /** Reserva nueva: siempre arranca PENDIENTE. */
    public Reserva(Turno turno, String clienteSub, int pesoKg, int edad, LocalDateTime creadaEn) {
        this.turno = turno;
        this.clienteSub = clienteSub;
        this.pesoKg = pesoKg;
        this.edad = edad;
        this.estado = EstadoReserva.PENDIENTE;
        this.creadaEn = creadaEn;
    }

    /** R8: sólo transiciones válidas; si no, 422. El motivo reemplaza al anterior. */
    public void cambiarEstado(EstadoReserva nuevo, String motivo) {
        if (!estado.puedePasarA(nuevo)) {
            throw new ReglaNegocioException("transición no permitida: " + estado + " -> " + nuevo);
        }
        this.estado = nuevo;
        this.motivo = motivo;
    }

    /** Cambia sólo el motivo (sin tocar el estado), p. ej. al revalidar una CONFIRMADA. */
    public void actualizarMotivo(String motivo) {
        this.motivo = motivo;
    }

    public void asignarPiloto(Long pilotoId, String nombre) {
        this.pilotoId = pilotoId;
        this.pilotoNombre = nombre;
    }

    public void liberarPiloto() {
        this.pilotoId = null;
        this.pilotoNombre = null;
    }

    public void moverA(Turno nuevo) {
        this.turno = nuevo;
    }

    public boolean esDe(String sub) {
        return clienteSub.equals(sub);
    }

    public Long getId() { return id; }
    public Turno getTurno() { return turno; }
    public String getClienteSub() { return clienteSub; }
    public int getPesoKg() { return pesoKg; }
    public int getEdad() { return edad; }
    public EstadoReserva getEstado() { return estado; }
    public Long getPilotoId() { return pilotoId; }
    public String getPilotoNombre() { return pilotoNombre; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
}
