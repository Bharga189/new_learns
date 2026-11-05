import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

/**
 * Angular 14 introduced standalone components/directives/pipes as a developer preview.
 * A standalone component declares its own dependencies via `imports`.
 */
@Component({
  selector: 'app-greeting',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article>
      <h3>Hello, {{ name }}!</h3>
      <p *ngIf="role">Role: {{ role }}</p>
    </article>
  `,
})
export class GreetingComponent {
  @Input({ required: false }) name = 'Angular';
  @Input() role?: string;
}

// Bootstrapping (conceptual, main.ts):
// import { bootstrapApplication } from '@angular/platform-browser';
// bootstrapApplication(GreetingComponent, { providers: [] });
