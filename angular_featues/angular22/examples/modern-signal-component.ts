import { Component, computed, effect, input, signal } from '@angular/core';

/**
 * Angular 22-era representative example (verify exact APIs in your 22.x docs).
 * Demonstrates local state, derived state, and an effect for controlled side effects.
 */
@Component({
  selector: 'app-live-counter',
  template: `
    <p>{{ label() }}: {{ value() }} ({{ parity() }})</p>
    <button type="button" (click)="inc()">+1</button>
  `,
})
export class LiveCounterComponent {
  readonly label = input('Count');
  readonly value = signal(0);
  readonly parity = computed(() => (this.value() % 2 === 0 ? 'even' : 'odd'));

  constructor() {
    effect(() => {
      // Keep effects for synchronization/logging; avoid using them as primary state derivation.
      if (this.value() > 0 && this.value() % 10 === 0) {
        console.log('milestone', this.value());
      }
    });
  }

  inc(): void {
    this.value.update((n) => n + 1);
  }
}
