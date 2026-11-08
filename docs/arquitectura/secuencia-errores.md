# Secuencias de error

## 1. Regla de negocio (ejemplo: peso fuera de rango, R1)

```mermaid
sequenceDiagram
    actor C as Cliente
    participant V as Servicio de Vuelos
    participant P as Servicio de Pilotos
    C->>V: POST /reservas (peso 150)
    V->>V: ReglasReserva.peso devuelve "peso fuera de rango"
    V->>V: guarda reserva RECHAZADA con motivo
    V-->>C: 422 {reservaId, estado RECHAZADA, motivo}
    Note over V,P: No se llama a Pilotos ni a Clima
```

Lo mismo ocurre con R2, R3, R7 y R10. Sin piloto certificado o libre (R4, R5) pasa igual, pero después de consultar a Pilotos (que responde 404 con el motivo).

## 2. Servicio de Pilotos caído

```mermaid
sequenceDiagram
    actor C as Cliente
    participant V as Servicio de Vuelos
    participant P as Servicio de Pilotos
    C->>V: POST /reservas (datos válidos)
    V->>V: reglas propias OK
    V->>P: GET disponibles
    P--xV: sin respuesta, timeout 5 s o 5xx
    V-->>C: 503 dependencia no disponible
    Note over V: No se guarda ninguna reserva
```

Si Pilotos cae en el paso de `asignar` (con la reserva ya guardada), Vuelos borra la reserva y responde 503 (marcha atrás simple).

## 3. Servicio de Clima caído (o Open-Meteo caído)

```mermaid
sequenceDiagram
    actor C as Cliente
    participant V as Servicio de Vuelos
    participant P as Servicio de Pilotos
    participant K as Servicio de Clima
    C->>V: POST /reservas (datos válidos)
    V->>P: busca y asigna piloto
    P-->>V: OK
    V->>K: GET veredicto
    K--xV: 503, timeout o error
    V->>V: PENDIENTE a CONFIRMACION_PENDIENTE
    V-->>C: 201 estado CONFIRMACION_PENDIENTE
    Note over V,P: El piloto sigue reservado. Se resuelve al revalidar
```

Clima no disponible nunca confirma. Cualquier falla de `ClimaClient` (5xx, 4xx, timeout, respuesta vacía) cuenta como no disponible.

## 4. Clima no apto (R6)

Clima responde NO_APTO: Vuelos llama a `DELETE /api/v1/internos/asignaciones/{reservaId}`, libera al piloto, guarda la reserva RECHAZADA con "clima no apto: ..." y responde 422.
