-- El sub del piloto 1 coincide con el usuario piloto1 de Keycloak
INSERT INTO piloto (id, nombre, keycloak_sub, activo) VALUES
 (1, 'Piloto Uno',  '22222222-2222-2222-2222-222222222222', TRUE),
 (2, 'Piloto Dos',  '44444444-4444-4444-4444-444444444444', TRUE),
 (3, 'Piloto Tres', '55555555-5555-5555-5555-555555555555', TRUE);

INSERT INTO certificacion (piloto_id, tipo_vuelo_codigo) VALUES
 (1, 'PARAPENTE_TANDEM'), (1, 'PRUEBA_APTO'), (1, 'PRUEBA_NO_APTO'),
 (2, 'GLOBO'), (2, 'PRUEBA_APTO'),
 (3, 'PARAPENTE_TANDEM'), (3, 'PRUEBA_APTO');

-- Como se insertaron ids fijos, hay que adelantar la secuencia
SELECT setval(pg_get_serial_sequence('piloto', 'id'), 3);
