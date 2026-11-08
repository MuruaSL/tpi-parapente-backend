package com.parapente.vuelos.tiposvuelo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Entrada y salida de tipos de vuelo (el id se ignora al crear). */
public record TipoVueloDTO(
        Long id,
        @NotBlank String codigo,
        @NotBlank String nombre,
        @NotNull @PositiveOrZero Integer pesoMin,
        @NotNull @PositiveOrZero Integer pesoMax,
        @NotNull @PositiveOrZero Integer edadMin,
        @NotNull @PositiveOrZero Integer vientoMaxKmh,
        @NotNull @PositiveOrZero Integer margenVientoKmh) {

    public static TipoVueloDTO desde(TipoVuelo t) {
        return new TipoVueloDTO(t.getId(), t.getCodigo(), t.getNombre(), t.getPesoMin(), t.getPesoMax(),
                t.getEdadMin(), t.getVientoMaxKmh(), t.getMargenVientoKmh());
    }
}
