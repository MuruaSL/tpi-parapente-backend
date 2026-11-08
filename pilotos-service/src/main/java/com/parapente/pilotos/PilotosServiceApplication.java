package com.parapente.pilotos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada del servicio de Pilotos (certificaciones y agenda de vuelos). */
@SpringBootApplication
public class PilotosServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PilotosServiceApplication.class, args);
    }
}
