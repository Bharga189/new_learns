# Angular 15 (November 16, 2022) — Deep Dive

Angular 15 makes the **standalone APIs stable and ergonomic** (`bootstrapApplication`, `provideRouter`, `provideHttpClient`), introduces the **directive composition API** (`hostDirectives`), ships the **`NgOptimizedImage`** directive, and promotes **functional router guards/resolvers**. It also removes the legacy `@angular/flex-layout` from the core Angular project’s recommended path (ecosystem shift) and continues refinements to stack traces, language service, and tree-shakability.

> **Upgrade context:** Angular 15 typically pairs with TypeScript ~4.8. Use `ng update @angular/core@15 @angular/cli@15`.

---

## 1. Standalone APIs are stable (and simpler bootstrapping)

### What changed
Standalone components/directives/pipes graduated to a stable, recommended model. Bootstrapping without `AppModule` is straightforward via `bootstrapApplication`, and routing/HTTP are configured via provider functions.

### Why it matters
- Less NgModule ceremony for new apps.
- Clear, tree-shakable configuration using `provide*` functions.
- Easier feature composition and lazy loading.

### `bootstrapApplication`
```ts
// main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideHttpClient(),
  ],
}).catch(err => console.error(err));
```

### `provideRouter` and router features
- Configure routes with `provideRouter(routes, ...features)`.
- Add capabilities via feature functions (names vary by version—e.g., preloading, in-memory scrolling, component input binding came later in v16). In v15, the key shift is **provider-based router setup**.

```ts
import { provideRouter, withPreloading, PreloadAllModules } from '@angular/router';
provideRouter(routes, withPreloading(PreloadAllModules));
```
(Confirm the exact feature function names for 15.x.)

### `provideHttpClient`
Replaces/modernizes `HttpClientModule`-style setup in standalone bootstrap:
```ts
import { provideHttpClient, withInterceptors } from '@angular/common/http';
provideHttpClient(withInterceptors([authInterceptor]));
```
(Interceptor APIs evolved; verify 15.x specifics—functional interceptors became prominent in 15+.)

### Interop: `importProvidersFrom`
Bridge remaining NgModule-based libraries:
```ts
import { importProvidersFrom } from '@angular/core';
import { SomeLegacyModule } from 'some-lib';

bootstrapApplication(AppComponent, {
  providers: [importProvidersFrom(SomeLegacyModule.forRoot())],
});
```

### Caveats
- Many third-party libs still ship NgModules; `importProvidersFrom` is the compatibility path.
- Prefer the v15+ `provide*` style for new standalone code.

---

## 2. Directive composition API (`hostDirectives`)

### What changed
A component or directive can **compose behavior by applying other standalone directives to its host** via the `hostDirectives` metadata property.

### Why it matters
- Reuse directive behavior (accessibility, styling hooks, listeners, inputs/outputs mapping patterns) without inheritance.
- Keeps components focused; composition over deep base classes.
- Makes cross-cutting host behavior explicit.

### Basic example
```ts
import { Component, Directive, HostBinding, HostListener } from '@angular/core';

@Directive({ selector: '[appHoverHighlight]', standalone: true })
export class HoverHighlightDirective {
  @HostBinding('class.hovered') hovered = false;
  @HostListener('mouseenter') onEnter() { this.hovered = true; }
  @HostListener('mouseleave') onLeave() { this.hovered = false; }
}

@Component({
  selector: 'app-card',
  standalone: true,
  hostDirectives: [HoverHighlightDirective],
  template: `<ng-content />`,
})
export class CardComponent {}
```

### Exposing inputs/outputs (where supported)
`hostDirectives` can be configured to include directives and, in supported forms, alias/forward inputs and outputs. The exact syntax/capabilities matured over versions—verify for 15.x:
```ts
hostDirectives: [{
  directive: SomeDirective,
  inputs: ['value: cardValue'],
  outputs: ['valueChange: cardValueChange'],
}]
```

