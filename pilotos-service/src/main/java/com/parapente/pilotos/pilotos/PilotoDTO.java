package com.parapente.pilotos.pilotos;

import java.util.List;

/** Vista de un piloto para la API de administración. */
public record PilotoDTO(Long id, String nombre, String keycloakSub, boolean activo, List<String> certificaciones) {
    static PilotoDTO de(Piloto p) {
        return new PilotoDTO(p.getId(), p.getNombre(), p.getKeycloakSub(), p.isActivo(),
                p.getCertificaciones().stream().sorted().toList());
    }
}
