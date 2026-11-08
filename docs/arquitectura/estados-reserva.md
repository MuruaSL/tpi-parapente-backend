# Estados de la reserva (R8)

Implementado en `EstadoReserva` (`transicionesPermitidas` y `puedePasarA`); `Reserva.cambiarEstado` lanza `ReglaNegocioException` (422) ante una transición inválida.

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE
    PENDIENTE --> RECHAZADA: R1 a R5, R7, R10 o clima no apto
    PENDIENTE --> CONFIRMADA: clima apto o condicional
    PENDIENTE --> CONFIRMACION_PENDIENTE: clima no disponible
    CONFIRMACION_PENDIENTE --> CONFIRMADA: revalidación, clima apto
    CONFIRMACION_PENDIENTE --> A_REPROGRAMAR: revalidación, clima no apto
    CONFIRMACION_PENDIENTE --> CANCELADA: cancelación
    CONFIRMADA --> A_REPROGRAMAR: revalidación, clima no apto
    CONFIRMADA --> CANCELADA: cancelación
    CONFIRMADA --> COMPLETADA: finalización
    A_REPROGRAMAR --> CONFIRMADA: reprogramación
    A_REPROGRAMAR --> CANCELADA: cancelación
    RECHAZADA --> [*]
    CANCELADA --> [*]
    COMPLETADA --> [*]
```

- Estados **activos** (ocupan cupo y cuentan para R7): PENDIENTE, CONFIRMADA, CONFIRMACION_PENDIENTE y A_REPROGRAMAR (`EstadoReserva.ACTIVAS`).
- Diferencia con el diagrama del cuestionario: se agrega CONFIRMACION_PENDIENTE a CANCELADA (el contrato técnico lo permite para que una reserva en espera pueda cancelarse).
- Test: `EstadoReservaTest`.
