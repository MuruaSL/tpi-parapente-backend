# Secuencia: reserva exitosa (`ReservaService.crear`)

```mermaid
sequenceDiagram
    actor C as Cliente
    participant G as API Gateway
    participant V as Servicio de Vuelos
    participant P as Servicio de Pilotos
    participant K as Servicio de Clima
    participant O as Open-Meteo
    C->>G: POST /api/v1/vuelos/reservas {turnoId, pesoKg, edad} + token
    G->>G: valida JWT, exige autenticación
    G->>V: reenvía
    V->>V: SecurityConfig: rol CLIENTE o ADMIN
    V->>V: ReglasReserva.evaluar (R1, R2, R3, R7, R10)
    V->>P: GET /api/v1/internos/pilotos/disponibles?tipoVuelo&fechaHora
    P-->>V: 200 pilotoId y nombre (R4, R5)
    V->>V: guarda reserva PENDIENTE
    V->>P: POST /api/v1/internos/asignaciones
    P-->>V: 201
    V->>K: GET /api/v1/clima/veredicto (coordenadas, hora, límites de viento)
    K->>O: GET /v1/forecast
    O-->>K: viento, ráfagas, lluvia
    K-->>V: APTO o CONDICIONAL (R6)
    V->>V: PENDIENTE a CONFIRMADA
    V-->>C: 201 ReservaDTO estado CONFIRMADA
```

Cada etapa puede cortar el proceso (ver `secuencia-errores.md`). Con CONDICIONAL la reserva también queda CONFIRMADA, con el motivo "viento cercano al límite".
