# Propuesta de TPI 2026: Reservas de vuelos en parapente y globo

Backend de Aplicaciones · UTN FRC

Integrantes: (completar)

## 1. El problema

Una empresa de vuelos recreativos (parapente y globo aerostático) recibe reservas de clientes. Hoy una reserva puede confirmarse sin saber si hay un piloto habilitado para ese vuelo ni si el viento permite volar, y recién se descubre el problema el día del vuelo.

Queremos un backend que **sólo confirme un vuelo cuando haya un piloto habilitado y las condiciones del clima lo permitan**, y que reaccione si esas condiciones cambian antes de la fecha.

## 2. Qué resuelve el sistema

- El cliente reserva un turno indicando su peso y su edad.
- El sistema verifica los requisitos del vuelo, busca un piloto certificado y disponible, y consulta el pronóstico del lugar de despegue.
- Según el resultado, la reserva se confirma, queda pendiente o se rechaza.
- Antes del vuelo, el sistema vuelve a revisar el clima: si empeora, la reserva se reprograma o se cancela y el piloto queda libre.

## 3. Proceso central

El recorrido principal, de punta a punta, es la **reserva de un vuelo**:

1. El cliente solicita una reserva para un turno.
2. Se validan los requisitos del pasajero y la disponibilidad del turno. Si no cumple, se rechaza con el motivo.
3. Se busca un piloto con la certificación del tipo de vuelo y sin otro vuelo en ese horario. Si no hay, se rechaza.
4. Se consulta el pronóstico para el lugar y la hora del vuelo:
   - **Apto:** la reserva queda confirmada.
   - **Condicional** (viento cerca del límite): queda confirmada con un aviso.
   - **No apto:** se libera al piloto y se propone reprogramar o se cancela.
   - **Clima no disponible:** la reserva queda pendiente de confirmación. Nunca se confirma sin saber el clima.
5. Antes de la fecha, una revalidación vuelve a consultar el clima de las reservas confirmadas y resuelve las pendientes.

**Estados de una reserva:** pendiente, confirmada, confirmación pendiente, rechazada, reprogramada, cancelada y completada.

## 4. Reglas de negocio

| N° | Regla |
|---|---|
| R1 | El peso del pasajero debe estar entre el mínimo y el máximo del tipo de vuelo. |
| R2 | El pasajero debe cumplir la edad mínima del tipo de vuelo. |
| R3 | No se puede superar el cupo del turno. |
| R4 | El piloto debe tener la certificación del tipo de vuelo. |
| R5 | Un piloto no puede tener dos vuelos a la misma hora ni más de un máximo de vuelos por día. |
| R6 | El clima es apto si el viento y las ráfagas están por debajo del límite del vuelo, condicional si están cerca del límite y no apto si lo superan o hay probabilidad de lluvia. |
| R7 | Un cliente no puede reservar dos veces el mismo turno. |
| R8 | Una reserva sólo puede cambiar de estado por caminos válidos (por ejemplo, una reserva cancelada no puede completarse). |
| R9 | Al reprogramar, tienen prioridad las reservas más antiguas. |

## 5. Microservicios y responsabilidades

| Componente | Responsabilidad |
|---|---|
| **API Gateway** | Único punto de entrada al sistema. Recibe las solicitudes, controla el acceso y las envía al servicio correspondiente. |
| **Servicio de Vuelos** | Tipos de vuelo, turnos y reservas. Es el responsable del proceso de reserva y coordina a los otros dos servicios. |
| **Servicio de Pilotos** | Pilotos, certificaciones y agenda. Decide quién puede volar qué tipo de vuelo y en qué horario. |
| **Servicio de Clima** | Consulta el pronóstico y responde si el vuelo es apto, condicional o no apto. No guarda datos propios. |

Cada servicio tiene su propia base de datos (excepto Clima) y no accede a la de los demás: la información se intercambia sólo a través de sus servicios.

## 6. Cómo colaboran los servicios

- **Vuelos → Pilotos:** busca un piloto disponible y certificado, y reserva su agenda. Si el proceso falla más adelante, le pide liberar esa reserva.
- **Vuelos → Clima:** pide el veredicto del clima para el lugar y el momento del vuelo, indicando los límites de viento del tipo de vuelo.
- **Clima → Open-Meteo (servicio externo):** obtiene el pronóstico real.

Si algún servicio no responde, la reserva queda en un estado controlado con un mensaje claro y nunca queda a medias.

## 7. Integración externa

Se utiliza **Open-Meteo**, un servicio público y gratuito de pronóstico, para conocer el viento, las ráfagas y la probabilidad de lluvia en el lugar y la hora del vuelo. Es el dato que decide si una reserva se confirma, se reprograma o se cancela.

## 8. Seguridad: roles y permisos

El acceso se gestiona con **Keycloak**, con tres roles:

| Acción | Cliente | Piloto | Administrador |
|---|---|---|---|
| Ver turnos disponibles | Sí | Sí | Sí |
| Crear y ver sus propias reservas | Sí | No | Sí |
| Ver todas las reservas | No | No | Sí |
| Ver su agenda de vuelos | No | Sí | Sí |
| Administrar turnos, tipos de vuelo y pilotos | No | No | Sí |
| Ejecutar la revalidación del clima | No | No | Sí |

Sin credenciales válidas, el sistema rechaza la solicitud. Con un rol que no corresponde, también.

## 9. Persistencia y despliegue

- Datos guardados en bases PostgreSQL, una por servicio.
- Todo el sistema se levanta con un solo comando (Docker Compose), con datos de prueba incluidos.
- Documentación de los servicios con Swagger.
- El gateway es el único servicio accesible desde afuera.

## 10. Decisiones de diseño

- **Tres servicios, con responsabilidades claras:** Pilotos tiene reglas y datos propios; Clima aísla la dependencia externa, de modo que cambiar de proveedor no afecta a Vuelos.
- **Un gateway como entrada única:** concentra el control de acceso y evita exponer los servicios internos.
- **Nunca confirmar sin clima:** ante una falla del servicio externo, la reserva queda pendiente en lugar de aprobarse a ciegas.
- **Marcha atrás simple:** si un paso falla después de reservar al piloto, el sistema lo libera, sin mecanismos complejos.
- **Código organizado por funcionalidad** (reservas, turnos, clima, pilotos) y comentado, para que cualquier integrante pueda explicarlo.

## 11. Principales desafíos

- Coordinar tres recursos independientes (piloto, turno y clima) en una única reserva consistente.
- Mantener la validez de una reserva a lo largo del tiempo, porque el clima puede cambiar después de confirmarla.
- Manejar con claridad las fallas de los servicios internos y del servicio externo.
- Aplicar control de acceso por rol en todos los puntos de entrada.

## 12. Alcance y entregables

- Tres servicios y un gateway funcionando, con el proceso de reserva completo y la revalidación del clima.
- Colecciones de Postman con el recorrido principal, los casos de error y las pruebas de seguridad.
- Pruebas automáticas de las reglas de negocio, del control de acceso y de las integraciones.
- Diagramas de arquitectura (modelo C4) y de secuencia.
- README con las instrucciones para ejecutar el sistema desde cero.

Todo lo anterior forma el compromiso de alcance. Cualquier cambio significativo se comunicará al equipo docente.
