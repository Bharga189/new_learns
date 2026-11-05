import { Component, inject, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { LiveOrdersService } from './live-orders.service';

@Component({
  selector: 'app-live-orders',
  standalone: true,
  imports: [DatePipe],
  template: `
    <h2>Live Orders (Angular 17: @if/@for + @defer)</h2>

    @if (connected()) {
      <p>Connected. Latest: {{ latest()?.item ?? '—' }}</p>
    } @else {
      <button type="button" (click)="connect()">Connect</button>
    }

    <ul>
      @for (o of orders(); track o.id) {
        <li>#{{ o.id }} {{ o.item }} — \${{ o.total }} ({{ o.at | date: 'mediumTime' }})</li>
      } @empty {
        <li>No orders yet.</li>
      }
    </ul>

    @defer (on interaction) {
      <aside>
        <h3>Details (deferred)</h3>
        <p>Total events received: {{ orders().length }}</p>
      </aside>
    } @placeholder {
      <p><em>Interact to load order details panel.</em></p>
    }
  `,
})
export class LiveOrdersComponent implements OnInit {
  private readonly svc = inject(LiveOrdersService);
  readonly connected = this.svc.connected;
  readonly latest = this.svc.latest;
  readonly orders = this.svc.orders;

  ngOnInit(): void {
    this.connect();
  }

  connect(): void {
    this.svc.connect();
  }
}
