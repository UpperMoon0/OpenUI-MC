# OpenUI Devtools 2.0

`UiInspector` is the runtime-facing devtools surface for the modern framework. Instrumentation remains disabled during normal gameplay and is enabled only while an inspector is open.

## Inspector diagnostics

For the currently hovered or selected component, the inspector exposes:

- retained component type, key, bounds, visibility/hover/focus state and dirty flags;
- active scoped effects, subscriptions, async tasks, remembered state and keyed resources;
- named signal dependencies and computed-signal dependency edges;
- declarative reconciliation counts (created, reused, removed and total managed nodes);
- build/layout/paint timings and declarative rebuild count;
- the exact named signal/context dependency that caused the most recent rebuild when known;
- recent capture/target/bubble event timing from the nearest declarative host;
- slow build/reconcile/layout/paint/event warnings;
- nearest semantic narration description;
- the retained component hierarchy with keys.

Named framework-owned signals use readable diagnostic names such as `state:<key>` and `context:<key>`. Unnamed/custom signals still receive a stable per-process identity label.

## Programmatic diagnostics

`UiProfiler` provides snapshots, a bounded chronological trace and warning samples. `DeclarativeHost.profiler()` exposes its profiler and `DeclarativeHost.reconcileDiagnostics()` exposes the most recent reconciliation counts.

`ScopedUIComponent.scopeDebugSnapshot()` returns read-only lifecycle/resource counts without exposing the mutable scope itself. `Signals.debug(...)` and `Signals.debugDependencies(...)` expose signal graph information without subscribing or changing dependency tracking.

These diagnostics are additive APIs intended for tooling. Existing retained components do not need to opt in, and normal rendering does not pay profiler trace costs while the inspector is closed.
