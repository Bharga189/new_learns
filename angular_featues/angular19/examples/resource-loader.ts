import { Component, resource, signal } from '@angular/core';
import { JsonPipe } from '@angular/common';

interface Todo { id: number; title: string; completed: boolean }

/**
 * Angular 19: resource() (experimental) models async fetch state as signals.
 * Use for request/response data; push-based realtime streams usually use a
 * signal store updated by WebSocket/SSE handlers.
 */
@Component({
  selector: 'app-todo-resource',
  imports: [JsonPipe],
  template: `
    <button type="button" (click)="id.set(id() + 1)">Next</button>
    <button type="button" (click)="todo.reload()">Reload</button>

    @if (todo.isLoading()) { <p>Loading…</p> }
    @if (todo.error(); as err) { <p>Error: {{ err }}</p> }
    @if (todo.value(); as value) { <pre>{{ value | json }}</pre> }
  `,
})
export class TodoResourceComponent {
  readonly id = signal(1);

  readonly todo = resource<Todo, { id: number }>({
    request: () => ({ id: this.id() }),
    loader: async ({ request, abortSignal }) => {
      const res = await fetch(`https://jsonplaceholder.typicode.com/todos/${request.id}`, {
        signal: abortSignal,
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      return (await res.json()) as Todo;
    },
  });
}
