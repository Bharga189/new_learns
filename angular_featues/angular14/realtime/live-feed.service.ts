import { Injectable, inject, NgZone, OnDestroy } from '@angular/core';
import { BehaviorSubject, Observable, Subscription, timer } from 'rxjs';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { retry, tap } from 'rxjs/operators';

export interface FeedEvent {
  id: string;
  message: string;
  at: string;
}

@Injectable({ providedIn: 'root' })
export class LiveFeedService implements OnDestroy {
  // Angular 14+: inject() is available in class field initializers (injection context).
  private readonly zone = inject(NgZone);

  private socket?: WebSocketSubject<FeedEvent | { type: string }>;
  private subscription?: Subscription;
  private readonly eventsSubject = new BehaviorSubject<FeedEvent[]>([]);

  readonly events$: Observable<FeedEvent[]> = this.eventsSubject.asObservable();

  connect(url = 'wss://example.invalid/feed'): void {
    if (this.socket) return;

    this.socket = webSocket<FeedEvent | { type: string }>(url);
    this.subscription = this.socket
      .pipe(
        retry({ delay: () => timer(1500) }),
        tap((payload) => {
          if ('id' in payload) {
            const event = payload as FeedEvent;
            this.zone.run(() => {
              const next = [event, ...this.eventsSubject.value].slice(0, 50);
              this.eventsSubject.next(next);
            });
          }
        }),
      )
      .subscribe({ error: (e) => console.error('feed error', e) });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
    this.socket?.complete();
    this.eventsSubject.complete();
  }
}
