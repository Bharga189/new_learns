import { Injectable, signal, computed } from '@angular/core';

export interface ChatMessage {
  id: string;
  author: string;
  text: string;
  at: number;
}

/**
 * Signal-based realtime state (zoneless-friendly when updates go through signals).
 * Replace connect() transport with WebSocket/SSE in a real app.
 */
@Injectable({ providedIn: 'root' })
export class LiveChatService {
  private socket?: WebSocket;
  readonly messages = signal<ChatMessage[]>([]);
  readonly status = signal<'idle' | 'connecting' | 'open' | 'closed' | 'error'>('idle');
  readonly unreadCount = computed(() => this.messages().filter((m) => m.author !== 'me').length);

  connect(url = 'wss://example.invalid/chat'): void {
    if (this.status() === 'open' || this.status() === 'connecting') return;
    this.status.set('connecting');

    this.socket = new WebSocket(url);
    this.socket.onopen = () => this.status.set('open');
    this.socket.onclose = () => this.status.set('closed');
    this.socket.onerror = () => this.status.set('error');
    this.socket.onmessage = (ev) => {
      const msg = JSON.parse(String(ev.data)) as ChatMessage;
      // Signal update notifies Angular reactively (works well in zoneless setups).
      this.messages.update((list) => [...list, msg].slice(-200));
    };
  }

  send(text: string): void {
    if (this.socket?.readyState !== WebSocket.OPEN) return;
    const msg: ChatMessage = { id: crypto.randomUUID(), author: 'me', text, at: Date.now() };
    this.socket.send(JSON.stringify(msg));
    this.messages.update((list) => [...list, msg]);
  }
}
