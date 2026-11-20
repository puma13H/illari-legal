import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminApi, ServiceRequest } from '../nucleo/api-administracion';
import { LawyerProfile, LegalCase } from '../nucleo/api-expedientes';
import { httpStatus } from '../nucleo/api-portal';

@Component({
    selector: 'illari-service-requests',
    imports: [ReactiveFormsModule],
    templateUrl: './solicitudes.component.html',
})
export class RequestsComponent implements OnInit {
    private readonly api = inject(AdminApi);
    readonly lawyers = input.required<LawyerProfile[]>();
    readonly assigned = output<LegalCase>();
    readonly sessionExpired = output<void>();
    readonly busyChange = output<boolean>();
    readonly items = signal<ServiceRequest[]>([]);
    readonly page = signal(0);
    readonly hasMore = signal(false);
    readonly loading = signal(false);
    readonly saving = signal(false);
    readonly error = signal('');
    readonly message = signal('');
    readonly selected = signal<ServiceRequest | null>(null);
    readonly form = new FormGroup({
        lawyer: new FormControl('', { nonNullable: true, validators: Validators.required }),
        description: new FormControl('', { nonNullable: true, validators: Validators.maxLength(2000) }),
    });
    readonly money = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' });
    ngOnInit(): void { void this.load(0); }
    async load(page = this.page()): Promise<void> {
        if (this.loading() || this.saving()) return;
        this.loading.set(true); this.error.set('');
        try {
            const result = await this.api.requests(page);
            this.items.set(result.items); this.page.set(result.pagina); this.hasMore.set(result.hayMas);
        } catch (error) {
            if (httpStatus(error) === 401) this.sessionExpired.emit();
            else this.error.set('No se pudieron consultar las solicitudes. Vuelve a actualizar.');
        } finally { this.loading.set(false); }
    }
    choose(item: ServiceRequest): void {
        this.selected.set(item); this.form.reset(); this.error.set(''); this.message.set('');
    }
    async assign(): Promise<void> {
        const item = this.selected();
        this.form.markAllAsTouched();
        if (!item || this.form.invalid || this.saving()) return;
        this.saving.set(true); this.busyChange.emit(true); this.error.set('');
        try {
            const value = this.form.getRawValue();
            const legalCase = await this.api.assignRequest(item, Number(value.lawyer), value.description.trim());
            this.assigned.emit(legalCase); this.selected.set(null);
            this.message.set(`Expediente #${legalCase.id} creado. El cliente y el abogado ya pueden consultarlo.`);
        } catch (error) {
            if (httpStatus(error) === 401) this.sessionExpired.emit();
            else this.error.set('No se pudo asignar. Actualiza la lista: puede que otra persona ya la haya atendido.');
        } finally { this.saving.set(false); this.busyChange.emit(false); }
        await this.load();
    }
}
