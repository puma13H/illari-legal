import { Component, input } from '@angular/core';
import { UiIcon } from './icono';

/** Portada común a las áreas del portal; las acciones se proyectan desde cada rol. */
@Component({
    selector: 'illari-page-banner',
    imports: [UiIcon],
    template: `<section class="portal-banner" [class.compact]="compact()">
        <img
            [src]="'/assets/images/portada-' + image() + '-1200.jpg'"
            [srcset]="
                '/assets/images/portada-' +
                image() +
                '-640.jpg 640w, /assets/images/portada-' +
                image() +
                '-1200.jpg 1200w'
            "
            sizes="(max-width: 720px) 100vw, 1160px"
            width="1200"
            height="800"
            alt=""
            aria-hidden="true"
        />
        <div class="portal-banner-copy">
            <p class="portal-eyebrow">{{ eyebrow() }}</p>
            <h1>{{ title() }}</h1>
            <p class="portal-banner-description">{{ description() }}</p>
            <div class="portal-banner-actions"><ng-content /></div>
        </div>
        <span class="portal-banner-signature"
            ><illari-icon name="scale" /><span>ILLARI LEGAL<small>Asesoría y gestión a tu alcance</small></span></span
        >
    </section>`,
})
export class PageBannerComponent {
    readonly eyebrow = input.required<string>();
    readonly title = input.required<string>();
    readonly description = input.required<string>();
    readonly image = input<'asesoria' | 'expedientes'>('asesoria');
    readonly compact = input(false);
}
