package com.parapente.pilotos.pilotos;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Administración de pilotos y certificaciones (sólo ADMIN, ver SeguridadConfig). */
@RestController
@RequestMapping("/api/v1/pilotos")
public class PilotoController {

    private final PilotoService service;

    public PilotoController(PilotoService service) { this.service = service; }

    @GetMapping
    public List<PilotoDTO> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public PilotoDTO obtener(@PathVariable Long id) { return service.obtener(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PilotoDTO crear(@Valid @RequestBody PilotoRequest r) { return service.crear(r); }

    @PutMapping("/{id}")
    public PilotoDTO modificar(@PathVariable Long id, @Valid @RequestBody PilotoRequest r) { return service.modificar(id, r); }

    @PostMapping("/{id}/certificaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public PilotoDTO agregarCertificacion(@PathVariable Long id, @Valid @RequestBody CertificacionRequest r) {
        return service.agregarCertificacion(id, r.tipoVuelo());
    }

    @DeleteMapping("/{id}/certificaciones/{tipoVuelo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarCertificacion(@PathVariable Long id, @PathVariable String tipoVuelo) {
        service.quitarCertificacion(id, tipoVuelo);
    }
}
