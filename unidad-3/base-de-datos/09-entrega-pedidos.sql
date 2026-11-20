ALTER TABLE orden_detalle ADD COLUMN IF NOT EXISTS tipo_entrega VARCHAR(20);
ALTER TABLE orden_detalle ADD COLUMN IF NOT EXISTS archivo_version VARCHAR(64);
ALTER TABLE orden_detalle ADD COLUMN IF NOT EXISTS archivo_nombre VARCHAR(255);
UPDATE orden_detalle SET tipo_entrega = CASE
    WHEN lower(nombre_historico) LIKE '%asesor%' OR lower(nombre_historico) LIKE '%consulta%'
        OR lower(nombre_historico) LIKE '%revisión de documentos%' THEN 'ASESORIA' ELSE 'ARCHIVO' END
WHERE tipo_entrega IS NULL;
ALTER TABLE orden_detalle ALTER COLUMN tipo_entrega SET NOT NULL;
UPDATE orden_detalle SET archivo_version='guia-bienvenida', archivo_nombre='Guia-de-bienvenida-ILLARI-LEGAL.pdf'
WHERE nombre_historico IN ('Guía de bienvenida digital','Prueba de pago con Yape') AND archivo_version IS NULL;
