ALTER TABLE casos ADD COLUMN IF NOT EXISTS pedido_id BIGINT;
ALTER TABLE casos ADD COLUMN IF NOT EXISTS pedido_detalle INTEGER;
ALTER TABLE casos ADD COLUMN IF NOT EXISTS servicio_contratado VARCHAR(255);
CREATE UNIQUE INDEX IF NOT EXISTS uk_caso_pedido_detalle ON casos(pedido_id,pedido_detalle);
