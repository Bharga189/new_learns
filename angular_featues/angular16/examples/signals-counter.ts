import { Component, computed, effect, signal } from '@angular/core';

/**
 * Angular 16: Signals (developer preview).
 * - signal(): writable reactive value
 * - computed(): derived reactive value
 * - effect(): side effect that re-runs when tracked signals change
 */
@Component({
  selector: 'app-signals-counter',
  standalone: true,
  template: `
    <p>Count: {{ count() }}</p>
    <p>Doubled: {{ doubled() }}</p>
    <button type="button" (click)="inc()">+1</button>
    <button type="button" (click)="reset()">Reset</button>
  `,
})
export class SignalsCounterComponent {
  readonly count = signal(0);
  readonly doubled = computed(() => this.count() * 2);

  constructor() {
    effect((onCleanup) => {
      const current = this.count();
      console.log('count is now', current);
      onCleanup(() => {/* cleanup if needed */});
    });
  }

  inc(): void {
    this.count.update((n) => n + 1);
  }

  reset(): void {
    this.count.set(0);
  }
}
