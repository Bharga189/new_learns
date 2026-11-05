# Angular 20 (May 2025) — Deep Dive (representative)

Angular 20 continued maturing the **signals-first, zoneless-ready** model: stabilization/maturation of zoneless change detection, refinement of async reactivity (`resource`/`rxResource` patterns and HTTP integration as evolved in 20.x), incremental hydration/SSR polish, and DX/tooling improvements. **Angular 20.x details vary by minor/patch—verify exact API names, import paths, stability status, and provider options against the official Angular 20 release notes and documentation.**

> **Upgrade context:** Use `ng update @angular/core@20 @angular/cli@20` and follow the Angular Update Guide for your exact 20.x target.

---

## 1. Zoneless change detection: maturation path

### What changed (direction)
The zoneless story progressed from experimental providers in earlier versions toward a more stable configuration path. In modern Angular, zoneless is enabled via a core provider (commonly `provideZonelessChangeDetection()` in the stabilized API line—**confirm the exact symbol and package for 20.x**).

### Why it matters
- Removes Zone.js overhead/patching in apps that follow Angular’s reactive update model.
- Encourages explicit, predictable state updates (signals, template events, framework-integrated async).

### Conceptual setup (verify for 20.x)
```ts
import { bootstrapApplication } from '@angular/platform-browser';
import { provideZonelessChangeDetection } from '@angular/core';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, {
  providers: [provideZonelessChangeDetection()],
});
```

### Migration considerations (deep)
| Area | What to check | Guidance |
|---|---|---|
| Third-party callbacks | Timers, WebSocket handlers, non-Angular libs | Ensure UI state updates go through signals or explicit Angular notification mechanisms supported in your version. |
| Async work | Promises, `setTimeout`, external events | With Zone.js removed, these don’t automatically trigger CD; structure updates reactively. |
| `AsyncPipe` / RxJS | Stream consumption | Prefer patterns that integrate cleanly with rendering; `toSignal` can bridge streams to signal reads. |
| Tests | TestBed, fakeAsync/zone helpers | Testing setup may need zoneless-specific configuration; follow 20.x testing docs. |
| Hybrid apps | Gradual migration | Consider incremental adoption strategies; validate each feature area. |

### Caveats
- “Zoneless-ready” ≠ “drop Zone.js with no code changes.” Audit all imperative UI update paths.
- Provider names/options evolved (experimental → stable naming). Do not assume 18’s experimental API name works unchanged in 20.

---

## 2. Async reactivity: `resource` / `rxResource` / HTTP patterns (maturation)

### What changed (direction)
Angular’s signal-based async primitives (introduced experimentally around v19) matured through subsequent releases. API shapes, status handling, and integration points (including HTTP helpers such as `httpResource` in later iterations of the design—**verify presence and API in 20.x**) evolved.

### Why it matters
- A consistent model for **request, loading, error, resolved value, refresh/cancel** semantics.
- Reactive reloading when request parameters (signals) change.

### Conceptual `rxResource` pattern (verify imports/API for 20.x)
```ts
import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
// import path and API shape: verify for your 20.x (e.g., rxResource from interop)
import { rxResource } from '@angular/core/rxjs-interop';

interface User { id: number; name: string }

@Component({ selector: 'app-user', template: `
  @if (user.isLoading()) { <p>Loading…</p> }
  @else if (user.error(); as e) { <p>Error</p> }
  @else if (user.value(); as u) { <p>{{ u.name }}</p> }
  <button (click)="id.update(n => n + 1)">Next</button>
` })
export class UserComponent {
  private http = inject(HttpClient);
  id = signal(1);
  user = rxResource({
    request: () => this.id(),
    loader: ({ request }) => this.http.get<User>(`/api/users/${request}`),
  });
}
```

### Design guidance
- Use `resource`/Rx interop resources for **parameterized request/response** flows.
- For **continuous push realtime** (WebSocket/SSE), use a signal store updated by the stream; derive views with `computed`. Mixing models is fine when each fits its problem.
- Always model loading/empty/error states in the UI.

### Caveats
- Experimental/iterating APIs: confirm method names (`value`, `isLoading`, `error`, `status`, `reload`) and loader signatures in 20.x.

See `examples/reactive-async.ts`.

---

## 3. Render effects and DOM synchronization

### What changed (direction)
APIs for post-render / phase-aware effects (evolving from `afterRender` hooks and experimental `afterRenderEffect` patterns) continued to be refined for safe DOM interop and SSR compatibility.

