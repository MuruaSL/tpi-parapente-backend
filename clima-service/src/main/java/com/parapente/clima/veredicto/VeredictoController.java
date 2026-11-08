package com.parapente.clima.veredicto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint de diagnóstico y de uso interno de vuelos: ¿se puede volar en ese lugar y hora? */
@RestController
@RequestMapping("/api/v1/clima")
@Validated
public class VeredictoController {

    private final VeredictoService servicio;

    public VeredictoController(VeredictoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/veredicto")
    public VeredictoDTO veredicto(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double latitud,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double longitud,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora,
            @RequestParam @DecimalMin("0") double vientoMaxKmh,
            @RequestParam @DecimalMin("0") double margenKmh) {
        return servicio.evaluar(latitud, longitud, fechaHora, vientoMaxKmh, margenKmh);
    }
}
