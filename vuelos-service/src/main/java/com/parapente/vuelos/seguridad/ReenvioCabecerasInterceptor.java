package com.parapente.vuelos.seguridad;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Reenvía a pilotos y clima el Authorization del usuario (así validan el mismo token) y el
 * X-Correlation-Id si vino, para poder seguir una petición por los tres servicios.
 */
public class ReenvioCabecerasInterceptor implements ClientHttpRequestInterceptor {

    public static final String CORRELATION_ID = "X-Correlation-Id";
    private static final java.util.regex.Pattern CORRELATION_VALIDO = java.util.regex.Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            copiar(attrs, HttpHeaders.AUTHORIZATION, request);
            // Se reenvía sólo si tiene formato seguro (evita inyección de cabeceras/logs); si no, se omite.
            String id = attrs.getRequest().getHeader(CORRELATION_ID);
            if (id != null && CORRELATION_VALIDO.matcher(id).matches()) {
                request.getHeaders().set(CORRELATION_ID, id);
            }
        }
        return execution.execute(request, body);
    }

    private void copiar(ServletRequestAttributes attrs, String cabecera, HttpRequest salida) {
        String valor = attrs.getRequest().getHeader(cabecera);
        if (valor != null) {
            salida.getHeaders().set(cabecera, valor);
        }
    }
}
