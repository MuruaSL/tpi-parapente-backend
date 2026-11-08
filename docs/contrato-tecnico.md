# Contrato técnico compartido (TPI Parapente)

Fuente de verdad para todos los agentes e integrantes. Si algo de acá choca con otra cosa, **gana este documento**. Contexto funcional: `cuestionario-compromiso.md` (respuestas enviadas a la cátedra, no se cambia el alcance).

## 1. Reglas generales
- **Estructura del repo:** `api-gateway/`, `pilotos-service/`, `vuelos-service/`, `clima-service/`, `keycloak/`, `docs/`, y en la raíz `docker-compose.yml`, `.env.example`, `README.md`. Cada agente escribe **sólo en sus carpetas**. Nadie hace `git commit`.
- **Java:** `java.version` 21, Spring Boot parent **3.5.6**, Spring Cloud **2025.0.0** (sólo gateway), Maven con wrapper (`mvn -N wrapper:wrapper`). **Sin Lombok** (esta Mac tiene JDK 25 y Lombok no compila): getters/setters a mano o `record`.
- **Paquetes:** `com.parapente.<servicio>` (`gateway`, `pilotos`, `vuelos`, `clima`), organizados **por funcionalidad** (Screaming Architecture), no por capa. Ej.: `com.parapente.vuelos.reservas`, `.turnos`, `.tiposvuelo`, `.clima`, `.pilotos`, `.seguridad`, `.errores`.
- **Nombres de clase (estilo del profe):** `XxxController`, `XxxService`, `XxxRepository`, `XxxClient`, `XxxDTO` (pueden ser `record`), `GlobalExceptionHandler`, `ReglaNegocioException`, `RecursoNoEncontradoException`. Reglas de negocio en clases puras sin Spring (`ReglasReserva`, `ReglaClima`, `ReglasAsignacion`).
- **Comentarios:** todo el código en español con Javadoc de 1 a 3 líneas por clase y comentarios del *por qué* en decisiones (reglas, orquestación, seguridad). Que un estudiante lo entienda leyéndolo.
- **Sin** Lombok, Eureka, mensajería, Prometheus/Grafana, librería compartida.
- **Errores JSON** (igual en los 3 servicios): `{"timestamp","status","error"}` y para validación `+ "detalles": {campo: mensaje}`. 400 validación, 401/403 seguridad, 404 `RecursoNoEncontradoException`, 422 `ReglaNegocioException`, 503 dependencia caída.
- **Tests:** JUnit 5 + Mockito + `@WebMvcTest` (+ spring-security-test) + `MockRestServiceServer`. **Prohibido** `@SpringBootTest` que necesite base o red. `mvn test` debe pasar sin Docker. Cubrir: cada regla (normal, límite, rechazo), seguridad 401/403/200 de endpoints clave, clientes HTTP (ok, 404, 500, timeout).
- **Docker no está instalado en esta Mac:** no se puede verificar `docker compose`. Verificar con `mvn test` y `mvn -q -DskipTests package`.

## 2. Puertos, rutas y configuración
| Pieza | Puerto | Publicado al host |
|---|---|---|
| api-gateway | 8080 | sí |
| pilotos-service | 8081 | no |
| vuelos-service | 8082 | no |
| clima-service | 8083 | no |
| keycloak | 8080 interno, **8180** host | sí |
| postgres 16 | 5432 | no (sólo red interna) |

- Config por variables de entorno con valor por defecto para correr local: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PILOTOS_URL` (def. `http://localhost:8081`), `CLIMA_URL` (def. `http://localhost:8083`), `VUELOS_URL`, `KEYCLOAK_JWK_URI` (def. `http://localhost:8180/realms/parapente/protocol/openid-connect/certs`), `OPEN_METEO_URL` (def. `https://api.open-meteo.com`).
- Bases: `vuelos_db` y `pilotos_db` en un mismo Postgres; Flyway (`db/migration`: `V1__esquema.sql`, `V2__datos_prueba.sql`) con `ddl-auto=validate`. `spring.jpa.open-in-view=false`. Clima no tiene base.
- Todos los controladores van bajo `/api/v1/<servicio>/...` (el gateway **no** recorta el path). Endpoints internos de pilotos: `/api/v1/internos/...` (el gateway no los enruta).
- Cada servicio expone `spring-boot-starter-actuator` con **sólo** `/actuator/health` (público), y su documentación en `/api/v1/<servicio>/v3/api-docs` (público; `springdoc.api-docs.path`), springdoc-openapi 2.8.x. Clients HTTP: `RestClient` con baseUrl desde propiedades y timeouts de 5 s (conexión y lectura).

## 3. Seguridad (Keycloak)
- Realm `parapente`. Roles de realm: `CLIENTE`, `PILOTO`, `ADMIN`. Clientes: `parapente-swagger` (público, PKCE, redirect al gateway), `parapente-postman` (público, `directAccessGrantsEnabled`).
- Usuarios de prueba (ids fijos para que coincidan con los datos de prueba):
  - `cliente1` / `Cliente123!` / rol CLIENTE / `sub` = `11111111-1111-1111-1111-111111111111`
  - `piloto1` / `Piloto123!` / rol PILOTO / `sub` = `22222222-2222-2222-2222-222222222222`
  - `admin1` / `Admin123!` / rol ADMIN / `sub` = `33333333-3333-3333-3333-333333333333`
