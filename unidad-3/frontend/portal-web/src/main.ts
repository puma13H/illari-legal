import { provideHttpClient } from '@angular/common/http';
import { bootstrapApplication } from '@angular/platform-browser';
import { PortalComponent } from './app/portal.component';

void bootstrapApplication(PortalComponent, { providers: [provideHttpClient()] });
