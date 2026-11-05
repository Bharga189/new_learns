import { Injectable, inject, NgZone, OnDestroy } from '@angular/core';
import { BehaviorSubject, Observable, Subscription, timer } from 'rxjs';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { catchError, retry, tap } from 'rxjs/operators';
import { EMPTY } from 'rxjs';

export interface DashboardMetric {
  name: string;
  value: number;
  unit?: string;
  updatedAt: number;
}

@Injectable({ providedIn: 'root' })
export class LiveDashboardService implements OnDestroy {
  private readonly zone = inject(NgZone);
  private socket?: WebSocketSubject<DashboardMetric>;
  private sub?: Subscription;
  private readonly state = new BehaviorSubject<Record<string, DashboardMetric>>({});

  /** Snapshot of latest metric per name. */
  readonly metrics$: Observable<Record<string, DashboardMetric>> = this.state.asObservable();

  connect(url = 'wss://example.invalid/metrics'): void {
    if (this.socket) return;
    this.socket = webSocket<DashboardMetric>(url);

    this.sub = this.socket
      .pipe(
        tap((metric) => {
          // Ensure UI updates in zoneful apps.
          this.zone.run(() => this.state.next({ ...this.state.value, [metric.name]: metric }));
        }),
        retry({ count: 5, delay: () => timer(1000) }),
        catchError((err) => {
          console.error('metrics stream failed', err);
          return EMPTY;
        }),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.socket?.complete();
    this.state.complete();
  }
}
