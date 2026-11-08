# C4 nivel 2: Contenedores

```mermaid
flowchart TB
    u["<b>Cliente, Piloto y Administrador</b><br/>[Personas]<br/>Swagger o Postman"]
    subgraph plat["Plataforma de Vuelos"]
        gw["<b>API Gateway</b><br/>[Spring Cloud Gateway :8080]<br/>Único punto de entrada: valida token, permisos gruesos, reenvía"]
        kc["<b>Keycloak</b><br/>[Keycloak 26 :8180]<br/>Usuarios, roles, emite tokens"]
        v["<b>Servicio de Vuelos</b><br/>[Spring Boot :8082]<br/>Turnos, tipos de vuelo, reservas; orquesta el proceso"]
        p["<b>Servicio de Pilotos</b><br/>[Spring Boot :8081]<br/>Pilotos, certificaciones, agenda"]
        c["<b>Servicio de Clima</b><br/>[Spring Boot :8083]<br/>Pronóstico y veredicto (R6), sin base"]
        vdb[("<b>vuelos_db</b><br/>[PostgreSQL]")]
        pdb[("<b>pilotos_db</b><br/>[PostgreSQL]")]
    end
    om["<b>Open-Meteo</b><br/>[Sistema externo]"]
    u -->|"Pide token"| kc
    u -->|"Solicitudes con token<br/>HTTPS/JSON"| gw
    gw -->|"Descarga claves (jwk-set-uri)"| kc
    gw -->|"/api/v1/vuelos/**"| v
    gw -->|"/api/v1/pilotos/**"| p
    gw -->|"/api/v1/clima/** (ADMIN)"| c
    v -->|"JDBC"| vdb
    p -->|"JDBC"| pdb
    v -->|"Busca, reserva y libera piloto<br/>/api/v1/internos/**"| p
    v -->|"Pide veredicto"| c
    c -->|"Pronóstico<br/>HTTPS/JSON"| om
    classDef persona fill:#08427b,color:#fff
    classDef cont fill:#438dd5,color:#fff
    classDef db fill:#2e6da4,color:#fff
    classDef externo fill:#999,color:#fff
    class u persona
    class gw,kc,v,p,c cont
    class vdb,pdb db
    class om externo
```

- Sólo el gateway y Keycloak publican puertos; los servicios y PostgreSQL quedan en la red interna de Docker.
- Cada servicio tiene su base (un solo PostgreSQL con dos bases) y nadie lee tablas ajenas.
- El gateway **no** enruta `/api/v1/internos/**`: esos endpoints de Pilotos sólo los llama Vuelos.
- Gateway y los tres servicios validan el JWT (todos con `jwk-set-uri` hacia Keycloak).
- Vuelos reenvía el `Authorization` del usuario a Pilotos y Clima.
