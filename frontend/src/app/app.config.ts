import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { RUNTIME_CONFIG, RuntimeConfig } from './core/runtime-config';

export function appConfig(runtimeConfig: RuntimeConfig): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideZonelessChangeDetection(),
      provideRouter(routes),
      provideHttpClient(withFetch()),
      { provide: RUNTIME_CONFIG, useValue: runtimeConfig },
    ],
  };
}
