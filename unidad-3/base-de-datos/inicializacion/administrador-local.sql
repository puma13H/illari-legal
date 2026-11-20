-- Ejecutar por la herramienta de inicio tras crear el usuario en Keycloak.
BEGIN;
INSERT INTO users (name, email, password, role_id)
SELECT 'Administrador Illari', 'admin@illari.local', '!keycloak', id
FROM roles
WHERE name = 'admin' AND NOT EXISTS (
    SELECT 1 FROM users JOIN roles ON roles.id = users.role_id WHERE roles.name = 'admin'
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO keycloak_identidades (issuer, subject, user_id, creada, operador)
SELECT 'http://keycloak:8080/realms/illari-legal', :'bootstrap_subject', users.id, now(), 'preparacion-local'
FROM users JOIN roles ON roles.id = users.role_id
WHERE users.email = 'admin@illari.local' AND roles.name = 'admin'
ON CONFLICT DO NOTHING;
COMMIT;
