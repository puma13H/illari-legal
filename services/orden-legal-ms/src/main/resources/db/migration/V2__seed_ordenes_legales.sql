INSERT INTO ordenes_legales (cliente_id, fecha_creacion, estado, total)
VALUES (1, CURRENT_TIMESTAMP, 'CREADA', 330.00);

INSERT INTO orden_servicios (orden_id, servicio_id)
SELECT id, 1 FROM ordenes_legales ORDER BY id LIMIT 1;
INSERT INTO orden_servicios (orden_id, servicio_id)
SELECT id, 2 FROM ordenes_legales ORDER BY id LIMIT 1;
