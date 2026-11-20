CREATE TABLE pagos_legales (
 id BIGSERIAL PRIMARY KEY,id_orden BIGINT NOT NULL UNIQUE,id_cliente VARCHAR(255) NOT NULL,
 monto NUMERIC(12,2) NOT NULL,estado VARCHAR(255) NOT NULL,fecha_creacion TIMESTAMPTZ NOT NULL,fecha_aprobacion TIMESTAMPTZ,
 mercado_pago_id VARCHAR(64),
 metodo_transferencia VARCHAR(10), titular_destino VARCHAR(160), celular_destino VARCHAR(9),
 codigo_operacion VARCHAR(64), confirmado_por VARCHAR(255), operacion_conciliada VARCHAR(74),
 yape_culqi BOOLEAN NOT NULL DEFAULT FALSE, culqi_intento VARCHAR(36), culqi_cargo_id VARCHAR(25),
 yape_checkout_api BOOLEAN NOT NULL DEFAULT FALSE, yape_intento VARCHAR(36), yape_ambiente VARCHAR(4),
 yape_token_huella VARCHAR(64), yape_orden_proveedor VARCHAR(64),
 CONSTRAINT uk_pago_operacion_transferencia UNIQUE (operacion_conciliada),
 CONSTRAINT uk_pago_yape_token UNIQUE (yape_token_huella),
 CONSTRAINT uk_pago_yape_orden UNIQUE (yape_orden_proveedor)
);
CREATE TABLE pago_outbox (
 id BIGSERIAL PRIMARY KEY,orden_id BIGINT NOT NULL UNIQUE,payload TEXT NOT NULL,publicado BOOLEAN NOT NULL,creado TIMESTAMPTZ NOT NULL
);

