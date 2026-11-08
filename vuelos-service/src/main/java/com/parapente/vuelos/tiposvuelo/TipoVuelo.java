package com.parapente.vuelos.tiposvuelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tipo de vuelo con sus requisitos de pasajero (peso, edad) y sus límites de viento. */
@Entity
@Table(name = "tipos_vuelo")
public class TipoVuelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "peso_min", nullable = false)
    private int pesoMin;

    @Column(name = "peso_max", nullable = false)
    private int pesoMax;

    @Column(name = "edad_min", nullable = false)
    private int edadMin;

    @Column(name = "viento_max_kmh", nullable = false)
    private int vientoMaxKmh;

    @Column(name = "margen_viento_kmh", nullable = false)
    private int margenVientoKmh;

    protected TipoVuelo() {
        // requerido por JPA
    }

    public TipoVuelo(Long id, String codigo, String nombre, int pesoMin, int pesoMax, int edadMin,
            int vientoMaxKmh, int margenVientoKmh) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.pesoMin = pesoMin;
        this.pesoMax = pesoMax;
        this.edadMin = edadMin;
        this.vientoMaxKmh = vientoMaxKmh;
        this.margenVientoKmh = margenVientoKmh;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public int getPesoMin() { return pesoMin; }
    public int getPesoMax() { return pesoMax; }
    public int getEdadMin() { return edadMin; }
    public int getVientoMaxKmh() { return vientoMaxKmh; }
    public int getMargenVientoKmh() { return margenVientoKmh; }

    /** Actualiza los datos editables (el código identifica al tipo y no cambia). */
    public void actualizar(String nombre, int pesoMin, int pesoMax, int edadMin, int vientoMaxKmh,
            int margenVientoKmh) {
        this.nombre = nombre;
        this.pesoMin = pesoMin;
        this.pesoMax = pesoMax;
        this.edadMin = edadMin;
        this.vientoMaxKmh = vientoMaxKmh;
        this.margenVientoKmh = margenVientoKmh;
    }
}
