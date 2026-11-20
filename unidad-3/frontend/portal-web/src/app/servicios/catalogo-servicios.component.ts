import { Component, computed, input, output, signal } from '@angular/core';
import { LegalService } from '../nucleo/api-portal';
import { UiIcon } from '../compartido/icono';
import { PageBannerComponent } from '../compartido/portada.component';

type CatalogSort = 'name' | 'lowest' | 'highest';

@Component({
    selector: 'illari-service-catalog',
    imports: [UiIcon, PageBannerComponent],
    templateUrl: './catalogo-servicios.component.html',
})
export class ServiceCatalogComponent {
    readonly services = input.required<LegalService[]>();
    readonly loading = input.required<boolean>();
    readonly notice = input.required<{ message: string; error: boolean }>();
    readonly yapeMinCentimos = input(124);
    readonly continueToPayment = output<number[]>();
    readonly reload = output<void>();
    readonly search = signal('');
    readonly category = signal('');
    readonly sort = signal<CatalogSort>('name');
    readonly selectedIds = signal<number[]>([]);
    readonly selected = computed(() => this.services().filter((service) => this.selectedIds().includes(service.id)));
    readonly total = computed(
        () => this.selected().reduce((cents, service) => cents + Math.round(service.precio * 100), 0) / 100,
    );
    readonly belowMinimum = computed(
        () => this.selected().length > 0 && Math.round(this.total() * 100) < this.yapeMinCentimos(),
    );
    readonly categories = computed(() =>
        [...new Set(this.services().map((service) => this.categoryLabel(service)))].sort((a, b) =>
            a.localeCompare(b, 'es'),
        ),
    );
    readonly filtered = computed(() => {
        const search = this.normalize(this.search().trim());
        const services = this.services().filter(
            (service) =>
                (!this.category() || this.categoryLabel(service) === this.category()) &&
                this.normalize(
                    `${service.nombre} ${service.descripcion ?? ''} ${this.categoryLabel(service)}`,
                ).includes(search),
        );
        return services.sort((a, b) => {
            const byName = a.nombre.localeCompare(b.nombre, 'es');
            if (this.sort() === 'lowest') return a.precio - b.precio || byName;
            if (this.sort() === 'highest') return b.precio - a.precio || byName;
            return byName;
        });
    });
    private readonly money = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' });

    amount(value: number): string {
        return this.money.format(value);
    }

    photo(service: LegalService): string {
        return this.icon(service) === 'scale' ? 'asesoria' : 'expedientes';
    }

    categoryLabel(service: LegalService): string {
        const category = service.categoria?.trim() || 'Servicios legales';
        return category === category.toLocaleUpperCase('es-PE')
            ? category.charAt(0).toLocaleUpperCase('es-PE') + category.slice(1).toLocaleLowerCase('es-PE')
            : category;
    }

    icon(service: LegalService): 'file' | 'scale' | 'briefcase' {
        const category = this.normalize(this.categoryLabel(service));
        if (category.includes('asesor')) return 'scale';
        if (category.includes('prueba') || category.includes('capacit')) return 'briefcase';
        return 'file';
    }

    searchServices(event: Event): void {
        this.search.set((event.target as HTMLInputElement).value);
    }
    changeCategory(event: Event): void {
        this.category.set((event.target as HTMLSelectElement).value);
    }
    changeSort(event: Event): void {
        const value = (event.target as HTMLSelectElement).value;
        if (value === 'name' || value === 'lowest' || value === 'highest') this.sort.set(value);
    }

    isSelected(id: number): boolean {
        return this.selectedIds().includes(id);
    }
    select(id: number): void {
        if (this.loading() || !this.services().some((service) => service.id === id)) return;
        this.selectedIds.update((ids) => (ids.includes(id) ? ids.filter((item) => item !== id) : [...ids, id]));
    }
    remove(id: number): void {
        if (!this.loading()) this.selectedIds.update((ids) => ids.filter((item) => item !== id));
    }
    clearSelection(): void {
        if (!this.loading()) this.selectedIds.set([]);
    }
    clearFilters(): void {
        this.search.set('');
        this.category.set('');
    }
    continue(): void {
        const services = this.selected();
        if (services.length && !this.belowMinimum() && !this.loading() && !this.notice().error)
            this.continueToPayment.emit(services.map((service) => service.id));
    }

    private normalize(value: string): string {
        return value
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .toLocaleLowerCase('es-PE');
    }
}
