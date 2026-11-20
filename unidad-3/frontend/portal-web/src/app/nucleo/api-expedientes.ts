import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PortalApi } from './api-portal';

export interface LegalCase {
    pedidoId?: number | null;
    servicioContratado?: string | null;
    id: number;
    nombreCaso: string;
    clienteId: number;
    clienteNombre: string;
    abogadoId: number;
    abogadoNombre: string;
    estado: string;
    fechaApertura: string;
    descripcion: string | null;
    tarifa: number;
    fechaAudiencia: string | null;
    referencia: boolean;
    abogadoEmail: string;
    abogadoTelefono: string;
}
export interface LawyerProfile {
    id: number;
    nombre: string;
    email: string;
    especialidad: string;
    telefono: string;
    direccion: string | null;
}
export interface CaseDocument {
    id: number;
    nombre: string;
    tipoArchivo: string;
    tamanoArchivo: number;
    casoId: number;
}

@Injectable({ providedIn: 'root' })
export class LawyerApi {
    private readonly http = inject(HttpClient);
    private readonly auth = inject(PortalApi);

    async cases(): Promise<LegalCase[]> {
        return firstValueFrom(
            this.http.get<LegalCase[]>('/api/casos', { headers: await this.auth.authorizationHeaders() }),
        );
    }
    async profile(): Promise<LawyerProfile> {
        return firstValueFrom(
            this.http.get<LawyerProfile>('/api/abogados/me', { headers: await this.auth.authorizationHeaders() }),
        );
    }
    async case(id: number): Promise<LegalCase> {
        return firstValueFrom(
            this.http.get<LegalCase>(`/api/casos/${id}`, { headers: await this.auth.authorizationHeaders() }),
        );
    }
    async documents(id: number): Promise<CaseDocument[]> {
        return firstValueFrom(
            this.http.get<CaseDocument[]>(`/api/documentos/caso/${id}`, {
                headers: await this.auth.authorizationHeaders(),
            }),
        );
    }
    async followup(id: number, estado: string, fechaAudiencia: string | null): Promise<LegalCase> {
        return firstValueFrom(
            this.http.patch<LegalCase>(
                `/api/casos/${id}/seguimiento`,
                { estado, fechaAudiencia },
                { headers: await this.auth.authorizationHeaders() },
            ),
        );
    }
    async upload(id: number, name: string, file: File): Promise<CaseDocument> {
        const body = new FormData();
        body.append('nombre', name.trim());
        body.append('archivo', file);
        return firstValueFrom(
            this.http.post<CaseDocument>(`/api/documentos/caso/${id}`, body, {
                headers: await this.auth.authorizationHeaders(),
            }),
        );
    }
    async download(id: number): Promise<Blob> {
        return firstValueFrom(
            this.http.get(`/api/documentos/${id}/download`, {
                responseType: 'blob',
                headers: await this.auth.authorizationHeaders(),
            }),
        );
    }
}
