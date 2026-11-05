# Angular 22 (May 2026) — Deep Dive (representative; verify with official 22.x notes)

Angular 22 is the current major line in this reference set (relative to 2026). Angular’s recent trajectory centers on **signal-first reactivity**, **zoneless change detection**, **SSR with incremental/event-replay hydration and `@defer`**, **modern control flow/authoring ergonomics**, and **fast tooling**. **This deep dive describes those feature areas and production patterns in depth, but you must confirm exact 22.x feature names, APIs, defaults, deprecations, and migration schematics in the official Angular 22 release blog, changelog, and documentation.**

> **Upgrade context:** Use `ng update @angular/core@22 @angular/cli@22` and follow the Angular Update Guide for your exact 22.x target and TypeScript/Node requirements.

---

## 1. Reactivity: signals as the core state model (representative)

### Deep view
- **Primitives:** `signal` (writable), `computed` (derived, dependency-tracked), `linkedSignal` (derived-but-writable reset patterns), `effect` (synchronization/side effects) — verify the full, stable 22.x set and semantics.
- **Component APIs:** signal-based `input()`/`output()`/`model()` and query APIs may be stable/preferred depending on 22.x; verify exact syntax, transforms, required inputs, and DI/query behavior.
- **Interop:** `toSignal`/`toObservable` and `rxResource`/resource-style APIs bridge RxJS and async work; confirm 22.x import paths and status semantics (`loading/error/value/reload`).

### Best practices (timeless)
- Use `computed` for derivations; keep them pure/idempotent.
- Use `effect` only for imperative sync (DOM, logging, external stores) and understand timing/cleanup.
- For collections rendered with `@for`, maintain stable identities and prefer immutable updates (`map/filter/slice`) or carefully managed mutations consistent with reactivity.
- Model async state explicitly: loading, success, empty, error.

### Example (verify for 22.x)
```ts
import { Component, computed, signal } from '@angular/core';

@Component({
  selector: 'app-metrics',
  template: `
    <input [value]="q()" (input)="q.set(($any($event.target)).value)" />
    @for (m of filtered(); track m.key) { <li>{{ m.key }}: {{ m.value }}</li> }
  `,
})
export class MetricsComponent {
  q = signal('');
  data = signal([{ key: 'cpu', value: 10 }, { key: 'mem', value: 20 }]);
  filtered = computed(() => {
    const q = this.q().toLowerCase();
    return this.data().filter(m => m.key.includes(q));
  });
}
```

---

## 2. Zoneless change detection (representative)

### Deep view
Zoneless mode removes reliance on Zone.js patching; rendering updates when Angular observes reactive changes (signals, supported framework events) or when explicit notification mechanisms are used as documented.

### Production checklist
- Enable the **22.x-documented zoneless provider**; follow official guidance on Zone.js removal.
- Ensure every UI-changing async path (timers, sockets, third-party SDK callbacks) updates signals.
- Validate OnPush/legacy patterns, custom CD code (`markForCheck`, `detectChanges`), and library compatibility.
- Update tests and e2e flows; fix flakiness caused by assumptions about Zone.js timing.

### Common failure mode
UI not updating → state mutated outside Angular’s tracking. Convert to `signal.set/update` or use the 22.x-supported notification API.

---

## 3. SSR, hydration, and deferrable UI (representative)

### Deep view
- **Hydration:** reuses server DOM; event replay can capture pre-hydration interactions; incremental/progressive hydration (as supported in 22.x) coordinates with `@defer` to prioritize critical islands.
- **`@defer`:** triggers (`viewport`, `interaction`, `idle`, `timer`, `when`), prefetching, and placeholder/loading/error blocks reduce initial cost.
- **Correctness:** deterministic SSR output, no unsafe DOM mutations, browser-only guards.

### Example (conceptual; verify 22.x features)
```ts
import { bootstrapApplication } from '@angular/platform-browser';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
// add incremental hydration / other features per 22.x docs

bootstrapApplication(AppComponent, {
  providers: [provideClientHydration(withEventReplay())],
});
```

