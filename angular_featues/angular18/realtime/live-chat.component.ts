import { Component, inject, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { LiveChatService } from './live-chat.service';

@Component({
  selector: 'app-live-chat',
  standalone: true,
  imports: [DatePipe],
  template: `
    <h2>Live Chat (Angular 18: @let + signals)</h2>

    @let s = status();
    @let list = messages();

    <p>Status: {{ s }} — messages: {{ list.length }}</p>

    @if (s === 'idle' || s === 'closed') {
      <button type="button" (click)="connect()">Connect</button>
    }

    <ul>
      @for (m of list; track m.id) {
        <li>
          <strong>{{ m.author }}</strong>
          <span>{{ m.at | date: 'shortTime' }}</span>:
          {{ m.text }}
        </li>
      } @empty {
        <li>No messages.</li>
      }
    </ul>
  `,
})
export class LiveChatComponent implements OnInit {
  private readonly chat = inject(LiveChatService);
  readonly status = this.chat.status;
  readonly messages = this.chat.messages;

  ngOnInit(): void {
    this.connect();
  }
  connect(): void {
    this.chat.connect();
  }
}
