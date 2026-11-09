# Guía del equipo: cómo levantar, probar, leer y defender el TPI

Documento de entrada para los cinco integrantes. Si sólo leés una cosa, leé esta. Del detalle se encargan los otros archivos de `docs/`, que se enlazan en cada sección.

## 1. Qué es, en un minuto

Un backend de microservicios para **reservar vuelos en parapente y globo**. Una reserva sólo se confirma si se cumplen tres cosas, y se evalúan en este orden:

1. **El pasajero cumple los requisitos** del tipo de vuelo (peso, edad, cupo del turno, no reservó antes el mismo turno).
2. **Hay un piloto certificado y libre** para ese tipo de vuelo y esa hora.
3. **El pronóstico lo permite** (viento y lluvia, datos reales de Open-Meteo).

Después de confirmada, un administrador puede **revalidar el clima**: lo que dejó de ser viable se libera y queda para reprogramar.

```
                    ┌──────────────┐
 Swagger/Postman ──▶│ api-gateway  │ 8080 (única entrada pública, valida el JWT)
                    └──────┬───────┘
          ┌────────────────┼─────────────────┐
          ▼                ▼                 ▼
   vuelos-service    pilotos-service    clima-service ──▶ Open-Meteo (externo)
   8082 (orquesta)   8081               8083 (sin base)
      │ vuelos_db        │ pilotos_db
      └───────── PostgreSQL ─────────┘          Keycloak 8180: login y roles
```

Vuelos es el **orquestador**: recibe la reserva y llama a Pilotos y a Clima. Pilotos y Clima no se conocen entre sí. Diagramas formales (C4, secuencia, estados): [`docs/arquitectura/`](arquitectura/).

## 2. Cómo levantarlo

Necesitás **Docker Desktop abierto** (nada más; Java y Maven se usan dentro de Docker).

```bash
cp .env.example .env
docker compose up --build
```

La primera vez tarda varios minutos (descarga imágenes y compila los 4 servicios). Cuando todo está `healthy`:

```bash
curl http://localhost:8080/actuator/health     # {"status":"UP"}
```

| Qué | Dónde |
|---|---|
| Swagger (los 3 servicios, selector arriba a la derecha) | http://localhost:8080/swagger-ui.html |
| Consola de Keycloak | http://localhost:8180 (usuario y clave en `.env`) |

**Usuarios de prueba**

| Usuario | Clave | Rol | Puede |
|---|---|---|---|
| `cliente1` | `Cliente123!` | CLIENTE | ver turnos, reservar, ver y cancelar **sus** reservas |
| `piloto1` | `Piloto123!` | PILOTO | ver su agenda |
| `admin1` | `Admin123!` | ADMIN | todo lo anterior más ver todas las reservas, revalidar, finalizar, administrar turnos/tipos/pilotos |

**Comandos útiles**

| Para | Comando |
|---|---|
| Empezar de cero (borra la base) | `docker compose down -v` y de nuevo `docker compose up --build` |
| Apagar sin borrar nada | `docker compose stop` |
| Volver a encender | `docker compose up -d` |
| Ver logs de un servicio | `docker compose logs -f vuelos-service` |
| Correr los tests (sin Docker) | `cd vuelos-service && mvn test` (igual en los otros 3) |

Cosas que conviene saber antes de la defensa:
- Si cerrás Docker Desktop, los contenedores se apagan. Al abrirlo, `docker compose up -d` los recupera con los datos.
- Los **turnos de prueba se calculan al migrar**: pasados ~2 días hay que hacer `down -v` y levantar de nuevo, o no habrá turnos futuros.
- Los vuelos `PRUEBA_APTO` y `PRUEBA_NO_APTO` dependen del clima real (lluvia ≥ 60 % o viento fuerte los vuelve NO_APTO). Si da distinto de lo esperado, repetir.
- **Cambiar de usuario en Swagger:** Keycloak guarda la sesión. Abrí `http://localhost:8180/realms/parapente/protocol/openid-connect/logout`, confirmá "Logout" y volvé a apretar Authorize. O usá una ventana de incógnito por usuario.

## 3. Cómo probarlo (recorrido para mostrarlo)

