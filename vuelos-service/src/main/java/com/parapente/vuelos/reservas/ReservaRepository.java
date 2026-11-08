package com.parapente.vuelos.reservas;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    long countByTurnoIdAndEstadoIn(Long turnoId, Collection<EstadoReserva> estados);

    boolean existsByTurnoIdAndClienteSubAndEstadoIn(Long turnoId, String clienteSub,
            Collection<EstadoReserva> estados);

    List<Reserva> findByClienteSubOrderByCreadaEnDesc(String clienteSub);

    /** Candidatas a revalidar (R9: de la más antigua a la más nueva). */
    List<Reserva> findByEstadoInAndTurnoFechaHoraBetweenOrderByCreadaEnAsc(Collection<EstadoReserva> estados,
            LocalDateTime desde, LocalDateTime hasta);
}