### Why it matters
- Coordinating third-party DOM libraries, measurements, and imperative focus/scroll behavior after Angular’s render cycle.

### Guidance
- Prefer declarative templates and bindings first.
- Use the **official render/effect hook** documented for 20.x; avoid relying on names from earlier experimental docs without verification.

---

## 4. Incremental hydration and SSR polish

### What changed (direction)
Hydration (event replay, incremental/progressive strategies) and `@defer` integration became more capable and better documented across 18–20. Angular 20 continued polish for SSR performance and correctness.

### Conceptual configuration (verify features for 20.x)
```ts
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
// incremental hydration feature name may be withIncrementalHydration() — verify 20.x
bootstrapApplication(AppComponent, {
  providers: [provideClientHydration(withEventReplay())],
});
```

### Caveats
- SSR/hydration constraints still apply: deterministic rendering, avoid unsafe DOM mutation, guard browser-only APIs.
- Validate `@defer` + hydration behavior for your routes/components.

---

## 5. Router, forms, and signal-centric authoring (representative)

### Direction
- **Router:** continued improvements to integration with modern providers, lazy loading, and (across releases) better alignment with reactivity and hydration/SSR. Verify 20.x-specific router features in the changelog.
- **Forms:** Angular’s long-term direction includes better signal integration; APIs evolved across 19–21+. In 20.x, rely on the documented reactive/template forms APIs for your version and treat “signal forms” proposals/experiments as version-specific.
- **Component APIs:** signal inputs/queries/outputs patterns matured across releases; verify which APIs are stable in 20.x.

### Practical guidance
- Use `input()`/`output()`/`model()` and query APIs as documented for 20.x if stable there; otherwise use established decorator patterns.
- Keep form state and async state explicit; avoid mixing too many paradigms in one feature.

---

## 6. Tooling, diagnostics, and testing

- **Builder/DX:** the Vite/esbuild-based toolchain continued to improve (HMR, speed, diagnostics). Check 20.x CLI behavior for your project.
- **Testing:** zoneless/signal-heavy apps may require updated `TestBed` setup; prefer component harnesses and avoid brittle timing assumptions.
- **Migrations:** `ng update` schematics cover many mechanical changes; review SSR/hydration and reactivity-related migrations.

---

## Realtime example (Angular 20 idiom) — detailed

**Folder:** `realtime/`

### Files
- `realtime/live-status.service.ts` — SSE/WebSocket-style live status feed updating signals; derived views via `computed`.
- `realtime/live-status.component.ts` — zoneless-friendly template reading signals directly.

### How it works
1. **Transport:** SSE (`EventSource`) for server→client status is a good fit; WebSocket suits bidirectional control. The key is where state lands.
2. **Signal store:** the service holds `events = signal<...>([])` and derives `latestByService = computed(...)`, `hasDown = computed(...)`. Computeds keep rendering logic pure and cacheable by dependency.
3. **Zoneless compatibility:** event callbacks call `signal.set/update`; Angular’s reactivity tracks reads in templates/computeds, so no Zone.js reliance is required for these updates (given correct setup).
4. **Backpressure:** cap retained history (`slice(0, 100)`) and consider throttling if events are bursty.
5. **Lifecycle:** close `EventSource`/socket on destroy; expose reconnect state if needed.

### Production considerations
- **Auth:** SSE with cookies or token-in-query patterns have security tradeoffs; prefer secure, same-origin designs and short-lived credentials where possible.
- **Reliability:** automatic reconnect with backoff; show “stale/disconnected” UI.
- **Correctness:** idempotent event IDs, ordering, and snapshot+delta patterns for consistency after reconnect.
- **Performance:** avoid creating new arrays/objects unnecessarily in hot paths; batch updates when rates are high.

---

## Migration checklist (19 → 20, representative)
- [ ] Run `ng update @angular/core@20 @angular/cli@20`; follow the official Update Guide.
- [ ] Confirm zoneless provider API for 20.x (rename/maturity from experimental) and audit async/UI update paths.
- [ ] Review `resource`/`rxResource`/HTTP resource API changes for 20.x; update imports and status handling.
- [ ] Re-validate SSR + hydration + `@defer` interactions.
- [ ] Update tests for zoneless/reactivity changes.
- [ ] Check router/forms/signal-API stability in 20.x before adopting new syntax broadly.

## References (for verification)
- Angular v20 release blog / changelog
- Angular Update Guide (to 20.x)
- Zoneless change detection docs for 20.x
- Async reactivity / `resource` docs for 20.x
- Hydration and SSR docs for 20.x