Todo desde Swagger, logueado como `cliente1` salvo que se indique otra cosa. Los turnos de prueba están cargados:

| Turno | Tipo | Para qué sirve |
|---|---|---|
| 1, 4, 7 | `PRUEBA_APTO` | reserva feliz: queda CONFIRMADA (el 4 tiene cupo 1, sirve para probar "turno completo") |
| 2 | `PRUEBA_NO_APTO` | clima no apto: queda RECHAZADA con el motivo |
| 3 | `SIN_PILOTO` | nadie certificado: RECHAZADA "sin piloto certificado" |
| 5 | `PARAPENTE_TANDEM` | con peso 300 → RECHAZADA "peso fuera de rango" |
| 6 | `GLOBO` | tipo de vuelo distinto, con otro piloto |

Cuerpo de la reserva: `POST /api/v1/vuelos/reservas` con `{"turnoId":1,"pesoKg":70,"edad":30}`. Una reserva por usuario y turno (R7).

**Lo que ya se probó contra el stack real** (Docker, PostgreSQL, Keycloak y Open-Meteo reales, no mocks):

| Escenario | Resultado verificado |
|---|---|
| Reserva con clima apto | 201 CONFIRMADA, piloto asignado |
| Doble reserva del mismo turno | 422 "ya reservó este turno" |
| Peso fuera de rango | 422 "peso fuera de rango" |
| Sin piloto certificado | 422 "sin piloto certificado" |
| Clima no apto (dato real de viento) | 422 "clima no apto: Viento excede el límite..." |
| Turno completo | 422 "turno completo" |
| **Clima-service detenido** | 201 **CONFIRMACION_PENDIENTE** con el piloto reservado |
| **Pilotos-service detenido** | **503** "Servicio de pilotos no disponible", sin guardar nada |
| Revalidación como admin | 200, resumen `{revisadas, confirmadas, aReprogramar, sinCambios}` |
| Cancelar una reserva | 200 CANCELADA, el piloto queda libre |
| Sin token / rol incorrecto | 401 / 403 |
| Cliente ve todas las reservas | 403 (sólo ADMIN) |
| Ruta interna `/api/v1/internos/**` desde afuera | 404 (el gateway no la enruta) |
| Login de Swagger con Keycloak (PKCE) | funciona |

## 4. Cómo leer el código

Estructura **por funcionalidad** ("Screaming Architecture"): las carpetas dicen qué hace el sistema, no qué tipo de clase es. Ejemplo en `vuelos-service/src/main/java/com/parapente/vuelos/`:

```
reservas/    ← el corazón: ReservaService, ReglasReserva, EstadoReserva, Reserva, controller
turnos/      tiposvuelo/      ← catálogo (administrado por ADMIN)
pilotos/     clima/           ← clientes HTTP que llaman a los otros servicios
seguridad/   errores/   config/
```

**Orden de lectura recomendado (de lo más importante a lo accesorio):**

1. [`ReservaService.crear`](../vuelos-service/src/main/java/com/parapente/vuelos/reservas/ReservaService.java): el proceso central. Orden fijo: reglas propias → buscar piloto → guardar y asignar → consultar clima → decidir (y deshacer si algo falla). Casi todo lo que van a preguntar sale de acá.
2. [`ReglasReserva`](../vuelos-service/src/main/java/com/parapente/vuelos/reservas/ReglasReserva.java) y [`EstadoReserva`](../vuelos-service/src/main/java/com/parapente/vuelos/reservas/EstadoReserva.java): reglas R1, R2, R3, R7, R10 y las transiciones válidas (R8). Son clases **puras** (sin Spring ni base), por eso se testean fácil.
3. [`ReglaClima`](../clima-service/src/main/java/com/parapente/clima/veredicto/ReglaClima.java): regla R6 (APTO / CONDICIONAL / NO_APTO).
4. [`ReglasAsignacion`](../pilotos-service/src/main/java/com/parapente/pilotos/asignaciones/ReglasAsignacion.java): certificación (R4) y disponibilidad (R5, máx. 4 vuelos por día).
5. `PilotosClient` y `ClimaClient` (en Vuelos): cómo se llama a otro servicio y cómo se traducen sus fallas.
6. `SecurityConfig` del gateway y de cada servicio, más `ConversorRoles`: de dónde salen los roles del token.
7. `application.yaml` del gateway: las rutas (`/api/v1/<servicio>/**`) y la agregación de Swagger.
8. `docker-compose.yml`, `keycloak/realm-parapente.json` y las migraciones Flyway (`src/main/resources/db/migration`): infraestructura y datos de prueba.

