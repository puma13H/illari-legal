-- Conserva los identificadores y precios; los recursos sin archivo quedan como borradores.
ALTER TABLE servicios_legales ADD COLUMN IF NOT EXISTS tipo_entrega VARCHAR(20);
ALTER TABLE servicios_legales ADD COLUMN IF NOT EXISTS archivo_version VARCHAR(64);
ALTER TABLE servicios_legales ADD COLUMN IF NOT EXISTS archivo_nombre VARCHAR(255);
UPDATE servicios_legales SET tipo_entrega = CASE
    WHEN lower(categoria) LIKE '%asesor%' THEN 'ASESORIA' ELSE 'ARCHIVO' END
WHERE tipo_entrega IS NULL;
ALTER TABLE servicios_legales ALTER COLUMN tipo_entrega SET NOT NULL;
UPDATE servicios_legales SET archivo_version='guia-bienvenida', archivo_nombre='Guia-de-bienvenida-ILLARI-LEGAL.pdf'
WHERE nombre IN ('Guía de bienvenida digital','Prueba de pago con Yape') AND archivo_version IS NULL;
UPDATE servicios_legales SET estado='INACTIVO'
WHERE tipo_entrega='ARCHIVO' AND archivo_version IS NULL AND estado IN ('ACTIVO','ACTIVA');
