import { Component, computed, input, output, signal } from '@angular/core';
import { Order, serviceName } from '../nucleo/api-portal';
import { UiIcon } from '../compartido/icono';
import { PageBannerComponent } from '../compartido/portada.component';

type OrderFilter = 'all' | 'pending' | 'paid' | 'cancelled';

const orderStatuses: Readonly<Record<string, string>> = {
    PENDIENTE_PAGO: 'Pendiente de pago',
    PAGADA: 'Pagada',
    CANCELACION_PENDIENTE: 'Cancelación pendiente',
    CANCELADA: 'Cancelada'
};

@Component({
    selector: 'illari-client-panel',
    imports: [UiIcon, PageBannerComponent],
    templateUrl: './panel-cliente.component.html'
})
export class ClientPanelComponent {
    readonly showingOrders = input.required<boolean>();
    readonly loading = input.required<boolean>();
    readonly orders = input.required<Order[]>();
    readonly notice = input.required<{ message: string; error: boolean }>();
    readonly displayName = input.required<string>();
    readonly purchase = output<void>();
    readonly viewOrders = output<void>();
    readonly refresh = output<void>();
    readonly reviewOrder = output<number>();
    readonly search = signal('');
    readonly filter = signal<OrderFilter>('all');
    readonly filters: { id: OrderFilter; label: string }[] = [
        { id: 'all', label: 'Todos' }, { id: 'pending', label: 'Pendientes' },
        { id: 'paid', label: 'Pagados' }, { id: 'cancelled', label: 'Cancelaciones' }
    ];
    readonly pending = computed(() => this.orders().filter((order) => order.estado === 'PENDIENTE_PAGO'));
    readonly paid = computed(() => this.orders().filter((order) => order.estado === 'PAGADA'));
    readonly paidTotal = computed(() => this.paid().reduce((total, order) => total + order.total, 0));
    readonly pendingTotal = computed(() => this.pending().reduce((total, order) => total + order.total, 0));
    readonly sortedOrders = computed(() => [...this.orders()].sort((a, b) =>
        (Date.parse(b.fechaCreacion) || 0) - (Date.parse(a.fechaCreacion) || 0) || b.id - a.id));
    readonly visibleOrders = computed(() => {
        if (!this.showingOrders()) return this.sortedOrders().slice(0, 3);
        const search = this.normalize(this.search().trim());
        return this.sortedOrders().filter((order) => {
            const matchesStatus = this.filter() === 'all'
                || (this.filter() === 'pending' && order.estado === 'PENDIENTE_PAGO')
                || (this.filter() === 'paid' && order.estado === 'PAGADA')
                || (this.filter() === 'cancelled' && ['CANCELADA', 'CANCELACION_PENDIENTE'].includes(order.estado));
            return matchesStatus && this.normalize(`${order.id} ${this.serviceTitle(order)}`).includes(search);
        });
    });
    readonly firstName = computed(() => this.displayName().trim().split(/\s+/)[0] || 'cliente');

    private readonly money = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' });
    private readonly date = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium' });

    searchOrders(event: Event): void { this.search.set((event.target as HTMLInputElement).value); }

    clearFilters(): void { this.search.set(''); this.filter.set('all'); }

    private normalize(value: string): string {
        return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es-PE');
    }

    serviceTitle(order: Order): string {
        return order.detalles.map((item) => serviceName(item.nombreServicio)).join(', ') || 'Servicios legales';
    }

    dateLabel(value: string): string {
        const timestamp = Date.parse(value);
        return Number.isFinite(timestamp) ? this.date.format(timestamp) : '';
    }

    statusLabel(status: string): string { return orderStatuses[status] ?? status.replaceAll('_', ' '); }

    amount(value: number): string { return this.money.format(value); }

    action(order: Order): string | null {
        if (order.metodoPago !== 'YAPE') return null;
        if (order.estado === 'PENDIENTE_PAGO') return 'Continuar pago';
        return order.estado === 'PAGADA' ? 'Ver pago' : null;
    }
}
