import { Component, OnInit, OnDestroy, inject, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
    PortalApi,
    LegalService,
    Order,
    Payment,
    YapeConfig,
    OrderResource,
    httpStatus,
    serviceName,
} from './nucleo/api-portal';
import { ClientPanelComponent } from './cliente/panel-cliente.component';
import { YapeSdk } from './pagos/yape-sdk';
import { UiIcon } from './compartido/icono';
import { ServiceCatalogComponent } from './servicios/catalogo-servicios.component';
import { WelcomeComponent } from './bienvenida/bienvenida.component';
import { LawyerPanelComponent, LawyerSection } from './abogado/panel-abogado.component';
import { AdminPanelComponent, AdminSection } from './administracion/panel-admin.component';
import { ClientCasesComponent } from './cliente/mis-expedientes.component';

type View =
    | 'loading'
    | 'login'
    | 'client-home'
    | 'client-cases'
    | 'orders'
    | 'catalog'
    | 'pay'
    | 'pending'
    | 'paid'
    | 'lawyer'
    | 'admin';
const orderStorageKey = 'illari.checkout.order';
const draftStorageKey = 'illari.checkout.draft';

@Component({
    selector: 'illari-portal',
    imports: [
        ReactiveFormsModule,
        ClientPanelComponent,
        ClientCasesComponent,
        UiIcon,
        ServiceCatalogComponent,
        WelcomeComponent,
        LawyerPanelComponent,
        AdminPanelComponent,
    ],
    templateUrl: './portal.component.html',
})
export class PortalComponent implements OnInit, OnDestroy {
    private readonly api = inject(PortalApi);
    private readonly yape = inject(YapeSdk);
    private readonly lawyerPanel = viewChild(LawyerPanelComponent);
    private readonly adminPanel = viewChild(AdminPanelComponent);
    private readonly money = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' });
    private orderId: string | null = null;
    private payment: Payment | null = null;
    private pollTimer?: ReturnType<typeof setTimeout>;
    private pollAttempts = 0;
    private destroyed = false;
    private resourceRequest = 0;
    ngOnDestroy(): void { this.destroyed = true; clearTimeout(this.pollTimer); }
    private pollPending(): void {
        clearTimeout(this.pollTimer);
        if (this.destroyed || this.view() !== 'pending' || !this.canCheck() || this.pollAttempts >= 30) return;
        this.pollTimer = setTimeout(() => {
            if (this.view() !== 'pending' || this.destroyed) return;
            this.pollAttempts++;
            void this.openOrder();
        }, 4000);
    }
    hasAdvisory(): boolean { return this.order()?.detalles.some(item => item.tipoEntrega === 'ASESORIA') ?? false; }

