import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { BehaviorSubject, Observable, Subscription, timer } from 'rxjs';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { retryWhen, tap, delayWhen } from 'rxjs/operators';

export interface PriceTick {
  symbol: string;
  price: number;
  ts: number;
}

/**
 * Angular 13 realtime pattern: RxJS WebSocket + BehaviorSubject.
 * - Manual lifecycle (destroy/close) is common.
 * - Reconnection is composed with RxJS operators.
 * - Consumers typically use the async pipe to avoid manual subscribe/unsubscribe in templates.
 */
@Injectable({ providedIn: 'root' })
export class LivePricesService implements OnDestroy {
  private socket$?: WebSocketSubject<PriceTick>;
  private sub?: Subscription;
  private readonly ticksSubject = new BehaviorSubject<PriceTick | null>(null);

  readonly ticks$: Observable<PriceTick | null> = this.ticksSubject.asObservable();

  constructor(private zone: NgZone) {}

  connect(url = 'wss://example.invalid/prices'): void {
    if (this.socket$ && !this.socket$.closed) return;

    // Run websocket callbacks inside NgZone if updates must trigger change detection
    // in zoneful apps (the default in Angular 13).
    this.socket$ = webSocket<PriceTick>({
      url,
      openObserver: { next: () => console.log('ws open') },
      closeObserver: { next: () => console.log('ws close') },
    });

    this.sub = this.socket$
      .pipe(
        tap((tick) => this.zone.run(() => this.ticksSubject.next(tick))),
        retryWhen((errors) => errors.pipe(delayWhen(() => timer(2000)))),
      )
      .subscribe({ error: (err) => console.error('ws error', err) });
  }

  send(message: unknown): void {
    this.socket$?.next(message as PriceTick);
  }

  close(): void {
    this.sub?.unsubscribe();
    this.socket$?.complete();
    this.socket$ = undefined;
  }

  ngOnDestroy(): void {
    this.close();
    this.ticksSubject.complete();
  }
}
