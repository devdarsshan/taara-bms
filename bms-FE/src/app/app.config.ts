import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideClientHydration } from '@angular/platform-browser';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import Aura from '@primeng/themes/aura';
import { definePreset } from '@primeng/themes';
import { ConfirmationService, MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { routes } from './app.routes';

const TaaraBluePreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#eef8ff',
      100: '#d9eeff',
      200: '#b8ddff',
      300: '#88c8ff',
      400: '#51acf6',
      500: '#278ed8',
      600: '#1e72b0',
      700: '#1f5b8c',
      800: '#214d72',
      900: '#213f5d',
      950: '#17283d'
    }
  }
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideClientHydration(),
    provideAnimationsAsync(),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
    providePrimeNG({
      ripple: true,
      inputStyle: 'filled',
      theme: {
        preset: TaaraBluePreset,
        options: {
          darkModeSelector: '.app-dark'
        }
      }
    }),
    MessageService,
    ConfirmationService
  ]
};
