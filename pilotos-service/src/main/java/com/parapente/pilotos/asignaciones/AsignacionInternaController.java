package com.parapente.pilotos.asignaciones;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Endpoints internos que usa vuelos-service; el gateway no los enruta. */
@RestController
@RequestMapping("/api/v1/internos")
public class AsignacionInternaController {

    private final AsignacionService service;

    public AsignacionInternaController(AsignacionService service) { this.service = service; }

    @GetMapping("/pilotos/disponibles")
    public PilotoDisponibleDTO disponibles(@RequestParam String tipoVuelo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora) {
        return service.buscarDisponible(tipoVuelo, fechaHora);
    }

    @PostMapping("/asignaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public void asignar(@Valid @RequestBody AsignacionRequest r) { service.asignar(r); }

    @DeleteMapping("/asignaciones/{reservaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void liberar(@PathVariable Long reservaId) { service.liberar(reservaId); }
}
