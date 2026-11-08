package com.parapente.vuelos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.parapente.vuelos.clima.ClimaClient;
import com.parapente.vuelos.pilotos.PilotosClient;
import com.parapente.vuelos.seguridad.ReenvioCabecerasInterceptor;

/** Arma los RestClient hacia pilotos y clima: baseUrl por propiedad, timeouts de 5 s y reenvío de cabeceras. */
@Configuration
public class ClientesHttpConfig {

    private static final int TIMEOUT_MS = 5000;

    @Bean
    public PilotosClient pilotosClient(@Value("${servicios.pilotos.url}") String url) {
        return new PilotosClient(crear(url));
    }

    @Bean
    public ClimaClient climaClient(@Value("${servicios.clima.url}") String url) {
        return new ClimaClient(crear(url));
    }

    private RestClient crear(String baseUrl) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(TIMEOUT_MS);
        fabrica.setReadTimeout(TIMEOUT_MS);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica)
                .requestInterceptor(new ReenvioCabecerasInterceptor()).build();
    }
}
