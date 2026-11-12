# Las pruebas de Postman, explicadas

Hay dos colecciones en `docs/postman/`. Las dos usan el environment **Parapente - Local**. Este documento explica para qué sirve cada carpeta y cada consulta, para poder hablar de ellas y tocarlas sin perderse.

## 1. Cómo funciona una consulta

Cada consulta tiene tres partes:

- **El pedido.** Un método (GET, POST, PUT, PATCH), una dirección y, si hace falta, un cuerpo JSON. Por ejemplo, `POST /api/v1/vuelos/reservas` con `{"turnoId": 1, "pesoKg": 70, "edad": 30}`.
- **El usuario.** Cada pedido lleva un token en la cabecera `Authorization`. Se guarda en variables: `tokenCliente`, `tokenPiloto` y `tokenAdmin`.
- **Las verificaciones.** Son las pestañas "Tests" del pedido. Cada `pm.test(...)` compara la respuesta con lo esperado (código, estado, motivo). Esas son las marcas verdes y rojas del Runner.

Las direcciones usan variables entre llaves dobles, por ejemplo `{{baseUrl}}`. Se definen en el environment:

| Variable | Valor | Para qué |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | El gateway, la puerta de entrada |
| `kcUrl` | `http://localhost:8180` | Keycloak, donde se pide el token |
| `turnoApto` ... `turnoReprogramar` | 1 a 7 | Los turnos sembrados en la base |
| `reservaId` y similares | vacías | Los scripts las llenan solos con la reserva que se crea |

**Los turnos de prueba:**

| Variable | Turno | Tipo de vuelo | Qué se prueba |
|---|---|---|---|
| `turnoApto` | 1 | PRUEBA_APTO | El caso feliz, sin límite de viento |
| `turnoNoApto` | 2 | PRUEBA_NO_APTO | El clima lo rechaza siempre |
| `turnoSinPiloto` | 3 | SIN_PILOTO | No hay piloto certificado |
| `turnoCupo1` | 4 | PRUEBA_APTO | Cupo de una sola persona |
| `turnoTandem` / `turnoGlobo` | 5 / 6 | Tándem y globo | Tipos reales |
| `turnoReprogramar` | 7 | PRUEBA_APTO | Destino de la reprogramación |

## 2. Qué verifican las reglas

Los nombres R1 a R7 son las reglas de negocio de la reserva:

| Regla | Qué exige | Ejemplo |
|---|---|---|
| R1 | Peso dentro del rango del tipo de vuelo | 200 kg se rechaza |
| R2 | Edad mínima del tipo de vuelo | 10 años se rechaza |
| R3 | Hay cupo en el turno | El turno 4 tiene un solo lugar |
| R4 | Hay un piloto disponible | El turno 3 no tiene |
| R7 | No tener ya una reserva activa en ese turno | Reservar dos veces el turno 1 |

Un 422 **no** es una falla del sistema: es una regla aplicándose bien. La reserva queda guardada como RECHAZADA con su motivo.

## 3. Colección "Parapente - Flujo completo"

Cuenta el recorrido de una reserva de principio a fin. **Se corre completa y en orden**, con la base recién sembrada.

### 0 Auth
Pide un token a Keycloak para `cliente1`, `piloto1` y `admin1` y lo guarda en una variable. Sin esto el resto da 401. Verifica solo que el token se haya pedido bien.

### 1 Turnos y flujo feliz
El camino sin problemas, con `cliente1`:
- **Listar turnos** y **detalle del turno 1:** ver qué se puede reservar.
- **Tipos de vuelo:** guarda el nombre y el id del tipo PRUEBA_APTO, que se usa más adelante.
- **Reservar turno 1:** debe dar **201 CONFIRMADA**. Es el caso central, y recorre las reglas, el piloto y el clima.
- **Mis reservas** y **detalle de la reserva propia:** la reserva aparece y está CONFIRMADA.
- **Agenda del piloto1:** con el token del piloto, `GET /pilotos/mi-agenda` devuelve una lista.

### 2 Reglas de negocio
Los rechazos, uno por regla. Todos esperan **422 RECHAZADA** y miran que el motivo mencione lo que corresponde:
- **R1** peso 200 kg, **R2** edad 10, **R7** reservar de nuevo el turno 1.
- **R3** en dos pasos: `cliente1` ocupa el único lugar del turno 4 (201) y después `admin1` intenta reservar y recibe "turno completo".
- **R4** turno 3, sin piloto certificado.

### 3 Clima y revalidación
Es la parte más rica del dominio:
- **Clima no apto (turno 2):** 422 RECHAZADA por el pronóstico.
- **Revalidar (admin):** el administrador pide re-chequear el clima de las reservas confirmadas. Con clima apto no cambia nada.
- **Bajar viento de PRUEBA_APTO a 0:** el admin edita el tipo de vuelo para que cualquier viento sea un problema.
- **Revalidar de nuevo:** ahora las reservas pasan a **A_REPROGRAMAR** y se libera al piloto.
- **Restaurar el viento:** deja el tipo como estaba.

