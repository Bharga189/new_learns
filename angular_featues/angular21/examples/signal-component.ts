import { Component, computed, input, output } from '@angular/core';

/**
 * Angular 21-era representative example: signal inputs/outputs and computed state.
 * Verify exact API details for your installed Angular version.
 */
@Component({
  selector: 'app-stock-row',
  template: `
    <button type="button" (click)="select.emit(symbol())">
      {{ symbol() }} — {{ price() }} ({{ trend() }})
    </button>
  `,
})
export class StockRowComponent {
  readonly symbol = input.required<string>();
  readonly price = input.required<number>();
  readonly previous = input(0);
  readonly select = output<string>();

  readonly trend = computed(() => {
    const delta = this.price() - this.previous();
    if (delta > 0) return 'up';
    if (delta < 0) return 'down';
    return 'flat';
  });
}
