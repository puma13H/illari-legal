-- Migración compatible con las bases existentes. Conserva los expedientes y sus documentos.
ALTER TABLE casos ADD COLUMN IF NOT EXISTS referencia boolean NOT NULL DEFAULT false;
ALTER TABLE casos ALTER COLUMN descripcion TYPE text;
UPDATE casos
SET referencia=true,
    nombre_caso=regexp_replace(nombre_caso, '^DEMO[[:space:]]*·[[:space:]]*', ''),
    descripcion='Expediente de referencia para conocer el seguimiento y la organización de documentos del portal.'
WHERE nombre_caso LIKE 'DEMO ·%';

UPDATE documentos d SET nombre='Anexo de referencia'
FROM casos c WHERE d.caso_id=c.id AND c.referencia AND d.nombre='DEMO · Anexo de práctica';
UPDATE documentos d SET nombre='Guía del expediente'
FROM casos c WHERE d.caso_id=c.id AND c.referencia AND d.nombre='Guía del expediente de demostración';
