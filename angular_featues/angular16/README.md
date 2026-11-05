# Angular 16 (May 3, 2023) — Deep Dive

Angular 16 is a turning point for reactivity and developer experience. It introduces **Angular Signals** (developer preview), **RxJS interop** (`toSignal`/`toObservable`), **required inputs**, `DestroyRef`/`takeUntilDestroyed`, **router component input binding**, **self-closing tag syntax** in templates, **non-destructive hydration** (developer preview), and an **esbuild/Vite-powered dev server** (developer preview in the CLI).

> **Upgrade context:** Angular 16 typically pairs with TypeScript ~5.0. Use `ng update @angular/core@16 @angular/cli@16`.

---

## 1. Angular Signals (developer preview)

### What changed
Angular added a **signals** primitives model for fine-grained reactivity: `signal`, `computed`, and `effect` (APIs in `@angular/core`).

### Why it matters
- **Explicit, synchronous reactive state** that integrates with Angular’s rendering model.
- **Fine-grained dependencies:** computed values and templates can depend on specific signals.
- Foundation for future zoneless and performance improvements.

### Core primitives

#### `signal(initialValue)`
Creates a writable reactive value.
```ts
import { signal } from '@angular/core';
const count = signal(0);
console.log(count());      // read
count.set(1);              // replace
count.update(n => n + 1);  // derive from previous
```

#### `computed(() => ...)`
Derives a read-only value from other signals. Recomputes lazily/caches until dependencies change (conceptually).
```ts
import { computed, signal } from '@angular/core';
const a = signal(2);
const b = signal(3);
const sum = computed(() => a() + b());
```

#### `effect(() => ...)`
Runs a side effect when tracked signal dependencies change. Use for synchronization (logging, imperative DOM interop, analytics) — **not** as primary state derivation.
```ts
import { effect, signal } from '@angular/core';
const name = signal('Ada');
effect(() => console.log('name is', name()));
```

### Key details / caveats (v16 preview)
- Signals were **developer preview** in 16; behavior/ergonomics evolved in 17+. Read the docs for your exact version.
- **Equality:** signals can take equality functions to avoid unnecessary notifications.
- **Effects & cleanup:** effects have specific execution timing and cleanup semantics; avoid writing signals in ways that create feedback loops.
- **Templates:** reading a signal in a template (`{{ count() }}`) tracks dependencies for that view (in supported compilation/runtime modes).
- **Zoneless future:** signals are a key enabler, but v16 apps still commonly run with Zone.js unless configured otherwise in later versions.

See `examples/signals-counter.ts`.

---

## 2. RxJS interop (`toSignal`, `toObservable`)

### What changed
Angular provided bridge utilities between RxJS Observables and signals (package placement/APIs: `@angular/core/rxjs-interop` in the interop story; verify for 16.x).

### Why it matters
- Many APIs (HttpClient, router events, WebSockets) are Observable-based. Interop lets you consume them as signals in templates/components.
- Enables gradual migration: keep RxJS for streams, use signals for view state.

### `toSignal(observable$, options?)`
```ts
import { toSignal } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';

const tick = toSignal(interval(1000), { initialValue: 0 });
// tick() reads the latest value
```

Important options (verify 16.x):
- `initialValue`: value before first emission (otherwise `undefined` may be possible depending on API).
- `requireSync`: assert synchronous emission behavior when appropriate.
- `manualCleanup` / injector context: subscription lifecycle is tied to an injection context by default in supported usage; understand cleanup.

### `toObservable(signal)`
```ts
import { toObservable } from '@angular/core/rxjs-interop';
const count$ = toObservable(countSignal);
```
Useful when interop-ing with RxJS operators or APIs that expect Observables.

### Caveats
- **Errors/completion:** understand how Observable errors/completion map to signal behavior in your version.
- **Multicasting:** `toSignal` subscribes; don’t accidentally create duplicate side-effectful subscriptions.
- **Injection context:** `toSignal` typically needs an injection context (or injector) to manage teardown.

See `realtime/live-ticker.*` for RxJS→signal realtime consumption.

---

## 3. Required inputs

### What changed
Component/directive inputs can be marked **required**, so Angular reports missing bindings (compile-time/template diagnostics and/or runtime behavior depending on context).

### Why it matters
- Prevents undefined-input bugs in components that cannot function without a value.
- Makes component contracts explicit.

### Usage
```ts
import { Component, Input } from '@angular/core';

@Component({ selector: 'app-user-card', template: `{{ userId }}` })
export class UserCardComponent {
  @Input({ required: true }) userId!: string;
}
```

### Caveats
- “Required” is enforced by Angular’s checks; TypeScript still sees the field type you declare—use definite assignment (`!`) or appropriate initialization patterns.
- Dynamic component creation must still provide required inputs (via instance/setInput patterns as applicable).
- Aliasing (`@Input('alias')`) works with required inputs.

---

## 4. `DestroyRef` and `takeUntilDestroyed`

### What changed
- `DestroyRef` provides a hook (`onDestroy`) to run cleanup logic without implementing `ngOnDestroy`.
- `takeUntilDestroyed` (RxJS interop) completes an Observable when the relevant scope is destroyed.

### Why it matters
- Cleaner composition in functions/services/utilities that need teardown.
- Reduces manual `Subject` + `takeUntil` boilerplate for component-scoped subscriptions.

