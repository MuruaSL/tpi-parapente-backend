package com.parapente.vuelos.reservas;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Endpoints de reservas. Los roles se controlan en SecurityConfig; acá sólo se extrae quién llama
 * (`sub` del token y si es ADMIN) para que el servicio decida la propiedad de la reserva.
 */
@RestController
@RequestMapping("/api/v1/vuelos/reservas")
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaDTO crear(@Valid @RequestBody CrearReservaDTO dto, Authentication auth) {
        return service.crear(auth.getName(), dto);
    }

    @GetMapping("/mias")
    public List<ReservaDTO> mias(Authentication auth) {
        return service.listarDe(auth.getName());
    }

    @GetMapping("/{id}")
    public ReservaDTO obtener(@PathVariable Long id, Authentication auth) {
        return service.obtener(id, auth.getName(), esAdmin(auth));
    }

    @GetMapping
    public Page<ReservaDTO> listar(Pageable pageable) {
        return service.listar(pageable);
    }

    @PatchMapping("/{id}/reprogramacion")
    public ReservaDTO reprogramar(@PathVariable Long id, @Valid @RequestBody ReprogramarReservaDTO dto,
            Authentication auth) {
        return service.reprogramar(id, dto.turnoId(), auth.getName());
    }

    @PatchMapping("/{id}/cancelacion")
    public ReservaDTO cancelar(@PathVariable Long id, Authentication auth) {
        return service.cancelar(id, auth.getName(), esAdmin(auth));
    }

    @PatchMapping("/{id}/finalizacion")
    public ReservaDTO finalizar(@PathVariable Long id) {
        return service.finalizar(id);
    }

    @PostMapping("/revalidacion")
    public ResumenRevalidacionDTO revalidar() {
        return service.revalidar();
    }

    private boolean esAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
