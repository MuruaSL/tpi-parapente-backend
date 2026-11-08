package com.parapente.pilotos.pilotos;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla piloto. */
public interface PilotoRepository extends JpaRepository<Piloto, Long> {
    List<Piloto> findByActivoTrueOrderById();
    Optional<Piloto> findByKeycloakSub(String keycloakSub);
    boolean existsByKeycloakSub(String keycloakSub);
}