### `DestroyRef`
```ts
import { DestroyRef, inject } from '@angular/core';

const destroyRef = inject(DestroyRef);
const id = setInterval(() => {}, 1000);
destroyRef.onDestroy(() => clearInterval(id));
```

### `takeUntilDestroyed`
```ts
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';

// In an injection context (e.g., constructor/field init):
interval(1000).pipe(takeUntilDestroyed()).subscribe(...);

// Outside constructor, pass DestroyRef:
interval(1000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(...);
```

### Caveats
- `takeUntilDestroyed()` without an argument must be called in an injection context; otherwise pass `DestroyRef`.
- Still prefer `async` pipe for template-bound streams when possible.

See `examples/take-until-destroyed.ts`.

---

## 5. Router: bind route data/params to component inputs

### What changed
The router can bind route information (params, data, resolved data, etc.) directly to **component inputs**, reducing boilerplate reads from `ActivatedRoute`.

### Why it matters
- Components become easier to test (plain inputs) and less coupled to router internals.
- Works nicely with typed inputs and, later, signal-based patterns.

### Enabling (provider feature; verify 16.x name)
```ts
import { provideRouter, withComponentInputBinding } from '@angular/router';
bootstrapApplication(AppComponent, {
  providers: [provideRouter(routes, withComponentInputBinding())],
});
```

### Component
```ts
@Component({ standalone: true, template: `User {{ id }}` })
export class UserComponent {
  @Input() id?: string; // bound from route param `:id` (matching key)
}
```

### Caveats
- Key names must align with param/data/resolver keys as defined by the binding behavior—verify mapping rules for 16.x.
- Type conversion is your responsibility (params are often strings).
- Available for routed components where the router controls instantiation.

---

## 6. Self-closing tags in templates

### What changed
Angular templates support self-closing syntax for components (and in general, template parsing improvements) in supported cases, e.g. `<app-foo />`.

### Why it matters
- Cleaner templates, especially for components with no projected content.

### Caveats
- Works where Angular’s template parser supports it for the element type; follow compiler diagnostics.

---

## 7. Non-destructive hydration (d
eveloper preview)

### What changed
Angular introduced **non-destructive hydration**: instead of destroying and re-rendering server-rendered DOM on the client, Angular attempts to **reuse** existing DOM and attach event listeners/state.

### Why it matters
- Better performance and less flicker/layout work during SSR hydration.
- Improves LCP/UX for server-rendered apps.

### Enabling (conceptual; verify 16.x API)
```ts
import { provideClientHydration } from '@angular/platform-browser';
bootstrapApplication(AppComponent, { providers: [provideClientHydration()] });
```

### Caveats (v16 preview)
- **DOM validity:** server and client must render compatible markup; direct DOM manipulation or mismatches can break hydration.
- Some third-party libs that mutate DOM may cause issues.
- Angular logs hydration mismatch warnings; treat them seriously.

---

## 8. esbuild-based build system / dev server (developer preview)

### What changed
The Angular CLI began offering an **esbuild/Vite-powered** builder/dev server path (developer preview in 16) for significantly faster builds and HMR-style workflows (exact builder names/options evolved).

### Why it matters
- Much faster startup and updates in many projects.
- Aligns Angular tooling with modern bundler performance.

### Caveats
- Preview in 16: feature parity and edge cases evolved in 17+. Check the builder documentation for your version.

---

## Realtime example (Angular 16 idiom) — detailed

**Folder:** `realtime/`

### Files
- `realtime/live-ticker.service.ts` — RxJS WebSocket/stream producing live ticks.
- `realtime/live-ticker.component.ts` — converts/consumes stream state via **signals interop** (`toSignal`) for template rendering.

### How it works
1. **Stream source:** RxJS manages the live transport (reconnect/multicast as needed).
2. **Interop:** `toSignal` exposes the latest value to the template as a signal, integrating with Angular’s reactive rendering.
3. **Cleanup:** interop and `takeUntilDestroyed` patterns reduce leak risk when used in proper injection contexts.
4. **CD/perf:** signal reads in templates enable fine-grained updates; in Zone.js apps CD still runs as configured, but the code style prepares for zoneless evolution.

### Production considerations
- Handle `undefined`/initial states from `toSignal` (`initialValue`).
- Decide error policy (surface error state vs. retry vs. fallback observable).
- For very high-frequency ticks, batch/throttle before updating UI state.

---

## Migration checklist (15 → 16)
- [ ] Run `ng update @angular/core@16 @angular/cli@16`.
- [ ] Adopt TypeScript 5.x as required.
- [ ] Evaluate signals for local/component state (preview) and `toSignal` for Observable→template bridges.
- [ ] Mark truly mandatory `@Input()`s as `required`.
- [ ] Use `DestroyRef`/`takeUntilDestroyed` to simplify cleanup.
- [ ] Enable `withComponentInputBinding()` if you want route→input binding.
- [ ] Consider `provideClientHydration()` for SSR apps (preview; test carefully).
- [ ] Try the esbuild-based dev/build preview if compatible with your project.

## References (for verification)
- Angular v16 release blog / changelog
- Signals guide (developer preview in 16)
- RxJS interop (`toSignal`, `toObservable`, `takeUntilDestroyed`)
- Required inputs
- Router component input binding
- Non-destructive hydration
