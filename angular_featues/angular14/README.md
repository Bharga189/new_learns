# Angular 14 (June 2, 2022) — Deep Dive

Angular 14 is a major developer-experience release. It introduces **standalone components/directives/pipes** (developer preview), **strictly typed reactive forms**, and **`inject()`** for dependency injection in valid injection contexts. It also improves router ergonomics, template diagnostics, and modern Angular authoring patterns.

> **Upgrade context:** Angular 14 typically pairs with TypeScript ~4.6–4.7 (verify your exact 14.x requirements). Use `ng update @angular/core@14 @angular/cli@14`.

---

## 1. Standalone components, directives, and pipes (developer preview)

### What changed
Components/directives/pipes can be declared with `standalone: true` and list their own dependencies via `imports`. They do **not** need to be declared in an `NgModule` (though they can interoperate with NgModules).

### Why it matters
- **Less boilerplate:** fewer NgModules for simple features.
- **Clear dependencies:** a component’s template dependencies live next to the component.
- **Incremental adoption:** standalone and NgModule-based code can coexist.
- **Path to simplified bootstrapping:** later (v15+) `bootstrapApplication` and standalone-first patterns become standard.

### Standalone component
```ts
import { Component } from '@angular/core';
import { NgIf } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-hello',
  standalone: true,
  imports: [NgIf, RouterLink],
  template: `
    <h1>Hello</h1>
    <a routerLink="/home">Home</a>
    <p *ngIf="true">Rendered</p>
  `,
})
export class HelloComponent {}
```

### Using standalone in an NgModule (interoperability)
```ts
@NgModule({
  imports: [HelloComponent], // standalone components are imported, not declared
  declarations: [LegacyComponent],
})
export class AppModule {}
```

### Routing with standalone (`loadComponent`)
```ts
export const routes: Routes = [
  {
    path: 'admin',
    loadComponent: () => import('./admin/admin.component').then(m => m.AdminComponent),
  },
];
```

### Caveats (v14 preview)
- Standalone APIs were **developer preview** in 14; ergonomics/best practices stabilized further in 15+.
- Be mindful of which directives/pipes you import (`NgIf` vs `CommonModule`, etc.).

See `examples/standalone.component.ts`.

---

## 2. Strictly typed reactive forms (major)

### What changed
Angular reactive forms became **strongly typed** by default in v14. `FormControl`, `FormGroup`, and `FormArray` carry type information, reducing `any`/unsafe access.

### Why it matters
- Compile-time checks for control names and value shapes.
- Better autocomplete/refactoring.
- Catches mismatches between form model and domain model earlier.

### Typed `FormGroup` / `FormControl`
```ts
import { FormControl, FormGroup, Validators } from '@angular/forms';

const form = new FormGroup({
  email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
  password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
});

form.controls.email.setValue('a@b.com'); // typed
const value = form.getRawValue(); // { email: string; password: string }
```

### `nonNullable` and nullability
- `nonNullable: true` (or `NonNullableFormBuilder`) keeps the control’s value within its non-null type.
- Choose based on your UX: can a field truly be “unset/null”?

### `UntypedForm*` escape hatch (migration)
```ts
import { UntypedFormGroup, UntypedFormControl } from '@angular/forms';
const legacy = new UntypedFormGroup({ name: new UntypedFormControl('') });
```
Treat untyped aliases as temporary and migrate to typed forms.

### Dynamic forms / `FormRecord`
When control keys are not known statically, use `FormRecord` (or appropriate typed constructs) instead of falling back to `any`.

### Caveats
- Template-driven forms typing is different; typed reactive forms are the main v14 win.
- Third-party libraries may lag in typings—check compatibility.

See `examples/typed-forms.ts`.

---

## 3. `inject()` function

### What changed
`inject(Token)` retrieves a dependency from Angular’s DI system in a valid **injection context** (e.g., constructor, field initializer in an Angular-managed class, provider `useFactory`, and other Angular-executed contexts).

### Why it matters
- Enables functional/composable patterns (standalone/router evolution).
- Reduces constructor boilerplate in some styles.
- Useful in provider factories and utilities that run within DI context.

