# Angular 13 (November 3, 2021) — Deep Dive

Angular 13 is a landmark release: it **removes the legacy View Engine**, making Ivy the only compilation and rendering pipeline. It also drops IE11, modernizes library output (APF), aligns with RxJS 7, and improves day-to-day build/test ergonomics.

> **Upgrade context:** Angular 13 requires TypeScript 4.4.x (at release time), supports Node.js 12.20+ / 14.15+ / 16.10+, and targets modern evergreen browsers. Always confirm exact peer-dependency ranges for your 13.x patch.

---

## 1. Ivy-only: View Engine removed

### What changed
- **View Engine** (the pre-Ivy compiler/runtime used through Angular 8/9 migration paths) is **no longer shipped**. `ngcc` compatibility processing for View Engine libraries is no longer the central model; the ecosystem moved to Ivy-compatible distribution.
- `@angular/compiler` and runtime behavior are Ivy-only. There is no flag to fall back to View Engine.

### Why it matters
- **Smaller/simpler framework:** one rendering pipeline means less conditional logic, fewer edge-case differences, and easier maintenance.
- **Better tree-shaking:** Ivy’s locality and instruction-based output enable bundlers to remove unused Angular features more effectively.
- **Consistent behavior:** components, directives, pipes, and DI behave the same across apps and libraries.
- **Foundation for future features:** standalone components, improved hydration directions, and modern compilation strategies build on Ivy.

### Conceptual model (Ivy)
Templates compile into generated instructions (conceptually “template functions”) that work with Ivy’s runtime. The important practical takeaway for Angular 13+ app authors is: **dynamic/runtime component patterns should use Ivy APIs** (`createComponent`, `ViewContainerRef`) rather than legacy factory-resolver patterns.

### Library impact (APF / partial compilation)
- Libraries should be built and published following the **Angular Package Format (APF)** guidance for the era (Ivy/partial compilation). Consumers no longer depend on View Engine metadata in the same way.
- If you maintain a library, ensure your build uses the Angular library toolchain (`ng-packagr` / Angular CLI library builder) appropriate for v13.

### Migration notes
- Remove any remaining View Engine–specific workarounds.
- Upgrade third-party libraries to versions that support Ivy/Angular 13.
- If an old dependency only ships View Engine output and cannot be upgraded, you must replace it—Angular 13 cannot consume View Engine-only packages in the legacy way.

---

## 2. Internet Explorer 11 support removed

### What changed
Angular 13 no longer supports IE11. The framework, CLI defaults, and ecosystem assumptions target modern browsers.

### Why it matters
- **Modern platform APIs:** can rely on native ES2015+ features, modern DOM/CSS capabilities, and better baseline performance.
- **Smaller bundles:** fewer downleveling/polyfill requirements in default configurations.
- **Less testing/compat burden:** teams avoid IE-specific bugs and hacks.

### Migration notes
- Remove IE11-specific polyfills (e.g., `classlist.js`, legacy zone/Promise shims added solely for IE) if they are no longer needed.
- Update `browserslist` to reflect supported browsers.
- Update differential-loading assumptions: modern-only output is the norm.
- Communicate the browser-support change to stakeholders; IE11 users must use a supported browser.

---

## 3. Dynamic component creation simplified (`ComponentFactoryResolver` no longer required)

### What changed
`ViewContainerRef.createComponent` accepts a **component class/type** directly. The common case no longer needs `ComponentFactoryResolver.resolveComponentFactory(...)`.

`ComponentFactoryResolver` still existed for compatibility in some contexts, but the modern Ivy path is simpler.

### Before (View Engine–era common pattern)
```ts
import { Component, ComponentFactoryResolver, ViewContainerRef } from '@angular/core';
import { BannerComponent } from './banner.component';

@Component({ selector: 'app-host', template: `<ng-container #slot></ng-container>` })
export class HostComponent {
  constructor(
    private vcr: ViewContainerRef,
    private cfr: ComponentFactoryResolver,
  ) {}

