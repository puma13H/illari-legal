-- Preserve the previous provider identifier for historical payments.
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS mercado_pago_id VARCHAR(64);
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS metodo_transferencia VARCHAR(10);
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS titular_destino VARCHAR(160);
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS celular_destino VARCHAR(9);
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS codigo_operacion VARCHAR(64);
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS confirmado_por VARCHAR(255);
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS operacion_conciliada VARCHAR(74);
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS yape_culqi BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS culqi_intento VARCHAR(36);
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS culqi_cargo_id VARCHAR(25);
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS yape_checkout_api BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS yape_intento VARCHAR(36);
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS yape_ambiente VARCHAR(4);
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS yape_token_huella VARCHAR(64);
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS yape_orden_proveedor VARCHAR(64);

UPDATE pagos_legales SET yape_checkout_api = TRUE
    WHERE yape_culqi AND NOT yape_checkout_api AND estado = 'PENDIENTE'
        AND culqi_intento IS NULL AND culqi_cargo_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_pago_operacion_transferencia
    ON pagos_legales (operacion_conciliada);
CREATE UNIQUE INDEX IF NOT EXISTS uk_pago_yape_token ON pagos_legales (yape_token_huella);
CREATE UNIQUE INDEX IF NOT EXISTS uk_pago_yape_orden ON pagos_legales (yape_orden_proveedor);
