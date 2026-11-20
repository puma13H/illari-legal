ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS yape_fecha_intento TIMESTAMPTZ;
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS yape_ultima_revision TIMESTAMPTZ;
UPDATE pagos_legales SET yape_fecha_intento=fecha_creacion
WHERE yape_intento IS NOT NULL AND yape_fecha_intento IS NULL;
CREATE INDEX IF NOT EXISTS ix_pago_yape_pendiente ON pagos_legales(estado,yape_ambiente)
WHERE yape_checkout_api AND yape_intento IS NOT NULL AND yape_rechazo_codigo IS NULL;
