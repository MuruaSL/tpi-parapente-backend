-- Datos de prueba con ids fijos (los usan los tests manuales y la colección de Postman).
INSERT INTO sitios (id, nombre, latitud, longitud) VALUES (1, 'Villa Carlos Paz', -31.4241, -64.4978);

INSERT INTO tipos_vuelo (id, codigo, nombre, peso_min, peso_max, edad_min, viento_max_kmh, margen_viento_kmh) VALUES
 (1, 'PARAPENTE_TANDEM', 'Parapente tándem', 40, 110, 12, 25, 10),
 (2, 'GLOBO',            'Globo aerostático', 30, 130, 8, 20, 8),
 (3, 'PRUEBA_APTO',      'Prueba clima apto', 40, 110, 12, 500, 0),
 (4, 'PRUEBA_NO_APTO',   'Prueba clima no apto', 40, 110, 12, 0, 0),
 (5, 'SIN_PILOTO',       'Prueba sin piloto', 40, 110, 12, 500, 0);

-- Fechas relativas a hoy (a las 10:00), calculadas al MIGRAR (no en cada arranque). Si pasan más de
-- 2 días desde que se creó la base, quedan fuera de la ventana: recrear el volumen con
-- `docker compose down -v`.
INSERT INTO turnos (id, sitio_id, tipo_vuelo_id, fecha_hora, cupo) VALUES
 (1, 1, 3, (CURRENT_DATE + 2) + TIME '10:00', 10),
 (2, 1, 4, (CURRENT_DATE + 3) + TIME '10:00', 10),
 (3, 1, 5, (CURRENT_DATE + 4) + TIME '10:00', 10),
 (4, 1, 3, (CURRENT_DATE + 5) + TIME '10:00', 1),
 (5, 1, 1, (CURRENT_DATE + 6) + TIME '10:00', 5),
 (6, 1, 2, (CURRENT_DATE + 7) + TIME '10:00', 5),
 (7, 1, 3, (CURRENT_DATE + 8) + TIME '10:00', 10);

-- Los ids se insertaron a mano: se adelantan las secuencias para que los nuevos no choquen.
SELECT setval(pg_get_serial_sequence('sitios', 'id'), 100);
SELECT setval(pg_get_serial_sequence('tipos_vuelo', 'id'), 100);
SELECT setval(pg_get_serial_sequence('turnos', 'id'), 100);
