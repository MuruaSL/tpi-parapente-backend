package com.parapente.vuelos.reservas;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parapente.vuelos.clima.ClimaClient;
import com.parapente.vuelos.clima.ClimaNoDisponibleException;
import com.parapente.vuelos.clima.Veredicto;
import com.parapente.vuelos.clima.VeredictoDTO;
import com.parapente.vuelos.errores.DependenciaNoDisponibleException;
import com.parapente.vuelos.errores.RecursoNoEncontradoException;
import com.parapente.vuelos.errores.ReglaNegocioException;
import com.parapente.vuelos.pilotos.PilotoDisponibleDTO;
import com.parapente.vuelos.pilotos.PilotoNoDisponibleException;
import com.parapente.vuelos.pilotos.PilotosClient;
import com.parapente.vuelos.tiposvuelo.TipoVuelo;
import com.parapente.vuelos.turnos.Turno;
import com.parapente.vuelos.turnos.TurnoRepository;

/**
 * Orquesta el proceso de reserva: reglas propias, piloto (pilotos-service) y clima (clima-service).
 *
 * <p>Los métodos que llaman a otros servicios NO son @Transactional a propósito: cada save() confirma
 * por su cuenta, así una reserva RECHAZADA queda guardada aunque después se lance la excepción 422
 * (con una transacción global, el rollback la borraría) y no se mantiene una conexión abierta
 * mientras se espera una respuesta HTTP.
 */
