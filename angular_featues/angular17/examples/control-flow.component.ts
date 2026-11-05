import { Component, signal } from '@angular/core';

interface Item { id: number; label: string; done: boolean }

@Component({
  selector: 'app-control-flow-demo',
  standalone: true,
  templateUrl: './control-flow.html',
})
export class ControlFlowDemoComponent {
  readonly user = signal<{ name: string } | null>({ name: 'Ada' });
  readonly status = signal<'idle' | 'loading' | 'error'>('idle');
  readonly items = signal<Item[]>([
    { id: 1, label: 'Write docs', done: true },
    { id: 2, label: 'Ship feature', done: false },
  ]);

  clearUser() { this.user.set(null); }
  setStatus(s: 'idle' | 'loading' | 'error') { this.status.set(s); }
}
