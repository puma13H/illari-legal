import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import Keycloak from 'keycloak-js';
import { firstValueFrom } from 'rxjs';

export interface LegalService {
    id: number;
    nombre: string;
    descripcion: string | null;
    categoria: string | null;
    precio: number;
    estado: string;
    tipoEntrega: 'ASESORIA' | 'ARCHIVO';
    archivoVersion?: string | null;
    archivoNombre?: string | null;
    archivoDisponible: boolean;
}

export interface Order {
    id: number;
    fechaCreacion: string;
    estado: string;
    metodoPago: string;
    total: number;
    detalles: { idServicio: number; nombreServicio: string; cantidad?: number; subtotal?: number; tipoEntrega?: 'ASESORIA' | 'ARCHIVO' }[];
}

export interface Payment {
    id: number;
    estado: string;
    yape: { enVerificacion: boolean; cargoId: string | null; rechazoCodigo?: string | null } | null;
}

export interface YapeConfig {
    llavePublica: string;
    montoCentimos: number;
    entorno: 'test' | 'live';
}
export interface OrderResource {
    codigo: string;
    titulo: string;
    formato: string;
    idServicio: number;
    disponible: boolean;
}
export function serviceName(name: string): string {
    return name === 'Prueba de pago con Yape' ? 'Guía de bienvenida digital' : name;
}

export function httpStatus(error: unknown): number | undefined {
    return error instanceof HttpErrorResponse ? error.status : undefined;
}

@Injectable({ providedIn: 'root' })
export class PortalApi {
    private readonly http = inject(HttpClient);
    private readonly callbackUrl = `${location.origin}/auth/callback`;
    private keycloak?: Keycloak;
    yapeMinCentimos = 124;

    get ready(): boolean {
        return this.keycloak !== undefined;
    }

    isClient(): boolean {
        return this.keycloak?.hasRealmRole('CLIENTE') ?? false;
    }
    isLawyer(): boolean {
        return this.keycloak?.hasRealmRole('ABOGADO') ?? false;
    }
    isAdmin(): boolean {
        return this.keycloak?.hasRealmRole('ADMIN') ?? false;
    }

    displayName(): string {
        const claims = this.keycloak?.tokenParsed;
        const name: unknown = claims?.['name'];
        const username: unknown = claims?.['preferred_username'];
        return typeof name === 'string' && name.trim()
            ? name.trim()
            : typeof username === 'string' && username.trim()
              ? username.trim()
              : 'Cliente';
    }

    async init(): Promise<boolean> {
        const config = await firstValueFrom(
            this.http.get<{ keycloakUrl: string; yapeMinCentimos?: number }>('/checkout/config.json'),
        );
        if (Number.isSafeInteger(config.yapeMinCentimos) && config.yapeMinCentimos! > 0)
            this.yapeMinCentimos = config.yapeMinCentimos!;
        this.keycloak = new Keycloak({ url: config.keycloakUrl, realm: 'illari-legal', clientId: 'illari-frontend' });
        return this.keycloak.init({ pkceMethod: 'S256', checkLoginIframe: false, redirectUri: this.callbackUrl });
    }

    login(): Promise<void> | undefined {
        return this.keycloak?.login({ redirectUri: this.callbackUrl });
    }

    logout(): Promise<void> | undefined {
        return this.keycloak?.logout({ redirectUri: `${location.origin}/` });
    }

    clearSession(): void {
        this.keycloak?.clearToken();
    }

    listServices(): Promise<LegalService[]> {
        return this.request('GET', '/servicios');
    }

    order(id: string): Promise<Order> {
        return this.request('GET', `/ordenes/${id}`);
    }

    myOrders(): Promise<Order[]> {
        const subject = this.keycloak?.subject;
        if (!subject) return Promise.reject(new HttpErrorResponse({ status: 401 }));
        return this.request('GET', `/ordenes/cliente/${encodeURIComponent(subject)}`);
    }

    payments(id: string): Promise<Payment[]> {
        return this.request('GET', `/pagos/orden/${id}`);
    }
    resources(id: number): Promise<OrderResource[]> {
        return this.request('GET', `/ordenes/${id}/recursos`);
    }
    async downloadGuide(id: number): Promise<Blob> {
        return firstValueFrom(
            this.http.get(`/api/v1/ordenes/${id}/recursos/guia-bienvenida`, {
                responseType: 'blob',
                headers: await this.authorizationHeaders(),
            }),
        );
    }

    async downloadResource(order: number, service: number): Promise<Blob> {
        return firstValueFrom(this.http.get(`/api/v1/ordenes/${order}/recursos/servicios/${service}`, {
            responseType: 'blob', headers: await this.authorizationHeaders(),
        }));
    }

    yapeConfig(id: number): Promise<YapeConfig> {
        return this.request('GET', `/pagos/${id}/yape`);
    }

    createOrder(services: readonly number[], idempotencyKey: string): Promise<Order> {
        return this.request('POST', '/ordenes', {
            body: { metodoPago: 'YAPE', detalles: services.map((idServicio) => ({ idServicio, cantidad: 1 })) },
            headers: { 'Idempotency-Key': idempotencyKey },
        });
    }

    charge(id: number, tokenId: string, celular: string, correo?: string): Promise<Payment> {
        return this.request('POST', `/pagos/${id}/yape`, { body: { tokenId, celular, correo } });
    }

    async authorizationHeaders(): Promise<Record<string, string>> {
        const keycloak = this.keycloak;
        if (!keycloak) throw new HttpErrorResponse({ status: 401 });
        try {
            await keycloak.updateToken(30);
        } catch {
            throw new HttpErrorResponse({ status: 401 });
        }
        return { Authorization: `Bearer ${keycloak.token}` };
    }

    private async request<T>(
        method: 'GET' | 'POST',
        path: string,
        options: { body?: object; headers?: Record<string, string> } = {},
    ): Promise<T> {
        const authorization = await this.authorizationHeaders();
        return firstValueFrom(
            this.http.request<T>(method, `/api/v1${path}`, {
                body: options.body,
                headers: { ...options.headers, ...authorization },
            }),
        );
    }
}