  load() {
    const factory = this.cfr.resolveComponentFactory(BannerComponent);
    const ref = this.vcr.createComponent(factory);
    ref.instance.title = 'Hello';
  }
}
```

### After (Angular 13+ / Ivy)
```ts
import { Component, ViewContainerRef } from '@angular/core';
import { BannerComponent } from './banner.component';

@Component({ selector: 'app-host', template: `` })
export class HostComponent {
  constructor(private vcr: ViewContainerRef) {}

  load() {
    this.vcr.clear();
    const ref = this.vcr.createComponent(BannerComponent);
    ref.instance.title = 'Hello'; // @Input() title
  }
}
```

### Key details / caveats
- **Inputs/outputs:** set inputs via the component instance or (in later Angular versions) `setInput`. Subscribe to `@Output()`s on the instance.
- **Change detection:** dynamically created components participate in CD; if you mutate inputs in ways Angular won’t notice, trigger CD appropriately.
- **Lifecycle:** call `ref.destroy()` or clear the container when removing. Avoid leaks from subscriptions started inside the dynamic component.
- **Injector/environment (later refinement):** newer APIs allow passing injector options; in v13 the basic class-based creation covers most cases.

See `examples/dynamic-component.ts`.

---

## 4. Persistent build cache (CLI)

### What changed
Angular CLI supports **persistent disk caching** of build results. In v13 this became a prominent, configurable feature to speed up repeated builds.

### Why it matters
- Rebuilds (`ng serve`, `ng build`, `ng test`) can reuse cached computation.
- Improves local iteration and can help when cache directories are preserved.

### Configuration (`angular.json`)
```json
{
  "cli": {
    "cache": {
      "enabled": true,
      "path": ".angular/cache",
      "environment": "all"
    }
  }
}
```

### Details
- `environment` controls when caching applies (use values appropriate to your workflow/CLI behavior).
- Add `.angular/cache` to `.gitignore`.
- **Invalidation:** cache is keyed by inputs/config; after major dependency/tooling changes, clear `.angular/cache` if you see stale behavior.

### Caveats
- CI caching requires explicitly persisting/restoring the cache directory.
- Cache improves repeat work; a cold build still pays full cost.

---

## 5. RxJS 7 alignment

### What changed
Angular 13 targets **RxJS 7.x**, bringing typing improvements and alignment with the RxJS 7 API surface.

### Why it matters
- Better TypeScript inference in many observable chains.
- Access to RxJS 7 behavior/fixes and preparation for the modern RxJS line.

### Common migration items (RxJS 6 → 7)
| RxJS 6 pattern | RxJS 7+ direction | Notes |
|---|---|---|
| `toPromise()` | `firstValueFrom()` / `lastValueFrom()` | `toPromise` deprecated; choose “first” vs “last” semantics explicitly. |
| Typing edge cases | Updated types/imports | Fix compilation errors surfaced by stricter typing. |
| Deprecated signatures/operators | RxJS 7 equivalents | Follow RxJS migration guidance for specifics. |

### Example
```ts
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

async loadConfig(http: HttpClient) {
  // "first value" semantics; errors if the source completes without emitting.
  const cfg = await firstValueFrom(http.get<{ apiUrl: string }>('/api/config'));
  return cfg;
}
```

### Realtime note
Realtime streams (WebSocket, SSE, event sources) are naturally modeled as `Observable`s. In Angular 13-era code, services often expose `Observable`/`Subject`-based APIs consumed with the `async` pipe—see the realtime example.

---

## 6. Modern Angular Package Format (APF) / ES2020 / tooling modernization

### What changed
Angular 13 advanced the **Angular Package Format** used for publishing libraries, aligning outputs with modern JS (including ES2020-era targets in the package format) and Ivy-first distribution expectations.

### Why it matters
- Libraries integrate better with modern build pipelines.
- Consumers benefit from more optimizable output.
- Reduces legacy compatibility baggage from View Engine distribution.

### Practical guidance
- **App authors:** benefit indirectly by upgrading dependencies to v13-compatible releases.
- **Library authors:** use `ng-packagr`/Angular CLI’s library builder for v13 and validate the produced package against APF expectations.

---

## 7. TestBed and testing ergonomics (representative)

### What changed
A notable improvement in this era is **module/fixture teardown** behavior, helping prevent state leaking between tests.

### Example: destroy fixtures after each test
```ts
import { getTestBed } from '@angular/core/testing';
import { BrowserDynamicTestingModule, platformBrowserDynamicTesting } from '@angular/platform-browser-dynamic/testing';

