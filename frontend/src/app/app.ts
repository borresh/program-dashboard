import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'pd-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, MatToolbarModule],
  template: `
    <mat-toolbar color="primary">
      <span class="font-medium">Program Dashboard</span>
    </mat-toolbar>
    <main class="mx-auto max-w-6xl p-6">
      <router-outlet />
    </main>
  `,
})
export class App {}
