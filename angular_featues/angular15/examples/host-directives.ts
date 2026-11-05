import { Component, Directive, ElementRef, HostListener, inject, Input } from '@angular/core';

@Directive({
  selector: '[appHoverBorder]',
  standalone: true,
})
export class HoverBorderDirective {
  private readonly el = inject(ElementRef<HTMLElement>);
  @Input() appHoverBorder = '2px solid #1976d2';

  @HostListener('mouseenter') onEnter() {
    this.el.nativeElement.style.outline = this.appHoverBorder;
  }
  @HostListener('mouseleave') onLeave() {
    this.el.nativeElement.style.outline = '';
  }
}

/**
 * Angular 15: Directive Composition API.
 * `hostDirectives` composes behavior into the component host element.
 */
@Component({
  selector: 'app-action-card',
  standalone: true,
  hostDirectives: [
    {
      directive: HoverBorderDirective,
      inputs: ['appHoverBorder: borderStyle'],
    },
  ],
  template: `<ng-content />`,
})
export class ActionCardComponent {
  // Consumers can bind [borderStyle] because we aliased the input above.
}
