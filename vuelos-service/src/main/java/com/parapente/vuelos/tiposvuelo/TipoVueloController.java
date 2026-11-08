package com.parapente.vuelos.tiposvuelo;

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

/** Endpoints de tipos de vuelo (permisos en SecurityConfig: lectura autenticado, escritura ADMIN). */
@RestController
@RequestMapping("/api/v1/vuelos/tipos-vuelo")
public class TipoVueloController {

    private final TipoVueloService service;

    public TipoVueloController(TipoVueloService service) {
        this.service = service;
    }

    @GetMapping
    public List<TipoVueloDTO> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TipoVueloDTO crear(@Valid @RequestBody TipoVueloDTO dto) {
        return service.crear(dto);
    }

    @PutMapping("/{id}")
    public TipoVueloDTO modificar(@PathVariable Long id, @Valid @RequestBody TipoVueloDTO dto) {
        return service.modificar(id, dto);
    }
}