@Service
public class ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);

    private final ReservaRepository reservas;
    private final TurnoRepository turnos;
    private final PilotosClient pilotos;
    private final ClimaClient clima;
    private final Clock reloj;

    public ReservaService(ReservaRepository reservas, TurnoRepository turnos, PilotosClient pilotos,
            ClimaClient clima, Clock reloj) {
        this.reservas = reservas;
        this.turnos = turnos;
        this.pilotos = pilotos;
        this.clima = clima;
        this.reloj = reloj;
    }

    // ---------------------------------------------------------------- crear

    /**
     * Crea una reserva. Orden fijo: (1) reglas propias, (2) buscar piloto, (3) guardar PENDIENTE y
     * reservar su agenda, (4) clima. Devuelve la reserva (CONFIRMADA o CONFIRMACION_PENDIENTE) o
     * lanza {@link ReservaRechazadaException}.
     */
    public ReservaDTO crear(String clienteSub, CrearReservaDTO dto) {
        Turno turno = buscarTurno(dto.turnoId());
        Reserva reserva = new Reserva(turno, clienteSub, dto.pesoKg(), dto.edad(), LocalDateTime.now(reloj));

        // 1) R1, R2, R3, R7, R10
        Optional<String> falla = validarRequisitos(turno, dto.pesoKg(), dto.edad(), clienteSub, false);
        if (falla.isPresent()) {
            throw rechazar(reserva, falla.get());
        }

        // 2) Piloto. Si pilotos está caído lanza 503 y todavía no se guardó nada.
        PilotoDisponibleDTO piloto;
        try {
            piloto = pilotos.buscarDisponible(turno.getTipoVuelo().getCodigo(), turno.getFechaHora());
        } catch (PilotoNoDisponibleException e) {
            throw rechazar(reserva, e.getMessage());
        }

        // 3) Se guarda PENDIENTE (hace falta el id) y se reserva la agenda del piloto.
        reserva.asignarPiloto(piloto.pilotoId(), piloto.nombre());
        reservas.save(reserva);
        try {
            pilotos.asignar(piloto.pilotoId(), turno.getFechaHora(), reserva.getId());
        } catch (DependenciaNoDisponibleException e) {
            // Marcha atrás simple: sin asignación no debe quedar la reserva.
            reservas.delete(reserva);
            throw e;
        }

        // 4) Clima
        VeredictoDTO v;
        try {
            v = consultarClima(turno);
        } catch (ClimaNoDisponibleException e) {
            // Nunca se confirma sin clima: queda a la espera de la revalidación y el piloto sigue reservado.
            reserva.cambiarEstado(EstadoReserva.CONFIRMACION_PENDIENTE, "clima no disponible, a confirmar");
            return ReservaDTO.desde(reservas.save(reserva));
        }
        if (v.veredicto() == Veredicto.NO_APTO) {
            // Si pilotos-service no responde no se deja la reserva PENDIENTE: se rechaza igual por clima
            // (el piloto puede quedar reservado en su agenda; liberar es idempotente y se limpia luego).
            try {
                pilotos.liberar(reserva.getId());
            } catch (DependenciaNoDisponibleException e) {
                log.warn("No se pudo liberar al piloto de la reserva {} al rechazarla por clima", reserva.getId());
            }
            reserva.liberarPiloto();
            throw rechazar(reserva, motivoClima(v));
        }
        reserva.cambiarEstado(EstadoReserva.CONFIRMADA, motivoConfirmacion(v));
        return ReservaDTO.desde(reservas.save(reserva));
    }

    // -------------------------------------------------------- reprogramar

    /**
     * Mueve una reserva A_REPROGRAMAR a otro turno repitiendo los pasos 1 a 4. Si algo falla la
     * reserva sigue A_REPROGRAMAR (sin piloto) y se informa el error.
     */
    public ReservaDTO reprogramar(Long id, Long turnoId, String clienteSub) {
        Reserva reserva = propia(id, clienteSub);
        if (reserva.getEstado() != EstadoReserva.A_REPROGRAMAR) {
            throw new ReglaNegocioException("transición no permitida: sólo se reprograma desde A_REPROGRAMAR");
        }
        Turno nuevo = buscarTurno(turnoId);
        boolean mismoTurno = reserva.getTurno().getId().equals(nuevo.getId());

        // R3 y R7 no cuentan a la propia reserva (por eso el último parámetro).
        validarRequisitos(nuevo, reserva.getPesoKg(), reserva.getEdad(), clienteSub, mismoTurno)
                .ifPresent(motivo -> {
                    throw new ReglaNegocioException(motivo);
                });

        PilotoDisponibleDTO piloto;
        try {
            piloto = pilotos.buscarDisponible(nuevo.getTipoVuelo().getCodigo(), nuevo.getFechaHora());
        } catch (PilotoNoDisponibleException e) {
            throw new ReglaNegocioException(e.getMessage());
        }
        pilotos.asignar(piloto.pilotoId(), nuevo.getFechaHora(), reserva.getId());

        VeredictoDTO v;
        try {
            v = consultarClima(nuevo);
        } catch (ClimaNoDisponibleException e) {
            pilotos.liberar(reserva.getId());
            throw new DependenciaNoDisponibleException("Servicio de clima no disponible, reintente la reprogramación", e);
        }
        if (v.veredicto() == Veredicto.NO_APTO) {
            pilotos.liberar(reserva.getId());
            throw new ReglaNegocioException(motivoClima(v));
        }
        reserva.moverA(nuevo);
        reserva.asignarPiloto(piloto.pilotoId(), piloto.nombre());
        reserva.cambiarEstado(EstadoReserva.CONFIRMADA, motivoConfirmacion(v));
        return ReservaDTO.desde(reservas.save(reserva));
    }

    // ------------------------------------------------- cancelar / finalizar

    /** Cancela (dueño o ADMIN): libera al piloto y el cupo (CANCELADA deja de ser activa). */
    public ReservaDTO cancelar(Long id, String sub, boolean esAdmin) {
        Reserva reserva = visible(id, sub, esAdmin);
        if (!reserva.getEstado().puedePasarA(EstadoReserva.CANCELADA)) {
            // Se valida antes de liberar al piloto para no dejar la reserva a medias.
            throw new ReglaNegocioException("transición no permitida: " + reserva.getEstado() + " -> CANCELADA");
        }
        // Siempre se libera (es idempotente): si falla con 503 se propaga y el estado no cambia.
        pilotos.liberar(reserva.getId());
        reserva.liberarPiloto();
        reserva.cambiarEstado(EstadoReserva.CANCELADA, "cancelada");
        return ReservaDTO.desde(reservas.save(reserva));
    }

    /** Registra que el vuelo se realizó (sólo desde CONFIRMADA, lo valida R8). */
    @Transactional
    public ReservaDTO finalizar(Long id) {
        Reserva reserva = reservas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("reserva " + id + " no existe"));
        reserva.cambiarEstado(EstadoReserva.COMPLETADA, "vuelo realizado");
        return ReservaDTO.desde(reservas.save(reserva));
    }

    // ---------------------------------------------------------- revalidar

    /**
     * Vuelve a consultar el clima de las reservas CONFIRMADA y CONFIRMACION_PENDIENTE próximas,
     * de la más antigua a la más nueva (R9).
     */
    public ResumenRevalidacionDTO revalidar() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<Reserva> candidatas = reservas.findByEstadoInAndTurnoFechaHoraBetweenOrderByCreadaEnAsc(
                List.of(EstadoReserva.CONFIRMADA, EstadoReserva.CONFIRMACION_PENDIENTE), ahora,
                ahora.plusDays(ReglasReserva.VENTANA_DIAS)).stream()
                .sorted(Comparator.comparing(Reserva::getCreadaEn)) // R9 explícito, no depende sólo de la consulta
                .toList();

        int confirmadas = 0;
        int aReprogramar = 0;
        int sinCambios = 0;
        for (Reserva r : candidatas) {
            VeredictoDTO v;
            try {
                v = consultarClima(r.getTurno());
            } catch (ClimaNoDisponibleException e) {
                sinCambios++; // sin clima no se cambia nada
                continue;
            }
            if (v.veredicto() == Veredicto.NO_APTO) {
                try {
                    pilotos.liberar(r.getId());
                } catch (DependenciaNoDisponibleException e) {
                    log.warn("No se pudo liberar al piloto de la reserva {}; queda sin cambios", r.getId());
                    sinCambios++;
                    continue;
                }
                r.liberarPiloto();
                r.cambiarEstado(EstadoReserva.A_REPROGRAMAR, motivoClima(v));
                reservas.save(r);
                aReprogramar++;
            } else {
                String motivo = motivoConfirmacion(v);
                if (r.getEstado() != EstadoReserva.CONFIRMADA) {
                    r.cambiarEstado(EstadoReserva.CONFIRMADA, motivo);
                    reservas.save(r);
                } else if (!Objects.equals(motivo, r.getMotivo())) {
                    // CONFIRMADA que pasa de CONDICIONAL a APTO (o al revés): se actualiza sólo el motivo.
                    r.actualizarMotivo(motivo);
                    reservas.save(r);
                }
                confirmadas++;
            }
        }
        return new ResumenRevalidacionDTO(candidatas.size(), confirmadas, aReprogramar, sinCambios);
    }

    // ------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public ReservaDTO obtener(Long id, String sub, boolean esAdmin) {
        return ReservaDTO.desde(visible(id, sub, esAdmin));
    }

    @Transactional(readOnly = true)
    public List<ReservaDTO> listarDe(String sub) {
        return reservas.findByClienteSubOrderByCreadaEnDesc(sub).stream().map(ReservaDTO::desde).toList();
    }

    @Transactional(readOnly = true)
    public Page<ReservaDTO> listar(Pageable pageable) {
        return reservas.findAll(pageable).map(ReservaDTO::desde);
    }

    // -------------------------------------------------------------- privados

    private Turno buscarTurno(Long id) {
        return turnos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("turno " + id + " no existe"));
    }

    /** Reserva visible para el usuario: ADMIN ve todas, el resto sólo las propias; si no, 404 (no filtra existencia). */
    private Reserva visible(Long id, String sub, boolean esAdmin) {
        return reservas.findById(id).filter(r -> esAdmin || r.esDe(sub))
                .orElseThrow(() -> new RecursoNoEncontradoException("reserva " + id + " no existe"));
    }

    /** Sólo el dueño (la reprogramación no admite ADMIN). */
    private Reserva propia(Long id, String sub) {
        return visible(id, sub, false);
    }

    /**
     * Reúne los datos que necesitan las reglas puras. {@code excluyendoPropia} es true al reprogramar
     * dentro del mismo turno: la propia reserva (activa) no debe contar contra el cupo ni como duplicada.
     */
    private Optional<String> validarRequisitos(Turno turno, int pesoKg, int edad, String clienteSub,
            boolean excluyendoPropia) {
        TipoVuelo tipo = turno.getTipoVuelo();
        long activas = reservas.countByTurnoIdAndEstadoIn(turno.getId(), EstadoReserva.ACTIVAS)
                - (excluyendoPropia ? 1 : 0);
        boolean yaTiene = !excluyendoPropia && reservas.existsByTurnoIdAndClienteSubAndEstadoIn(turno.getId(),
                clienteSub, EstadoReserva.ACTIVAS);
        // ponytail: contar y guardar no es atómico; dos reservas simultáneas al último cupo podrían pasar.
        return ReglasReserva.evaluar(pesoKg, edad, tipo.getPesoMin(), tipo.getPesoMax(), tipo.getEdadMin(),
                activas, turno.getCupo(), yaTiene, turno.getFechaHora(), LocalDateTime.now(reloj));
    }

    private VeredictoDTO consultarClima(Turno turno) {
        TipoVuelo tipo = turno.getTipoVuelo();
        return clima.obtenerVeredicto(turno.getSitio().getLatitud(), turno.getSitio().getLongitud(),
                turno.getFechaHora(), tipo.getVientoMaxKmh(), tipo.getMargenVientoKmh());
    }

    /** Guarda la reserva como RECHAZADA (R8 lo permite desde PENDIENTE) y arma la excepción 422. */
    private ReservaRechazadaException rechazar(Reserva reserva, String motivo) {
        reserva.cambiarEstado(EstadoReserva.RECHAZADA, motivo);
        reservas.save(reserva);
        return new ReservaRechazadaException(ReservaDTO.desde(reserva));
    }

    private String motivoClima(VeredictoDTO v) {
        return "clima no apto" + (v.motivo() != null ? ": " + v.motivo() : "");
    }

    private String motivoConfirmacion(VeredictoDTO v) {
        return v.veredicto() == Veredicto.CONDICIONAL ? "viento cercano al límite" : null;
    }
}
