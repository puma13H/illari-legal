import { Component, input } from '@angular/core';

const paths = {
    calendar: 'M7 2v4M17 2v4M3 5h18v16H3zM3 10h18M7 14h3M14 14h3M7 17h3',
    download: 'M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5',
    upload: 'M12 16V3m-5 5 5-5 5 5M4 16v5h16v-5',
    close: 'm6 6 12 12M6 18 18 6',
    briefcase: 'M8 6V4h8v2M3 6h18v14H3zM3 11c5 3 13 3 18 0M10 12h4v3h-4z',
    clock: 'M12 8v4l3 2M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0',
    check: 'm8 12 3 3 5-6M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0',
    arrow: 'M4 12h16m-6-6 6 6-6 6',
    search: 'm16 16 5 5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
    refresh: 'M20 7v5h-5M4 17v-5h5M5 8a8 8 0 0 1 13-3l2 3M4 16l2 3a8 8 0 0 0 13-3',
    file: 'M14 2H4v20h16V8zM14 2v6h6M8 13h8M8 17h5',
    shield: 'M12 2 3 6v6c0 5 9 10 9 10s9-5 9-10V6zM8 12l3 3 5-6',
    scale: 'M12 3v18M6 21h12M4 7h16M6 7l-4 8h8zM18 7l-4 8h8z',
    logout: 'M9 3H3v18h6M9 12h12m-5-5 5 5-5 5'
};

@Component({
    selector: 'illari-icon',
    template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="path()" /></svg>'
})
export class UiIcon {
    readonly name = input.required<keyof typeof paths>();
    path(): string { return paths[this.name()]; }
}
