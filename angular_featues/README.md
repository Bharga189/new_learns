# Angular Features by Version (Angular 13–22)

This project documents major Angular features from **Angular 13** through **Angular 22**, with concise explanations and practical code examples for each feature. Each version folder also includes a **realtime example** showing the idiomatic approach for live data in that era (RxJS WebSocket/streams, SSE patterns, and later signal-/resource-based realtime integration).

> Scope note: These are focused, version-tagged examples intended for learning and reference. They are not full, installable Angular workspaces per version (Angular’s dependency/build toolchain changes significantly across releases). Use the snippets inside a matching Angular app version, or follow the setup notes in each version README.

## Layout

```
angular_featues/
  README.md                 overview (this file)
  angular13/                Angular 13 examples + realtime example
  angular14/                Angular 14 examples + realtime example
  ...
  angular22/                Angular 22 examples + realtime example
```

Each version directory contains:
- `README.md` — feature-by-feature explanation, what changed, and why it matters.
- `examples/` — focused TypeScript/HTML/config snippets per feature.
- `realtime/` — a realtime data example (service + component/template pattern) using APIs idiomatic to that version.

## Version summary

| Version | Release | Headline features covered |
|---------|---------|---------------------------|
| 13 | Nov 2021 | Ivy-only (View Engine removed), IE11 dropped, dynamic components simplified, persistent build cache, RxJS 7, modernized APF, test improvements |
| 14 | Jun 2022 | Standalone components (dev preview), typed reactive forms, `inject()`, environment injectors, router `Title` strategy, extended template diagnostics |
| 15 | Nov 2022 | Standalone APIs stable, directive composition API, `NgOptimizedImage` stable, functional router guards/resolvers, router default-import unwrap |
| 16 | May 2023 | Signals (developer preview), `required` inputs, router input binding, `takeUntilDestroyed`, non-destructive hydration, `DestroyRef`, esbuild-based build (dev preview) |
| 17 | Nov 2023 | Built-in control flow (`@if/@for/@switch`), `@defer` (deferrable views), SSR/hydration improvements + `@angular/ssr`, Vite/esbuild dev server for new apps, new lifecycle hooks (`afterRender`/`afterNextRender`) |
| 18 | May 2024 | Zoneless change detection (experimental), `@let`, event replay, fallback `ng-content`, function-based redirects, unified control-state change events, Material 3 direction |
| 19 | Nov 2024 | Standalone defaults, linked signals, `resource()`/`rxResource()` (experimental), incremental hydration, `afterRenderEffect`, HMR improvements, stricter standalone ergonomics |
| 20 | May 2025 | Reactivity/resource API maturation, zoneless/stabilization progress, template and DX refinements, hydration/SSR polish |
| 21 | Nov 2025 | Continued signals-first evolution, zoneless/readiness improvements, router/forms/reactivity refinements, tooling updates |
| 22 | May 2026 | Latest release-line refinements covered here: signals/zoneless/hydration/reactivity ergonomics and contemporary realtime patterns |

## Realtime examples

Realtime data is a cross-cutting concern. Each version’s `realtime/` folder shows a small “live prices / live feed” pattern using the style common for that version:

- **13–15:** RxJS-centric services (`webSocket`, `Subject`/`BehaviorSubject`), async pipe, manual subscription hygiene.
- **16:** Signals preview + RxJS interop (`toSignal`/`toObservable` patterns emerging), `takeUntilDestroyed`.
- **17–18:** Built-in control flow + `@defer`/zoneless-aware patterns; `@let` and cleaner template consumption.
- **19–22:** Signal/resource-driven consumption (`resource`/`rxResource` where applicable), linked/computed state, zoneless-friendly updates.

## How to use

1. Pick the version that matches your app (`ng version`).
2. Read that version’s `README.md` for the feature explanation and migration notes.
3. Copy/adapt snippets from `examples/` or `realtime/` into your app.
4. For realtime samples, replace the demo endpoint with your WebSocket/SSE/HTTP stream source and keep reconnection/auth/error handling appropriate for production.

## Verification approach

Because this repository contains reference examples (not 10 full Angular workspaces), verification is by inspection and consistency with the documented Angular release behavior. To run a snippet for real, place it in an Angular app created with the matching major version and execute the framework’s normal build/test commands.
