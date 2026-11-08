package com.parapente.pilotos.asignaciones;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla asignacion. */
public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {
    List<Asignacion> findByPilotoIdOrderByFechaHora(Long pilotoId);
    void deleteByReservaId(Long reservaId);
}
