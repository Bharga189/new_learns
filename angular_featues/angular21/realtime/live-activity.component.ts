import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { LiveActivityService } from './live-activity.service';

@Component({
  selector: 'app-live-activity',
  imports: [DatePipe],
  template: `
    <h2>Live Activity (Angular 21: signal store, zoneless-ready)</h2>

    @let st = state();
    <p>
      Connection: {{ st }}
      @if (error(); as e) { <span> — {{ e }}</span> }
    </p>

    @if (st === 'idle' || st === 'closed') {
      <button type="button" (click)="connect()">Connect</button>
    } @else {
      <button type="button" (click)="disconnect()">Disconnect</button>
    }

    @if (last(); as item) {
      <p>Latest: {{ item.type }} / {{ item.detail }} @ {{ item.at | date: 'mediumTime' }}</p>
    }

    <ul>
      @for (a of items(); track a.id) {
        <li>{{ a.type }} — {{ a.detail }}</li>
      } @empty {
        <li>No activity.</li>
      }
    </ul>
  `,
})
export class LiveActivityComponent implements OnInit, OnDestroy {
  private readonly svc = inject(LiveActivityService);
  readonly state = this.svc.state;
  readonly items = this.svc.items;
  readonly last = this.svc.last;
  readonly error = this.svc.error;

  ngOnInit(): void { this.connect(); }
  connect(): void { this.svc.connect(); }
  disconnect(): void { this.svc.disconnect(); }
  ngOnDestroy(): void { /* keep service alive if app-scoped; disconnect for demo if desired */ }
}
