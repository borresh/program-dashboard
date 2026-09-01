import { bootstrapApplication } from '@angular/platform-browser';

import { App } from './app/app';
import { appConfig } from './app/app.config';
import { loadRuntimeConfig } from './app/core/runtime-config';

// Runtime configuration is fetched before bootstrap rather than through an app
// initializer, so nothing in the application can ever observe a half-configured
// state and the token can be a plain useValue.
loadRuntimeConfig()
  .then((runtimeConfig) => bootstrapApplication(App, appConfig(runtimeConfig)))
  .catch((error: unknown) => {
    console.error('Program Dashboard failed to start.', error);
    document.body.textContent =
      'Program Dashboard failed to start. See the browser console for details.';
  });
