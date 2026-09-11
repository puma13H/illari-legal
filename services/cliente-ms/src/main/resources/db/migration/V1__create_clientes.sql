CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    dni VARCHAR(20) NOT NULL,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    email VARCHAR(160),
    telefono VARCHAR(30),
    direccion VARCHAR(240),
    estado VARCHAR(20) NOT NULL,
    CONSTRAINT uk_clientes_dni UNIQUE (dni),
    CONSTRAINT ck_clientes_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);
