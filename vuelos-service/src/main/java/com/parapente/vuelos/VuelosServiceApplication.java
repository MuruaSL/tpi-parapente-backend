package com.parapente.vuelos;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Punto de entrada del Servicio de Vuelos (turnos, tipos de vuelo y reservas).
 */
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class VuelosServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VuelosServiceApplication.class, args);
    }

    /** Reloj inyectable: en los tests se reemplaza por uno fijo para probar fechas (R10). */
    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
