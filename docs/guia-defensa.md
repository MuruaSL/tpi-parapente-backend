# Guía de defensa

> El formato de la exposición (guión general más una parte por integrante, flujo de autenticación incluido) está en [`guion-exposicion.md`](guion-exposicion.md). Esta guía conserva el guion de 10 minutos y las 12 preguntas probables.

## Guion de 10 minutos

| Min | Qué mostrar | Qué decir |
|---|---|---|
| 0-1 | Problema (cuestionario sec. 3) | Se confirman reservas sin saber si hay piloto ni si el viento deja volar. |
| 1-3 | `c4-contenedores.md` | Gateway como única entrada, tres servicios, una base por servicio, Keycloak, Open-Meteo. |
| 3-6 | Flujo en Swagger o Postman: `cliente1` reserva en turno PRUEBA_APTO y queda CONFIRMADA; luego peso fuera de rango y queda RECHAZADA con motivo | La reserva se decide en tres etapas: requisitos propios, piloto y clima. |
| 6-7 | Reserva en PRUEBA_NO_APTO, o servicio de Clima detenido | Clima no apto rechaza; clima caído deja CONFIRMACION_PENDIENTE. |
| 7-8 | Revalidación como `admin1` | Libera pilotos de reservas inviables; reprogramación posterior. |
| 8-9 | Seguridad: sin token 401, `cliente1` en `GET /reservas` 403 | Gateway y servicios validan el token; roles y propiedad. |
| 9-10 | `reglas.md` y `trazabilidad.md`, `mvn test` | Cada regla tiene su clase pura y su test; lo que falta está dicho. |

## 12 preguntas probables

1. **¿Por qué esta distribución en tres servicios?** Porque Vuelos, Pilotos y Clima tienen datos y reglas distintos, y cada uno tiene dueño y despliegue propio.
2. **¿Dónde están las reglas de negocio?** En clases puras sin Spring: `ReglasReserva` y `EstadoReserva` (Vuelos), `ReglasAsignacion` (Pilotos), `ReglaClima` (Clima); ver `reglas.md`.
3. **¿Cómo colaboran los servicios?** Vuelos orquesta y llama por HTTP/JSON a Pilotos (buscar, asignar, liberar) y a Clima (veredicto), reenviando el token del usuario.
4. **¿Qué papel cumple Open-Meteo?** Es la fuente externa del pronóstico; sólo Clima la consulta y aplica R6.
5. **¿Cómo es la seguridad y los roles?** Keycloak emite el JWT con roles CLIENTE, PILOTO y ADMIN; el gateway y cada servicio lo validan, y hay reglas por pertenencia (reserva ajena es 404).
6. **¿Por qué Keycloak?** Porque resuelve usuarios, contraseñas, roles y tokens estándar (OpenID Connect) sin que nuestros servicios manejen contraseñas.
7. **¿Por qué no hay librería común?** Lo compartido es mínimo y una librería acoplaría versiones y despliegues; se repite a propósito dentro de cada servicio.
8. **¿Qué pasa si cae Clima?** La reserva queda CONFIRMACION_PENDIENTE con el piloto reservado y se resuelve al revalidar; nunca se confirma sin pronóstico.
9. **¿Qué pasa si cae Pilotos?** Vuelos responde 503 y no guarda nada; si cae después de guardar, borra la reserva.
10. **¿Por qué la estructura por funcionalidad?** Para que el código muestre el negocio (`reservas`, `turnos`, `clima`) y cada cambio toque una sola carpeta.
11. **¿Por qué el gateway no enruta `/internos`?** Porque esos endpoints son sólo para llamadas entre servicios y no deben ser accesibles desde afuera.
12. **¿Qué falta o qué limitaciones tiene?** Sin pruebas de integración con base real, arranque Docker sin verificar en la máquina de desarrollo, y cupo no atómico bajo concurrencia; ver `trazabilidad.md`.
