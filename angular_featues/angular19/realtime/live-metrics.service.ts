import { Injectable, OnDestroy, signal } from '@angular/core';

export interface MetricPoint {
  key: string;
  value: number;
  at: number;
}

/**
 * Push-based realtime: update signals from the stream.
 * (resource/rxResource are ideal for request/response; WebSocket pushes fit signal stores.)
 */
@Injectable({ providedIn: 'root' })
export class LiveMetricsService implements OnDestroy {
  readonly points = signal<MetricPoint[]>([]);
  readonly connected = signal(false);
  private timer?: ReturnType<typeof setInterval>;

  connect(): void {
    if (this.timer) return;
    this.connected.set(true);
    this.timer = setInterval(() => {
      const point: MetricPoint = {
        key: ['cpu', 'mem', 'rps'][Math.floor(Math.random() * 3)],
        value: Math.round(Math.random() * 100),
        at: Date.now(),
      };
      this.points.update((list) => [point, ...list].slice(0, 30));
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
    this.connected.set(false);
  }
}
