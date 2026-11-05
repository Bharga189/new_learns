# Angular 19 (November 19, 2024) — Deep Dive

Angular 19 continues the signals-first evolution: **standalone becomes the default** for components/directives/pipes, introduces **`linkedSignal`**, adds experimental **`resource()` / `rxResource()`** for async data, expands **incremental hydration**, and refines render effects and DX (including HMR improvements).

> **Upgrade context:** Angular 19 typically pairs with TypeScript 5.5+ (verify 19.x requirements). Use `ng update @angular/core@19 @angular/cli@19`. Schematics can update `standalone` flags to match the new defaults.

---

## 1. Standalone by default

### What changed
`standalone: true` is now the **default** for `@Component`, `@Directive`, and `@Pipe`. NgModule-based declarations must explicitly set `standalone: false`.

### Why it matters
- Reduces boilerplate; most new code is standalone.
- Aligns the framework with the modern, module-optional architecture.

### Examples
```ts
@Component({
  selector: 'app-hello',
  imports: [CommonModule],
  template: `<p>Hello</p>`,
})
export class HelloComponent {} // standalone by default
```

```ts
@Component({
  selector: 'app-legacy',
  standalone: false, // declared in an NgModule
  template: `...`,
})
export class LegacyComponent {}
```

### Migration notes
- `ng update` schematics adjust metadata to preserve existing behavior (e.g., add `standalone: false` where needed).
- Review third-party components and internal shared libraries for correct `standalone`/`imports` declarations.

---

## 2. `linkedSignal`

### What changed
`linkedSignal` creates a **writable signal** whose value is derived from a reactive computation/source and can be **reset/recomputed** when that source changes—while still allowing local writes.

### Why it matters
- Solves the common “editable local copy derived from prop/server state” problem (e.g., form draft initialized from selected item, reset when selection changes) without manual effect bookkeeping.

### Basic usage
```ts
import { Component, input, linkedSignal } from '@angular/core';

@Component({ selector: 'app-edit', template: `<input [value]="draft()" (input)="draft.set($any($event.target).value)">` })
export class EditComponent {
  name = input.required<string>();
  // resets to name() when name changes, but user can edit locally
  draft = linkedSignal(() => this.name());
}
```

### Advanced form (computation + previous; verify 19.x signature)
`linkedSignal` supports patterns that consider the previous source/value for smarter resets (API shape can include `{ source, computation }` depending on version). **Verify the exact overloads for 19.x.**

### Caveats
- `linkedSignal` is for derived-but-writable state; pure derivations should use `computed`.
- Be explicit about when local edits should be preserved vs. overwritten by upstream changes (design choice).

See `examples/linked-signal.ts`.

---

## 3. `resource()` and `rxResource()` (experimental)

### What changed
Angular introduced experimental signal-based APIs for managing async operations: `resource()` (promise/loader-based) and `rxResource()` (RxJS Observable-based, in interop packages as appropriate).

### Why it matters
- Standardizes **loading / error / value / status** handling as signals.
- Integrates with reactive dependencies (`request`) so data reloads when inputs change.
- Provides `reload()` and cancellation semantics (via `abortSignal` in the loader pattern, verify details).

### `resource()` (conceptual; verify 19.x API)
```ts
import { resource, signal } from '@angular/core';

const userId = signal(1);
const user = resource({
  request: () => ({ id: userId() }),
  loader: async ({ request, abortSignal }) => {
    const res = await fetch(`/api/users/${request.id}`, { signal: abortSignal });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  },
});

// user.value(), user.isLoading(), user.error(), user.status(), user.reload()
```

### `rxResource()` (conceptual)
```ts
import { rxResource } from '@angular/core/rxjs-interop';
const user = rxResource({
  request: () => this.userId(),
  loader: ({ request }) => this.http.get<User>(`/api/users/${request}`),
});
```

### When to use (and when not)
- **Good fit:** request/response data fetching tied to reactive parameters.
- **Not a direct fit:** continuous push streams (WebSocket/SSE fan-out). For push realtime, a signal store updated by stream handlers is often clearer (you can still derive views with `computed`).

### Caveats
- **Experimental in 19:** API names/shapes/status evolved in later releases—verify for your exact version.
- Handle error states and empty/loading UX explicitly.

