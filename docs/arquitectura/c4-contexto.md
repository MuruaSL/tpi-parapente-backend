# C4 nivel 1: Contexto

```mermaid
flowchart TB
    cliente["<b>Cliente</b><br/>[Persona]<br/>Reserva vuelos y consulta sus reservas"]
    piloto["<b>Piloto</b><br/>[Persona]<br/>Consulta su agenda"]
    admin["<b>Administrador</b><br/>[Persona]<br/>Gestiona turnos y pilotos, revalida el clima"]
    plataforma["<b>Plataforma de Vuelos</b><br/>[Sistema]<br/>Decide si una reserva se confirma según pasajero, piloto y clima"]
    om["<b>Open-Meteo</b><br/>[Sistema externo]<br/>Pronóstico público de viento, ráfagas y lluvia"]
    cliente -->|"Reserva y consulta<br/>HTTPS/JSON"| plataforma
    piloto -->|"Consulta su agenda<br/>HTTPS/JSON"| plataforma
    admin -->|"Administra<br/>HTTPS/JSON"| plataforma
    plataforma -->|"Consulta el pronóstico<br/>HTTPS/JSON"| om
    classDef persona fill:#08427b,color:#fff,stroke:#052e56
    classDef sistema fill:#1168bd,color:#fff,stroke:#0b4884
    classDef externo fill:#999,color:#fff,stroke:#666
    class cliente,piloto,admin persona
    class plataforma sistema
    class om externo
```

Tres tipos de usuario usan la API (desde Swagger o Postman). La plataforma es el sistema que construimos y el único sistema externo del que depende es Open-Meteo. La autenticación (Keycloak) forma parte de lo que desplegamos y se ve en el nivel 2.
