# Desafíos y decisiones comparadas

## Desafíos reales

1. **Coordinar tres recursos independientes** (pasajero y turno, piloto, clima) en una reserva consistente. `ReservaService.crear` fija el orden: reglas propias, piloto, clima, y deshace lo necesario (borra la reserva o libera al piloto) si algo falla.
2. **Mantener válida una reserva en el tiempo.** El clima cambia después de confirmar; por eso existe la revalidación, que libera al piloto de lo que dejó de ser viable.
3. **Fallas de servicios internos y externos sin dejar datos a medias.** Pilotos caído da 503 sin guardar nada; clima caído deja CONFIRMACION_PENDIENTE; nunca se confirma sin pronóstico.
4. **Seguridad por rol y por pertenencia** en el gateway y en cada servicio.

## Decisiones comparadas

| Decisión | Elegimos | Alternativa | Por qué |
|---|---|---|---|
| Cantidad de servicios | 3 (Vuelos, Pilotos, Clima) | 2 (Pilotos dentro de Vuelos, o Clima dentro de Vuelos) | Cada uno tiene dueño de datos y regla distintos (reservas, agenda de pilotos, pronóstico sin base). Con 2 servicios la colaboración entre servicios sería artificial. Costo: más contenedores y más memoria. |
| Gateway | Rutas estáticas en `application.yaml` | Descubrimiento de servicios (Eureka) | Son 3 rutas fijas; Eureka agregaría una pieza sin necesidad. Costo: si cambia una dirección se edita la configuración (hoy vía variables de entorno). |
| JWT | Se valida en el gateway **y** en cada servicio | Sólo en el gateway | Un servicio no debe confiar ciegamente en quien lo llama. Costo: cuatro configuraciones de seguridad, una por pieza. |
| Rollback | Compensación simple (borrar la reserva, `DELETE` de la asignación) | Saga o transacción distribuida | Hay un solo paso con efecto externo (reservar piloto). Límite: si liberar falla queda la reserva PENDIENTE y hay que limpiarla a mano (anotado en el código). |
| Reevaluación del clima | Revalidación por endpoint, la dispara el administrador | Tarea programada automática | Simple y fácil de demostrar; la reprogramación automática está fuera de alcance. Costo: alguien tiene que ejecutarla. |
| Validación del JWT | `jwk-set-uri` sin validar el emisor (`iss`) | Validar issuer | El host de Keycloak difiere dentro y fuera de Docker (`localhost:8180` contra `keycloak:8080`), así que el issuer no coincidiría. Se verifican firma y expiración. Riesgo: acepta tokens de otro realm firmado con las mismas claves; en producción se fija el issuer. |
| Librería común | No hay | `parapente-commons` | Lo repetido es poco (conversor de roles, manejo de errores, cliente HTTP) y compartirlo acopla versiones. |
| Estructura de paquetes | Por funcionalidad (`reservas`, `turnos`, `clima`...) | Por capa (`controller`, `service`) | El código "grita" el negocio y cada cambio toca una sola carpeta. |
| Transacciones | `crear` y `revalidar` sin `@Transactional` | Una transacción global | Así el RECHAZADA queda guardado aunque se lance 422, y no se retiene una conexión durante llamadas HTTP. |

## Límites conocidos (honestos)

- Cupo y doble reserva (R3, R7) no son atómicos ante concurrencia.
- No hay pruebas de integración con PostgreSQL ni con Keycloak real; las pruebas usan mocks y MockMvc.
- El arranque con Docker no se pudo verificar en la máquina de desarrollo.
