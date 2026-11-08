package com.parapente.vuelos.tiposvuelo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoVueloRepository extends JpaRepository<TipoVuelo, Long> {
    boolean existsByCodigo(String codigo);
}
