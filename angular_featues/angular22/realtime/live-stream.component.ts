import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { DatePipe, JsonPipe } from '@angular/common';
import { LiveStreamService } from './live-stream.service';

@Component({
  selector: 'app-live-stream',
  imports: [DatePipe, JsonPipe],
  template: `
    <h2>Live Stream (Angular 22: contemporary realtime pattern)</h2>

    @let st = status();
    <p>
      Status: <strong>{{ st }}</strong>
      @if (lastError(); as err) { <em> — {{ err }}</em> }
    </p>

    <div>
      @if (st === 'idle' || st === 'closed' || st === 'error') {
        <button type="button" (click)="connect()">Connect</button>
      } @else {
        <button type="button" (click)="disconnect()">Disconnect</button>
      }
      <label>
        Filter channel:
        <input [value]="filter()" (input)="filter.set(($any($event.target)).value)" placeholder="(all)" />
      </label>
    </div>

    @if (last(); as m) {
      <section>
        <h3>Latest</h3>
        <p>{{ m.channel }} @ {{ m.receivedAt | date: 'mediumTime' }}</p>
        <pre>{{ m.payload | json }}</pre>
      </section>
    }

    <h3>Events ({{ visible().length }})</h3>
    <ul>
      @for (m of visible(); track m.id) {
        <li>
          <strong>{{ m.channel }}</strong>
          <small>{{ m.receivedAt | date: 'HH:mm:ss' }}</small>
        </li>
      } @empty {
        <li>No events received.</li>
      }
    </ul>
  `,
})
export class LiveStreamComponent implements OnInit, OnDestroy {
  private readonly stream = inject(LiveStreamService);

  readonly status = this.stream.status;
  readonly lastError = this.stream.lastError;
  readonly last = this.stream.lastMessage;
  readonly filter = signal('');

  readonly visible = computed(() => {
    const q = this.filter().trim().toLowerCase();
    const list = this.stream.messages();
    return q ? list.filter((m) => m.channel.toLowerCase().includes(q)) : list;
  });

  ngOnInit(): void {
    this.connect();
  }

  connect(): void {
    this.stream.connect();
  }

  disconnect(): void {
    this.stream.disconnect();
  }

  ngOnDestroy(): void {
    // Optional: disconnect if this component owns the connection lifecycle.
  }
}
