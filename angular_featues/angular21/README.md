# Angular 21 (November 2025) — Deep Dive (representative)

Angular 21 is a recent release line (verify exact date/features in the official 21.x notes). The deep-dive themes for this era are **signal-first authoring**, **zoneless change detection readiness**, **SSR/incremental hydration maturity**, **async/resource ergonomics**, and continued **router/forms/tooling** evolution. **Because 21.x details are version-specific, treat this as a structured guide to the feature areas and patterns; confirm every API name, import path, default, and stability status in the Angular 21 documentation and changelog.**

> **Upgrade context:** Use `ng update @angular/core@21 @angular/cli@21` and follow the Angular Update Guide for your exact 21.x target.

---

## 1. Signal-first component model (representative)

### What this means
Modern Angular authoring increasingly centers on:
- **Signals** for local/derived state (`signal`, `computed`, `linkedSignal`, `effect` — use the 21.x-approved set).
- **Signal-based component APIs** (inputs/outputs/queries/models) where stable in 21.x.
- **RxJS interop** (`toSignal`, `toObservable`, `rxResource` patterns) for stream-based sources.

### Why it matters
- Explicit dependencies and predictable updates.
- Better alignment with zoneless rendering and fine-grained template reactivity.
- Clearer data flow than ad-hoc mutable state.

### Example (verify API names for 21.x)
```ts
import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-product-row',
  template: `
    <button type="button" (click)="pick.emit(id())">
      {{ name() }} — {{ label() }}
    </button>
  `,
})
export class ProductRowComponent {
  id = input.required<string>();
  name = input.required<string>();
  price = input(0);
  pick = output<string>();
  label = computed(() => (this.price() > 100 ? 'premium' : 'standard'));
}
```

### Deep details to verify in 21.x
- Exact `input()` options (`required`, `alias`, `transform`) and stability.
- `model()` two-way binding semantics if used.
- Signal queries (`viewChild`, `contentChild`, etc.) vs decorator queries.
- `effect()` timing/cleanup rules and recommended use cases.

### Caveats
- Don’t replace all RxJS with signals: streams, multicasting, backpressure, and event composition remain strong RxJS use cases.
- Keep effects for synchronization, not primary derivation.

See `examples/signal-component.ts`.

---

## 2. Zoneless readiness and change detection (representative)

### Direction
Zoneless operation (configurable via the 21.x-documented provider) is a first-class path for apps built on reactive updates.

### Deep migration checklist
1. **Bootstrap:** enable the zoneless provider per 21.x docs; remove `zone.js` polyfill/imports only when the app and dependencies are ready (follow official guidance).
2. **State updates:** convert imperative UI mutations to signal writes or framework-supported patterns.
3. **External async:** wrap WebSocket/SSE/timer/third-party callback updates in signal updates (or supported notification APIs).
4. **HTTP/forms/router:** confirm all flows render correctly without Zone.js-triggered CD.
5. **Tests:** update `TestBed`/async helpers for zoneless mode; eliminate timing flakiness.
6. **Libraries:** audit component libraries and SDKs; shim or isolate Zone-dependent behavior.

### Caveats
- If the UI “doesn’t update” under zoneless, the cause is almost always a state change Angular isn’t tracking—fix the reactivity path, don’t sprinkle manual CD.

---

## 3. SSR, hydration, and `@defer` (representative)

### Direction
Server rendering, event replay, and incremental/progressive hydration integrate with deferrable views to reduce JS cost and improve interactivity.

### Deep considerations
- **Determinism:** server and client must produce compatible initial output.
- **Browser-only APIs:** guard `window`/`document`/WebSocket creation; start realtime connections after hydration/platform checks.
- **Hydration triggers:** align `@defer` and hydration behavior (syntax and features are version-specific—verify 21.x).
- **Streaming/SSR config:** follow 21.x `@angular/ssr` and builder documentation.

### Example (conceptual)
```ts
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
// add incremental hydration features as documented for 21.x
bootstrapApplication(AppComponent, {
  providers: [provideClientHydration(withEventReplay())],
});
```

---

## 4. Async data and resource patterns (representative)

### Direction
Async reactivity APIs (`resource`/Rx interop and HTTP-aligned helpers, as documented for 21.x) provide standardized status/loading/error/value semantics.

### Deep guidance
- **Request/response:** use resource-style APIs for parameterized fetches with cancellation/reload semantics (verify method names).
- **Push streams:** model WebSocket/SSE as a signal store; use `computed` for projections, and `@for` with stable `track` for rendering.
- **Errors:** distinguish transport errors, auth failures, and domain errors; surface actionable UI states.
- **Concurrency:** avoid stale overwrites (latest-wins, request IDs, or framework-provided cancellation).

---

## 5. Router and forms (representative)

- **Router:** verify 21.x additions (guards/resolvers, lazy loading, input binding, view transitions/hydration interactions as applicable).
- **Forms:** reactive/template forms remain core; signal-integration features may be in preview/experimental or stable depending on 21.x—use only what the official docs mark for your version.

---

## 6. Tooling, testing, and diagnostics

- **CLI/build:** continue using the modern builder; monitor config changes in 21.x migrations.
- **Diagnostics:** enable strict/template checks and address new warnings during upgrades.
- **Tests:** prefer deterministic, signal-aware tests; for realtime, mock transports and assert state transitions (idle→connecting→open→error/closed).

---

## Realtime example (Angular 21 idiom) — detailed

**Folder:** `realtime/`

### Files
- `realtime/live-activity.service.ts` — signal-based connection state machine + buffered events + computed projections; WebSocket with reconnect/backoff.
- `realtime/live-activity.component.ts` — renders status, errors, and live list; zoneless-ready (signal updates).

### How it works (deep)
1. **State machine:** `status` is an explicit union (`idle|connecting|open|reconnecting|closed`), preventing ambiguous UI.
2. **Idempotent connect:** guards against duplicate sockets; `manualClose`/retry counters control reconnect behavior.
3. **Buffered projection:** `items.update(xs => [item, ...xs].slice(0, 100))` bounds memory; `last = computed(...)` derives a view.
4. **Template reactivity:** `@if/@let/@for` with `track a.id` reads signals directly—compatible with zoneless.
5. **Separation of concerns:** transport lives in the service; component orchestrates lifecycle/intent.

### Production considerations
- **Auth refresh:** on 401/close codes, refresh tokens and reconnect with jitter; avoid tight loops.
- **Ordering:** include server sequence IDs; dedupe and reorder if the channel is not strictly ordered.
- **Backpressure:** batch UI updates (microtask/animation frame) at high rates; consider virtual scrolling for large lists.
- **Observability:** expose metrics (connect count, reconnects, message lag).
- **Security:** validate/sanitize payloads; never render untrusted HTML.

---

## Migration checklist (20 → 21, representative)
- [ ] Run `ng update @angular/core@21 @angular/cli@21`; follow the official Update Guide.
- [ ] Verify zoneless and hydration provider APIs/options for 21.x.
- [ ] Confirm stability of any signal input/query/model/resource APIs you adopt.
- [ ] Re-test realtime flows under zoneless + SSR/hydration constraints.
- [ ] Update third-party dependencies to 21.x-compatible versions.

## References (for verification)
- Angular v21 release blog / changelog
- Angular Update Guide (to 21.x)
- Zoneless, hydration/SSR, and reactivity docs for 21.x
