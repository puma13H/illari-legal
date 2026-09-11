CREATE TABLE pagos (
    id BIGSERIAL PRIMARY KEY,
    orden_id BIGINT NOT NULL,
    monto NUMERIC(12,2) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL,
    fecha_aprobacion TIMESTAMPTZ,
    CONSTRAINT ck_pago_monto CHECK (monto > 0),
    CONSTRAINT ck_pago_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO', 'ANULADO'))
);
CREATE INDEX idx_pago_orden ON pagos(orden_id);