- Gateway y los 3 servicios son *resource servers* JWT con `jwk-set-uri` (sin validar issuer). Un `ConversorRoles` (en cada servicio, paquete `seguridad`) mapea `realm_access.roles` a `ROLE_<rol>`. Gateway: `SecurityWebFilterChain` reactivo; servicios: `SecurityFilterChain` MVC, stateless, csrf deshabilitado. Los endpoints `/api/v1/internos/**` exigen token válido (cualquier rol); el `RestClient` de `vuelos` reenvía el `Authorization` del usuario con un interceptor.
- Permisos (gateway grueso, servicios fino con `@PreAuthorize` o reglas de ruta):

| Endpoint | Roles |
|---|---|
| GET `/api/v1/vuelos/turnos`, `/turnos/{id}`, `/tipos-vuelo` | cualquier autenticado |
| POST/PUT `/api/v1/vuelos/turnos`, `/tipos-vuelo` | ADMIN |
| POST `/api/v1/vuelos/reservas` | CLIENTE, ADMIN |
| GET `/api/v1/vuelos/reservas/mias` | CLIENTE, ADMIN |
| GET `/api/v1/vuelos/reservas/{id}` | dueño (CLIENTE) o ADMIN; ajena = 404 |
| GET `/api/v1/vuelos/reservas` (paginado) | ADMIN |
| PATCH `.../reservas/{id}/reprogramacion` | dueño |
| PATCH `.../reservas/{id}/cancelacion` | dueño o ADMIN |
| PATCH `.../reservas/{id}/finalizacion`, POST `.../reservas/revalidacion` | ADMIN |
| `/api/v1/pilotos/**` administración | ADMIN |
| GET `/api/v1/pilotos/mi-agenda` | PILOTO, ADMIN |
| GET `/api/v1/clima/veredicto` | ADMIN (y llamadas internas con token de usuario ya validado) |

> Nota: `vuelos` llama a `clima` con el token del usuario, que puede ser CLIENTE. Por eso `clima-service` acepta `veredicto` de **cualquier autenticado**, y es el **gateway** el que limita `/api/v1/clima/**` a ADMIN. Mismo criterio para `/api/v1/internos/**` en pilotos.

## 4. Contratos entre servicios
**Pilotos (interno)**
- `GET /api/v1/internos/pilotos/disponibles?tipoVuelo={codigo}&fechaHora={ISO-8601}` → 200 `{"pilotoId":1,"nombre":"..."}`. Si ningún piloto activo tiene la certificación → 404 con `error` "sin piloto certificado". Si hay certificados pero todos ocupados (R5) → 404 con `error` "sin piloto disponible".
- `POST /api/v1/internos/asignaciones` body `{"pilotoId":1,"fechaHora":"...","reservaId":10}` → 201. Revalida R5 → 422 si no corresponde.
- `DELETE /api/v1/internos/asignaciones/{reservaId}` → 204 (idempotente).

**Clima**
- `GET /api/v1/clima/veredicto?latitud=&longitud=&fechaHora=&vientoMaxKmh=&margenKmh=` → 200 `{"veredicto":"APTO|CONDICIONAL|NO_APTO","vientoKmh":..,"rafagasKmh":..,"probLluviaPct":..,"motivo":"..."}`. Fecha pasada o a más de 16 días → 422. Open-Meteo caído/timeout → **503**.
- Open-Meteo: `GET {OPEN_METEO_URL}/v1/forecast?latitude&longitude&hourly=wind_speed_10m,wind_gusts_10m,precipitation_probability&wind_speed_unit=kmh&forecast_days=16&timezone=auto`, se toma la hora más cercana a `fechaHora`.
- **R6:** `v = max(viento, ráfagas)`; `v <= vientoMax` → APTO; `v <= vientoMax + margen` → CONDICIONAL; si no → NO_APTO. Si `probLluvia >= 60` → NO_APTO (motivo lluvia).

