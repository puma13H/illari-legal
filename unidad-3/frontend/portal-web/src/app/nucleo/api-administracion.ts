import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PortalApi, LegalService } from './api-portal';
import { LawyerProfile, LegalCase } from './api-expedientes';

export interface ClientProfile {
    id: number;
    nombre: string;
    email: string;
    telefono: string;
    direccion: string | null;
}
export type ServiceInput = Pick<LegalService, 'nombre' | 'descripcion' | 'categoria' | 'precio' | 'estado' | 'tipoEntrega'>;
export interface ServiceRequest {
    pedidoId: number; posicion: number; servicio: string; cliente: string; clienteId: number | null;
    importe: number; cantidad: number; fecha: string; expedienteId: number | null; abogado: string | null;
}
export interface RequestPage { items: ServiceRequest[]; pagina: number; hayMas: boolean; }
export interface CaseInput {
    nombreCaso: string;
    clienteId: number;
    abogadoId: number;
    estado: string;
    fechaApertura: string;
    descripcion: string;
    tarifa: number;
    fechaAudiencia: string | null;
}
export type PersonKind = 'clientes' | 'abogados';
export interface PersonInput {
    nombre: string;
    email: string;
    telefono: string;
    direccion: string;
    especialidad?: string;
    usuario?: string;
    claveInicial?: string;
}
export interface RegisteredPerson {
    id: number;
    nombre: string;
    email: string;
    usuario: string | null;
    accesoHabilitado: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminApi {
    private readonly http = inject(HttpClient);
    private readonly auth = inject(PortalApi);
    async lawyers(): Promise<LawyerProfile[]> {
        return firstValueFrom(
            this.http.get<LawyerProfile[]>('/api/abogados', { headers: await this.auth.authorizationHeaders() }),
        );
    }
    async clients(): Promise<ClientProfile[]> {
        return firstValueFrom(
            this.http.get<ClientProfile[]>('/api/clientes', { headers: await this.auth.authorizationHeaders() }),
        );
    }
    async saveService(id: number | null, body: ServiceInput): Promise<LegalService> {
        const headers = await this.auth.authorizationHeaders();
        return firstValueFrom(
            id === null
                ? this.http.post<LegalService>('/api/v1/servicios', body, { headers })
                : this.http.put<LegalService>(`/api/v1/servicios/${id}`, body, { headers }),
        );
    }
    async uploadServiceFile(id: number, file: File): Promise<LegalService> {
        const data = new FormData(); data.append('archivo', file);
        return firstValueFrom(this.http.post<LegalService>(`/api/v1/servicios/${id}/archivo`, data,
            { headers: await this.auth.authorizationHeaders() }));
    }
    async requests(page: number): Promise<RequestPage> {
        return firstValueFrom(this.http.get<RequestPage>(`/api/solicitudes?pagina=${page}`,
            { headers: await this.auth.authorizationHeaders() }));
    }
    async assignRequest(item: ServiceRequest, lawyer: number, description: string): Promise<LegalCase> {
        return firstValueFrom(this.http.post<LegalCase>(`/api/solicitudes/${item.pedidoId}/${item.posicion}/asignacion`,
            { abogadoId: lawyer, descripcion: description }, { headers: await this.auth.authorizationHeaders() }));
    }
    async saveCase(id: number | null, body: CaseInput): Promise<LegalCase> {
        const headers = await this.auth.authorizationHeaders();
        return firstValueFrom(
            id === null
                ? this.http.post<LegalCase>('/api/casos', body, { headers })
                : this.http.put<LegalCase>(`/api/casos/${id}`, body, { headers }),
        );
    }
    async savePerson(kind: PersonKind, id: number | null, body: PersonInput): Promise<RegisteredPerson> {
        const headers = await this.auth.authorizationHeaders();
        return firstValueFrom(
            id === null
                ? this.http.post<RegisteredPerson>(`/api/registros/${kind}`, body, { headers })
                : this.http.put<RegisteredPerson>(`/api/registros/${kind}/${id}`, body, { headers }),
        );
    }
}
