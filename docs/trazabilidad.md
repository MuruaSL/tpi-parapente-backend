# Trazabilidad: cuestionario, implementación y pruebas

Estado: **hecho** = implementado y con test automático que lo cubre; **parcial** = implementado pero sin test propio, o con un hueco; **pendiente** = no verificado. Los tests automáticos son de `mvn test` en cada servicio. Los escenarios de Postman se citan por el nombre de la sección 12 del cuestionario; `docs/postman/` se está completando aparte y no se verificó su contenido, por eso la columna Postman figura como "a confirmar" en lugar de afirmar que existe.

Rutas: `V` = `vuelos-service/.../vuelos`, `P` = `pilotos-service/.../pilotos`, `C` = `clima-service/.../clima`.

## Funcionalidades

| ID | Endpoint | Clase real | Test automático | Escenario Postman (sec. 12) | Estado |
|---|---|---|---|---|---|
| F1 | GET `/api/v1/vuelos/turnos`, `/turnos/{id}` | `V/turnos/TurnoController`, `TurnoService` | ninguno de lógica (no hay `TurnoServiceTest`) | Carpeta 1 "Consulta de turnos" (a confirmar) | parcial: sin test propio |
| F2 | POST `/api/v1/vuelos/reservas` | `V/reservas/ReservaController`, `ReservaService.crear` | `ReservaServiceTest` (apto, condicional, no apto, clima caído, sin piloto, pilotos caído, falla al asignar), `SeguridadReservasWebMvcTest.clienteCreaReservaUsandoSuSubEs201` | "Reserva confirmada con clima apto" (a confirmar) | hecho (unitario con mocks; sin prueba de integración real) |
| F3 | GET `/reservas/mias`, `/reservas/{id}` | `ReservaController`, `ReservaService.listarDe/obtener` | `SeguridadReservasWebMvcTest.adminVeCualquierReservaYClienteSoloLaSuya`, `ReservaServiceTest.cancelarLiberaPilotoYReservaAjenaEs404` | "Consulta de reservas propias" (a confirmar) | hecho |
| F4 | POST `/reservas/revalidacion` | `ReservaService.revalidar` | `ReservaServiceTest.revalidaPorAntiguedadYCuentaResultados`, `SeguridadReservasWebMvcTest.clienteNoPuedeRevalidarNiFinalizarEs403` | "Revalidación que deja una reserva pendiente como Confirmada"; "Confirmada que pasa a A reprogramar" (a confirmar) | hecho |
| F5 | PATCH `/reservas/{id}/reprogramacion` | `ReservaService.reprogramar` | `reprogramarDesdeAReprogramarConfirma`, `reprogramarDesdeOtroEstadoEs422`, `reprogramarConClimaNoAptoLiberaYDejaAReprogramar` | "Reprogramación de una reserva A reprogramar" (a confirmar) | hecho |
| F6 | PATCH `/reservas/{id}/cancelacion` | `ReservaService.cancelar` | `ReservaServiceTest.cancelarLiberaPilotoYReservaAjenaEs404` | "cancelación" (a confirmar) | hecho |
| F7 | PATCH `/reservas/{id}/finalizacion` | `ReservaService.finalizar` | solo permiso 403 de cliente; transición cubierta por `EstadoReservaTest` | sin escenario propio en sec. 12 | parcial |
| F8 | POST/PUT `/turnos`, `/tipos-vuelo`; GET `/tipos-vuelo` | `V/turnos/TurnoController`, `V/tiposvuelo/TipoVueloController` y sus services | ninguno | sin escenario propio en sec. 12 | parcial: implementado, sin tests |
| F9 | `/api/v1/pilotos` (CRUD) y `/{id}/certificaciones` | `P/pilotos/PilotoController`, `PilotoService` | `SeguridadWebTest` (sólo permisos) | sin escenario propio en sec. 12 | parcial: sin test de lógica |
| F10 | GET `/api/v1/pilotos/mi-agenda` | `P/agenda/AgendaController`, `AsignacionService` | `SeguridadWebTest.agendaConPilotoEs200` y variantes de rol | "agenda del piloto" (a confirmar) | parcial: permisos probados, no el contenido filtrado por piloto |
| F11 | GET `/api/v1/clima/veredicto` | `C/veredicto/VeredictoController`, `VeredictoService`, `ReglaClima`, `OpenMeteoClient` | `ReglaClimaTest`, `OpenMeteoClientTest`, `VeredictoControllerSeguridadTest` | carpeta 4 "Clima y revalidación" (a confirmar) | hecho |
| F12 | GET `/api/v1/vuelos/reservas` (paginado) | `ReservaController.listar` | `SeguridadReservasWebMvcTest.clienteNoPuedeListarTodasEs403`, `adminListaTodasEs200` | "Sin token (401), rol incorrecto (403)" (a confirmar) | hecho |

