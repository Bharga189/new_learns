import { Component, inject, linkedSignal, OnInit } from '@angular/core';
import { LiveMetricsService, MetricPoint } from './live-metrics.service';

@Component({
  selector: 'app-live-metrics',
  imports: [],
  template: `
    <h2>Live Metrics (Angular 19: signals + linkedSignal)</h2>

    <ul>
      @for (p of points(); track p.at + p.key) {
        <li>
          <button type="button" (click)="selectedKey.set(p.key)">{{ p.key }}</button>
          = {{ p.value }}%
        </li>
      } @empty {
        <li>Waiting for metrics…</li>
      }
    </ul>

    @let key = selectedKey();
    @if (key) {
      <p>Selected key (local linked state): {{ key }}</p>
    }
  `,
})
export class LiveMetricsComponent implements OnInit {
  private readonly svc = inject(LiveMetricsService);
  readonly points = this.svc.points;

  // Derived/resettable local selection based on latest point's key (demo).
  readonly selectedKey = linkedSignal<string | null>(() => this.points()[0]?.key ?? null);

  ngOnInit(): void {
    this.svc.connect();
  }
}
