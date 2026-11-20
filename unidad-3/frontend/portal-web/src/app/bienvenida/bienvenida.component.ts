import { Component, input, output } from '@angular/core';
import { UiIcon } from '../compartido/icono';

@Component({
    selector: 'illari-welcome',
    imports: [UiIcon],
    templateUrl: './bienvenida.component.html'
})
export class WelcomeComponent {
    readonly initializing = input.required<boolean>();
    readonly redirecting = input.required<boolean>();
    readonly notice = input.required<{ message: string; error: boolean }>();
    readonly signIn = output<void>();
    readonly retry = output<void>();
}
