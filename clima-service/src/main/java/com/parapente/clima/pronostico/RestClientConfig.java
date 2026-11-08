package com.parapente.clima.pronostico;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Crea el RestClient hacia Open-Meteo con timeouts de conexión y lectura. */
@Configuration
public class RestClientConfig {

    @Bean
    RestClient openMeteoRestClient(@Value("${open-meteo.url}") String url,
                                   @Value("${open-meteo.timeout-ms}") int timeoutMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(timeoutMs));
        fabrica.setReadTimeout(Duration.ofMillis(timeoutMs));
        return RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }
}
