CREATE TABLE servicios_legales (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(140) NOT NULL UNIQUE,
    descripcion VARCHAR(500) NOT NULL,
    precio NUMERIC(12,2) NOT NULL,
    categoria VARCHAR(40) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    CONSTRAINT ck_servicio_precio CHECK (precio > 0),
    CONSTRAINT ck_servicio_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);
