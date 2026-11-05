import { AsyncPipe, KeyValuePipe, NgFor, NgIf } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { LiveDashboardService } from './live-dashboard.service';

@Component({
  selector: 'app-live-dashboard',
  standalone: true,
  imports: [AsyncPipe, NgFor, NgIf, KeyValuePipe],
  template: `
    <h2>Live Dashboard (Angular 15 style)</h2>
    <ng-container *ngIf="metrics$ | async as metrics">
      <p *ngIf="(metrics | keyvalue).length === 0">Connecting…</p>
      <ul>
        <li *ngFor="let item of metrics | keyvalue">
          {{ item.value.name }}: {{ item.value.value }} {{ item.value.unit || '' }}
        </li>
      </ul>
    </ng-container>
  `,
})
export class LiveDashboardComponent implements OnInit {
  private readonly dashboard = inject(LiveDashboardService);
  readonly metrics$ = this.dashboard.metrics$;

  ngOnInit(): void {
    this.dashboard.connect();
  }
}