Mapa regla → clase → test: [`docs/reglas.md`](reglas.md). Endpoints, roles y contratos entre servicios: [`docs/contrato-tecnico.md`](contrato-tecnico.md).

**Ideas que hay que poder explicar con tus palabras:**
- *Orquestación:* Vuelos manda; si Pilotos falla antes de guardar → 503 y nada queda a medias; si el clima no responde → CONFIRMACION_PENDIENTE (nunca se confirma sin pronóstico).
- *Marcha atrás simple:* si falla algo después de reservar al piloto, se lo libera (sin sagas ni transacciones distribuidas).
- *Seguridad en dos capas:* el gateway valida el JWT, y cada servicio vuelve a validarlo. El rol sale de `realm_access.roles`; la propiedad de una reserva, del `sub` del token (una reserva ajena responde 404).
- *Por qué Keycloak:* usuarios, contraseñas (hasheadas), roles y tokens estándar sin escribirlo nosotros.

## 5. Estado frente a la consigna

La rúbrica de corrección tiene 12 criterios y 105 puntos (los vimos al armar el plan). Estado hoy, sin maquillar:

| Criterio | Pts | Estado | Qué hay / qué falta |
|---|---|---|---|
| NEG-01 Proceso de negocio de punta a punta | 15 | **Listo y verificado** | Reserva, rechazos, clima caído, pilotos caído, cancelación y revalidación probados contra el stack real. |
| ARQ-01 Arquitectura | 12 | **Listo** | Gateway + 3 servicios, una base por servicio, estructura por funcionalidad, C4 y secuencias en `docs/arquitectura/`. |
| INT-01 Integración entre servicios y externa | 12 | **Listo y verificado** | Vuelos→Pilotos, Vuelos→Clima, Clima→Open-Meteo (datos reales). |
| NEG-02 Reglas de negocio | 10 | **Listo** | R1 a R10, cada una con su clase y test (`docs/reglas.md`). |
| TEC-01 Calidad técnica | 10 | **Listo con huecos** | 104 tests verdes, errores con formato único, Swagger, código comentado. Hueco: no hay tests de lógica para turnos, tipos de vuelo, CRUD de pilotos ni contenido de la agenda. |
| INF-01 Persistencia y despliegue | 10 | **Listo; verificado sólo en una Mac** | PostgreSQL + Flyway + Docker Compose funcionando con un comando. **Falta probarlo en Windows/Linux** de otros integrantes. |
| SEG-01 Seguridad | 8 | **Listo y verificado** | Keycloak con 3 roles, 401/403, propiedad de reservas, login real desde Swagger. |
| ALC-01 Alcance comprometido (cuestionario) | 8 | **Casi** | Todo lo comprometido está implementado. Diferencia conocida: reprogramar con el clima caído responde 503 en vez de dejar CONFIRMACION_PENDIENTE. Revisar el cuestionario enviado contra esto antes de la fecha. |
| DES-01 Desafío asumido | 5 | **Listo** | [`docs/desafio.md`](desafio.md): desafíos, decisiones comparadas y límites. |
| API-01 Colecciones de Postman | 5 | **Armadas, sin ejecutar** | Hay 2 colecciones + environment en `docs/postman/`. **Nunca se corrieron**: puede haber scripts que fallen. |
| PRE-01 Presentación y defensa | 5 | **Material listo, falta ensayar** | Guion de 10 minutos y 12 preguntas en [`docs/guia-defensa.md`](guia-defensa.md). |
| DOC-01 Apreciación docente | 5 | Depende del docente | Prolijidad, README, que todos sepan explicarlo. |

Extras que **no** están (decisión del grupo): Prometheus y Grafana. Se sacaron "por ahora".

## 6. Pruebas que faltan (en orden de importancia)

