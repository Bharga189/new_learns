import { Injectable, computed, signal } from '@angular/core';

export type ConnState = 'idle' | 'connecting' | 'open' | 'reconnecting' | 'closed';
export interface Activity { id: string; type: string; detail: string; at: number }

@Injectable({ providedIn: 'root' })
export class LiveActivityService {
  readonly state = signal<ConnState>('idle');
  readonly items = signal<Activity[]>([]);
  readonly last = computed(() => this.items()[0] ?? null);
  readonly error = signal<string | null>(null);

  private ws?: WebSocket;
  private retries = 0;

  connect(url = 'wss://example.invalid/activity'): void {
    this.open(url);
  }

  private open(url: string): void {
    this.ws?.close();
    this.state.set(this.retries ? 'reconnecting' : 'connecting');
    this.error.set(null);

    const ws = new WebSocket(url);
    this.ws = ws;
    ws.onopen = () => { this.retries = 0; this.state.set('open'); };
    ws.onmessage = (ev) => {
      const item = JSON.parse(ev.data) as Activity;
      this.items.update((xs) => [item, ...xs].slice(0, 100));
    };
    ws.onerror = () => this.error.set('Realtime connection error');
    ws.onclose = () => {
      this.state.set('closed');
      if (this.retries++ < 5) setTimeout(() => this.open(url), Math.min(1000 * this.retries, 8000));
    };
  }

  disconnect(): void {
    this.retries = Number.MAX_SAFE_INTEGER;
    this.ws?.close();
    this.state.set('closed');
  }
}
