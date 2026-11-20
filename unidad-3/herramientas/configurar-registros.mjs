// Prepara la consola local y la credencial de servicio para gestionar personas.
import fs from 'node:fs';
import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';
const file = 'credenciales/runtime.env';
let content = fs.readFileSync(file, 'utf8');
const env = Object.fromEntries(
    content
        .split(/\r?\n/)
        .filter((line) => /^[A-Z_]+=/.test(line))
        .map((line) => {
            const i = line.indexOf('=');
            return [
                line.slice(0, i),
                line
                    .slice(i + 1)
                    .trim()
                    .replace(/^(["'])(.*)\1$/, '$2'),
            ];
        }),
);
const base = (env.KEYCLOAK_PUBLIC_URL || 'http://localhost:18180').replace(/\/$/, '');

function postgres(args) {
    const result = spawnSync('docker', [
        'compose', '--env-file', file, '-f', 'docker-compose.yml',
        'exec', '-T', 'postgres', 'psql', '-U', 'postgres', '-d', 'illari_juridico',
        '-v', 'ON_ERROR_STOP=1', ...args,
    ], { encoding: 'utf8', timeout: 30000, windowsHide: true });
    if (result.status !== 0) throw new Error('No se pudo preparar la cuenta administrativa en PostgreSQL.');
    return result.stdout.trim();
}

async function prepararAdministrador(api) {
    const password = env.ILLARI_ADMIN_INICIAL_PASSWORD;
    if (!password) return;
    const origin = new URL(base);
    if (!['localhost', '127.0.0.1'].includes(origin.hostname)) {
        throw new Error('La cuenta inicial se configura desde el Keycloak local.');
    }
    const existingAdmins = postgres(['-Atc',
        "SELECT count(*) FROM users JOIN roles ON roles.id=users.role_id WHERE roles.name='admin'"]);
    if (existingAdmins !== '0') return;
    const username = 'admin.illari';
    const email = 'admin@illari.local';
    const search = '/users?exact=true&username=' + username;
    let [user] = await api('GET', search);
    const role = await api('GET', '/roles/ADMIN');
    if (!user) {
        await api('POST', '/users', {
            username, email, enabled: true, emailVerified: true, firstName: 'Administrador', lastName: 'Illari',
            credentials: [{ type: 'password', value: password, temporary: false }],
        });
        [user] = await api('GET', search);
    }
    if (user?.email !== email || !/^[a-f0-9-]{36}$/i.test(user?.id)) {
        throw new Error('La cuenta inicial no coincide con el usuario local esperado.');
    }
    await api('POST', '/users/' + user.id + '/role-mappings/realm', [role]);
    postgres(['-v', 'bootstrap_subject=' + user.id, '-f', '/migrations/inicializacion/administrador-local.sql']);
    const bound = postgres(['-Atc', "SELECT count(*) FROM keycloak_identidades WHERE subject='" + user.id + "'"]);
    if (bound !== '1') throw new Error('El administrador de Keycloak no quedó vinculado al portal.');
    console.log('Administrador del portal habilitado: ' + username);
}

async function run() {
    const auth = await fetch(base + '/realms/master/protocol/openid-connect/token', {
        method: 'POST',
        body: new URLSearchParams({
            grant_type: 'password',
            client_id: 'admin-cli',
            username: env.KEYCLOAK_ADMIN || 'admin',
            password: env.KEYCLOAK_ADMIN_PASSWORD || '',
        }),
        signal: AbortSignal.timeout(15000),
    });
    if (!auth.ok) throw new Error('No se pudo autenticar la configuración de accesos.');
    const { access_token } = await auth.json();
    const headers = { Authorization: 'Bearer ' + access_token, 'Content-Type': 'application/json' };
    async function request(method, path, body) {
        const response = await fetch(base + '/admin/realms/' + path, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body),
            signal: AbortSignal.timeout(15000),
        });
        if (!response.ok) throw new Error('No se pudo configurar el realm ' + path.split('/')[0] + '. HTTP ' + response.status);
        return response.status === 204 || response.status === 201 ? null : response.json();
    }

    // La consola usa el inicio de sesión de master, incluso con un hostname de administración separado.
    const master = await request('GET', 'master');
    if (master.attributes?.frontendUrl !== base) {
        await request('PUT', 'master', { attributes: { ...master.attributes, frontendUrl: base } });
        console.log('Consola de Keycloak configurada en ' + base + '/admin/master/console/');
    }
    if (process.argv.includes('--solo-consola')) return;

    const api = (method, path, body) => request(method, 'illari-legal' + path, body);
    const realm = await api('GET', '');
    const policies = (realm.passwordPolicy || '').split(' and ').filter(Boolean);
    const existingLength = policies.find(value => /^length\(\d+\)$/.test(value));
    const minimum = existingLength ? Math.max(12, Number(existingLength.match(/\d+/)[0])) : 12;
    const passwordPolicy = [...policies.filter(value => !/^length\(/.test(value)), `length(${minimum})`].join(' and ');
    await api('PUT', '', { passwordPolicy });
    if (realm.attributes?.['illari.passwordPolicyVersion'] !== '1') {
        const permanent = new Set(['mateo.cliente', 'lucia.abogada', 'valeria.admin']);
        for (let first = 0; ; first += 100) {
            const users = await api('GET', '/users?first=' + first + '&max=100');
            for (const user of users) {
                if (user.serviceAccountClientId || user.username?.startsWith('service-account-') || permanent.has(user.username)) continue;
                await api('PUT', '/users/' + user.id, {
                    requiredActions: [...new Set([...(user.requiredActions || []), 'UPDATE_PASSWORD'])],
                });
            }
            if (users.length < 100) break;
        }
    }
    await api('PUT', '', { passwordPolicy,
        attributes: { ...realm.attributes, 'illari.passwordPolicyVersion': '1' } });
    console.log('Contraseñas: mínimo de 12 caracteres para altas y cambios.');

    let [client] = await api('GET', '/clients?clientId=illari-registros');
    if (!client) {
        await api('POST', '/clients', {
            clientId: 'illari-registros',
            name: 'Gestión de personas de ILLARI LEGAL',
            enabled: true,
            protocol: 'openid-connect',
            publicClient: false,
            serviceAccountsEnabled: true,
            standardFlowEnabled: false,
            directAccessGrantsEnabled: false,
            fullScopeAllowed: true,
            secret: crypto.randomBytes(32).toString('base64url'),
        });
        [client] = await api('GET', '/clients?clientId=illari-registros');
    }
    const { value: secret } = await api('GET', '/clients/' + client.id + '/client-secret');
    const account = await api('GET', '/clients/' + client.id + '/service-account-user');
    const [management] = await api('GET', '/clients?clientId=realm-management');
    const roles = [];
    for (const name of ['manage-users', 'view-users', 'query-users', 'view-realm'])
        roles.push(await api('GET', '/clients/' + management.id + '/roles/' + name));
    await api('POST', '/users/' + account.id + '/role-mappings/clients/' + management.id, roles);
    const assignment = 'ILLARI_REGISTROS_CLIENT_SECRET=' + secret;
    content = /^ILLARI_REGISTROS_CLIENT_SECRET=.*$/m.test(content)
        ? content.replace(/^ILLARI_REGISTROS_CLIENT_SECRET=.*$/m, assignment)
        : content.trimEnd() + '\n' + assignment + '\n';
    fs.writeFileSync(file, content);
    console.log('Gestión de registros configurada. Credencial guardada en el entorno local protegido.');
    await prepararAdministrador(api);
}
run().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
});