### Basic usage
```ts
import { Component, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({ selector: 'app-data', standalone: true, template: `...` })
export class DataComponent {
  private http = inject(HttpClient);
  load() { return this.http.get('/api/items'); }
}
```

### Injection context rules
`inject()` must run where Angular has an active injection context. Valid places include: class field initializers (during construction), constructors, and provider `useFactory` functions. It is **not valid** in arbitrary callbacks, `setTimeout` handlers, or plain functions called outside a context—capture dependencies first or use `runInInjectionContext` patterns where your version supports them.

### Options (verify signature in your version)
```ts
const svc = inject(MyService, { optional: true });
```
Flags like `optional`, `self`, `skipSelf`, and `host` exist in DI options—confirm exact API for 14.x.

### Caveats
- Overuse can obscure dependencies; adopt team conventions.
- Be careful with inheritance and field initialization order.

See `examples/standalone.component.ts` and `realtime/live-feed.service.ts`.

---

## 4. Router improvements

### 4.1 Page `title`
Routes can specify a static `title`, and router support for title management improved (custom `TitleStrategy` patterns became more prominent across 14+).

```ts
export const routes: Routes = [
  { path: 'about', component: AboutComponent, title: 'About Us' },
];
```
For dynamic titles, implement/extend a `TitleStrategy` appropriate to your version.

### 4.2 Standalone lazy loading
Use `loadComponent` (see above) to lazy-load a standalone component without a lazy NgModule wrapper.

### Caveats
- Title behavior details and strategy APIs evolved; confirm the exact approach for 14.x.

---

## 5. Extended template diagnostics

### What changed
The compiler/CLI added more configurable template diagnostics (beyond strict type checking) to catch common mistakes.

### Why it matters
- Catches issues like invalid banana-in-box usage and other template problems (checks vary by 14.x).
- Configurable severity allows gradual adoption.

### Configuration (conceptual; verify schema for 14.x)
```json
{
  "angularCompilerOptions": {
    "extendedDiagnostics": {
      "checks": {
        "invalidBananaInBox": "warning"
      }
    }
  }
}
```

---

## 6. Modern DI ergonomics (direction)

Angular’s DI continued evolving in the standalone era. In v14, groundwork around `inject()`, standalone providers, and injector APIs made more composable patterns feasible. For advanced injector creation (`EnvironmentInjector` patterns, `runInInjectionContext`), verify the exact 14.x API surface.

---

## 7. CLI / template / CDK notes (representative)

- **Strict template checking:** language-service and compiler diagnostics help catch invalid template access patterns.
- **Dynamic views / CDK:** improvements supported advanced scenarios.
- **CLI:** continued workflow refinements (building on persistent cache from 13).

---

## Realtime example (Angular 14 idiom) — detailed

**Folder:** `realtime/`

### Files
- `realtime/live-feed.service.ts` — leverages `inject()`-friendly patterns and exposes an RxJS-based realtime feed.
- `realtime/live-feed.component.ts` — standalone component consuming the feed.

### How it works
1. **DI style:** `inject()` can be used in valid contexts (e.g., field initializers/services).
2. **Transport/state:** RxJS manages the live stream; use multicasting (`shareReplay`, subjects) to avoid duplicate connections.
3. **Rendering:** `async` pipe remains a solid choice (signals arrive as preview in v16).
4. **Lifecycle:** clean up subscriptions if not using `async` pipe; close connections when the owning scope is destroyed.

### Production considerations
Same as v13 (auth, reconnect/backoff, schema validation, ordering, cleanup). If using `inject()` in utilities, ensure they run in an injection context or accept dependencies explicitly.

---

## Migration checklist (13 → 14)
- [ ] Run `ng update` for core/CLI/material as applicable.
- [ ] Migrate reactive forms to typed forms (or temporarily use `UntypedForm*`).
- [ ] Consider `inject()` in new code/factories where it improves clarity (optional).
- [ ] Try standalone + `loadComponent` for new routes/features (preview in 14).
- [ ] Enable useful extended template diagnostics.
- [ ] Re-test RxJS/HTTP/form-heavy flows due to typing changes.

## References (for verification)
- Angular v14 release blog / changelog
- Angular update guide (to v14)
- Typed forms documentation (v14+)
- Standalone components guide (developer preview in v14)
