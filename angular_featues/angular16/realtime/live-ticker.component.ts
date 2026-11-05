import { Component, computed, inject } from '@angular/core';
import { NgIf } from '@angular/common';
import { LiveTickerService } from './live-ticker.service';

@Component({
  selector: 'app-live-ticker',
  standalone: true,
  imports: [NgIf],
  template: `
    <h2>Live Ticker (Angular 16 style: RxJS → Signal)</h2>

    <p *ngIf="state().status === 'connecting'">Connecting…</p>

    <p *ngIf="state().status === 'error'">
      Error: {{ errorMessage() }}
    </p>

    <section *ngIf="liveTick() as tick">
      <strong>{{ tick.symbol }}</strong>
      <span>{{ tick.price }}</span>
      <em>Δ {{ tick.change }}</em>
    </section>
  `,
})
export class LiveTickerComponent {
  private readonly ticker = inject(LiveTickerService);
  readonly state = this.ticker.state;

  readonly liveTick = computed(() => {
    const s = this.state();
    return s.status === 'live' ? s.tick : null;
  });

  readonly errorMessage = computed(() => {
    const s = this.state();
    return s.status === 'error' ? s.message : '';
  });
}
