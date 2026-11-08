package com.parapente.clima;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada del servicio de clima (no usa base de datos). */
@SpringBootApplication
public class ClimaApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClimaApplication.class, args);
    }
}
