import { AsyncPipe, NgFor } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { LiveFeedService } from './live-feed.service';

@Component({
  selector: 'app-live-feed',
  standalone: true,
  imports: [AsyncPipe, NgFor],
  template: `
    <h2>Live Feed (Angular 14 style)</h2>
    <ul>
      <li *ngFor="let e of (events$ | async) ?? []">
        <strong>{{ e.at }}</strong>: {{ e.message }}
      </li>
    </ul>
  `,
})
export class LiveFeedComponent implements OnInit {
  private readonly feed = inject(LiveFeedService);
  readonly events$ = this.feed.events$;

  ngOnInit(): void {
    this.feed.connect();
  }
}
