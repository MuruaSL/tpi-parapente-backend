# TPI 2026: Reservas de vuelos en parapente y globo

Trabajo Práctico Integrador de Backend de Aplicaciones, UTN FRC.

## Qué es
Backend de microservicios que confirma una reserva de vuelo (parapente o globo) sólo si hay un piloto certificado disponible y el clima lo permite. Alcance: [propuesta-tpi-parapente.md](propuesta-tpi-parapente.md).

## Arquitectura
- **api-gateway** (8080): única entrada pública; valida el JWT y enruta.
- **vuelos-service** (8082): turnos, tipos de vuelo y reservas; orquesta a los otros dos.
- **pilotos-service** (8081): pilotos, certificaciones y asignaciones.
- **clima-service** (8083): consulta Open-Meteo y devuelve el veredicto APTO / CONDICIONAL / NO_APTO.
- **keycloak** (8180) para login y roles, y **postgres 16** con `vuelos_db` y `pilotos_db`.

Detalle técnico (rutas, roles, reglas): [docs/contrato-tecnico.md](docs/contrato-tecnico.md).

## Requisitos
- Docker Desktop (Windows, Mac o Linux). Alcanza para correr todo.
- JDK 21 y Maven 3.9 sólo si se quiere desarrollar o correr tests fuera de Docker.

## Arranque
```bash
cp .env.example .env
docker compose up --build
```
La primera vez tarda unos minutos (compila los 4 servicios dentro de Docker). Para empezar de cero (borra la base): `docker compose down -v`.

## URLs
- Swagger: http://localhost:8080/swagger-ui.html
- Keycloak (consola admin): http://localhost:8180 (usuario y clave en `.env`)

## Usuarios de prueba
| Usuario | Clave | Rol |
|---|---|---|
| `cliente1` | `Cliente123!` | CLIENTE |
| `piloto1` | `Piloto123!` | PILOTO |
| `admin1` | `Admin123!` | ADMIN |

### Cambiar de usuario en Swagger
Keycloak mantiene la sesión abierta y por eso el login sale con el usuario anterior. Para entrar con otro:
abrir http://localhost:8180/realms/parapente/protocol/openid-connect/logout, confirmar el "Logout" y volver a apretar Authorize en Swagger. Alternativa: una ventana de incógnito por usuario.

## Tests
Sin Docker, por servicio:
```bash
cd vuelos-service && mvn test
```
(igual para `pilotos-service`, `clima-service` y `api-gateway`).

## Notas para Windows
- Docker Desktop con backend **WSL2** activado.
- Fin de línea: `.gitattributes` fuerza LF en los `.sh`; si igual falla el script de Postgres, revisá que `infra/postgres/init/01-crear-bases.sh` no esté en CRLF.
- Memoria: cada servicio Java usa hasta 256 MB de heap. Conviene dar a Docker Desktop 4 GB o más (`.wslconfig`).

## Estructura
```
api-gateway/  pilotos-service/  vuelos-service/  clima-service/   (un Dockerfile cada uno)
keycloak/     realm-parapente.json (usuarios y roles precargados)
infra/postgres/init/   script que crea las bases
docs/         contrato técnico y colecciones de Postman
docker-compose.yml  .env.example
```

## Decisiones y límites conocidos
- Los servicios internos confían en el token del usuario que reenvía el gateway; el aislamiento lo da la red de Docker (sólo el gateway publica puertos).
- Keycloak corre en modo desarrollo (`start-dev`, sin TLS) y las contraseñas de prueba son sólo para demo.
- El issuer del token no se valida: el host de Keycloak difiere dentro (`keycloak:8080`) y fuera de Docker (`localhost:8180`).
- Cambiar la clave de admin de Keycloak: editar `KEYCLOAK_ADMIN_PASSWORD` en `.env` y recrear el contenedor con `docker compose down -v && docker compose up --build` (Keycloak corre en modo desarrollo y guarda sus datos dentro del contenedor, así que la clave se aplica al crearlo), o cambiarla desde la consola admin.
- Los turnos de prueba se calculan al migrar: si pasan más de 2 días hay que recrear el volumen con `docker compose down -v`.
- Los vuelos `PRUEBA_APTO` y `PRUEBA_NO_APTO` dependen del clima real (lluvia ≥60% los vuelve NO_APTO, o viento exactamente 0); si algo da distinto, repetir la prueba.
- Docker aún no fue probado por el grupo: en el primer arranque seguí la checklist de abajo.

### Checklist del primer `docker compose up`
1. `cp .env.example .env`
2. Liberar los puertos 8080 y 8180.
3. Darle 4 GB de RAM a Docker.
4. Si hubo un intento previo: `docker compose down -v`.
5. `docker compose up --build` y esperar a que todo quede healthy.
6. `curl http://localhost:8080/actuator/health` debe responder UP.
7. Abrir Swagger (http://localhost:8080/swagger-ui.html) y pedir un token con un usuario de prueba.
