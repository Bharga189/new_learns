import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { rxResource } from '@angular/core/rxjs-interop'; // API shape varies; verify for your 20.x

interface User { id: number; name: string }

/**
 * Angular 20-era pattern (representative): signal-driven request + resource state.
 * Push realtime streams still typically update a signal store directly.
 */
@Component({
  selector: 'app-user-loader',
  template: `
    <button type="button" (click)="id.update(n => n + 1)">Next</button>
    @if (user.isLoading()) { <p>Loading…</p> }
    @else if (user.error()) { <p>Failed.</p> }
    @else if (user.value(); as u) { <p>{{ u.name }}</p> }
  `,
})
export class UserLoaderComponent {
  private readonly http = inject(HttpClient);
  readonly id = signal(1);

  readonly user = rxResource({
    request: () => this.id(),
    loader: ({ request }) => this.http.get<User>(`/api/users/${request}`),
  });
}
