import { Component, OnInit, computed, inject, output, signal } from '@angular/core';
import { LawyerApi, LegalCase, CaseDocument } from '../nucleo/api-expedientes';
import { httpStatus } from '../nucleo/api-portal';
import { UiIcon } from '../compartido/icono';
import { PageBannerComponent } from '../compartido/portada.component';

@Component({
    selector: 'illari-client-cases',
    imports: [UiIcon, PageBannerComponent],
    templateUrl: './mis-expedientes.component.html',
})
export class ClientCasesComponent implements OnInit {
    private readonly api = inject(LawyerApi);
    private request = 0;
    readonly purchase = output<void>();
    readonly sessionExpired = output<void>();
    readonly cases = signal<LegalCase[]>([]);
    readonly loading = signal(true);
    readonly error = signal('');
    readonly search = signal('');
    readonly filter = signal('all');
    readonly selected = signal<LegalCase | null>(null);
    readonly detailLoading = signal(false);
    readonly detailError = signal('');
    readonly documents = signal<CaseDocument[]>([]);
    readonly documentLoading = signal(false);
    readonly documentError = signal('');
    readonly downloading = signal<number | null>(null);
    readonly active = computed(() => this.cases().filter((c) => !this.closed(c)));
    readonly hearings = computed(() =>
        this.active()
            .filter((c) => c.fechaAudiencia && c.fechaAudiencia >= this.today())
            .sort((a, b) => a.fechaAudiencia!.localeCompare(b.fechaAudiencia!)),
    );
    readonly filtered = computed(() =>
        this.cases().filter(
            (c) =>
                (this.filter() === 'all' || (this.filter() === 'closed' ? this.closed(c) : !this.closed(c))) &&
                this.normalize(`${c.id} ${c.nombreCaso} ${c.abogadoNombre}`).includes(this.normalize(this.search())),
        ),
    );
    ngOnInit(): void {
        void this.load();
    }
    today(): string {
        const d = new Date();
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    }
    closed(item: LegalCase): boolean {
        return ['cerrado', 'concluido', 'finalizado', 'cancelado'].includes(this.normalize(item.estado));
    }
    date(value: string | null): string {
        if (!value) return 'Por coordinar';
        const d = new Date(value + 'T12:00:00');
        return Number.isNaN(d.getTime())
            ? 'Por coordinar'
            : new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium' }).format(d);
    }
    money(value: number): string {
        return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(value);
    }
    size(bytes: number): string {
        return bytes >= 1048576 ? (bytes / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.ceil(bytes / 1024)) + ' KB';
    }
    searchInput(event: Event): void {
        this.search.set((event.target as HTMLInputElement).value);
    }
    filterInput(event: Event): void {
        this.filter.set((event.target as HTMLSelectElement).value);
    }
    async load(): Promise<void> {
        this.loading.set(true);
        this.error.set('');
        try {
            const selectedId = this.selected()?.id;
            const items = (await this.api.cases()).sort((a, b) => b.id - a.id);
            this.cases.set(items);
            if (selectedId && this.selected()?.id === selectedId) {
                const current = items.find((item) => item.id === selectedId);
                this.selected.set(current ?? null);
                if (current) await this.loadDocuments(current.id);
            }
        } catch (e) {
            this.fail(e, () => this.error.set('No se pudieron consultar tus expedientes. Vuelve a intentarlo.'));
        } finally {
            this.loading.set(false);
        }
    }
    async open(id: number): Promise<void> {
        const request = ++this.request;
        this.selected.set(null);
        this.detailError.set('');
        this.detailLoading.set(true);
        this.documents.set([]);
        this.documentError.set('');
        window.scrollTo({ top: 0, behavior: 'auto' });
        try {
            const c = await this.api.case(id);
            if (request !== this.request) return;
            this.selected.set(c);
            await this.loadDocuments(id);
        } catch (e) {
            if (request === this.request)
                this.fail(e, () =>
                    this.detailError.set(
                        'Este expediente no está disponible en tu cuenta. Regresa al listado y actualiza la información.',
                    ),
                );
        } finally {
            if (request === this.request) this.detailLoading.set(false);
        }
    }
    close(): void {
        this.request++;
        this.selected.set(null);
        this.detailError.set('');
        this.detailLoading.set(false);
    }
    async loadDocuments(id: number): Promise<void> {
        const request = this.request;
        this.documentLoading.set(true);
        this.documentError.set('');
        try {
            const docs = await this.api.documents(id);
            if (request === this.request) this.documents.set(docs);
        } catch (e) {
            if (request === this.request)
                this.fail(e, () => this.documentError.set('No se pudieron consultar los documentos.'));
        } finally {
            if (request === this.request) this.documentLoading.set(false);
        }
    }
    async download(item: CaseDocument): Promise<void> {
        if (this.downloading() !== null) return;
        this.downloading.set(item.id);
        this.documentError.set('');
        try {
            const blob = await this.api.download(item.id);
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download =
                item.nombre.replace(/[\\/:*?"<>|]/g, '-') +
                (item.nombre.toLowerCase().endsWith('.' + item.tipoArchivo.toLowerCase())
                    ? ''
                    : '.' + item.tipoArchivo.toLowerCase());
            a.click();
            setTimeout(() => URL.revokeObjectURL(url), 1000);
        } catch (e) {
            this.fail(e, () => this.documentError.set('No se pudo descargar el documento. Vuelve a intentarlo.'));
        } finally {
            this.downloading.set(null);
        }
    }
    private fail(e: unknown, callback: () => void): void {
        if (httpStatus(e) === 401) this.sessionExpired.emit();
        else callback();
    }
    private normalize(value: string): string {
        return value
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .toLowerCase()
            .trim();
    }
}