```html
@defer (on viewport) { <app-analytics-panel /> }
@placeholder { <p>Loading analytics…</p> }
```

### Verification steps
- Test with JS disabled/slow network/slow CPU profiles.
- Check hydration mismatch warnings in dev.
- Confirm realtime/live widgets defer or initialize browser-side appropriately.

---

## 4. Router, forms, HTTP, and tooling (representative)

- **Router:** use functional guards/resolvers, lazy `loadComponent`/routes, and input binding as documented for 22.x. Verify any 22.x-specific navigation, view transition, or hydration behaviors.
- **Forms:** rely on the 22.x-documented reactive/template forms APIs; adopt signal-form integrations only if/when they are stable and documented for 22.x.
- **HTTP:** `provideHttpClient(...)` with interceptors/fetch features per 22.x docs. Pair with resource patterns for request/response state where appropriate.
- **Tooling:** Vite/esbuild-based builder, HMR, and diagnostics continue to evolve—review 22.x CLI config and migrations.

---

## 5. Realtime architecture (Angular 22 style) — deep dive

**Folder:** `realtime/` (`live-stream.service.ts`, `live-stream.component.ts`)

### Pattern
1. **Transport abstraction:** WebSocket for bidirectional low-latency; SSE for server-push; HTTP polling/resource refresh for simpler cases. Keep transport details inside the service.
2. **Signal store:** `status`, `messages`, `lastError` as signals; derived `lastMessage`, `channels`, filtered views as `computed`.
3. **Connection state machine:** explicit states (`idle/connecting/open/reconnecting/closed/error`) drive UX and prevent duplicate connections.
4. **Reliability:** exponential backoff with cap, manual-close detection, bounded buffers, message validation, and optional `send()` only when `OPEN`.
5. **Rendering:** `@let`/`@if/@for` with stable `track`; read signals in templates (zoneless-friendly). Defer heavy visualizations with `@defer`.
6. **SSR:** never open sockets during server render; start in the browser (platform guard or client-only lifecycle) and consider “rehydrate + reconnect” UX.

### Production checklist
- Auth/token refresh and secure transport (wss/https, origin/cookie policies).
- Ordering/idempotency (sequence IDs), snapshot+delta on reconnect.
- Backpressure/batching (rAF/microtask) and virtualized lists for high volume.
- Sanitization of payloads before rendering.
- Observability (latency, reconnect rate, dropped messages) and user-visible staleness.

---

## 6. Migration checklist (21 → 22, representative)
- [ ] Run `ng update @angular/core@22 @angular/cli@22`; follow the official Update Guide for 22.x.
- [ ] Verify zoneless provider/features and Zone.js removal guidance for 22.x.
- [ ] Confirm stability and exact APIs for signal components/queries/resources in 22.x.
- [ ] Re-validate SSR + hydration + `@defer` + realtime startup ordering.
- [ ] Update tests (zoneless, signal timing, transport mocks).
- [ ] Audit third-party libs for 22.x/zoneless/hydration compatibility.

## 7. How to verify “all features” for Angular 22.x (important)
1. Read the **official Angular 22.0 release blog post** and the `@angular/core`/CLI/router/etc. **changelogs** for 22.0–22.x.
2. Cross-check API status (stable/experimental/deprecated) in the **Angular API reference** for 22.x.
3. Run `ng new` on 22.x and inspect defaults (standalone, builder, SSR options, zoneless guidance).
4. Apply `ng update` to a sample app and review generated migrations.
5. Replace any “representative” snippet in this folder with the 22.x-documented form if it differs.

## References (for verification)
- Angular v22 release blog / changelog (official)
- Angular Update Guide (to 22.x)
- Reactivity/signals, zoneless, SSR/hydration, `@defer`, router, forms, and HTTP docs for 22.x