### 4 Reprogramación y cancelación
- **Reprogramar la reserva 1 al turno 7:** pasa de A_REPROGRAMAR a CONFIRMADA en otro día.
- **Cancelar la reserva 1** y **cancelar la del turno 4:** pasan a CANCELADA y liberan el lugar y el piloto.
- **Listado completo paginado (admin):** `?page=0&size=5`, solo para el administrador.

**Ojo con el clima real.** Open-Meteo da el pronóstico verdadero. Si el turno 4 cae en un día de lluvia, R3 y la cancelación del turno 4 fallan. Eso no es un error del código.

## 4. Colección "Parapente - Seguridad y errores"

No cuenta un recorrido. Prueba que el sistema **se defienda bien**. Se corren las carpetas 0 a 3.

### 0 Auth
Igual que en la otra colección.

### 1 Autenticación y roles
Quién puede hacer qué:
- **Sin token** o **token inválido:** 401 (no estás identificado).
- **`cliente1` en el listado completo de reservas:** 403 (estás identificado, pero no te corresponde).
- **`cliente1` crea turnos, revalida, lista pilotos o consulta clima:** todo 403.
- **`piloto1` lista pilotos:** 403. Solo puede ver su propia agenda.
- **`admin1` lista pilotos:** 200.
- **`cliente1` pide `mi-agenda`:** 403, porque es del piloto.
- **`admin1` veredicto de clima:** 200, o 503 si Open-Meteo no responde.

### 2 Reserva ajena
Que un cliente no vea lo de otro:
- `admin1` crea una reserva y se guarda su id.
- **`cliente1` la pide o la cancela:** 404. El sistema responde "no existe" y no "no tenés permiso", para no revelar que existe.
- **Reserva inexistente:** 404.
- **Cuerpo vacío:** 400, con el detalle de qué campos faltan.
- **Turno inexistente:** 404.
- **Limpieza:** el admin cancela la reserva de prueba.

### 3 Endpoints internos no expuestos
Los servicios se hablan por rutas `/api/v1/internos/...`. Estas pruebas confirman que **desde afuera no se llega**: dan 401, 403 o 404, aun con un token de admin.

### 4 Integración (manual)
Requieren **apagar un servicio** a propósito, desde Docker Desktop. Por eso no se corren con el resto.

| Prueba | Con qué apagado | Qué espera |
|---|---|---|
| V7 | `pilotos-service` | 503 "Servicio de pilotos no disponible", sin guardar nada |
| V7b | `pilotos-service` | La lista de reservas no suma ninguna nueva |
| V5 | `clima-service` | 201 CONFIRMACION_PENDIENTE (se guarda y se reserva al piloto) |
| V5b | `clima-service` | Revalidar da `sinCambios` y no falla |
| V5c | clima ya encendido | Revalidar confirma la reserva pendiente |
| V5d | clima ya encendido | La reserva figura CONFIRMADA |

Pasos: apagar el servicio, ejecutar V7 y V7b o V5 y V5b, encender, esperar a que esté healthy, y después V5c y V5d.

V7 usa el turno 1 y V5 el turno 7. Si `cliente1` ya tiene una reserva activa en ese turno, la respuesta es 422 "ya reservó" y no llega a probar nada. En ese caso hay que resetear la base o cambiar el turno del pedido.

## 5. Códigos de respuesta

| Código | Significado en este proyecto |
|---|---|
| 200, 201 | Salió bien |
| 400 | El cuerpo del pedido es inválido (faltan campos) |
| 401 | No estás identificado, o el token venció |
| 403 | Estás identificado, pero tu rol no puede hacerlo |
| 404 | No existe, o es de otro usuario |
| 422 | Una regla de negocio lo rechaza |
| 503 | Cayó un servicio del que depende |

## 6. Qué conviene saber para cuando el profesor "toque las consultas"

- **Cambiar un valor y ver qué pasa.** Es lo más probable: probar otro peso, otra edad o un turno distinto. Cada cambio tiene que dar el código que corresponde a la regla.
- **Cambiar de usuario.** Se cambia con la variable del token en la cabecera o en la pestaña Authorization. El mismo pedido con otro rol da otro código.
- **Repetir una reserva.** Da 422 por R7. Para volver a empezar, se resetea la base con `docker compose down -v && docker compose up -d`.
- **Apagar un servicio y volver a probar.** Es el 503 de pilotos o el CONFIRMACION_PENDIENTE de clima.
- **Un resultado distinto al esperado no siempre es un error.** Puede ser el clima real del día o una reserva previa. Se resuelve leyendo el motivo que trae la respuesta.

Para preparar la demo, conviene correr una vez antes de la exposición con la base reseteada, y chequear que el turno 4 no caiga en un día de lluvia.
