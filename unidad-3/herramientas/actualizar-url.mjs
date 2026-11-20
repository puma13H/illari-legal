// Sincroniza el enlace de Cloudflare y los redireccionamientos del inicio de sesión.
// Las contraseñas se leen desde credenciales; nunca se imprimen ni se guardan aquí.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
function readEnv(file) {
    return Object.fromEntries(fs.readFileSync(path.join(root, file), 'utf8').split(/\r?\n/).filter(line => /^[A-Za-z_][A-Za-z0-9_]*=/.test(line)).map(line => {
        const index = line.indexOf('='); let value = line.slice(index + 1).trim();
        if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) value = value.slice(1, -1);
        return [line.slice(0, index), value];
    }));
}
function docker(args) {
    const result = spawnSync('docker', args, { encoding: 'utf8', windowsHide: true });
    if (result.error || result.status !== 0) throw new Error('No se pudo consultar el túnel de Docker.');
    return result.stdout + result.stderr;
}
function request(url, options = {}) { return fetch(url, { ...options, signal: AbortSignal.timeout(15000) }); }
async function run() {
    const env = readEnv('credenciales/runtime.env');
    const container = docker(['ps', '--filter', 'label=com.docker.compose.project=illari-s12', '--filter', 'label=com.docker.compose.service=publico-tunel', '--format', '{{.ID}}']).trim();
    if (!/^[0-9a-f]+$/.test(container)) throw new Error('El túnel HTTPS debe estar iniciado.');
    const logs = docker(['logs', '--tail', '200', container]);
    const pattern = /https:\/\/[a-z0-9-]+\.trycloudflare\.com/g;
    const origins = logs.match(pattern) ?? docker(['logs', container]).match(pattern);
    if (!origins?.length) throw new Error('Cloudflare aún no publicó la URL. Reintenta en unos segundos.');
    const origin = origins.at(-1);
    const local = env.KEYCLOAK_PUBLIC_URL || 'http://localhost:18180';
    const tokenResponse = await request(local + '/realms/master/protocol/openid-connect/token', {
        method: 'POST', body: new URLSearchParams({ grant_type: 'password', client_id: 'admin-cli', username: env.KEYCLOAK_ADMIN || 'admin', password: env.KEYCLOAK_ADMIN_PASSWORD || '' })
    });
    if (!tokenResponse.ok) throw new Error('No se pudo autenticar la configuración de accesos. Revisa las credenciales locales de Keycloak.');
    const { access_token } = await tokenResponse.json();
    const headers = { Authorization: 'Bearer ' + access_token, 'Content-Type': 'application/json' };
    const clientsResponse = await request(local + '/admin/realms/illari-legal/clients?clientId=illari-frontend', { headers });
    if (!clientsResponse.ok) throw new Error('No se pudo consultar la configuración del portal.');
    const [client] = await clientsResponse.json();
    if (!client?.id) throw new Error('No existe el cliente illari-frontend en Keycloak.');
    const frontend = env.FRONTEND_PUBLIC_URL || 'http://localhost:4200';
    client.redirectUris = [...new Set([frontend + '/auth/callback', origin + '/auth/callback'])];
    client.webOrigins = [...new Set([frontend, origin])];
    client.rootUrl = origin; client.baseUrl = '/';
    client.attributes = { ...client.attributes, 'post.logout.redirect.uris': frontend + '/##' + origin + '/' };
    const saved = await request(local + '/admin/realms/illari-legal/clients/' + client.id, { method: 'PUT', headers, body: JSON.stringify(client) });
    if (!saved.ok) throw new Error('No se pudieron actualizar los redireccionamientos del portal.');
    fs.writeFileSync(path.join(root, 'credenciales/public-demo.env'), 'ILLARI_PUBLIC_URL=' + origin + '\nILLARI_PUBLIC_HOST=' + new URL(origin).hostname + '\n');
    console.log('URL del portal: ' + origin);
    console.log('Configuración de acceso sincronizada.');
}
run().catch(error => { console.error(error.message); process.exitCode = 1; });
