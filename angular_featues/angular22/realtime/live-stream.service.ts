import { Injectable, computed, signal } from '@angular/core';

export type StreamStatus = 'idle' | 'connecting' | 'open' | 'reconnecting' | 'closed' | 'error';

export interface StreamMessage<T = unknown> {
  id: string;
  channel: string;
  payload: T;
  receivedAt: number;
}

/**
 * Angular 22-style realtime store (representative): explicit connection state,
 * signal-based event buffer, computed projections, and zoneless-friendly updates.
 * Swap the transport for WebSocket/SSE as needed.
 */
@Injectable({ providedIn: 'root' })
export class LiveStreamService {
  readonly status = signal<StreamStatus>('idle');
  readonly messages = signal<StreamMessage[]>([]);
  readonly lastError = signal<string | null>(null);
  readonly lastMessage = computed(() => this.messages()[0] ?? null);
  readonly channels = computed(() => [...new Set(this.messages().map((m) => m.channel))].sort());

  private ws?: WebSocket;
  private attempt = 0;
  private manualClose = false;

  connect(url = 'wss://example.invalid/stream'): void {
    this.manualClose = false;
    this.attempt = 0;
    this.open(url);
  }

  private open(url: string): void {
    this.ws?.close();
    this.status.set(this.attempt === 0 ? 'connecting' : 'reconnecting');

    const ws = new WebSocket(url);
    this.ws = ws;

    ws.onopen = () => {
      this.attempt = 0;
      this.lastError.set(null);
      this.status.set('open');
    };

    ws.onmessage = (ev) => {
      try {
        const parsed = JSON.parse(ev.data) as Partial<StreamMessage>;
        const msg: StreamMessage = {
          id: parsed.id ?? crypto.randomUUID(),
          channel: parsed.channel ?? 'default',
          payload: parsed.payload ?? parsed,
          receivedAt: Date.now(),
        };
        this.messages.update((xs) => [msg, ...xs].slice(0, 200));
      } catch (e) {
        this.lastError.set(`Invalid message: ${String(e)}`);
      }
    };

    ws.onerror = () => this.lastError.set('WebSocket error');
    ws.onclose = () => {
      if (this.manualClose) {
        this.status.set('closed');
        return;
      }
      this.status.set('reconnecting');
      const delay = Math.min(1000 * 2 ** this.attempt++, 15000);
      setTimeout(() => this.open(url), delay);
    };
  }

  send(channel: string, payload: unknown): void {
    if (this.ws?.readyState !== WebSocket.OPEN) {
      this.lastError.set('Cannot send: socket not open');
      return;
    }
    this.ws.send(JSON.stringify({ channel, payload, sentAt: Date.now() }));
  }

  disconnect(): void {
    this.manualClose = true;
    this.ws?.close();
    this.status.set('closed');
  }
}
