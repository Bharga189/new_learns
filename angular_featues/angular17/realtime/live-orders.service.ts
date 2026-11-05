import { Injectable, OnDestroy, signal } from '@angular/core';
import { Subscription, interval } from 'rxjs';
import { map } from 'rxjs/operators';

export interface OrderEvent {
  id: number;
  item: string;
  total: number;
  at: Date;
}

/**
 * Demo realtime source (replace interval with webSocket/SSE in production).
 * Exposes state as signals for ergonomic consumption in Angular 17 templates.
 */
@Injectable({ providedIn: 'root' })
export class LiveOrdersService implements OnDestroy {
  readonly connected = signal(false);
  readonly latest = signal<OrderEvent | null>(null);
  readonly orders = signal<OrderEvent[]>([]);

  private sub?: Subscription;
  private seq = 0;

  connect(): void {
    if (this.connected()) return;
    this.connected.set(true);
    this.sub = interval(1200)
      .pipe(
        map((): OrderEvent => ({
          id: ++this.seq,
          item: ['Keyboard', 'Mouse', 'Monitor', 'Cable'][this.seq % 4],
          total: Math.round((20 + Math.random() * 200) * 100) / 100,
          at: new Date(),
        })),
      )
      .subscribe((event) => {
        this.latest.set(event);
        this.orders.update((list) => [event, ...list].slice(0, 20));
      });
  }

  disconnect(): void {
    this.sub?.unsubscribe();
    this.sub = undefined;
    this.connected.set(false);
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
