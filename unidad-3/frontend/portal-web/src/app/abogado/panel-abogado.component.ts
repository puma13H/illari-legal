import { Component, OnInit, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { httpStatus } from '../nucleo/api-portal';
import { CaseDocument, LawyerApi, LawyerProfile, LegalCase } from '../nucleo/api-expedientes';
import { UiIcon } from '../compartido/icono';
import { PageBannerComponent } from '../compartido/portada.component';

export type LawyerSection = 'home' | 'cases' | 'agenda';

@Component({
    selector: 'illari-lawyer-panel',
    imports: [ReactiveFormsModule, UiIcon, PageBannerComponent],
    templateUrl: './panel-abogado.component.html'
})
export class LawyerPanelComponent implements OnInit {
    private readonly api = inject(LawyerApi);
    private readonly formatter = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium' });
    private detailRequest = 0;
    private fileInput: HTMLInputElement | null = null;
    readonly section = input.required<LawyerSection>();
    readonly displayName = input.required<string>();
    readonly sessionExpired = output<void>();
    readonly navigate = output<LawyerSection>();
    readonly cases = signal<LegalCase[]>([]);
    readonly profile = signal<LawyerProfile | null>(null);
    readonly loading = signal(true);
    readonly error = signal('');
    readonly search = signal('');
    readonly filter = signal('all');
    readonly detail = signal<LegalCase | null>(null);
    readonly detailLoading = signal(false);
    readonly detailError = signal('');
    readonly saving = signal(false);
    readonly followupMessage = signal('');
    readonly followupFailed = signal(false);
    readonly documents = signal<CaseDocument[]>([]);
    readonly documentsLoading = signal(false);
    readonly documentError = signal('');
    readonly uploading = signal(false);
    readonly downloading = signal<number | null>(null);
    readonly uploadMessage = signal('');
    readonly uploadFailed = signal(false);
    readonly selectedFile = signal<File | null>(null);
    readonly includePast = signal(false);
    readonly states = ['Iniciado', 'En trámite', 'En revisión', 'Cerrado'];
    readonly followup = new FormGroup({
        estado: new FormControl('', { nonNullable: true, validators: Validators.required }),
        fechaAudiencia: new FormControl('', { nonNullable: true })
    });
    readonly documentName = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(160)] });
    readonly firstName = computed(() => this.displayName().trim().split(/\s+/)[0] || 'profesional');
    readonly activeCases = computed(() => this.cases().filter((item) => !this.closed(item)));
    readonly closedCases = computed(() => this.cases().filter((item) => this.closed(item)));
    readonly demo = computed(() => this.cases().some((item) => item.referencia));
    readonly hearings = computed(() => this.activeCases().filter((item) => item.fechaAudiencia)
        .sort((a, b) => a.fechaAudiencia!.localeCompare(b.fechaAudiencia!)));
    readonly upcoming = computed(() => this.hearings().filter((item) => item.fechaAudiencia! >= this.today()));
    readonly agenda = computed(() => this.includePast() ? this.hearings() : this.upcoming());
    readonly visibleCases = computed(() => {
        if (this.section() === 'home') return this.cases().slice(0, 3);
        const search = this.normalize(this.search().trim());
        const cases = this.cases().filter((item) => (this.filter() === 'all'
            || (this.filter() === 'active' && !this.closed(item)) || (this.filter() === 'closed' && this.closed(item)))
            && this.normalize(`${item.id} ${item.nombreCaso} ${item.clienteNombre}`).includes(search));
        return cases;
    });

    constructor() {
        effect(() => { this.section(); this.closeDetail(); });
    }

    ngOnInit(): void { void this.load(); }

    async load(): Promise<void> {
        if (!this.loading()) this.loading.set(true);
        this.error.set('');
        try {
            const [profile, cases] = await Promise.all([this.api.profile(), this.api.cases()]);
            this.profile.set(profile);
            this.cases.set(cases);
        } catch (error) { this.error.set(this.failure(error, 'No se pudieron cargar tus expedientes. Vuelve a intentarlo.')); }
        finally { this.loading.set(false); }
    }

    async openCase(id: number): Promise<void> {
        window.scrollTo({ top: 0, behavior: 'auto' });
        const request = ++this.detailRequest;
        this.detail.set(null); this.detailLoading.set(true); this.detailError.set('');
        this.documents.set([]); this.documentError.set(''); this.followupMessage.set(''); this.uploadMessage.set('');
        this.selectedFile.set(null); this.documentName.reset();
        try {
            const detail = await this.api.case(id);
            if (request !== this.detailRequest) return;
            this.detail.set(detail);
            this.followup.reset({ estado: detail.estado, fechaAudiencia: detail.fechaAudiencia ?? '' });
            await this.loadDocuments(id);
        } catch (error) { if (request === this.detailRequest) this.detailError.set(this.failure(error, 'No se pudo abrir el expediente.')); }
        finally { if (request === this.detailRequest) this.detailLoading.set(false); }
    }

    closeDetail(): void {
        window.scrollTo({ top: 0, behavior: 'auto' });
        this.detailRequest++;
        this.detail.set(null); this.detailLoading.set(false); this.detailError.set('');
        this.documents.set([]); this.documentsLoading.set(false); this.selectedFile.set(null);
        this.documentName.reset(); this.uploadMessage.set(''); this.followupMessage.set('');
    }

    async loadDocuments(id: number): Promise<void> {
        this.documentsLoading.set(true); this.documentError.set('');
        try {
            const documents = await this.api.documents(id);
            if (this.detail()?.id === id) this.documents.set(documents);
        } catch (error) { if (this.detail()?.id === id) this.documentError.set(this.failure(error, 'No se pudieron consultar los documentos.')); }
        finally { if (this.detail()?.id === id) this.documentsLoading.set(false); }
    }

    async saveFollowup(): Promise<void> {
        const detail = this.detail();
        if (!detail || this.saving() || this.followup.invalid || !this.followup.dirty) return;
        this.saving.set(true); this.followupMessage.set(''); this.followupFailed.set(false);
        try {
            const value = this.followup.getRawValue();
            const updated = await this.api.followup(detail.id, value.estado, value.fechaAudiencia || null);
            this.cases.update((cases) => cases.map((item) => item.id === updated.id ? updated : item));
            if (this.detail()?.id === detail.id) {
                this.detail.set(updated);
                this.followup.reset({ estado: updated.estado, fechaAudiencia: updated.fechaAudiencia ?? '' });
                this.followupMessage.set('Seguimiento actualizado.');
            }
        } catch (error) {
            if (this.detail()?.id === detail.id) { this.followupFailed.set(true); this.followupMessage.set(this.failure(error, 'No se pudo guardar el seguimiento.')); }
        } finally { this.saving.set(false); }
    }

    chooseFile(event: Event): void {
        this.fileInput = event.target as HTMLInputElement;
        const file = this.fileInput.files?.[0] ?? null;
        this.uploadMessage.set(''); this.uploadFailed.set(false); this.selectedFile.set(null);
        if (!file) return;
        if (!file.size || file.size > 5 * 1024 * 1024 || !/\.(pdf|docx?|txt|png|jpe?g)$/i.test(file.name)) {
            this.uploadFailed.set(true); this.uploadMessage.set('Elige un archivo PDF, Word, texto o imagen de hasta 5 MB.');
            this.fileInput.value = ''; return;
        }
        this.selectedFile.set(file);
        if (!this.documentName.value.trim()) this.documentName.setValue(file.name.replace(/\.[^.]+$/, '').slice(0, 160));
    }

    async upload(): Promise<void> {
        const detail = this.detail(), file = this.selectedFile();
        if (!detail || !file || this.documentName.invalid || !this.documentName.value.trim() || this.uploading()) return;
        this.uploading.set(true); this.uploadMessage.set(''); this.uploadFailed.set(false);
        try {
            const document = await this.api.upload(detail.id, this.documentName.value, file);
            if (this.detail()?.id === detail.id) {
                this.documents.update((documents) => [document, ...documents]);
                this.selectedFile.set(null); this.documentName.reset();
                if (this.fileInput) this.fileInput.value = '';
                this.uploadMessage.set('Documento adjuntado al expediente.');
            }
        } catch (error) {
            if (this.detail()?.id === detail.id) { this.uploadFailed.set(true); this.uploadMessage.set(this.failure(error, 'No se pudo adjuntar el documento. Intenta nuevamente.')); }
        } finally { this.uploading.set(false); }
    }

    async download(document: CaseDocument): Promise<void> {
        if (this.downloading() !== null) return;
        this.downloading.set(document.id); this.documentError.set('');
        try {
            const file = await this.api.download(document.id);
            const url = URL.createObjectURL(file);
            const link = window.document.createElement('a');
            link.href = url;
            const extension = '.' + document.tipoArchivo.toLocaleLowerCase('es-PE');
            link.download = document.nombre.toLocaleLowerCase('es-PE').endsWith(extension) ? document.nombre : document.nombre + extension;
            link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
        } catch (error) { this.documentError.set(this.failure(error, 'No se pudo descargar el documento.')); }
        finally { this.downloading.set(null); }
    }

    searchCases(event: Event): void { this.search.set((event.target as HTMLInputElement).value); }
    filterCases(event: Event): void { this.filter.set((event.target as HTMLSelectElement).value); }
    clearFilters(): void { this.search.set(''); this.filter.set('all'); }
    closed(item: LegalCase): boolean { return ['cerrado', 'concluido', 'finalizado', 'cancelado'].includes(this.normalize(item.estado)); }
    dateLabel(value: string | null): string { return value ? this.formatter.format(new Date(value + 'T12:00:00')) : 'Sin fecha registrada'; }
    day(value: string | null): string { return value ? value.slice(8, 10) : ''; }
    month(value: string | null): string { return value ? new Intl.DateTimeFormat('es-PE', { month: 'short' }).format(new Date(value + 'T12:00:00')).replace('.', '') : ''; }
    fileSize(bytes: number): string { return bytes >= 1024 * 1024 ? (bytes / 1024 / 1024).toFixed(1) + ' MB' : Math.max(1, Math.ceil(bytes / 1024)) + ' KB'; }
    private today(): string { const now = new Date(); return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`; }
    private normalize(value: string): string { return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLocaleLowerCase('es-PE'); }
    private failure(error: unknown, fallback: string): string {
        if (httpStatus(error) === 401) this.sessionExpired.emit();
        if (httpStatus(error) === 403) return 'No tienes acceso a estos datos. Comunícate con el administrador.';
        if (httpStatus(error) === 413) return 'El archivo supera los 5 MB permitidos.';
        return fallback;
    }
}
