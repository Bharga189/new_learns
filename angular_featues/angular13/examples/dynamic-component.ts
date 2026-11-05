import { Component, Input, ViewChild, ViewContainerRef } from '@angular/core';

@Component({
  selector: 'app-banner',
  standalone: false,
  template: `<section class="banner"><h3>{{ title }}</h3></section>`,
})
export class BannerComponent {
  @Input() title = 'Banner';
}

@Component({
  selector: 'app-dynamic-host',
  standalone: false,
  template: `
    <button type="button" (click)="load()">Load banner</button>
    <ng-container #slot></ng-container>
  `,
})
export class DynamicHostComponent {
  @ViewChild('slot', { read: ViewContainerRef, static: true })
  private slot!: ViewContainerRef;

  load(): void {
    // Angular 13+: no ComponentFactoryResolver needed for this common case.
    this.slot.clear();
    const ref = this.slot.createComponent(BannerComponent);
    ref.setInput?.('title', 'Loaded at ' + new Date().toLocaleTimeString());
    // For broader compatibility in older codebases you might assign:
    // ref.instance.title = '...';
  }
}
