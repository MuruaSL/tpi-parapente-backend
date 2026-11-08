-- Esquema de vuelos_db. piloto_id es una referencia lógica (sin FK): vive en pilotos_db.
CREATE TABLE sitios (
    id        BIGSERIAL PRIMARY KEY,
    nombre    VARCHAR(100)     NOT NULL,
    latitud   DOUBLE PRECISION NOT NULL,
    longitud  DOUBLE PRECISION NOT NULL
);

CREATE TABLE tipos_vuelo (
    id                BIGSERIAL PRIMARY KEY,
    codigo            VARCHAR(50)  NOT NULL UNIQUE,
    nombre            VARCHAR(100) NOT NULL,
    peso_min          INTEGER      NOT NULL,
    peso_max          INTEGER      NOT NULL,
    edad_min          INTEGER      NOT NULL,
    viento_max_kmh    INTEGER      NOT NULL,
    margen_viento_kmh INTEGER      NOT NULL
);

CREATE TABLE turnos (
    id            BIGSERIAL PRIMARY KEY,
    sitio_id      BIGINT    NOT NULL REFERENCES sitios (id),
    tipo_vuelo_id BIGINT    NOT NULL REFERENCES tipos_vuelo (id),
    fecha_hora    TIMESTAMP NOT NULL,
    cupo          INTEGER   NOT NULL
);

CREATE TABLE reservas (
    id            BIGSERIAL PRIMARY KEY,
    turno_id      BIGINT       NOT NULL REFERENCES turnos (id),
    cliente_sub   VARCHAR(64)  NOT NULL,
    peso_kg       INTEGER      NOT NULL,
    edad          INTEGER      NOT NULL,
    estado        VARCHAR(30)  NOT NULL,
    piloto_id     BIGINT,
    piloto_nombre VARCHAR(100),
    motivo        VARCHAR(255),
    creada_en     TIMESTAMP    NOT NULL
);

CREATE INDEX idx_reservas_turno ON reservas (turno_id);
CREATE INDEX idx_reservas_cliente ON reservas (cliente_sub);
