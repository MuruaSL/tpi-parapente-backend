package com.parapente.vuelos.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** X-Correlation-Id sólo se reenvía si cumple ^[A-Za-z0-9-]{1,64}$. */
class ReenvioCabecerasInterceptorTest {

    @AfterEach
    void limpiar() {
        RequestContextHolder.resetRequestAttributes();
    }

    private String reenviado(String id) throws Exception {
        MockHttpServletRequest entrada = new MockHttpServletRequest();
        entrada.addHeader("X-Correlation-Id", id);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(entrada));
        MockClientHttpRequest salida = new MockClientHttpRequest(HttpMethod.GET, java.net.URI.create("http://x"));
        new ReenvioCabecerasInterceptor().intercept(salida, new byte[0],
                (req, body) -> new MockClientHttpResponse(new byte[0], 200));
        return salida.getHeaders().getFirst("X-Correlation-Id");
    }

    @Test
    void validoSeReenvia() throws Exception {
        assertEquals("abc-123", reenviado("abc-123"));
    }

    @Test
    void invalidoSeOmite() throws Exception {
        assertNull(reenviado("a b;c"));
        assertNull(reenviado("x".repeat(65)));
    }
}
