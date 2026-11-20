import { randomBytes } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';

const destination = 'credenciales/runtime.env';
const grafanaPassword = 'credenciales/grafana-admin.txt';
mkdirSync('credenciales', { recursive: true });
try {
    writeFileSync(grafanaPassword, randomBytes(24).toString('hex'), { flag: 'wx', mode: 0o600 });
    console.log('Contraseña inicial de Grafana guardada en ' + grafanaPassword);
} catch (error) {
    if (error.code !== 'EEXIST') throw error;
}
if (process.argv.includes('--solo-grafana')) process.exit(0);

if (existsSync(destination)) {
    console.log('Ya existe credenciales/runtime.env. Usa esa configuración para arrancar.');
    process.exit(0);
}

const isolated = process.argv.includes('--aislado');
const name = isolated ? 'illari-local-' + randomBytes(4).toString('hex') : 'illari-s12';
const portalPort = isolated ? 14200 : 4200;
const keycloakPort = isolated ? 28180 : 18180;
const environment = {
    COMPOSE_PROJECT_NAME: name,
    ILLARI_CONTAINER_PREFIX: isolated ? name : 'illari',
    CHECKOUT_PORT: portalPort,
    FRONTEND_PUBLIC_URL: 'http://localhost:' + portalPort,
    KEYCLOAK_PORT: keycloakPort,
    KEYCLOAK_PUBLIC_URL: 'http://localhost:' + keycloakPort,
    GATEWAY_PORT: isolated ? 28080 : 18080,
    GRAFANA_PORT: isolated ? 23000 : 3000,
    PROMETHEUS_PORT: isolated ? 29090 : 9090,
    LOKI_PORT: isolated ? 23100 : 13100,
    BLACKBOX_PORT: isolated ? 29115 : 19115,
    ALLOY_PORT: isolated ? 22345 : 12345,
};
for (const key of [
    'POSTGRES_PASSWORD', 'CLIENTES_DB_PASSWORD', 'SERVICIOS_DB_PASSWORD', 'ORDENES_DB_PASSWORD',
    'PAGOS_DB_PASSWORD', 'LEGACY_DB_PASSWORD', 'KEYCLOAK_DB_PASSWORD', 'KEYCLOAK_ADMIN_PASSWORD',
    'ILLARI_ADMIN_INICIAL_PASSWORD',
]) {
    environment[key] = randomBytes(24).toString('hex');
}
const content = readFileSync('.env.example', 'utf8').trimEnd()
    .split(/\r?\n/).map(line => {
        const key = line.split('=', 1)[0];
        return Object.hasOwn(environment, key) ? key + '=' + environment[key] : line;
    }).join('\n');
writeFileSync(destination, content + '\n', { flag: 'wx', mode: 0o600 });
console.log('Entorno local preparado: ' + name);
console.log('Configuración y contraseña inicial en ' + destination);
