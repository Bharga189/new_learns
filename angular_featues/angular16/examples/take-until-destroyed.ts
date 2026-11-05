import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval, map } from 'rxjs';

@Component({
  selector: 'app-heartbeat',
  standalone: true,
  template: `<p>Heartbeat tick: {{ last }}</p>`,
})
export class HeartbeatComponent {
  private readonly destroyRef = inject(DestroyRef);
  last = 0;

  constructor() {
    // takeUntilDestroyed ties the subscription to the component/injection context lifetime.
    interval(1000)
      .pipe(
        map((n) => n + 1),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((n) => (this.last = n));
  }
}