    readonly view = signal<View>('loading');
    readonly authenticated = signal(false);
    readonly openingLogin = signal(false);
    readonly client = signal(false);
    readonly lawyer = signal(false);
    readonly admin = signal(false);
    readonly adminSection = signal<AdminSection>('home');
    readonly lawyerSection = signal<LawyerSection>('home');
    readonly clientName = signal('Cliente');
    readonly busy = signal(false);
    readonly notice = signal({ message: 'Preparando el portal…', error: false });
    readonly services = signal<LegalService[]>([]);
    readonly order = signal<Order | null>(null);
    readonly clientOrders = signal<Order[]>([]);
    readonly paymentConfig = signal<YapeConfig | null>(null);
    readonly receipt = signal<string | null>(null);
    readonly resources = signal<OrderResource[]>([]);
    readonly downloadingGuide = signal(false);
    readonly resourceLoading = signal(false);
    readonly resourceError = signal(false);
    readonly canCheck = signal(true);
    readonly checkout = new FormGroup({
        phone: new FormControl('', { nonNullable: true, validators: Validators.required }),
        email: new FormControl('', { nonNullable: true }),
        otp: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern(/^[0-9]{6}$/)],
        }),
    });

    ngOnInit(): void {
        void this.start();
    }

    format(value: number): string {
        return this.money.format(value);
    }

    initials(): string {
        return this.clientName()
            .split(/\s+/)
            .slice(0, 2)
            .map((part) => part[0] ?? '')
            .join('')
            .toLocaleUpperCase('es-PE');
    }

    summaryName(order: Order): string {
        return order.detalles.map((item) => serviceName(item.nombreServicio)).join(', ');
    }
    serviceName = serviceName;
    yapeMinCentimos(): number {
        return this.api.yapeMinCentimos;
    }
    async loadResources(): Promise<void> {
        const id = this.order()?.id;
        if (!id) return;
        const request = ++this.resourceRequest;
        this.resourceLoading.set(true);
        this.resourceError.set(false);
        try {
            for (let attempt = 0; attempt < 12; attempt++) {
                if (request !== this.resourceRequest || this.destroyed || this.view() !== 'paid' || this.order()?.id !== id) return;
                const order = await this.api.order(String(id));
                if (request !== this.resourceRequest || this.destroyed || this.view() !== 'paid' || this.order()?.id !== id) return;
                if (order.estado === 'PAGADA') {
                    this.order.set(order);
                    const items = await this.api.resources(id);
                    if (request === this.resourceRequest && this.view() === 'paid' && this.order()?.id === id) this.resources.set(items);
                    return;
                }
                await new Promise(resolve => setTimeout(resolve, 600));
            }
            this.resourceError.set(true);
        } catch (e) {
            if (request === this.resourceRequest && this.view() === 'paid' && this.order()?.id === id) {
                if (httpStatus(e) === 401) this.expireSession();
                else this.resourceError.set(true);
            }
        } finally {
            if (request === this.resourceRequest) this.resourceLoading.set(false);
        }
    }
    async downloadResource(resource: OrderResource): Promise<void> {
        const id = this.order()?.id;
        if (!id || this.downloadingGuide()) return;
        this.downloadingGuide.set(true);
        try {
            const blob = await this.api.downloadResource(id, resource.idServicio);
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = resource.titulo.replace(/[^a-zA-Z0-9áéíóúñÁÉÍÓÚÑ ._-]/g, '-') + '.pdf';
            a.click();
            setTimeout(() => URL.revokeObjectURL(url), 1000);
        } catch (e) {
            this.showError(e, 'No se pudo descargar el recurso. Vuelve a consultar el pedido.');
        } finally {
            this.downloadingGuide.set(false);
        }
    }

    async login(): Promise<void> {
        if (this.openingLogin() || this.view() === 'loading') return;
        this.message('');
        this.openingLogin.set(true);
        try {
            await this.api.login();
        } catch {
            this.openingLogin.set(false);
            this.message('No se pudo abrir el inicio de sesión. Vuelve a intentarlo.', true);
        }
    }

    reloadPage(): void {
        location.reload();
    }

    openLawyerSection(section: LawyerSection): void {
        if (!this.lawyer()) return;
        this.lawyerPanel()?.closeDetail();
        this.setOrder(null);
        this.lawyerSection.set(section);
        this.view.set('lawyer');
    }

    openAdminSection(section: AdminSection): void {
        if (!this.admin() || this.adminPanel()?.working()) return;
        this.adminPanel()?.changeSection(section);
        this.adminSection.set(section);
    }
    adminWorking(): boolean {
        return this.adminPanel()?.working() ?? false;
    }
    openClientCases(): void {
        if (this.busy() || !this.client()) return;
        this.checkout.reset();
        this.setOrder(null);
        this.message('');
        this.view.set('client-cases');
    }

    expireSession(): void {
        this.api.clearSession();
        this.authenticated.set(false);
        this.client.set(false);
        this.lawyer.set(false);
        this.admin.set(false);
        this.openingLogin.set(false);
        this.view.set('login');
        this.message('Tu sesión terminó. Inicia sesión para continuar.', true);
    }

    logout(): void {
        sessionStorage.removeItem(orderStorageKey);
        sessionStorage.removeItem(draftStorageKey);
        void this.api.logout()?.catch(() => this.message('No se pudo cerrar la sesión.', true));
    }

    orderCatalogServices(ids: number[]): void {
        const services = [...new Set(ids)].sort((a, b) => a - b);
        if (
            this.busy() ||
            !services.length ||
            services.length > 100 ||
            services.some((id) => !this.services().some((item) => item.id === id))
        )
            return;
        const totalCentimos = this.services()
            .filter((item) => services.includes(item.id))
            .reduce((total, item) => total + Math.round(item.precio * 100), 0);
        if (totalCentimos < this.api.yapeMinCentimos) {
            this.message(
                `El total mínimo para pagar con Yape en este portal es ${this.format(this.api.yapeMinCentimos / 100)}.`,
                true,
            );
            return;
        }
        void this.create(services);
    }

    async openOrder(): Promise<void> {
        clearTimeout(this.pollTimer);
        const id = this.orderId;
        if (!id) return;
        this.view.set('pending');
        this.canCheck.set(true);
        this.busy.set(true);
        this.paymentConfig.set(null);
        this.message('Consultando el pedido…');
        try {
            const order = await this.api.order(id);
            this.order.set(order);
            if (order.metodoPago !== 'YAPE') {
                this.canCheck.set(false);
                this.message('Este pedido usa otro medio de pago. Elige un servicio para pagar con Yape.', true);
                return;
            }
            const payment = await this.findPayment(id);
            this.payment = payment;
            if (order.estado === 'PAGADA' || payment?.estado === 'VALIDADO') {
                this.showPaid(payment);
                return;
            }
            if (!payment) {
                this.message('Estamos preparando tu pago. Consulta el estado en unos segundos.');
                return;
            }
            if (!payment.yape || payment.estado !== 'PENDIENTE') {
                this.canCheck.set(false);
                this.message('Este pedido no admite un pago con código de aprobación.', true);
                return;
            }
            if (payment.yape.rechazoCodigo === 'invalid_transaction_amount') {
                this.canCheck.set(false);
                this.message(
                    'Mercado Pago no admitió el importe de este pedido y no generó una orden de cobro. Estamos revisando el importe; no necesitas ingresar otro código de Yape.',
                    true,
                );
                return;
            }
            if (payment.yape.enVerificacion) {
                this.message('El pago está en verificación. Consulta el estado antes de volver a intentar.');
                return;
            }
            if (Math.round(order.total * 100) < this.api.yapeMinCentimos) {
                this.canCheck.set(false);
                this.message(
                    `El total mínimo para pagar con Yape en este portal es ${this.format(this.api.yapeMinCentimos / 100)}. Este pedido conserva su importe y no se enviará un cobro.`,
                    true,
                );
                return;
            }
            const config = await this.api.yapeConfig(payment.id);
            const phone = config.entorno === 'test' ? /^11111111[1-8]$/ : /^9[0-9]{8}$/;
            this.checkout.controls.phone.setValidators([Validators.required, Validators.pattern(phone)]);
            this.checkout.controls.phone.updateValueAndValidity();
            this.checkout.controls.email.setValidators(
                config.entorno === 'live' ? [Validators.required, Validators.email, Validators.maxLength(254)] : [],
            );
            this.checkout.controls.email.updateValueAndValidity();
            this.checkout.controls.otp.reset();
            this.paymentConfig.set(config);
            this.view.set('pay');
            this.message('');
        } catch (error) {
            const message =
                httpStatus(error) === 404
                    ? 'Este pedido no está disponible en tu cuenta.'
                    : 'No se pudo consultar el pago. Intenta nuevamente.';
            this.showError(error, message);
        } finally {
            this.busy.set(false);
            this.pollPending();
        }
    }

    async pay(): Promise<void> {
        const config = this.paymentConfig();
        const payment = this.payment;
        if (this.busy() || !config || !payment || this.checkout.invalid) return;
        const { phone, otp, email } = this.checkout.getRawValue();
        this.busy.set(true);
        this.message('Validando el código de Yape…');
        try {
            let token: string;
            try {
                token = await this.yape.token(config.llavePublica, phone, otp);
            } catch {
                this.message(
                    'No se pudo validar el código. Revisa tu conexión y genera un código nuevo en Yape.',
                    true,
                );
                return;
            } finally {
                this.checkout.controls.otp.reset();
            }
            this.message('Enviando el pago a Yape…');
            const result = await this.api.charge(
                payment.id,
                token,
                phone,
                config.entorno === 'live' ? email.trim() : undefined,
            );
            if (result.estado === 'VALIDADO') this.showPaid(result);
            else await this.openOrder();
        } catch (error) {
            if (httpStatus(error) === 402) {
                this.message('Yape rechazó el pago. Genera un código nuevo para reintentar.', true);
            } else if (httpStatus(error) === 401) {
                this.showError(error, '');
            } else if (httpStatus(error) === 422) {
                await this.openOrder();
            } else {
                this.view.set('pending');
                this.message('Consulta el estado del pago antes de volver a intentar.', true);
            }
        } finally {
            this.busy.set(false);
            this.pollPending();
        }
    }

    newOrder(): void {
        if (this.busy()) return;
        this.checkout.reset();
        this.setOrder(null);
        void this.loadCatalog();
    }

    clientHome(): void {
        if (this.busy()) return;
        this.checkout.reset();
        this.setOrder(null);
        this.message('');
        this.view.set('client-home');
        void this.refreshClientOrders();
    }

    async openClientOrders(): Promise<void> {
        if (this.busy()) return;
        this.checkout.reset();
        this.setOrder(null);
        this.view.set('orders');
        await this.refreshClientOrders();
    }

    async refreshClientOrders(): Promise<void> {
        if (this.busy()) return;
        this.busy.set(true);
        this.message('Consultando tus pedidos…');
        try {
            this.clientOrders.set(await this.api.myOrders());
            this.message('');
        } catch (error) {
            this.showError(error, 'No se pudieron cargar tus pedidos. Vuelve a intentarlo en unos momentos.');
        } finally {
            this.busy.set(false);
        }
    }

    openClientOrder(id: number): void {
        if (this.busy()) return;
        this.order.set(null);
        this.payment = null;
        this.setOrder(String(id));
        void this.openOrder();
    }

    private async start(): Promise<void> {
        try {
            const queryId = new URLSearchParams(location.search).get('orden');
            const callback = location.pathname === '/auth/callback';
            this.orderId = callback ? sessionStorage.getItem(orderStorageKey) : queryId;
            if (this.orderId && /^[1-9][0-9]{0,15}$/.test(this.orderId)) {
                sessionStorage.setItem(orderStorageKey, this.orderId);
            } else {
                this.orderId = null;
                sessionStorage.removeItem(orderStorageKey);
            }
            this.authenticated.set(await this.api.init());
            this.admin.set(this.authenticated() && this.api.isAdmin());
            this.lawyer.set(this.authenticated() && !this.admin() && this.api.isLawyer());
            this.client.set(this.authenticated() && !this.admin() && !this.lawyer() && this.api.isClient());
            this.clientName.set(this.api.displayName());
            if (!this.authenticated()) {
                this.view.set('login');
                this.message('');
            } else if (this.admin()) {
                this.setOrder(null);
                this.message('');
                this.view.set('admin');
            } else if (this.lawyer()) {
                this.setOrder(null);
                this.message('');
                this.view.set('lawyer');
            } else if (this.client() && this.orderId) {
                this.setOrder(this.orderId);
                await this.openOrder();
            } else if (this.client()) {
                this.clientHome();
            } else {
                this.expireSession();
                this.message('Tu cuenta no tiene un perfil habilitado en el portal. Consulta al administrador.', true);
            }
        } catch {
            if (this.api.ready) this.view.set('login');
            this.message('No se pudo iniciar el portal. Recarga la página o intenta iniciar sesión nuevamente.', true);
        }
    }

    private async loadCatalog(): Promise<void> {
        this.payment = null;
        this.paymentConfig.set(null);
        this.order.set(null);
        this.view.set('catalog');
        this.busy.set(true);
        this.message('Cargando servicios…');
        try {
            this.services.set(
                (await this.api.listServices()).filter((item) => item.estado === 'ACTIVO' && item.precio > 0),
            );
            this.message(
                this.services().length ? '' : 'El catálogo aún está vacío. Un administrador puede publicar servicios.',
            );
        } catch (error) {
            this.services.set([]);
            this.showError(error, 'No se pudo cargar el catálogo. Recarga la página para volver a intentarlo.');
        } finally {
            this.busy.set(false);
        }
    }

    private async create(services: number[]): Promise<void> {
        this.busy.set(true);
        this.message('Creando pedido…');
        try {
            const order = await this.api.createOrder(services, this.draftFor(services));
            this.setOrder(String(order.id));
            sessionStorage.removeItem(draftStorageKey);
            await this.openOrder();
        } catch (error) {
            const message =
                httpStatus(error) === 503
                    ? 'Yape no está disponible en este momento.'
                    : 'No se pudo crear el pedido. Vuelve a intentarlo.';
            this.showError(error, message);
        } finally {
            this.busy.set(false);
        }
    }

    private draftFor(serviceIds: number[]): string {
        const ids = [...serviceIds].sort((a, b) => a - b);
        try {
            const existing = JSON.parse(sessionStorage.getItem(draftStorageKey) ?? 'null') as {
                serviceIds?: number[];
                serviceId?: number;
                key?: string;
            } | null;
            const previousIds = Array.isArray(existing?.serviceIds)
                ? [...existing.serviceIds].sort((a, b) => a - b)
                : typeof existing?.serviceId === 'number'
                  ? [existing.serviceId]
                  : [];
            if (
                typeof existing?.key === 'string' &&
                /^[0-9a-f-]{36}$/i.test(existing.key) &&
                JSON.stringify(previousIds) === JSON.stringify(ids)
            )
                return existing.key;
        } catch {
            /* Replace an unreadable local draft with a valid one. */
        }
        const key = crypto.randomUUID();
        sessionStorage.setItem(draftStorageKey, JSON.stringify({ serviceIds: ids, key }));
        return key;
    }

    private async findPayment(id: string): Promise<Payment | null> {
        for (let attempt = 0; attempt < 8; attempt += 1) {
            const [payment] = await this.api.payments(id);
            if (payment) return payment;
            await new Promise((resolve) => setTimeout(resolve, 700));
        }
        return null;
    }

    private setOrder(id: string | null): void {
        clearTimeout(this.pollTimer);
        this.pollAttempts = 0;
        this.resourceRequest++;
        this.resourceLoading.set(false);
        this.orderId = id;
        const url = new URL('/', location.origin);
        if (id) {
            sessionStorage.setItem(orderStorageKey, id);
            url.searchParams.set('orden', id);
        } else {
            sessionStorage.removeItem(orderStorageKey);
        }
        history.replaceState(null, '', url);
    }

    private showPaid(payment: Payment | null): void {
        clearTimeout(this.pollTimer);
        this.receipt.set(payment?.yape?.cargoId ?? null);
        this.view.set('paid');
        this.message('');
        this.resources.set([]);
        void this.loadResources();
    }

    private message(message: string, error = false): void {
        this.notice.set({ message, error });
    }

    private showError(error: unknown, message: string): void {
        if (httpStatus(error) === 401) {
            this.expireSession();
            return;
        }
        this.message(message, true);
    }
}
