package com.parapente.vuelos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
import com.parapente.vuelos.turnos.Sitio;
import com.parapente.vuelos.turnos.Turno;
import com.parapente.vuelos.turnos.TurnoRepository;

/** Orquestación de reservas con todas las dependencias simuladas (sin base ni red). */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 6, 1, 9, 0);
    private static final LocalDateTime FECHA_TURNO = AHORA.plusDays(2).withHour(10);

    @Mock ReservaRepository reservas;
    @Mock TurnoRepository turnos;
    @Mock PilotosClient pilotos;
    @Mock ClimaClient clima;

    ReservaService service;
    Turno turno;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new ReservaService(reservas, turnos, pilotos, clima, reloj);
        turno = turno(3L, FECHA_TURNO);
        lenient().when(turnos.findById(3L)).thenReturn(Optional.of(turno));
        // save() simula la base: asigna el id 10 a las reservas nuevas
        lenient().when(reservas.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            if (r.getId() == null) {
                ReflectionTestUtils.setField(r, "id", 10L);
            }
            return r;
        });
        lenient().when(pilotos.buscarDisponible("PRUEBA_APTO", FECHA_TURNO))
                .thenReturn(new PilotoDisponibleDTO(1L, "Piloto Uno"));
    }

    private Turno turno(Long id, LocalDateTime fecha) {
        TipoVuelo tipo = new TipoVuelo(3L, "PRUEBA_APTO", "Prueba", 40, 110, 12, 500, 0);
        return new Turno(id, new Sitio(1L, "Villa Carlos Paz", -31.4241, -64.4978), tipo, fecha, 10);
    }

    private VeredictoDTO veredicto(Veredicto v) {
        return new VeredictoDTO(v, 10.0, 15.0, 5.0, "motivo " + v);
    }

    private void climaDevuelve(Veredicto v) {
        when(clima.obtenerVeredicto(anyDouble(), anyDouble(), any(), anyInt(), anyInt())).thenReturn(veredicto(v));
    }

    private CrearReservaDTO pedido() {
        return new CrearReservaDTO(3L, 70, 30);
    }

    // ------------------------------------------------------------ crear

    @Test
    void aptoConfirma() {
        climaDevuelve(Veredicto.APTO);
        ReservaDTO r = service.crear("cli", pedido());
        assertEquals(EstadoReserva.CONFIRMADA, r.estado());
        assertEquals(1L, r.pilotoId());
        assertEquals("Piloto Uno", r.pilotoNombre());
        assertNull(r.motivo());
        verify(pilotos).asignar(1L, FECHA_TURNO, 10L);
    }

    @Test
    void condicionalConfirmaConAviso() {
        climaDevuelve(Veredicto.CONDICIONAL);
        ReservaDTO r = service.crear("cli", pedido());
        assertEquals(EstadoReserva.CONFIRMADA, r.estado());
        assertEquals("viento cercano al límite", r.motivo());
    }

    @Test
    void noAptoLiberaPilotoYRechaza() {
        climaDevuelve(Veredicto.NO_APTO);
        ReservaRechazadaException e = assertThrows(ReservaRechazadaException.class,
                () -> service.crear("cli", pedido()));
        assertEquals(EstadoReserva.RECHAZADA, e.getReserva().estado());
        assertEquals(10L, e.getReserva().id());
        assertEquals("clima no apto: motivo NO_APTO", e.getReserva().motivo());
        assertNull(e.getReserva().pilotoId());
        verify(pilotos).liberar(10L);
    }

    @Test
    void climaCaidoDejaConfirmacionPendienteConPilotoReservado() {
        when(clima.obtenerVeredicto(anyDouble(), anyDouble(), any(), anyInt(), anyInt()))
                .thenThrow(new ClimaNoDisponibleException("caído", null));
        ReservaDTO r = service.crear("cli", pedido());
        assertEquals(EstadoReserva.CONFIRMACION_PENDIENTE, r.estado());
        assertEquals(1L, r.pilotoId());
        verify(pilotos, never()).liberar(anyLong());
    }

    @Test
    void sinPilotoRechazaYNoReservaAgenda() {
        when(pilotos.buscarDisponible("PRUEBA_APTO", FECHA_TURNO))
                .thenThrow(new PilotoNoDisponibleException("sin piloto certificado"));
        ReservaRechazadaException e = assertThrows(ReservaRechazadaException.class,
                () -> service.crear("cli", pedido()));
        assertEquals("sin piloto certificado", e.getReserva().motivo());
        assertEquals(EstadoReserva.RECHAZADA, e.getReserva().estado());
        verify(pilotos, never()).asignar(any(), any(), any());
        verify(clima, never()).obtenerVeredicto(anyDouble(), anyDouble(), any(), anyInt(), anyInt());
    }

    @Test
    void pilotosCaidoDa503SinGuardarNada() {
        when(pilotos.buscarDisponible("PRUEBA_APTO", FECHA_TURNO))
                .thenThrow(new DependenciaNoDisponibleException("pilotos caído"));
        assertThrows(DependenciaNoDisponibleException.class, () -> service.crear("cli", pedido()));
        verify(reservas, never()).save(any());
    }

    @Test
    void fallaAlAsignarBorraLaReservaYDa503() {
        doThrow(new DependenciaNoDisponibleException("falló asignación")).when(pilotos).asignar(any(), any(), any());
        assertThrows(DependenciaNoDisponibleException.class, () -> service.crear("cli", pedido()));
        verify(reservas).delete(any(Reserva.class));
        verify(clima, never()).obtenerVeredicto(anyDouble(), anyDouble(), any(), anyInt(), anyInt());
    }

    @Test
    void reglaPropiaRechazaSinLlamarAPilotos() {
        ReservaRechazadaException e = assertThrows(ReservaRechazadaException.class,
                () -> service.crear("cli", new CrearReservaDTO(3L, 200, 30)));
        assertEquals("peso fuera de rango", e.getReserva().motivo());
        verify(pilotos, never()).buscarDisponible(any(), any());
    }

    @Test
    void turnoCompletoYDobleReservaRechazan() {
        when(reservas.countByTurnoIdAndEstadoIn(3L, EstadoReserva.ACTIVAS)).thenReturn(10L);
        assertEquals("turno completo", assertThrows(ReservaRechazadaException.class,
                () -> service.crear("cli", pedido())).getReserva().motivo());

        when(reservas.countByTurnoIdAndEstadoIn(3L, EstadoReserva.ACTIVAS)).thenReturn(1L);
        when(reservas.existsByTurnoIdAndClienteSubAndEstadoIn(3L, "cli", EstadoReserva.ACTIVAS)).thenReturn(true);
        assertEquals("ya reservó este turno", assertThrows(ReservaRechazadaException.class,
                () -> service.crear("cli", pedido())).getReserva().motivo());
    }

    @Test
    void turnoInexistenteEs404() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.crear("cli", new CrearReservaDTO(99L, 70, 30)));
    }

    // ------------------------------------------------------- revalidar

    private Reserva reservaDeTurno(long id, Turno t, LocalDateTime creada, EstadoReserva estado) {
        Reserva r = new Reserva(t, "cli", 70, 30, creada);
        ReflectionTestUtils.setField(r, "id", id);
        r.asignarPiloto(1L, "Piloto Uno");
        ReflectionTestUtils.setField(r, "estado", estado); // se fija el estado directo, sin pasar por R8
        return r;
    }

    @Test
    void revalidaPorAntiguedadYCuentaResultados() {
        Turno t1 = turno(1L, AHORA.plusDays(3));
        Turno t2 = turno(2L, AHORA.plusDays(4));
        Turno t3 = turno(3L, AHORA.plusDays(5));
        Reserva vieja = reservaDeTurno(1, t1, AHORA.minusDays(3), EstadoReserva.CONFIRMADA);
        Reserva media = reservaDeTurno(2, t2, AHORA.minusDays(2), EstadoReserva.CONFIRMACION_PENDIENTE);
        Reserva nueva = reservaDeTurno(3, t3, AHORA.minusDays(1), EstadoReserva.CONFIRMADA);
        // el repositorio las devuelve desordenadas: el servicio debe procesarlas de la más antigua a la más nueva
        when(reservas.findByEstadoInAndTurnoFechaHoraBetweenOrderByCreadaEnAsc(any(), any(), any()))
                .thenReturn(List.of(nueva, vieja, media));
        when(clima.obtenerVeredicto(anyDouble(), anyDouble(), eq(t1.getFechaHora()), anyInt(), anyInt()))
                .thenReturn(veredicto(Veredicto.NO_APTO));
        when(clima.obtenerVeredicto(anyDouble(), anyDouble(), eq(t2.getFechaHora()), anyInt(), anyInt()))
                .thenReturn(veredicto(Veredicto.APTO));
        when(clima.obtenerVeredicto(anyDouble(), anyDouble(), eq(t3.getFechaHora()), anyInt(), anyInt()))
                .thenThrow(new ClimaNoDisponibleException("caído", null));

        ResumenRevalidacionDTO resumen = service.revalidar();

        assertEquals(new ResumenRevalidacionDTO(3, 1, 1, 1), resumen);
        InOrder orden = inOrder(clima);
        orden.verify(clima).obtenerVeredicto(anyDouble(), anyDouble(), eq(t1.getFechaHora()), anyInt(), anyInt());
        orden.verify(clima).obtenerVeredicto(anyDouble(), anyDouble(), eq(t2.getFechaHora()), anyInt(), anyInt());
        orden.verify(clima).obtenerVeredicto(anyDouble(), anyDouble(), eq(t3.getFechaHora()), anyInt(), anyInt());
        assertEquals(EstadoReserva.A_REPROGRAMAR, vieja.getEstado());
        assertNull(vieja.getPilotoId());
        verify(pilotos).liberar(1L);
        assertEquals(EstadoReserva.CONFIRMADA, media.getEstado());
        assertEquals(EstadoReserva.CONFIRMADA, nueva.getEstado()); // sin cambios
    }

    // ------------------------------------------- cancelar / reprogramar

    @Test
    void cancelarLiberaPilotoYReservaAjenaEs404() {
        Reserva r = reservaDeTurno(10, turno, AHORA, EstadoReserva.CONFIRMADA);
        when(reservas.findById(10L)).thenReturn(Optional.of(r));

        assertThrows(RecursoNoEncontradoException.class, () -> service.cancelar(10L, "otro", false));
        verify(pilotos, never()).liberar(anyLong());

        ReservaDTO dto = service.cancelar(10L, "cli", false);
        assertEquals(EstadoReserva.CANCELADA, dto.estado());
        verify(pilotos).liberar(10L);
    }

    @Test
    void reprogramarDesdeAReprogramarConfirma() {
        Turno nuevo = turno(7L, AHORA.plusDays(6).withHour(10));
        when(turnos.findById(7L)).thenReturn(Optional.of(nuevo));
        when(pilotos.buscarDisponible("PRUEBA_APTO", nuevo.getFechaHora()))
                .thenReturn(new PilotoDisponibleDTO(3L, "Piloto Tres"));
        Reserva r = reservaDeTurno(10, turno, AHORA, EstadoReserva.A_REPROGRAMAR);
        r.liberarPiloto();
        when(reservas.findById(10L)).thenReturn(Optional.of(r));
        climaDevuelve(Veredicto.APTO);

        ReservaDTO dto = service.reprogramar(10L, 7L, "cli");

        assertEquals(EstadoReserva.CONFIRMADA, dto.estado());
        assertEquals(7L, dto.turnoId());
        assertEquals(3L, dto.pilotoId());
    }

    @Test
    void reprogramarDesdeOtroEstadoEs422() {
        Reserva r = reservaDeTurno(10, turno, AHORA, EstadoReserva.CONFIRMADA);
        when(reservas.findById(10L)).thenReturn(Optional.of(r));
        assertThrows(ReglaNegocioException.class, () -> service.reprogramar(10L, 7L, "cli"));
    }

    @Test
    void reprogramarConClimaNoAptoLiberaYDejaAReprogramar() {
        Turno nuevo = turno(7L, AHORA.plusDays(6).withHour(10));
        when(turnos.findById(7L)).thenReturn(Optional.of(nuevo));
        when(pilotos.buscarDisponible("PRUEBA_APTO", nuevo.getFechaHora()))
                .thenReturn(new PilotoDisponibleDTO(3L, "Piloto Tres"));
        Reserva r = reservaDeTurno(10, turno, AHORA, EstadoReserva.A_REPROGRAMAR);
        when(reservas.findById(10L)).thenReturn(Optional.of(r));
        climaDevuelve(Veredicto.NO_APTO);

        assertThrows(ReglaNegocioException.class, () -> service.reprogramar(10L, 7L, "cli"));

        verify(pilotos).liberar(10L);
        assertEquals(EstadoReserva.A_REPROGRAMAR, r.getEstado());
        assertEquals(3L, r.getTurno().getId());
    }

    // ------------------------------------------------ correcciones

    private Reserva reservaEn(EstadoReserva estado, String motivo) {
        Reserva r = new Reserva(turno, "cli", 70, 30, AHORA);
        ReflectionTestUtils.setField(r, "id", 10L);
        r.asignarPiloto(1L, "Piloto Uno");
        if (estado != EstadoReserva.PENDIENTE) {
            r.cambiarEstado(EstadoReserva.CONFIRMADA, motivo);
            if (estado != EstadoReserva.CONFIRMADA) {
                r.cambiarEstado(estado, motivo);
            }
        }
        return r;
    }

    @Test
    void noAptoConLiberarCaidoIgualRechaza() {
        climaDevuelve(Veredicto.NO_APTO);
        doThrow(new DependenciaNoDisponibleException("pilotos caído", null)).when(pilotos).liberar(10L);
        ReservaRechazadaException ex = assertThrows(ReservaRechazadaException.class,
                () -> service.crear("cli", pedido()));
        assertEquals(EstadoReserva.RECHAZADA, ex.getReserva().estado());
    }

    @Test
    void cancelarSiempreLiberaAunSinPiloto() {
        Reserva r = reservaEn(EstadoReserva.CONFIRMADA, null);
        r.liberarPiloto();
        when(reservas.findById(10L)).thenReturn(Optional.of(r));
        service.cancelar(10L, "cli", false);
        verify(pilotos).liberar(10L);
        assertEquals(EstadoReserva.CANCELADA, r.getEstado());
    }

    @Test
    void cancelarConLiberarCaidoPropaga503SinCambiarEstado() {
        Reserva r = reservaEn(EstadoReserva.CONFIRMADA, null);
        when(reservas.findById(10L)).thenReturn(Optional.of(r));
        doThrow(new DependenciaNoDisponibleException("pilotos caído", null)).when(pilotos).liberar(10L);
        assertThrows(DependenciaNoDisponibleException.class, () -> service.cancelar(10L, "cli", false));
        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
        verify(reservas, never()).save(any(Reserva.class));
    }

    @Test
    void revalidarActualizaMotivoDeConfirmadaEntreCondicionalYApto() {
        Reserva r = reservaEn(EstadoReserva.CONFIRMADA, "viento cercano al límite");
        when(reservas.findByEstadoInAndTurnoFechaHoraBetweenOrderByCreadaEnAsc(any(), any(), any()))
                .thenReturn(List.of(r));
        climaDevuelve(Veredicto.APTO);
        service.revalidar();
        assertNull(r.getMotivo());
        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
        verify(reservas).save(r);
    }
}
