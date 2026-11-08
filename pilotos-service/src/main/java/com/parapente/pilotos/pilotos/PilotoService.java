package com.parapente.pilotos.pilotos;

import com.parapente.pilotos.errores.ReglaNegocioException;
import com.parapente.pilotos.errores.RecursoNoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administración de pilotos y sus certificaciones. */
@Service
@Transactional
public class PilotoService {

    private final PilotoRepository repo;

    public PilotoService(PilotoRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public List<PilotoDTO> listar() { return repo.findAll().stream().map(PilotoDTO::de).toList(); }

    @Transactional(readOnly = true)
    public PilotoDTO obtener(Long id) { return PilotoDTO.de(buscar(id)); }

    public PilotoDTO crear(PilotoRequest r) {
        if (repo.existsByKeycloakSub(r.keycloakSub())) throw new ReglaNegocioException("el keycloakSub ya está registrado");
        Piloto p = new Piloto();
        p.setNombre(r.nombre());
        p.setKeycloakSub(r.keycloakSub());
        p.setActivo(r.activo() == null || r.activo());
        return PilotoDTO.de(repo.save(p));
    }

    public PilotoDTO modificar(Long id, PilotoRequest r) {
        Piloto p = buscar(id);
        if (!r.keycloakSub().equals(p.getKeycloakSub()) && repo.existsByKeycloakSub(r.keycloakSub()))
            throw new ReglaNegocioException("el keycloakSub ya está registrado");
        p.setNombre(r.nombre());
        p.setKeycloakSub(r.keycloakSub());
        if (r.activo() != null) p.setActivo(r.activo()); // baja = activo:false
        return PilotoDTO.de(p);
    }

    public PilotoDTO agregarCertificacion(Long id, String tipoVuelo) {
        Piloto p = buscar(id);
        p.getCertificaciones().add(tipoVuelo); // Set: repetir no duplica
        return PilotoDTO.de(p);
    }

    public void quitarCertificacion(Long id, String tipoVuelo) {
        if (!buscar(id).getCertificaciones().remove(tipoVuelo))
            throw new RecursoNoEncontradoException("el piloto no tiene la certificación " + tipoVuelo);
    }

    private Piloto buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("piloto " + id + " no existe"));
    }
}
