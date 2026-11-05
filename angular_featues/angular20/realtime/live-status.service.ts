import { Injectable, computed, signal } from '@angular/core';

export interface StatusEvent {
  service: string;
  state: 'up' | 'degraded' | 'down';
  latencyMs: number;
  at: number;
}

@Injectable({ providedIn: 'root' })
export class LiveStatusService {
  readonly events = signal<StatusEvent[]>([]);
  readonly latestByService = computed(() => {
    const map = new Map<string, StatusEvent>();
    for (const e of [...this.events()].reverse()) map.set(e.service, e);
    return [...map.values()].sort((a, b) => a.service.localeCompare(b.service));
  });
  readonly hasDown = computed(() => this.latestByService().some((e) => e.state === 'down'));

  private es?: EventSource;

  connect(url = '/api/status-stream'): void {
    if (this.es) return;
    // SSE is a good fit for server->client realtime status; WebSocket fits bidirectional cases.
    this.es = new EventSource(url);
    this.es.onmessage = (ev) => {
      const event = JSON.parse(ev.data) as StatusEvent;
      // Signal updates drive rendering (zoneless-friendly).
      this.events.update((list) => [event, ...list].slice(0, 100));
    };
    this.es.onerror = () => {
      this.es?.close();
      this.es = undefined;
    };
  }
}
