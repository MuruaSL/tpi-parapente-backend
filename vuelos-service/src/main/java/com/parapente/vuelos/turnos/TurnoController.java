package com.parapente.vuelos.turnos;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Endpoints de turnos (permisos en SecurityConfig: lectura autenticado, escritura ADMIN). */
@RestController
@RequestMapping("/api/v1/vuelos/turnos")
public class TurnoController {

    private final TurnoService service;

    public TurnoController(TurnoService service) {
        this.service = service;
    }

    @GetMapping
    public List<TurnoDTO> listar() {
        return service.listarDisponibles();
    }

    @GetMapping("/{id}")
    public TurnoDTO obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TurnoDTO crear(@Valid @RequestBody CrearTurnoDTO dto) {
        return service.crear(dto);
    }

    @PutMapping("/{id}")
    public TurnoDTO modificar(@PathVariable Long id, @Valid @RequestBody ModificarTurnoDTO dto) {
        return service.modificar(id, dto);
    }
}