## Reglas

| ID | Clase real | Test | Escenario Postman | Estado |
|---|---|---|---|---|
| R1 | `ReglasReserva.peso` | `ReglasReservaTest.r1...` | "Peso fuera de rango (R1)" | hecho |
| R2 | `ReglasReserva.edad` | `r2...` | "menor de edad (R2)" | hecho |
| R3 | `ReglasReserva.cupo` | `r3...`, `ReservaServiceTest.turnoCompletoYDobleReservaRechazan` | "turno completo (R3)" | hecho |
| R4 | `ReglasAsignacion.estaCertificado` | `ReglasAsignacionTest`, `ReservaServiceTest.sinPilotoRechazaYNoReservaAgenda` | "sin piloto certificado (R4)" | hecho |
| R5 | `ReglasAsignacion.estaLibre` | `ReglasAsignacionTest` | sin escenario propio en sec. 12 | hecho (unitario) |
| R6 | `ReglaClima.evaluar` | `ReglaClimaTest`, `ReservaServiceTest` | "Clima no apto" | hecho |
| R7 | `ReglasReserva.noDuplicada` | `r7...` | "doble reserva (R7)" | hecho |
| R8 | `EstadoReserva` | `EstadoReservaTest` | sin escenario propio | hecho |
| R9 | `ReservaService.revalidar` | `revalidaPorAntiguedadYCuentaResultados` | sin escenario propio | hecho |
| R10 | `ReglasReserva.ventana` | `r10...` | sin escenario propio | hecho |

## Transversales

| Tema | Dónde | Test | Estado |
|---|---|---|---|
| 401, 403 por rol | `SecurityConfig` de gateway, vuelos, `SeguridadConfig` de pilotos, `SecurityConfig` de clima | `GatewayTest`, `SeguridadReservasWebMvcTest`, `SeguridadWebTest`, `VeredictoControllerSeguridadTest` | hecho |
| Reserva ajena = 404 | `ReservaService.visible` | `SeguridadReservasWebMvcTest`, `ReservaServiceTest` | hecho |
| Pilotos caído = 503 sin datos a medias (V7) | `PilotosClient`, `ReservaService.crear` | `ReservaServiceTest.pilotosCaidoDa503SinGuardarNada`, `fallaAlAsignarBorraLaReservaYDa503`, `PilotosClientTest` | hecho en unitario; el escenario real "Servicio detenido" requiere Docker |
| Clima caído = CONFIRMACION_PENDIENTE (V5) | `ClimaClient`, `ReservaService.crear` | `climaCaidoDejaConfirmacionPendienteConPilotoReservado`, `ClimaClientTest` | hecho en unitario |
| Endpoints internos no expuestos | `api-gateway/src/main/resources/application.yaml` (no hay ruta a `/internos`) | `GatewayTest.rutasDelContrato` | hecho |
| Arranque con Docker | `docker-compose.yml`, Dockerfiles, `keycloak/realm-parapente.json` | ninguno | pendiente: Docker no está instalado en la Mac de desarrollo, el arranque completo no se verificó |
| Flujo de punta a punta contra servicios reales | todos | no hay pruebas de integración ni de Flyway/PostgreSQL | pendiente |
