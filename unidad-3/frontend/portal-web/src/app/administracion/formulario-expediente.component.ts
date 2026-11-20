import { Component, effect, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminApi, ClientProfile } from '../nucleo/api-administracion';
import { LawyerProfile, LegalCase } from '../nucleo/api-expedientes';
import { httpStatus } from '../nucleo/api-portal';

@Component({
    selector: 'illari-case-editor',
    imports: [ReactiveFormsModule],
    templateUrl: './formulario-expediente.component.html',
})
export class CaseEditorComponent {
    private readonly api = inject(AdminApi);
    readonly clients = input.required<ClientProfile[]>();
    readonly lawyers = input.required<LawyerProfile[]>();
    readonly current = input<LegalCase | null>(null);
    readonly saved = output<LegalCase>();
    readonly cancel = output<void>();
    readonly sessionExpired = output<void>();
    readonly saving = signal(false);
    readonly error = signal('');
    readonly busyChange = output<boolean>();
    readonly form = new FormGroup({
        nombreCaso: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(180)],
        }),
        clienteId: new FormControl('', { nonNullable: true, validators: Validators.required }),
        abogadoId: new FormControl('', { nonNullable: true, validators: Validators.required }),
        estado: new FormControl('Iniciado', { nonNullable: true, validators: Validators.required }),
        fechaApertura: new FormControl('', { nonNullable: true, validators: Validators.required }),
        fechaAudiencia: new FormControl('', { nonNullable: true }),
        descripcion: new FormControl('', { nonNullable: true, validators: Validators.maxLength(2000) }),
        tarifa: new FormControl<number | null>(0, [
            Validators.required,
            Validators.min(0),
            Validators.max(9999999999.99),
            Validators.pattern(/^\d+(\.\d{1,2})?$/),
        ]),
    });
    today(): string {
        const d = new Date();
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    }
    constructor() {
        effect(() => {
            const c = this.current();
            this.error.set('');
            if (c?.pedidoId) this.form.controls.clienteId.disable();
            else this.form.controls.clienteId.enable();
            this.form.reset({
                nombreCaso: c?.nombreCaso ?? '',
                clienteId: c ? String(c.clienteId) : '',
                abogadoId: c ? String(c.abogadoId) : '',
                estado: c?.estado ?? 'Iniciado',
                fechaApertura: c?.fechaApertura ?? this.today(),
                fechaAudiencia: c?.fechaAudiencia ?? '',
                descripcion: c?.descripcion ?? '',
                tarifa: c?.tarifa ?? 0,
            });
        });
    }
    async save(): Promise<void> {
        this.form.markAllAsTouched();
        if (this.saving() || this.form.invalid) return;
        const v = this.form.getRawValue();
        if (v.fechaAudiencia && v.fechaAudiencia < v.fechaApertura) {
            this.error.set('La audiencia no puede ser anterior a la apertura del expediente.');
            return;
        }
        this.saving.set(true);
        this.busyChange.emit(true);
        this.error.set('');
        try {
            this.saved.emit(
                await this.api.saveCase(this.current()?.id ?? null, {
                    ...v,
                    nombreCaso: v.nombreCaso.trim(),
                    clienteId: Number(v.clienteId),
                    abogadoId: Number(v.abogadoId),
                    tarifa: v.tarifa!,
                    fechaAudiencia: v.fechaAudiencia || null,
                }),
            );
        } catch (e) {
            if (httpStatus(e) === 401) this.sessionExpired.emit();
            else
                this.error.set(
                    'No se pudo guardar el expediente. Revisa las personas asignadas y los datos registrados.',
                );
        } finally {
            this.saving.set(false);
            this.busyChange.emit(false);
        }
    }
}
