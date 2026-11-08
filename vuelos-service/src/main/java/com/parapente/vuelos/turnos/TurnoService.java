package com.parapente.vuelos.turnos;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parapente.vuelos.errores.RecursoNoEncontradoException;
import com.parapente.vuelos.reservas.EstadoReserva;
import com.parapente.vuelos.reservas.ReservaRepository;
import com.parapente.vuelos.tiposvuelo.TipoVuelo;
import com.parapente.vuelos.tiposvuelo.TipoVueloRepository;

/** Consulta y administración de turnos. */
@Service
public class TurnoService {

    private final TurnoRepository turnos;
    private final SitioRepository sitios;
    private final TipoVueloRepository tipos;
    private final ReservaRepository reservas;
    private final Clock reloj;

    public TurnoService(TurnoRepository turnos, SitioRepository sitios, TipoVueloRepository tipos,
            ReservaRepository reservas, Clock reloj) {
        this.turnos = turnos;
        this.sitios = sitios;
        this.tipos = tipos;
        this.reservas = reservas;
        this.reloj = reloj;
    }

    /** Turnos futuros con cupo restante. */
    @Transactional(readOnly = true)
    public List<TurnoDTO> listarDisponibles() {
        // ponytail: una consulta de conteo por turno (N+1); alcanza para pocos turnos, agrupar con GROUP BY si crecen.
        return turnos.findByFechaHoraAfterOrderByFechaHoraAsc(LocalDateTime.now(reloj)).stream()
                .map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public TurnoDTO obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public TurnoDTO crear(CrearTurnoDTO dto) {
        Sitio sitio = sitios.findById(dto.sitioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("sitio " + dto.sitioId() + " no existe"));
        TipoVuelo tipo = tipos.findById(dto.tipoVueloId()).orElseThrow(
                () -> new RecursoNoEncontradoException("tipo de vuelo " + dto.tipoVueloId() + " no existe"));
        return aDto(turnos.save(new Turno(null, sitio, tipo, dto.fechaHora(), dto.cupo())));
    }

    @Transactional
    public TurnoDTO modificar(Long id, ModificarTurnoDTO dto) {
        Turno t = buscar(id);
        t.modificar(dto.fechaHora(), dto.cupo());
        return aDto(t);
    }

    private Turno buscar(Long id) {
        return turnos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("turno " + id + " no existe"));
    }

    private TurnoDTO aDto(Turno t) {
        return TurnoDTO.desde(t, reservas.countByTurnoIdAndEstadoIn(t.getId(), EstadoReserva.ACTIVAS));
    }
}
