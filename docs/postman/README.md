# Colecciones de Postman - TPI Parapente

Archivos:
- `Parapente - Flujo completo.postman_collection.json`
- `Parapente - Seguridad y errores.postman_collection.json`
- `Parapente - Local.postman_environment.json`

## Importar
1. Postman > Import > seleccionar los 3 archivos.
2. Elegir el environment **Parapente - Local** (arriba a la derecha).
3. Variables: `baseUrl=http://localhost:8080` (gateway), `kcUrl=http://localhost:8180`, `realm=parapente`, `clientId=parapente-postman`. Los tokens (`tokenCliente`, `tokenPiloto`, `tokenAdmin`) y los ids de reserva se completan solos por scripts de test. `turnoApto..turnoReprogramar` son los ids fijos de los datos de prueba (1 a 7). `fechaClima` debe ser una fecha futura (max. 16 dias) para el request de clima; actualizarla.

## Requisitos previos
Sistema levantado (`docker compose up`, o servicios locales + Keycloak en 8180) **con datos recien sembrados**: el flujo cambia estado (cupos, reservas, limites del tipo PRUEBA_APTO) y no es re-ejecutable sobre la misma base (R7 y cupos). Para repetir: `docker compose down -v && docker compose up -d`.

## Orden de ejecucion (Collection Runner)
1. **Flujo completo**, todas las carpetas en orden (0 Auth, 1 Turnos y flujo feliz, 2 Reglas, 3 Clima y revalidacion, 4 Reprogramacion y cancelacion). "Run collection" con el environment elegido, sin delay.
2. **Seguridad y errores**, carpetas 0 a 3 (desmarcar la carpeta 4 "MANUAL").
3. Carpeta 4 de Seguridad: escenarios manuales (abajo), ejecutando cada request a mano.

Los tokens duran poco (5 min por defecto en Keycloak): si se pasa de tiempo, volver a correr la carpeta 0 Auth.

## Que cubre
- Flujo feliz turno 1 (PRUEBA_APTO) 201 CONFIRMADA; mis reservas; agenda de piloto1.
- R1 peso, R2 edad, R3 turno 4 (cupo 1: cliente1 y luego admin1), R4 turno 3 sin piloto, R7 doble reserva.
- Clima no apto turno 2: 422 RECHAZADA. Revalidacion admin; se baja el viento de PRUEBA_APTO a 0 (PUT tipos-vuelo/{id}) y se revalida: A_REPROGRAMAR; se restaura y se reprograma al turno 7 (CONFIRMADA) y se cancela.
- Seguridad: sin token 401, cliente en listado completo 403, reserva ajena 404, `/api/v1/internos/**` por el gateway 404/403.

## Escenarios manuales de integracion (carpeta 4 de Seguridad)
Correr con datos frescos o con `cliente1` sin reserva activa en los turnos usados (R7).

**V7 - pilotos caido (503, nada guardado)**
1. Parar pilotos: `docker compose stop pilotos-service` (local: cortar el proceso en 8081).
2. Ejecutar "MANUAL V7" (espera 503) y "V7b".
3. Levantar: `docker compose start pilotos-service`.

**V5 - clima caido (CONFIRMACION_PENDIENTE)**
1. Parar clima: `docker compose stop clima-service` (local: proceso en 8083).
2. Ejecutar "MANUAL V5" (201 CONFIRMACION_PENDIENTE, usa turno 7) y "V5b" (revalidar: sinCambios).
3. Levantar: `docker compose start clima-service`; esperar healthy.
4. Ejecutar "V5c" y "V5d": la reserva pasa a CONFIRMADA.

## Discrepancias detectadas contrato vs codigo
Ver el reporte entregado junto con estas colecciones.
