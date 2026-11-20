-- Conserva la referencia del rechazo sin guardar el celular, OTP ni token de pago.
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS yape_rechazo_codigo VARCHAR(64);
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS yape_rechazo_referencia VARCHAR(64);
ALTER TABLE pagos_legales ADD COLUMN IF NOT EXISTS yape_rechazo_fecha TIMESTAMPTZ;