### Caveats
- Composed directives run as directives on the host; understand their lifecycle and change detection implications.
- Prefer composition for host-level behavior; don’t hide critical behavior in opaque host directives.
- `hostDirectives` works with **standalone** directives.

See `examples/host-directives.ts`.

---

## 3. `NgOptimizedImage` directive

### What changed
Angular introduced `NgOptimizedImage` (in `@angular/common`) to enforce and enable best practices for image loading (LCP/perf). In v15 it became a key recommended tool for image-heavy apps.

### Why it matters
- Improves Core Web Vitals (especially LCP) when used correctly.
- Encourages explicit `width`/`height` (reduces layout shift), prioritization for LCP images, and appropriate loading behavior.
- Integrates with image loaders/CDNs via configuration.

### Basic usage
```ts
import { NgOptimizedImage } from '@angular/common';

@Component({
  standalone: true,
  imports: [NgOptimizedImage],
  template: `
    <!-- LCP / above-the-fold hero: prioritize -->
    <img ngSrc="/assets/hero.jpg" width="1200" height="600" priority />

    <!-- Below-the-fold: lazy by default behavior/attributes as appropriate -->
    <img ngSrc="/assets/thumb.jpg" width="300" height="200" />
  `,
})
export class GalleryComponent {}
```

### Key attributes/concepts
- **`ngSrc`**: required; replaces `src` for optimized handling.
- **`width` / `height`**: required in most cases to prevent layout shift (or use `fill` mode where supported—verify).
- **`priority`**: marks the image as high priority (preload hints / eager behavior as appropriate). Use for the likely LCP image; avoid overusing.
- **`fill`**: for cases where the image fills a positioned parent (CSS sizing) — verify 15.x support/details.
- **Loaders**: `provideImgixLoader`, `provideCloudinaryLoader`, custom loaders, etc., can transform `ngSrc` into CDN URLs.

```ts
import { provideImgixLoader } from '@angular/common';
bootstrapApplication(AppComponent, {
  providers: [provideImgixLoader('https://my.imgix.net/')],
});
```

### Caveats
- Don’t set `priority` on many images; reserve for true LCP candidates.
- Ensure correct intrinsic dimensions to avoid distortion/CLS issues.
- If using a CDN, configure the right loader/base path.
- `NgOptimizedImage` logs warnings in dev for common mistakes—pay attention to them.

---

## 4. Functional router guards and resolvers

### What changed
The router supports **functional** guards/resolvers (e.g., `CanActivateFn`, `CanMatchFn`, `ResolveFn`) that can use `inject()` directly, reducing the need for class-based guards in many cases.

### Why it matters
- Less boilerplate; easier composition.
- Works naturally with `inject()` and standalone/provider-based configuration.

### Functional `CanActivateFn`
```ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isLoggedIn() ? true : router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url },
  });
};
```

```ts
export const routes: Routes = [
  { path: 'admin', canActivate: [authGuard], loadComponent: () => import('./admin.component').then(m => m.AdminComponent) },
];
```

### Functional resolver
```ts
import { ResolveFn } from '@angular/router';
export const userResolver: ResolveFn<User> = (route) => inject(UserApi).get(route.paramMap.get('id')!);
```

### Caveats
- Class-based guards still work; migrate incrementally.
- `inject()` rules apply (injection context). Router invokes these functions in a context where `inject()` is valid.
- Return types can include `boolean | UrlTree | Observable<...> | Promise<
...>` depending on guard type—match router expectations.

See `examples/functional-guard.ts`.

---

## 5. Functional `CanMatch` and route composition patterns

### What changed
Alongside `CanActivateFn`/`CanDeactivateFn`/`ResolveFn`, the router’s functional API style (including `CanMatchFn` for route matching control, depending on exact 15.x coverage) supports concise, injectable logic.

### Why it matters
- `CanMatch` can prevent loading/matching a route entirely (useful for feature flags, A/B routes, or role-based route trees).
- Functional style composes well with `inject()`.

### Example (conceptual; verify `CanMatchFn` availability/signature in 15.x)
```ts
import { CanMatchFn } from '@angular/router';
import { inject } from '@angular/core';

export const adminMatch: CanMatchFn = () => inject(AuthService).hasRole('admin');
```

