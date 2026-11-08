# C4 nivel 3: Componentes del Servicio de Vuelos

Paquetes en `vuelos-service/src/main/java/com/parapente/vuelos/`, organizados por funcionalidad.

```mermaid
flowchart TB
    gw["API Gateway"]
    subgraph vs["Servicio de Vuelos"]
        subgraph seg["seguridad"]
            sc["SecurityConfig + ConversorRoles<br/>JWT, roles por ruta"]
            ri["ReenvioCabecerasInterceptor<br/>reenvía Authorization y correlation id"]
        end
        subgraph res["reservas"]
            rc["ReservaController<br/>/api/v1/vuelos/reservas"]
            rs["ReservaService<br/>orquesta el proceso"]
            rr["ReglasReserva<br/>R1, R2, R3, R7, R10 (pura)"]
            er["EstadoReserva<br/>transiciones R8 (pura)"]
            rrepo["ReservaRepository"]
        end
        subgraph tu["turnos"]
            tc["TurnoController"] --> ts["TurnoService"]
            ts --> trepo["TurnoRepository / SitioRepository"]
        end
        subgraph tv["tiposvuelo"]
            tvc["TipoVueloController"] --> tvs["TipoVueloService"]
            tvs --> tvrepo["TipoVueloRepository"]
        end
        subgraph cl["clima"]
            cc["ClimaClient"]
        end
        subgraph pi["pilotos"]
            pc["PilotosClient"]
        end
        eh["errores: GlobalExceptionHandler<br/>404, 422, 503"]
    end
    db[("vuelos_db")]
    pil["Servicio de Pilotos"]
    cli["Servicio de Clima"]
    gw --> rc & tc & tvc
    rc --> rs
    rs --> rr & er & rrepo & trepo & pc & cc
    rrepo & trepo & tvrepo --> db
    pc --> ri --> pil
    cc --> ri --> cli
    classDef comp fill:#85bbf0,color:#000,stroke:#5d82a8
    classDef ext fill:#438dd5,color:#fff
    classDef dbc fill:#2e6da4,color:#fff
    class sc,ri,rc,rs,rr,er,rrepo,tc,ts,trepo,tvc,tvs,tvrepo,cc,pc,eh comp
    class gw,pil,cli ext
    class db dbc
```

- `ReservaService` es el único que decide el orden: reglas propias, piloto, clima. No usa `@Transactional` en `crear` a propósito, para que una reserva RECHAZADA quede guardada aunque se lance el 422 y para no sostener una conexión durante llamadas HTTP.
- `ReglasReserva` y `EstadoReserva` son puras (sin Spring ni base), por eso se prueban con tests unitarios simples.
- `PilotosClient` y `ClimaClient` traducen las fallas HTTP a excepciones propias: sin piloto = `PilotoNoDisponibleException` (regla), pilotos caído = `DependenciaNoDisponibleException` (503), clima caído = `ClimaNoDisponibleException` (no confirma).