See `examples/resource-loader.ts`.

---

## 4. Incremental hydration

### What changed
Angular expanded hydration capabilities toward **incremental hydration**, allowing parts of a server-rendered app to hydrate progressively (often coordinated with `@defer` triggers and hydration features).

### Why it matters
- Improves TTI by prioritizing interactive hydration for critical regions and deferring the rest.

### Conceptual setup (verify 19.x providers/syntax)
```ts
import { provideClientHydration, withIncrementalHydration } from '@angular/platform-browser';

bootstrapApplication(AppComponent, {
  providers: [provideClientHydration(withIncrementalHydration())],
});
```
Combine with `@defer` blocks and appropriate triggers so non-critical UI hydrates later.

### Caveats
- Requires SSR and correct hydration-compatible rendering.
- Verify exact provider names, constraints, and `@defer` hydration trigger syntax for 19.x.

---

## 5. `afterRenderEffect` and render-phase effects (experimental/preview evolution)

### What changed
Angular introduced/iterated on an effect-like API that runs with awareness of rendering phases (for DOM read/write coordination after render). In 19 this area was evolving (often referenced as `afterRenderEffect` in the release narrative; **verify the exact API name, stability, and signature for 19.x**).

### Why it matters
- Provides a structured way to synchronize with the DOM after Angular renders, reducing brittle timing hacks.
- Helps avoid layout thrashing by respecting read/write phase patterns (as supported).

### Caveats
- API status and naming changed across releases; consult 19.x docs.
- Prefer declarative bindings; use render-phase effects only for necessary DOM interop.

---

## 6. HMR and developer experience

### What changed
Angular 19 improved hot replacement workflows in the modern builder (notably styles and, in supported setups, template/component update ergonomics), speeding up iteration.

### Why it matters
- Faster feedback loops during development.

### Caveats
- HMR behavior depends on builder configuration and project structure; verify what’s supported in 19.x for your setup.

---

## 7. Diagnostics, defaults, and ecosystem

- Continued improvements to compiler diagnostics and strictness defaults.
- `ng update` schematics help migrate standalone defaults and related metadata.
- Material/CDK and other ecosystem packages align with Angular 19—check compatibility for UI libraries.

---

## Realtime example (Angular 19 idiom) — detailed

**Folder:** `realtime/`

### Files
- `realtime/live-metrics.service.ts` — push-based updates written into signals (suitable for WebSocket/SSE fan-out).
- `realtime/live-metrics.component.ts` — uses **`linkedSignal`** for local UI state derived from live data, plus built-in control flow / `@let` patterns as appropriate.

### How it works
1. **Push transport:** live events arrive continuously (not a single request/response). The service updates a signal store (`signal<T[]>`, `signal(status)`).
2. **Local derived UI state:** `linkedSignal` models selection/filter state that resets when upstream data/context changes, but remains user-editable.
3. **Request/response vs push:** `resource`/`rxResource` are ideal for parameterized fetch/refresh; for true push streams, prefer a signal store fed by the transport (as shown here).
4. **Zoneless readiness:** signal writes drive updates even in zoneless configurations.

### Production considerations
- Batch/coalesce high-frequency metric updates (e.g., `requestAnimationFrame`, buffer windows) to avoid render thrash.
- Use stable `track` keys in `@for`.
- Handle disconnect/reconnect and stale-data indicators.

---

## Migration checklist (18 → 19)
- [ ] Run `ng update @angular/core@19 @angular/cli@19` (review standalone-flag migrations).
- [ ] Adopt `linkedSignal` for derived-but-editable local state.
- [ ] Evaluate `resource()`/`rxResource()` for request/response data (experimental; verify API/stability in 19.x).
- [ ] For SSR apps, explore incremental hydration (`withIncrementalHydration()` / `@defer` integration as supported).
- [ ] Audit render-timing code for `afterRenderEffect`/render hooks (verify 19.x API).
- [ ] Re-test zoneless/hydration/standalone interop with third-party dependencies.

## References (for verification)
- Angular v19 release blog / changelog
- `linkedSignal` API docs
- `resource` / `rxResource` (experimental) docs for 19.x
- Incremental hydration / `@defer` hydration docs
- Render effects (`afterRenderEffect` naming/status may vary) docs for 19.x
