import { Component, input, linkedSignal } from '@angular/core';

/**
 * Angular 19: linkedSignal creates writable state derived from a reactive source.
 * When the source (here, an input) changes, the linked signal recomputes/resets.
 */
@Component({
  selector: 'app-editable-label',
  standalone: true, // optional to state in v19; standalone is default
  template: `
    <label>
      Edit (resets when source changes): 
      <input [value]="draft()" (input)="draft.set(($any($event.target)).value)" />
    </label>
    <p>Source: {{ label() }} | Draft: {{ draft() }}</p>
    <button type="button" (click)="save()">Save draft</button>
  `,
})
export class EditableLabelComponent {
  readonly label = input.required<string>();
  readonly draft = linkedSignal(() => this.label());

  save(): void {
    // In a real app, persist draft() and/or update the source.
    console.log('save', this.draft());
  }
}