## 5. Vuelos: reservas
- `POST /api/v1/vuelos/reservas` body `{"turnoId":1,"pesoKg":70,"edad":30}`. `cliente_sub` = `sub` del JWT. DTO de respuesta `ReservaDTO`: `id, turnoId, tipoVuelo, fechaHoraVuelo, estado, pilotoId, pilotoNombre, motivo, pesoKg, edad, creadaEn`.
- **Estados:** `PENDIENTE, CONFIRMADA, CONFIRMACION_PENDIENTE, A_REPROGRAMAR, RECHAZADA, CANCELADA, COMPLETADA`. Transiciones válidas (R8): PENDIENTE→{RECHAZADA, CONFIRMADA, CONFIRMACION_PENDIENTE}; CONFIRMACION_PENDIENTE→{CONFIRMADA, A_REPROGRAMAR, CANCELADA}; CONFIRMADA→{A_REPROGRAMAR, CANCELADA, COMPLETADA}; A_REPROGRAMAR→{CONFIRMADA, CANCELADA}. RECHAZADA, CANCELADA y COMPLETADA son finales. Transición inválida → 422.
- **Orquestación de crear reserva (en este orden):**
  1. Turno inexistente → 404. Validar R1 peso, R2 edad, R3 cupo (reservas activas del turno < cupo; activas = PENDIENTE, CONFIRMADA, CONFIRMACION_PENDIENTE, A_REPROGRAMAR), R7 (el cliente no tiene reserva activa en ese turno), R10 (turno futuro y a ≤16 días). Si falla: **se guarda la reserva RECHAZADA con el motivo y se responde 422** con `{timestamp,status,error,reservaId,estado:"RECHAZADA",motivo}`.
  2. `GET disponibles` a pilotos. Pilotos inalcanzable/5xx/timeout → **503, no se guarda nada**. 404 → guardar RECHAZADA + 422.
  3. Guardar reserva PENDIENTE; `POST asignaciones`. Si falla → borrar la reserva y 503.
  4. `GET veredicto` a clima con los límites del tipo de vuelo y las coordenadas del sitio. APTO/CONDICIONAL → CONFIRMADA (CONDICIONAL con motivo "viento cercano al límite"), 201. NO_APTO → `DELETE asignaciones`, RECHAZADA con motivo de clima, 422. Clima caído (503/timeout) → CONFIRMACION_PENDIENTE, 201, piloto reservado.
- **Revalidación** `POST /api/v1/vuelos/reservas/revalidacion`: toma reservas CONFIRMADA y CONFIRMACION_PENDIENTE con turno futuro (≤16 días), ordenadas por `creadaEn` ascendente (R9). Consulta clima por reserva: NO_APTO → libera piloto y A_REPROGRAMAR; APTO/CONDICIONAL → CONFIRMADA; clima caído → sin cambios. Responde `{"revisadas":n,"confirmadas":n,"aReprogramar":n,"sinCambios":n}`.
- **Reprogramación** `PATCH .../{id}/reprogramacion` body `{"turnoId":2}`: sólo desde A_REPROGRAMAR; repite pasos 1 a 4 sobre la misma reserva con el nuevo turno (R3 excluye a la propia reserva). Éxito → CONFIRMADA. Si falla alguna regla → 422 y la reserva sigue A_REPROGRAMAR. **Cancelación:** libera piloto (DELETE asignación) y cupo. **Finalización:** sólo desde CONFIRMADA.
- Listado admin `GET /reservas?page=&size=` con `Pageable`.

## 6. Datos de prueba (Flyway V2, ids fijos)
**vuelos_db** — sitio 1: "Villa Carlos Paz" (-31.4241, -64.4978). Tipos (codigo, peso_min–max, edad_min, viento_max, margen):
1 `PARAPENTE_TANDEM` 40–110, 12, 25, 10 · 2 `GLOBO` 30–130, 8, 20, 8 · 3 `PRUEBA_APTO` 40–110, 12, 500, 0 · 4 `PRUEBA_NO_APTO` 40–110, 12, 0, 0 · 5 `SIN_PILOTO` 40–110, 12, 500, 0.
Turnos (fecha = `CURRENT_DATE + n días` a las 10:00, n entre 2 y 8): 1 PRUEBA_APTO cupo 10 · 2 PRUEBA_NO_APTO cupo 10 · 3 SIN_PILOTO cupo 10 · 4 PRUEBA_APTO cupo 1 · 5 PARAPENTE_TANDEM cupo 5 · 6 GLOBO cupo 5 · 7 PRUEBA_APTO cupo 10 (otro día, para reprogramar).
**pilotos_db** — pilotos: 1 "Piloto Uno" (`sub` 2222…, certificado PARAPENTE_TANDEM, PRUEBA_APTO, PRUEBA_NO_APTO) · 2 "Piloto Dos" (GLOBO, PRUEBA_APTO) · 3 "Piloto Tres" (PARAPENTE_TANDEM, PRUEBA_APTO). Ninguno certificado en SIN_PILOTO. Máximo 4 vuelos por día por piloto.

## 7. Infraestructura
- `docker-compose.yml`: `postgres:16` (script de init crea `vuelos_db` y `pilotos_db`), `quay.io/keycloak/keycloak:26.0` (`start-dev --import-realm`, `./keycloak` montado en `/opt/keycloak/data/import`, host 8180), `api-gateway`, `pilotos-service`, `vuelos-service`, `clima-service` con `healthcheck` y `depends_on: condition: service_healthy`. Sólo gateway y keycloak publican puertos. Variables desde `.env` (`.env.example` versionado, `.env` ignorado). `JAVA_TOOL_OPTIONS=-Xmx256m` por servicio.
- Un `Dockerfile` multi-stage por servicio: `maven:3.9-eclipse-temurin-21` para compilar y `eclipse-temurin:21-jre` para correr; `.dockerignore`.
- `.gitattributes` con `*.sh text eol=lf`.
