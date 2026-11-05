import { Component, inject, OnInit } from '@angular/core';
import { LiveStatusService } from './live-status.service';

@Component({
  selector: 'app-live-status',
  template: `
    <h2>Live Status (Angular 20: zoneless-friendly signals + SSE)</h2>

    @if (hasDown()) {
      <p role="alert">One or more services are down.</p>
    }

    <table>
      <tbody>
        @for (row of latest(); track row.service) {
          <tr>
            <td>{{ row.service }}</td>
            <td>{{ row.state }}</td>
            <td>{{ row.latencyMs }} ms</td>
          </tr>
        } @empty {
          <tr><td colspan="3">Waiting for status events…</td></tr>
        }
      </tbody>
    </table>
  `,
})
export class LiveStatusComponent implements OnInit {
  private readonly svc = inject(LiveStatusService);
  readonly latest = this.svc.latestByService;
  readonly hasDown = this.svc.hasDown;

  ngOnInit(): void {
    this.svc.connect();
  }
}
