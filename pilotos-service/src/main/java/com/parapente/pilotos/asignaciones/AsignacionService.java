package com.parapente.pilotos.asignaciones;

import com.parapente.pilotos.errores.RecursoNoEncontradoException;
import com.parapente.pilotos.errores.ReglaNegocioException;
import com.parapente.pilotos.pilotos.Piloto;
import com.parapente.pilotos.pilotos.PilotoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orquesta búsqueda, asignación y liberación de pilotos aplicando ReglasAsignacion. */
@Service
@Transactional
public class AsignacionService {

    private final PilotoRepository pilotos;
    private final AsignacionRepository asignaciones;

    public AsignacionService(PilotoRepository pilotos, AsignacionRepository asignaciones) {
        this.pilotos = pilotos;
        this.asignaciones = asignaciones;
    }

    /** Primer piloto activo (por id) certificado y libre. Dos 404 distintos para que vuelos explique el rechazo. */
    @Transactional(readOnly = true)
    public PilotoDisponibleDTO buscarDisponible(String tipoVuelo, LocalDateTime fechaHora) {
        List<Piloto> certificados = pilotos.findByActivoTrueOrderById().stream()
                .filter(p -> ReglasAsignacion.estaCertificado(p.getCertificaciones(), tipoVuelo))
                .toList();
        if (certificados.isEmpty()) throw new RecursoNoEncontradoException("sin piloto certificado");
        return certificados.stream()
                .filter(p -> ReglasAsignacion.estaLibre(fechas(p.getId()), fechaHora))
                .findFirst()
                .map(p -> new PilotoDisponibleDTO(p.getId(), p.getNombre()))
                .orElseThrow(() -> new RecursoNoEncontradoException("sin piloto disponible"));
    }

    /** Reserva la agenda. Revalida R5 porque entre "disponibles" y "asignar" otro pudo ocupar al piloto. */
    public void asignar(AsignacionRequest r) {
        pilotos.findById(r.pilotoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("piloto " + r.pilotoId() + " no existe"));
        // Si la reserva ya tenía asignación (reprogramación) se reemplaza, así no choca consigo misma
        asignaciones.deleteByReservaId(r.reservaId());
        asignaciones.flush();
        if (!ReglasAsignacion.estaLibre(fechas(r.pilotoId()), r.fechaHora()))
            throw new ReglaNegocioException("sin piloto disponible");
        asignaciones.save(new Asignacion(r.pilotoId(), r.fechaHora(), r.reservaId()));
    }

    /** Idempotente: si no hay asignación no pasa nada. */
    public void liberar(Long reservaId) { asignaciones.deleteByReservaId(reservaId); }

    /** Agenda del piloto identificado por el sub del JWT. */
    @Transactional(readOnly = true)
    public List<AgendaItemDTO> agendaDe(String sub) {
        Piloto p = pilotos.findByKeycloakSub(sub)
                .orElseThrow(() -> new RecursoNoEncontradoException("no hay un piloto asociado a este usuario"));
        return asignaciones.findByPilotoIdOrderByFechaHora(p.getId()).stream()
                .map(a -> new AgendaItemDTO(a.getReservaId(), a.getFechaHora()))
                .toList();
    }

    private List<LocalDateTime> fechas(Long pilotoId) {
        return asignaciones.findByPilotoIdOrderByFechaHora(pilotoId).stream().map(Asignacion::getFechaHora).toList();
    }
}
