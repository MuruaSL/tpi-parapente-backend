package com.parapente.vuelos.turnos;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TurnoRepository extends JpaRepository<Turno, Long> {
    List<Turno> findByFechaHoraAfterOrderByFechaHoraAsc(LocalDateTime desde);
}
