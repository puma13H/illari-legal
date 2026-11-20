import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminApi, ClientProfile } from '../nucleo/api-administracion';
import { PortalApi, LegalService, httpStatus } from '../nucleo/api-portal';
import { LawyerApi, LegalCase, LawyerProfile, CaseDocument } from '../nucleo/api-expedientes';
import { UiIcon } from '../compartido/icono';
import { PageBannerComponent } from '../compartido/portada.component';
import { CaseEditorComponent } from './formulario-expediente.component';
import { PeopleManagementComponent } from './gestion-personas.component';
import { RequestsComponent } from './solicitudes.component';

export type AdminSection = 'home' | 'services' | 'cases' | 'team' | 'requests';

@Component({
    selector: 'illari-admin-panel',
    imports: [ReactiveFormsModule, UiIcon, PageBannerComponent, CaseEditorComponent, PeopleManagementComponent, RequestsComponent],
    templateUrl: './panel-admin.component.html',
})
export class AdminPanelComponent implements OnInit {
    private readonly api = inject(AdminApi);
    private readonly portal = inject(PortalApi);
    private readonly legal = inject(LawyerApi);
    private readonly money = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' });
    private readonly dates = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium' });
    private detailRequest = 0;
    readonly section = input.required<AdminSection>();
    readonly displayName = input.required<string>();
    readonly navigate = output<AdminSection>();
    readonly sessionExpired = output<void>();
    readonly loading = signal(true);
    readonly services = signal<LegalService[]>([]);
    readonly cases = signal<LegalCase[]>([]);
    readonly lawyers = signal<LawyerProfile[]>([]);
    readonly clients = signal<ClientProfile[]>([]);
    readonly failures = signal<Partial<Record<'services' | 'cases' | 'team', string>>>({});
    readonly search = signal('');
    readonly status = signal('all');
    readonly servicePage = signal(0);
    readonly pageSize = 6;
    readonly editing = signal(false);
    readonly editingId = signal<number | null>(null);
    readonly saving = signal(false);
    readonly serviceFile = signal<File | null>(null);
    readonly childBusy = signal(false);
    readonly working = computed(() => this.saving() || this.childBusy());
    readonly caseEditing = signal(false);
    readonly editingCase = signal<LegalCase | null>(null);
    readonly message = signal('');
    readonly saveError = signal('');
    readonly selectedCase = signal<LegalCase | null>(null);
    readonly documents = signal<CaseDocument[]>([]);
    readonly documentLoading = signal(false);
    readonly documentError = signal('');
    readonly downloading = signal<number | null>(null);
    readonly firstName = computed(() => this.displayName().trim().split(/\s+/)[0] || 'administrador');
    readonly activeServices = computed(() => this.services().filter((item) => item.estado === 'ACTIVO'));
    readonly activeCases = computed(() => this.cases().filter((item) => !this.closed(item)));
    readonly filteredServices = computed(() =>
        this.services().filter(
            (item) =>
                (this.status() === 'all' || item.estado === this.status()) &&
                this.normalize(item.nombre + ' ' + (item.categoria ?? '')).includes(this.normalize(this.search())),
        ),
    );
    readonly filteredCases = computed(() =>
        this.cases().filter(
            (item) =>
                (this.status() === 'all' || (this.status() === 'closed' ? this.closed(item) : !this.closed(item))) &&
                this.normalize(`${item.id} ${item.nombreCaso} ${item.clienteNombre} ${item.abogadoNombre}`).includes(
                    this.normalize(this.search()),
                ),
        ),
    );
    readonly servicePages = computed(() => Math.max(1, Math.ceil(this.filteredServices().length / this.pageSize)));
    readonly visibleServices = computed(() =>
        this.filteredServices().slice(this.servicePage() * this.pageSize, (this.servicePage() + 1) * this.pageSize),
    );
    readonly serviceRange = computed(() =>
        this.filteredServices().length
            ? `${this.servicePage() * this.pageSize + 1}–${Math.min((this.servicePage() + 1) * this.pageSize, this.filteredServices().length)}`
            : '0',
    );
    readonly form = new FormGroup({
        nombre: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(150)],
        }),
        categoria: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)],
        }),
        descripcion: new FormControl('', { nonNullable: true, validators: Validators.maxLength(2000) }),
        precio: new FormControl<number | null>(null, [
            Validators.required,
            Validators.min(0),
            Validators.max(9999999.99),
            Validators.pattern(/^\d+(\.\d{1,2})?$/),
        ]),
        tipoEntrega: new FormControl<'ASESORIA' | 'ARCHIVO'>('ASESORIA', { nonNullable: true }),
        estado: new FormControl('ACTIVO', { nonNullable: true, validators: Validators.required }),
    });

    ngOnInit(): void {
        void this.load();
    }
    amount(value: number): string {
        return this.money.format(value);
    }
    closed(item: LegalCase): boolean {
        return ['cerrado', 'concluido', 'finalizado', 'cancelado'].includes(this.normalize(item.estado));
    }
    initials(name: string): string {
        return name
            .split(/\s+/)
            .slice(0, 2)
            .map((part) => part[0])
            .join('')
            .toLocaleUpperCase('es-PE');
    }
    date(value: string | null): string {
        if (!value) return 'Sin fecha';
        const date = new Date(value + 'T12:00:00');
        return Number.isNaN(date.getTime()) ? 'Sin fecha' : this.dates.format(date);
    }
    searchInput(event: Event): void {
        this.servicePage.set(0);
        this.search.set((event.target as HTMLInputElement).value);
    }
    statusInput(event: Event): void {
        this.servicePage.set(0);
        this.status.set((event.target as HTMLSelectElement).value);
    }
    changePage(delta: number): void {
        this.servicePage.update((page) => Math.max(0, Math.min(page + delta, this.servicePages() - 1)));
    }
    changeSection(section: AdminSection): void {
        if (this.working()) return;
        this.search.set('');
        this.status.set('all');
        this.servicePage.set(0);
        this.selectedCase.set(null);
        this.detailRequest++;
        this.editing.set(false);
        this.saveError.set('');
        this.message.set('');
        this.caseEditing.set(false);
        this.navigate.emit(section);
    }

    async load(): Promise<void> {
        if (this.saving()) return;
        this.loading.set(true);
        this.failures.set({});
        const [services, cases, lawyers, clients] = await Promise.allSettled([
            this.portal.listServices(),
            this.legal.cases(),
            this.api.lawyers(),
            this.api.clients(),
        ]);
        for (const result of [services, cases, lawyers, clients])
            if (result.status === 'rejected' && httpStatus(result.reason) === 401) {
                this.sessionExpired.emit();
                return;
            }
        const failures: Partial<Record<'services' | 'cases' | 'team', string>> = {};
        if (services.status === 'fulfilled') this.services.set(services.value);
        else failures.services = 'No se pudo consultar el catálogo.';
        if (cases.status === 'fulfilled')
            this.cases.set(
                [...cases.value].sort((a, b) => b.fechaApertura.localeCompare(a.fechaApertura) || b.id - a.id),
            );
        else failures.cases = 'No se pudieron consultar los expedientes.';
        if (lawyers.status === 'fulfilled') this.lawyers.set(lawyers.value);
        else failures.team = 'No se pudo consultar el equipo completo.';
        if (clients.status === 'fulfilled') this.clients.set(clients.value);
        else failures.team = 'No se pudo consultar el equipo completo.';
        this.servicePage.set(Math.min(this.servicePage(), this.servicePages() - 1));
        this.failures.set(failures);
        this.loading.set(false);
    }
    editCase(item: LegalCase | null): void {
        if (this.working()) return;
        this.selectedCase.set(null);
        this.editingCase.set(item);
        this.caseEditing.set(true);
        this.message.set('');
        setTimeout(() => document.getElementById('case-name')?.focus());
    }
    caseSaved(item: LegalCase): void {
        this.cases.update((items) => [item, ...items.filter((c) => c.id !== item.id)]);
        this.caseEditing.set(false);
        this.message.set('El expediente se guardó y está disponible para el cliente y el abogado asignados.');
    }

    edit(service: LegalService | null): void {
        if (this.saving()) return;
        this.editingId.set(service?.id ?? null);
        this.serviceFile.set(null);
        this.saveError.set('');
        this.message.set('');
        this.form.reset({
            nombre: service?.nombre ?? '',
            categoria: service?.categoria ?? '',
            descripcion: service?.descripcion ?? '',
            precio: service?.precio ?? null,
            estado: service?.estado ?? 'ACTIVO',
            tipoEntrega: service?.tipoEntrega ?? 'ASESORIA',
        });
        this.editing.set(true);
        setTimeout(() => document.getElementById('admin-service-name')?.focus());
    }
    cancelEdit(): void {
        if (!this.saving()) {
            this.editing.set(false);
            this.saveError.set('');
        }
    }
    chooseFile(event: Event): void {
        const file = (event.target as HTMLInputElement).files?.[0] ?? null;
        this.saveError.set('');
        if (file && (!file.name.toLowerCase().endsWith('.pdf') || file.size > 10 * 1024 * 1024 || !file.size)) {
            this.saveError.set('Selecciona un PDF de hasta 10 MB.'); this.serviceFile.set(null); return;
        }
        this.serviceFile.set(file);
    }
    requestAssigned(item: LegalCase): void {
        this.cases.update(items => [item, ...items.filter(c => c.id !== item.id)]);
    }
    async saveService(): Promise<void> {
        this.form.markAllAsTouched();
        if (
            this.saving() ||
            this.form.invalid ||
            !this.form.controls.nombre.value.trim() ||
            !this.form.controls.categoria.value.trim()
        )
            return;
        this.saving.set(true);
        this.saveError.set('');
        this.message.set('');
        try {
            const value = this.form.getRawValue();
            const wasNew = this.editingId() === null;
            const file = this.serviceFile();
            const publishAfterUpload = value.tipoEntrega === 'ARCHIVO' && !!file && value.estado === 'ACTIVO';
            let result = await this.api.saveService(this.editingId(), {
                ...value,
                nombre: value.nombre.trim(),
                categoria: value.categoria.trim(),
                descripcion: value.descripcion.trim(),
                precio: value.precio!,
                estado: publishAfterUpload ? 'INACTIVO' : value.estado,
            });
            this.editingId.set(result.id);
            if (file && value.tipoEntrega === 'ARCHIVO') {
                // Keep the draft recoverable if the upload fails.
                this.services.update(items => [...items.filter(i => i.id !== result.id), result]);
                result = await this.api.uploadServiceFile(result.id, file);
                this.serviceFile.set(null);
                if (publishAfterUpload) result = await this.api.saveService(result.id, { ...value, precio: value.precio! });
            }
            this.services.update((items) =>
                [...items.filter((item) => item.id !== result.id), result].sort((a, b) => a.id - b.id),
            );
            this.servicePage.set(Math.min(this.servicePage(), this.servicePages() - 1));
            this.message.set(
                wasNew ? 'Servicio creado correctamente.' : 'Los cambios del servicio se guardaron.',
            );
            this.editing.set(false);
        } catch (error) {
            if (httpStatus(error) === 401) this.sessionExpired.emit();
            else
                this.saveError.set(
                    httpStatus(error) === 403
                        ? 'Tu cuenta no tiene permiso para modificar servicios.'
                        : httpStatus(error) === 422 ? 'Para publicar un recurso digital, adjunta su PDF o guárdalo como inactivo.' : 'No se pudo completar el guardado. Actualiza el catálogo para comprobar el estado del servicio antes de repetir.',
                );
        } finally {
            this.saving.set(false);
        }
    }
    async openCase(item: LegalCase): Promise<void> {
        const request = ++this.detailRequest;
        this.selectedCase.set(item);
        this.documents.set([]);
        this.documentError.set('');
        this.documentLoading.set(true);
        try {
            const documents = await this.legal.documents(item.id);
            if (request === this.detailRequest) this.documents.set(documents);
        } catch (error) {
            if (httpStatus(error) === 401) this.sessionExpired.emit();
            else if (request === this.detailRequest) this.documentError.set('No se pudieron consultar los documentos.');
        } finally {
            if (request === this.detailRequest) this.documentLoading.set(false);
        }
    }
    closeCase(): void {
        this.detailRequest++;
        this.selectedCase.set(null);
    }
    async download(document: CaseDocument): Promise<void> {
        if (this.downloading() !== null) return;
        this.downloading.set(document.id);
        this.documentError.set('');
        try {
            const blob = await this.legal.download(document.id);
            const url = URL.createObjectURL(blob);
            const anchor = globalThis.document.createElement('a');
            anchor.href = url;
            anchor.download =
                document.nombre.replace(/[\\/:*?"<>|]/g, '-') +
                (document.nombre.toLowerCase().endsWith('.' + document.tipoArchivo.toLowerCase())
                    ? ''
                    : '.' + document.tipoArchivo.toLowerCase());
            anchor.click();
            setTimeout(() => URL.revokeObjectURL(url), 1000);
        } catch (error) {
            if (httpStatus(error) === 401) this.sessionExpired.emit();
            else this.documentError.set('No se pudo descargar el documento.');
        } finally {
            this.downloading.set(null);
        }
    }
    private normalize(value: string): string {
        return value
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .toLocaleLowerCase('es-PE')
            .trim();
    }
}
