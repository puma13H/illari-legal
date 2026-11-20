import { DOCUMENT, Injectable, inject } from '@angular/core';

interface MercadoPagoBrowser {
    yape(data: { otp: string; phoneNumber: string }): { create(): Promise<{ id?: string }> };
}

type MercadoPagoConstructor = new (key: string, options: { locale: 'es-PE' }) => MercadoPagoBrowser;

@Injectable({ providedIn: 'root' })
export class YapeSdk {
    private readonly document = inject(DOCUMENT);
    private loading?: Promise<MercadoPagoConstructor>;

    async token(key: string, phoneNumber: string, otp: string): Promise<string> {
        const MercadoPago = await this.load();
        const result = await new MercadoPago(key, { locale: 'es-PE' }).yape({ otp, phoneNumber }).create();
        if (!result.id) throw new Error('Mercado Pago no devolvió un token de Yape');
        return result.id;
    }

    private load(): Promise<MercadoPagoConstructor> {
        this.loading ??= new Promise<MercadoPagoConstructor>((resolve, reject) => {
            const script = this.document.createElement('script');
            script.src = 'https://sdk.mercadopago.com/js/v2';
            script.referrerPolicy = 'no-referrer';
            script.onload = () => {
                const browser = window as Window & { MercadoPago?: MercadoPagoConstructor };
                if (browser.MercadoPago) resolve(browser.MercadoPago);
                else reject(new Error('SDK de Mercado Pago no disponible'));
            };
            script.onerror = () => reject(new Error('No se pudo cargar el SDK de Mercado Pago'));
            this.document.head.append(script);
        }).catch((error: unknown) => {
            this.loading = undefined;
            throw error;
        });
        return this.loading;
    }
}
