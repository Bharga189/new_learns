# Angular 17 (November 8, 2023) — Deep Dive

Angular 17 is a major modernization release: **built-in control flow** (`@if`, `@for`, `@switch`), **deferrable views** (`@defer`), significant **SSR/hydration** improvements with `@angular/ssr` tooling, **Vite + esbuild** as the default build experience for new projects, maturing **signals** usage, and new render hooks like `afterRender`/`afterNextRender`.

> **Upgrade context:** Angular 17 typically pairs with TypeScript 5.2+. Use `ng update @angular/core@17 @angular/cli@17`. New apps default to the new application builder; existing apps can migrate.

---

## 1. Built-in control flow (`@if`, `@for`, `@switch`)

### What changed
Angular introduced a new, built-in template control-flow syntax that replaces common `*ngIf`/`*ngFor`/`ngSwitch` structural directive patterns for many use cases.

### Why it matters
- **Better readability** and JS-like structure.
- **Performance/semantics:** `@for` requires an explicit `track` expression, encouraging correct identity tracking (avoids DOM churn bugs).
- **Type narrowing:** `@if` improves template type narrowing in common patterns.
- **Less dependency on `CommonModule` imports** for basic control flow in standalone contexts.

### `@if` / `@else if` / `@else`
```html
@if (user(); as u) {
  <p>Hello {{ u.name }}</p>
} @else if (loading()) {
  <p>Loading…</p>
} @else {
  <p>Please sign in.</p>
}
```
**`as` aliasing** captures a value (often narrowing away null/undefined).

### `@for` with required `track`
```html
@for (item of items(); track item.id) {
  <li>{{ item.name }}</li>
} @empty {
  <li>No items.</li>
}
```
- **`track` is required.** Use a stable unique identity (`item.id`). Avoid `track $index` when items can reorder/insert/delete (causes DOM reuse bugs). Use `$index` only for truly static/index-keyed cases.
- **`@empty`** renders when the collection is empty.
- Implicit variables: `$index`, `$first`, `$last`, `$even`, `$odd`, `$count` (verify names/availability).

### `@switch`
```html
@switch (status()) {
  @case ('open') { <span>Open</span> }
  @case ('closed') { <span>Closed</span> }
  @default { <span>Unknown</span> }
}
```

### Migration
Schematic: `ng generate @angular/core:control-flow`.

### Caveats
- `@for` tracking mistakes are a common rendering bug source—choose the right key.
- Legacy structural directives still work; migrate incrementally.

See `examples/control-flow.component.ts` and `examples/control-flow.html`.

---

## 2. Deferrable views (`@defer`)

### What changed
`@defer` lazily loads/renders parts of a template (and dependencies) based on triggers, with `@placeholder`, `@loading`, and `@error` blocks.

### Why it matters
- Reduces initial bundle/JS work and improves Core Web Vitals.
- Declarative lazy UI without custom wiring for many cases.
- Integrates with SSR/hydration strategies.

### Example
```html
@defer (on viewport) {
  <app-heavy-chart />
} @placeholder (minimum 200ms) {
  <div class="chart-skeleton"></div>
} @loading (after 100ms; minimum 500ms) {
  <p>Loading chart…</p>
} @error {
  <p>Failed to load chart.</p>
}
```

### Triggers (common; verify exact syntax for 17.x)
`on idle`, `on viewport`, `on interaction`, `on hover`, `on immediate`, `on timer(500ms)`, `when <condition>`. Supports prefetch (e.g., `prefetch on idle`).

### Caveats
- Ensure placeholder/loading states avoid layout shift.
- Dependencies inside the deferred block follow standalone/import/loadability rules.
- Understand SSR behavior: what renders on server vs. defers on client, and hydration interactions.

See `examples/defer-view.html`.

---

## 3. SSR, hydration, and `@angular/ssr`

### What changed
Angular 17 streamlined SSR/hydration with `@angular/ssr` tooling, better integration with the new builder, and continued hydration reliability improvements.

### Why it matters
- Easier SSR adoption for modern apps.
- Non-destructive hydration (v16+) and event-replay direction improve performance/interactivity.

### Setup (high level)
- `ng add @angular/ssr` (exact command/schematics may vary).
- Configure client hydration (e.g., `provideClientHydration(...)` with features as supported).

### Hydration mismatch
If server/client render different DOM, hydration fails or falls back. Causes: non-deterministic rendering (`Date.now()`, random), direct DOM mutation, browser-only APIs during SSR. Make rendering deterministic or guard browser-only code.

---

## 4. `afterRender` / `afterNextRender`

### What changed
Angular added hooks for running code **after rendering** (DOM measurement/third-party lib integration) with phases to coordinate read/write work.

### Why it matters
- Replaces ad-hoc `setTimeout`/`AfterViewChecked` patterns for post-render DOM work.
- Better SSR safety in supported usage (verify behavior for 17.x).

### Example (conceptual; verify API details)
```ts
import { afterNextRender } from '@angular/core';

afterNextRender(() => {
  document.querySelector('#chart')?.scrollIntoView();
});
```

### Caveats
- Prefer declarative bindings; use render hooks for unavoidable DOM interop.
- Avoid heavy work after every render unnecessarily.

---

## 5. New application builder (Vite + esbuild) and DX

### What changed
New projects default to the **application builder** powered by Vite and esbuild, delivering much faster dev-server startup and rebuilds. `ng serve`/`ng build` configuration differs from the legacy webpack builder.

### Why it matters
- Dramatic local DX improvement for many projects.

### Migration/caveats
- Existing projects can migrate (evaluate compatibility with custom webpack configs, SSR setup, i18n, assets).
- Check builder documentation for your exact 17.x behavior.

---

## 6. Signals and reactivity direction

Signals (v16) continued to mature. In 17, control flow and deferrable views work naturally with signal-based state and RxJS interop.

---

## Realtime example (Angular 17 idiom) — detailed

**Folder:** `realtime/`

### Files
- `realtime/live-orders.service.ts` — live order stream/state.
- `realtime/live-orders.component.ts` — uses `@if/@for/@empty` and can defer non-critical panels with `@defer`.

### How it works
1. **State:** service holds live orders (signal/subject pattern) with connection status.
2. **Rendering:** `@for (...; track order.id)` ensures stable DOM updates; `@if` handles loading/empty/error states with type narrowing.
3. **Performance:** heavy detail panels (charts/maps) can be wrapped in `@defer (on viewport)`.
4. **Post-render DOM:** for third-party chart interop on live data, use `afterRender`/`afterNextRender` appropriately.

### Production considerations
- Batch high-rate updates to limit render churn.
- Keep `track` keys stable and unique.
- With SSR, start live connections only in the browser (platform guards / post-hydration).

---

## Migration checklist (16 → 17)
- [ ] Run `ng update @angular/core@17 @angular/cli@17`.
- [ ] Run control-flow migration: `ng generate @angular/core:control-flow` (optional but recommended).
- [ ] Adopt `@for` with correct `track` (audit `$index`).
- [ ] Identify non-critical UI for `@defer`.
- [ ] Evaluate migration to the Vite+esbuild application builder.
- [ ] For SSR apps, review `@angular/ssr` setup and hydration.
- [ ] Replace post-render hacks with `afterRender`/`afterNextRender` where appropriate.

## References (for verification)
- Angular v17 release blog / changelog
- Built-in control flow guide and migration schematic
- Deferrable views (`@defer`) guide
- SSR and hydration / `@angular/ssr`
- `afterRender` / `afterNextRender` API docs
