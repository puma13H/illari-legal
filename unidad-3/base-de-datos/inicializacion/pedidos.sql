-- Esquema de pedidos para una instalación nueva. El inicio aplica después las migraciones.
CREATE TABLE ordenes_legales (
 id BIGSERIAL PRIMARY KEY,id_cliente VARCHAR(255) NOT NULL,fecha_creacion TIMESTAMPTZ NOT NULL,
 estado VARCHAR(255) NOT NULL,total NUMERIC(12,2) NOT NULL,metodo_pago VARCHAR(255) NOT NULL,detalles_json TEXT NOT NULL,
 transferencia_real BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE orden_outbox (
 id BIGSERIAL PRIMARY KEY,orden_id BIGINT NOT NULL,payload TEXT NOT NULL,publicado BOOLEAN NOT NULL,creado TIMESTAMPTZ NOT NULL
);

