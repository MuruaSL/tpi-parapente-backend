package com.parapente.gateway.correlacion;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtro global: si el request no trae X-Correlation-Id lo genera, lo reenvía a los servicios
 * y lo devuelve en la respuesta, para poder seguir un pedido a través de todos los logs.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";
    private static final java.util.regex.Pattern VALIDO = java.util.regex.Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String id = exchange.getRequest().getHeaders().getFirst(HEADER);
        // Sólo se acepta un id entrante "seguro" (evita inyección en logs/headers); si no, se genera uno
        if (id == null || !VALIDO.matcher(id).matches()) {
            id = UUID.randomUUID().toString();
        }
        // El request es inmutable: se muta una copia con el header y se la pasa al resto de la cadena
        ServerHttpRequest request = exchange.getRequest().mutate().header(HEADER, id).build();
        exchange.getResponse().getHeaders().set(HEADER, id);
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
