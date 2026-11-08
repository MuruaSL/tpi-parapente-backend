#!/bin/bash
# Crea las dos bases del TPI en el mismo Postgres (se ejecuta sólo la primera vez, con el volumen vacío).
set -e
for db in vuelos_db pilotos_db; do
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -c "CREATE DATABASE $db"
done
