-- Datos base del módulo legado.
-- Las contraseñas NO se versionan. Los usuarios demo del backend legado se crean
-- únicamente cuando se proporcionan LEGACY_ADMIN_PASSWORD,
-- LEGACY_ABOGADO_PASSWORD y LEGACY_CLIENTE_PASSWORD en el entorno.

INSERT INTO roles (name, display_name) VALUES
    ('admin', 'Administrador'),
    ('abogado', 'Abogado'),
    ('cliente', 'Cliente')
ON CONFLICT (name) DO NOTHING;