---

## 6. Better stack traces and debugging

### What changed
Angular 15 improved developer-facing stack traces (especially in development builds) to make framework/runtime errors easier to trace back to application code.

### Why it matters
- Faster debugging of DI errors, template/runtime issues, and zone-related problems.
- Less noise from framework internals in common failure cases.

### Caveats
- Stack trace formatting differs between dev/prod and optimization settings.

---

## 7. Language Service improvements (auto-imports and DX)

### What changed
The Angular Language Service gained improvements such as better auto-import/completion support in templates and more helpful diagnostics (capabilities evolved across releases).

### Why it matters
- Faster template authoring, especially in standalone components where imports must be explicit.
- Reduces “unknown element/pipe/directive” friction.

### Practical tip
Ensure your editor uses the workspace Angular Language Service version compatible with your project.

---

## 8. Router / HTTP / forms refinements (representative)

- **Provider-based configuration** (`provideRouter`, `provideHttpClient`) is the default story for standalone apps.
- **Functional interceptors** (HTTP) became a key pattern in the v15 era; verify the exact `HttpInterceptorFn`/`withInterceptors` API in 15.x.
- **Typed forms** (from v14) continue to be the standard; v15 apps benefit from the stabilized standalone+typed ecosystem.

---

## 9. CDK and ecosystem notes

- The Angular team repositioned `@angular/flex-layout` relative to core recommendations (it moved to a different maintenance/status path). Prefer modern CSS (Flexbox/Grid) or other supported layout approaches for new work.
- Angular Material/CDK continued to evolve; check MDC-based component migration status relevant to your Material version.

---

## Realtime example (Angular 15 idiom) — detailed

**Folder:** `realtime/`

### Files
- `realtime/live-dashboard.service.ts` — realtime data stream (RxJS/WebSocket-style abstraction) suitable for a standalone, provider-configured app.
- `realtime/live-dashboard.component.ts` — standalone component consuming live updates.

### How it works
1. **App setup:** in a real app, bootstrap with `bootstrapApplication` and `provideHttpClient()`/router as needed; realtime can use WebSocket or SSE.
2. **State:** service multicasts updates; components subscribe via `async` pipe or explicit subscription with cleanup.
3. **UI composition:** `hostDirectives` can attach cross-cutting host behavior (e.g., visibility, accessibility) without changing the realtime logic—see `examples/host-directives.ts` for the composition concept.
4. **Auth/guards:** protect realtime dashboards with functional guards (`examples/functional-guard.ts`).

### Production considerations
- Authenticate before connecting (token refresh flows); handle unauthorized/expired sessions.
- Reconnect with backoff; expose `connected/reconnecting/error` state to the UI.
- Buffer or compact high-frequency updates (throttle/sample/batch) to avoid rendering storms.
- Use `NgOptimizedImage`/CD strategies where appropriate for mixed static+live dashboards.

---

## Migration checklist (14 → 15)
- [ ] Run `ng update @angular/core@15 @angular/cli@15` (and Material/CDK if used).
- [ ] Migrate bootstrapping to `bootstrapApplication` + `provideRouter`/`provideHttpClient` when ready.
- [ ] Replace class-based guards/resolvers with functional `*Fn` forms where straightforward.
- [ ] Adopt `hostDirectives` for host-level behavior composition (standalone directives).
- [ ] Use `NgOptimizedImage` (`ngSrc`, dimensions, `priority` for LCP) for key images.
- [ ] Review flex-layout usage and plan CSS/alternative layout strategy if applicable.
- [ ] Validate HTTP interceptor setup under the v15 API style.

## References (for verification)
- Angular v15 release blog / changelog
- Angular update guide (to v15)
- Standalone APIs guide (`bootstrapApplication`, `provideRouter`, `provideHttpClient`)
- `NgOptimizedImage` documentation
- Directive composition API (`hostDirectives`)
- Router functional guards/resolvers
