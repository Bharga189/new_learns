import { Injectable, Signal, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { webSocket } from 'rxjs/webSocket';
import { catchError, map, retry, startWith } from 'rxjs/operators';
import { of, timer } from 'rxjs';

export interface Ticker {
  symbol: string;
  price: number;
  change: number;
  at: number;
}

export type TickerState =
  | { status: 'connecting' }
  | { status: 'live'; tick: Ticker }
  | { status: 'error'; message: string };

/**
 * Angular 16 realtime pattern: RxJS WebSocket as the source of truth,
 * exposed to the UI as a Signal via toSignal().
 */
@Injectable({ providedIn: 'root' })
export class LiveTickerService {
  // In a real app, inject configuration/environment for the URL.
  private readonly source$ = webSocket<Ticker>('wss://example.invalid/ticker').pipe(
    map((tick): TickerState => ({ status: 'live', tick })),
    retry({ delay: () => timer(2000) }),
    catchError((err) => of<TickerState>({ status: 'error', message: String(err?.message ?? err) })),
    startWith<TickerState>({ status: 'connecting' }),
  );

  // toSignal manages subscription in an injection context.
  readonly state: Signal<TickerState> = toSignal(this.source$, {
    initialValue: { status: 'connecting' },
    requireSync: false,
  });
}
