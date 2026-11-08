package com.parapente.pilotos.pilotos;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/** Piloto con las certificaciones (códigos de tipo de vuelo) que posee. */
@Entity
@Table(name = "piloto")
public class Piloto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(name = "keycloak_sub")
    private String keycloakSub;

    private boolean activo = true;

    // Eager: son pocas y siempre se necesitan juntas (open-in-view está apagado)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "certificacion", joinColumns = @JoinColumn(name = "piloto_id"))
    @Column(name = "tipo_vuelo_codigo")
    private Set<String> certificaciones = new HashSet<>();

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getKeycloakSub() { return keycloakSub; }
    public void setKeycloakSub(String keycloakSub) { this.keycloakSub = keycloakSub; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Set<String> getCertificaciones() { return certificaciones; }
}
