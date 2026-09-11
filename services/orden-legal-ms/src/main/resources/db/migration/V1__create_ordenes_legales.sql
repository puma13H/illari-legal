CREATE TABLE ordenes_legales (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL,
    estado VARCHAR(30) NOT NULL,
    total NUMERIC(12,2) NOT NULL,
    CONSTRAINT ck_orden_estado CHECK (estado IN ('CREADA', 'EN_PROCESO', 'COMPLETADA', 'CANCELADA')),
    CONSTRAINT ck_orden_total CHECK (total >= 0)
);

CREATE TABLE orden_servicios (
    orden_id BIGINT NOT NULL,
    servicio_id BIGINT NOT NULL,
    CONSTRAINT fk_orden_servicios_orden FOREIGN KEY (orden_id)
        REFERENCES ordenes_legales(id) ON DELETE CASCADE
);

CREATE INDEX idx_orden_cliente ON ordenes_legales(cliente_id);
CREATE INDEX idx_orden_servicios_orden ON orden_servicios(orden_id);
