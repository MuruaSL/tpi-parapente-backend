package com.parapente.pilotos.agenda;

import com.parapente.pilotos.asignaciones.AgendaItemDTO;
import com.parapente.pilotos.asignaciones.AsignacionService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Agenda del piloto autenticado: se identifica por el sub del JWT, nunca por un parámetro. */
@RestController
@RequestMapping("/api/v1/pilotos/mi-agenda")
public class AgendaController {

    private final AsignacionService service;

    public AgendaController(AsignacionService service) { this.service = service; }

    @GetMapping
    public List<AgendaItemDTO> miAgenda(@AuthenticationPrincipal Jwt jwt) {
        return service.agendaDe(jwt.getSubject());
    }
}