**A. Lo que más riesgo tiene: nunca se ejecutó**
1. **Colecciones de Postman** (API-01). Importar las dos colecciones y el environment de `docs/postman/`, correrlas con el Runner y anotar qué falla. Instrucciones en [`docs/postman/README.md`](postman/README.md). **Ojo:** el flujo sólo funciona sobre datos recién sembrados (cambia cupos y reservas, y no se puede repetir sobre la misma base), así que antes de correrlo hay que hacer `docker compose down -v && docker compose up -d`. La base de esta Mac ya tiene reservas de las pruebas manuales.
2. **Levantarlo en Windows** (y en cualquier otra PC del grupo): clonar el repo desde cero, `cp .env.example .env` (o `copy`), `docker compose up --build`. Vigilar el aviso de saltos de línea del script `infra/postgres/init/01-crear-bases.sh`. Cada integrante debería hacerlo una vez.

**B. Funciones que no se probaron a mano contra el stack real**
3. **Roles en Swagger**: entrar como `piloto1` (agenda 200; reservar debe dar 403) y como `admin1` (listar todas las reservas, revalidar).
4. **Finalizar una reserva** (`PATCH /reservas/{id}/finalizacion`, admin): CONFIRMADA → COMPLETADA; y probar una transición inválida (completar una CANCELADA debe dar 422).
5. **Reprogramación** (`PATCH /reservas/{id}/reprogramacion`): hace falta una reserva en A_REPROGRAMAR. Con clima real es difícil provocarla a demanda; está cubierta por tests unitarios. Pensar cómo mostrarla (o decir claramente que se demuestra con el test).
6. **Administración como admin**: crear un turno, un tipo de vuelo, un piloto y una certificación (son las funciones sin tests automáticos).
7. **Ver los estados en el momento**: pasar un turno de `PRUEBA_NO_APTO` y reservar, y detener `clima-service` para ver CONFIRMACION_PENDIENTE (así se hace la demo: `docker compose stop clima-service`).

**C. Antes de la entrega**
8. **Empezar de cero**: `docker compose down -v` + `up --build`, para tener turnos futuros frescos el día de la defensa.
9. **`mvn test` en los 4 módulos** en la PC donde se va a mostrar.
10. **Ensayar el guion de 10 minutos** con todos, y que cada uno responda 3 preguntas de la guía al azar (todos tienen que poder explicar todo; no hay reparto por piezas).
11. **Releer el cuestionario enviado** contra el estado real (sección 5, ALC-01).

**D. Opcional si sobra tiempo (mejora TEC-01)**
12. Tests de lógica para `TurnoService`, `TipoVueloService`, `PilotoService`, el contenido de `mi-agenda` y `finalizar`.
13. Hacer atómico el control de cupo (hoy dos reservas simultáneas al último lugar podrían pasar; está marcado en el código).

## 7. Límites conocidos (decirlos en la defensa, no esconderlos)

- El cupo y la doble reserva no son atómicos ante concurrencia.
- El `issuer` del token no se valida (el host de Keycloak difiere dentro y fuera de Docker); se valida firma y expiración.
- Los servicios internos confían en el token que reenvía el gateway; el aislamiento lo da la red de Docker.
- Keycloak corre en modo desarrollo (`start-dev`, sin TLS) y las claves de usuarios de prueba son sólo para demo.
- La revalidación la dispara un administrador; no hay tarea programada automática.
- No hay pruebas de integración automáticas contra PostgreSQL o Keycloak; esas se hicieron a mano (sección 3).

## 8. Plan sugerido para repartirse la lectura

Como todos defienden todo, la idea no es dividir la defensa sino **leer en paralelo y contárselo al resto**:

| Quién | Lee y explica al grupo |
|---|---|
| Integrante 1 | Secciones 1 y 2 + `docker-compose.yml` y Keycloak (infraestructura y arranque) |
| Integrante 2 | `ReservaService.crear` y estados (el proceso central) |
| Integrante 3 | Reglas R1–R10 (`docs/reglas.md`) y sus tests |
| Integrante 4 | Pilotos y Clima: `ReglasAsignacion`, `ReglaClima`, Open-Meteo |
| Integrante 5 | Seguridad: gateway, `ConversorRoles`, roles y JWT |

Luego se rotan los temas. Cierra el grupo con el ensayo de la defensa.