getTestBed().initTestEnvironment(BrowserDynamicTestingModule, platformBrowserDynamicTesting(), {
  teardown: { destroyAfterEach: true },
});
```

### Why it matters
- Fewer order-dependent/flaky tests caused by leftover DOM, subscriptions, or shared test state.

### Caveats
- Enabling teardown can expose hidden dependencies between tests (good to fix). Run the full suite after changing teardown settings.

---

## 8. TypeScript / Node / ecosystem notes

- **TypeScript:** v13 supports TS 4.4.x (use the version range Angular 13 specifies).
- **Node.js:** ensure a supported Node version; CI images often need updating during major upgrades.
- **`ng update`:** schematics handle many mechanical changes; still review RxJS, removed browser support, and third-party library compatibility manually.

---

## 9. How these changes set up later Angular

Ivy-only and modern tooling in v13 laid groundwork for: standalone APIs (14/15), typed forms (14), signals and non-destructive hydration direction (16), built-in control flow and `@defer` (17), zoneless/hydration refinements (18+), and the signals-first model (19+).

---

## Realtime example (Angular 13 idiom) — detailed

**Folder:** `realtime/`

### Files
- `realtime/live-prices.service.ts` — creates/manages a WebSocket (via RxJS `webSocket`), exposes a multicasted `Observable` of price updates, and demonstrates reconnect/error handling hooks.
- `realtime/live-prices.component.ts` — renders live values using the `async` pipe and/or subscription patterns.

### How it works
1. **Transport:** `rxjs/webSocket` wraps the browser WebSocket as an observable stream.
2. **State distribution:** updates are pushed through a `BehaviorSubject` (or multicasting) so late subscribers get the latest value and components can share one stream.
3. **Rendering:** templates bind with `price$ | async`, letting Angular manage subscribe/unsubscribe with view lifetime.
4. **Change detection:** in a default Zone.js app, async events typically trigger CD. With OnPush, the `async` pipe marks for check.

### Production considerations
- **Authentication:** use negotiated auth patterns appropriate to WebSockets (browsers limit custom headers).
- **Reconnect/backoff:** exponential backoff + jitter; cap retries; surface connection state.
- **Message schema:** validate/parse payloads; handle versioned message types.
- **Ordering/idempotency:** sequence IDs/timestamps; dedupe/reorder if needed.
- **Cleanup:** close sockets and complete subjects on destroy.
- **Fallbacks:** consider SSE or polling when WebSockets are unavailable.

---

## Quick checklist for adopting Angular 13 patterns
- [ ] Confirm all dependencies support Angular 13/Ivy.
- [ ] Remove IE11 polyfills and update `browserslist`.
- [ ] Replace `ComponentFactoryResolver` dynamic creation where trivial.
- [ ] Migrate RxJS `toPromise()` to `firstValueFrom`/`lastValueFrom`.
- [ ] Enable/configure persistent build cache (`.angular/cache` gitignored).
- [ ] Consider `TestBed` teardown to reduce flaky tests.
- [ ] For realtime features, centralize stream state in a service and consume via `async` pipe.

## References (for verification)
- Angular v13 release blog / changelog
- Angular update guide (to v13)
- RxJS 7 migration notes
- Angular Package Format (APF) documentation for the v13 era
