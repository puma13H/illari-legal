import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminApi, ClientProfile, PersonKind } from '../nucleo/api-administracion';
import { LawyerProfile } from '../nucleo/api-expedientes';
import { httpStatus } from '../nucleo/api-portal';
import { UiIcon } from '../compartido/icono';

type Person = ClientProfile | LawyerProfile;
@Component({
    selector: 'illari-people-management',
    imports: [ReactiveFormsModule, UiIcon],
    templateUrl: './gestion-personas.component.html',
})
export class PeopleManagementComponent {
    private readonly api = inject(AdminApi);
    readonly clients = input.required<ClientProfile[]>();
    readonly lawyers = input.required<LawyerProfile[]>();
    readonly loading = input(false);
    readonly changed = output<void>();
    readonly sessionExpired = output<void>();
    readonly busyChange = output<boolean>();
    readonly kind = signal<PersonKind>('clientes');
    readonly search = signal('');
    readonly editing = signal(false);
    readonly editingId = signal<number | null>(null);
    readonly saving = signal(false);
    readonly error = signal('');
    readonly message = signal('');
    readonly reveal = signal(false);
    readonly visible = computed(() => {
        const text = this.normalize(this.search());
        return (this.kind() === 'clientes' ? this.clients() : this.lawyers()).filter((p) =>
            this.normalize(p.nombre + ' ' + p.email).includes(text),
        );
    });
    readonly form = new FormGroup({
        nombre: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(150)],
        }),
        email: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required, Validators.email, Validators.maxLength(200)],
        }),
        telefono: new FormControl('', { nonNullable: true, validators: Validators.maxLength(30) }),
        direccion: new FormControl('', { nonNullable: true, validators: Validators.maxLength(250) }),
        especialidad: new FormControl('', { nonNullable: true }),
        usuario: new FormControl('', { nonNullable: true }),
        claveInicial: new FormControl('', { nonNullable: true }),
    });
    initials(name: string): string {
        return name
            .split(/\s+/)
            .slice(0, 2)
            .map((p) => p[0])
            .join('')
            .toUpperCase();
    }
    specialty(person: Person): string {
        return 'especialidad' in person ? person.especialidad : 'Cliente de ILLARI LEGAL';
    }
    searchInput(event: Event): void {
        this.search.set((event.target as HTMLInputElement).value);
    }
    select(kind: PersonKind): void {
        if (this.saving()) return;
        this.kind.set(kind);
        this.editing.set(false);
        this.form.reset();
        this.search.set('');
        this.message.set('');
        this.error.set('');
    }
    edit(person: Person | null): void {
        if (this.saving()) return;
        this.editingId.set(person?.id ?? null);
        this.reveal.set(false);
        this.error.set('');
        this.message.set('');
        this.form.controls.especialidad.setValidators(
            this.kind() === 'abogados'
                ? [Validators.required, Validators.pattern(/\S/), Validators.maxLength(150)]
                : [],
        );
        this.form.controls.usuario.setValidators(
            person ? [] : [Validators.required, Validators.pattern(/^[a-zA-Z0-9._-]{3,50}$/)],
        );
        this.form.controls.claveInicial.setValidators(
            person ? [] : [Validators.required, Validators.minLength(12), Validators.maxLength(128)],
        );
        this.form.reset({
            nombre: person?.nombre ?? '',
            email: person?.email ?? '',
            telefono: person?.telefono === 'Por registrar' ? '' : (person?.telefono ?? ''),
            direccion: person?.direccion ?? '',
            especialidad: person && 'especialidad' in person ? person.especialidad : '',
            usuario: '',
            claveInicial: '',
        });
        this.editing.set(true);
        setTimeout(() => document.getElementById('person-name')?.focus());
    }
    cancel(): void {
        if (!this.saving()) {
            this.editing.set(false);
            this.form.reset();
            this.reveal.set(false);
        }
    }
    generate(): void {
        const bytes = crypto.getRandomValues(new Uint8Array(15));
        const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789';
        this.form.controls.claveInicial.setValue('Il!' + [...bytes].map((n) => alphabet[n % alphabet.length]).join(''));
        this.reveal.set(true);
    }
    async save(): Promise<void> {
        this.form.markAllAsTouched();
        if (this.saving() || this.form.invalid) return;
        this.saving.set(true);
        this.busyChange.emit(true);
        this.error.set('');
        try {
            const v = this.form.getRawValue();
            const result = await this.api.savePerson(this.kind(), this.editingId(), {
                nombre: v.nombre.trim(),
                email: v.email.trim(),
                telefono: v.telefono.trim(),
                direccion: v.direccion.trim(),
                especialidad: this.kind() === 'abogados' ? v.especialidad.trim() : undefined,
                ...(this.editingId() === null ? { usuario: v.usuario.trim(), claveInicial: v.claveInicial } : {}),
            });
            this.message.set(
                this.editingId() === null
                    ? `Registro creado. Usuario de acceso: ${result.usuario}. Al ingresar por primera vez se solicitará cambiar la contraseña inicial.`
                    : 'Los datos de contacto y la cuenta vinculada se actualizaron.',
            );
            this.editing.set(false);
            this.form.reset();
            this.reveal.set(false);
            this.changed.emit();
        } catch (e) {
            if (httpStatus(e) === 401) this.sessionExpired.emit();
            else
                this.error.set(
                    httpStatus(e) === 409
                        ? 'El usuario o correo ya pertenece a otro registro. Revisa los datos.'
                        : 'No se pudo guardar el registro. Revisa los datos o vuelve a intentarlo.',
                );
        } finally {
            this.saving.set(false);
            this.busyChange.emit(false);
        }
    }
    private normalize(value: string): string {
        return value
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .toLowerCase()
            .trim();
    }
}
