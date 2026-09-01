import { Routes } from '@angular/router';

/**
 * Two screens, as specified. No other routes in v1.
 *
 * <p>Both are lazy: the detail page pulls in the markdown renderer and its sanitiser, which
 * the overview never needs.
 */
export const routes: Routes = [
  {
    path: '',
    title: 'Programs · Program Dashboard',
    loadComponent: () =>
      import('./programs/program-overview/program-overview').then((m) => m.ProgramOverview),
  },
  {
    path: 'programs/:slug',
    title: 'Program · Program Dashboard',
    loadComponent: () =>
      import('./programs/program-detail/program-detail').then((m) => m.ProgramDetail),
  },
  { path: '**', redirectTo: '' },
];
