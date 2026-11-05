import { Component, OnDestroy, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { LivePricesService, PriceTick } from './live-prices.service';

@Component({
  selector: 'app-live-prices',
  standalone: false,
  template: `
    <h2>Live Prices (Angular 13 style)</h2>

    <!-- async pipe handles subscribe/unsubscribe in the template -->
    <p *ngIf="ticks$ | async as tick; else waiting">
      {{ tick.symbol }}: {{ tick.price }} ({{ tick.ts | date: 'mediumTime' }})
    </p>
    <ng-template #waiting>Waiting for data…</ng-template>
  `,
})
export class LivePricesComponent implements OnInit, OnDestroy {
  readonly ticks$: Observable<PriceTick | null> = this.prices.ticks$;

  constructor(private prices: LivePricesService) {}

  ngOnInit(): void {
    this.prices.connect();
  }

  ngOnDestroy(): void {
    // In a root-provided service this may be app-lifetime; component-level
    // services would typically be closed here.
    // this.prices.close();
  }
}
