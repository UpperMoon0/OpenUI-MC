# OpenUI MC — Modern Framework Evolution Plan

## Goal

Evolve OpenUI from a strong reactive retained-mode Minecraft UI toolkit into a modern declarative application framework while preserving source and binary compatibility for existing consumers.

The target architecture borrows the useful parts of React, Solid and Jetpack Compose without importing browser-specific complexity:

```text
application state
      ↓
declarative component descriptions
      ↓
keyed reconciler
      ↓
retained OpenUI component/render tree
      ↓
layout · input · animation · overlays
      ↓
Minecraft rendering/platform adapter
```

OpenUI will remain Java-first, Minecraft-native and usable through the existing `UIComponent`/`Ui` APIs throughout the migration.

## Compatibility contract

This roadmap is additive by default.

1. Existing `UIComponent` subclasses remain supported.
2. Existing `Ui.*` factories retain their signatures and behavior.
3. Existing mutable tree operations (`addChild`, `removeChild`, `replaceChild`) remain available.
4. Existing signals, controls, screens, themes, overlays, navigation and native-widget integrations continue to work.
5. The new declarative runtime is introduced beside the retained API and internally adapts to it.
6. No existing public method is removed or narrowed before a future major version with an explicit migration path.
7. CI must continue compiling and booting precompiled downstream consumers.
8. Cross-version APIs must not expose a newer Minecraft renderer unless explicitly documented as a low-level escape hatch.

## Phase 1 — Framework ownership scopes

Introduce `UiScope`, a lifecycle-owned resource scope attached to every mounted component.

It owns:

- `Effect`
- `Subscription`
- `AutoCloseable` resources
- remembered signals/state
- future async jobs and animation handles

Required behavior:

- resources registered while mounted are closed exactly once on unmount;
- remounting creates a fresh mounted scope;
- disposal remains idempotent;
- legacy components that never use a scope behave exactly as before.

Public convenience APIs should include:

```java
protected final UiScope scope();

scope().effect(() -> { ... });
scope().subscribe(signal, value -> { ... });
Signal<T> state = scope().remember("key", initialValue);
scope().own(resource);
```

This removes manual effect/subscription cleanup without changing the existing signal API.

## Phase 2 — Generic context/provider system

Introduce typed context keys and inherited providers:

```java
ContextKey<MyService> SERVICE = ContextKey.create("service");

Ui.provide(SERVICE, service, subtree);
MyService service = context(SERVICE);
```

Requirements:

- nearest provider wins;
- lookup follows component ancestry;
- provider updates invalidate only dependent consumers where practical;
- missing required context produces a useful diagnostic;
- context keys use identity, not string equality.

Migrate theme inheritance internally onto the context mechanism only after compatibility tests prove identical behavior. Existing `.theme(...)` remains supported.

## Phase 3 — Declarative descriptions and keyed reconciler

Introduce an immutable/lightweight description layer separate from retained `UIComponent` instances.

A description contains at minimum:

- component type/factory;
- optional key;
- immutable/configurable props;
- child descriptions.

The reconciler compares old and new descriptions by type + key and performs the minimum retained-tree mutation.

Rules:

- same type + same key → reuse retained instance and update props;
- different type or key → unmount old subtree and mount replacement;
- keyed sibling reorder → preserve component identity/state;
- removed nodes → deterministic unmount and scope cleanup;
- duplicate sibling keys → development diagnostic;
- unkeyed children retain positional semantics.

The first implementation should be deliberately small. Minecraft does not need a browser DOM, hydration, server components or React Fiber.

## Phase 4 — Functional/declarative components

Add a component abstraction whose build function may be rerun safely:

```java
Ui.component(scope -> Ui.column(
    Ui.text(() -> title.get()),
    productList()
));
```

A component owns:

- scope;
- remembered state slots;
- signal dependencies read during build;
- context dependencies;
- child description identity.

Signal changes should invalidate the narrowest owning component rather than requiring developers to mutate retained nodes manually.

Legacy retained components can be embedded inside declarative components, and declarative components can be hosted by existing `UiScreen` implementations.

## Phase 5 — Deterministic update scheduler

Create a runtime scheduler that coalesces application changes into a frame transaction:

```text
signal writes
   ↓
batched invalidations
   ↓
rebuild dirty declarative components
   ↓
reconcile
   ↓
layout dirty roots
   ↓
paint
```

Requirements:

- multiple signal writes in one tick/frame do not cause redundant rebuild/layout passes;
- updates triggered while flushing are queued deterministically;
- runaway update loops are detected in development mode;
- overlay-only layout keeps its existing fast path;
- legacy direct invalidation continues to work.

## Phase 6 — Public API, SPI and internal boundaries

Organize compatibility expectations explicitly:

```text
com.nstut.openui.api       stable consumer API
com.nstut.openui.spi       stable extension/component-author API
com.nstut.openui.internal  implementation detail
```

Introduce source-retained documentation annotations:

- `@PublicApi`
- `@Experimental`
- `@Internal`
- `@Since`

Do not physically move existing public classes merely to satisfy package naming if that would break binary compatibility. Existing packages can be grandfathered into the public contract and migrated only through aliases/facades.

CI additions:

- binary API baseline comparison;
- source compatibility fixtures;
- precompiled consumer boot tests;
- all supported Minecraft/loader targets;
- compatibility report attached to release PR/build.

## Phase 7 — Distribution for downstream developers

Publish release artifacts to a real Maven repository in addition to CurseForge/GitHub releases.

Target consumer experience:

```gradle
repositories {
    maven("https://<repository>/releases")
}

dependencies {
    modImplementation("com.nstut:openui-mc-<target>:<version>")
}
```

Composite builds remain documented for OpenUI development but stop being the primary dependency path.

Publishing must include:

- runtime jar;
- sources jar;
- Gradle module/POM metadata;
- checksums;
- loader/Minecraft compatibility metadata;
- immutable release coordinates.

## Phase 8 — Stable rendering SPI

Power-user component libraries should normally render through OpenUI abstractions rather than Minecraft-version-specific graphics classes.

Expand `UiCanvas`/platform abstractions to cover the stable primitive set required by custom controls:

- text/components;
- rectangles/rounded rectangles/borders;
- textures/sprites;
- items;
- clipping;
- transforms where supported;
- tooltip/narration hooks.

Keep direct `GuiGraphics`/`GuiGraphicsExtractor` access as an explicitly low-level compatibility escape hatch.

## Phase 9 — Reusable style system

Build a lightweight style layer on top of `Theme` tokens, not CSS.

Support reusable immutable styles for:

- padding/margin;
- background/border/radius;
- width/height constraints;
- typography intent;
- state variants (hover/focus/disabled/pressed);
- composition/override.

Existing component fluent methods remain valid and can translate into the style model internally over time.

## Phase 10 — Semantics, narration and controller navigation

Introduce a semantic tree independent from rendering:

- role;
- accessible label;
- value/state;
- enabled/disabled;
- actions;
- heading/group semantics.

Use it for Minecraft narration and richer keyboard/controller navigation. Preserve current Tab/focus behavior while adding spatial/directional focus as an opt-in/default-safe capability.

## Phase 11 — Devtools 2.0

Extend `UiInspector` with:

- build/reconcile/layout/paint timings;
- per-component update counts;
- "why did this update?" signal/context causes;
- active subscriptions/effects/resources;
- signal dependency graph;
- event capture/target/bubble trace;
- slow-frame/component warnings;
- key/reconciliation diagnostics.

All instrumentation must be cheap or disabled in production mode.

## Phase 12 — Async composition and cancellation

Integrate async work with component scopes:

- cancellation on unmount/replacement;
- stale-result suppression;
- declarative loading/error/success boundaries;
- optional retry policies;
- no Minecraft client state mutation from background threads.

Existing `AsyncValue` and `AsyncComponent` remain compatible facades.

## Non-goals

OpenUI should not copy web-specific machinery that does not solve Minecraft UI problems:

- DOM emulation;
- HTML/CSS parser;
- JSX requirement;
- SSR/hydration;
- server components;
- browser event compatibility;
- React Fiber/concurrent rendering complexity for its own sake.

## Implementation sequence

The safe dependency order is:

1. lifecycle scope;
2. context foundation;
3. description/reconciler internals;
4. functional component host;
5. scheduler integration;
6. API/ABI enforcement;
7. Maven publishing;
8. rendering SPI;
9. style layer;
10. semantics/controller support;
11. devtools profiling;
12. async scope integration.

Each phase must ship with tests before the next phase relies on it.

## Phase 1 acceptance tests

The first implementation PR starts with lifecycle scopes because it is additive and immediately useful to power users.

Required tests:

- owned `AutoCloseable` closes on unmount;
- owned resource closes only once across repeated unmount/dispose calls;
- `scope.effect` reacts while mounted and stops reacting after unmount;
- `scope.subscribe` stops receiving values after unmount;
- remembered state remains stable during one mount;
- remount creates a clean scope;
- child scopes close when a parent subtree is removed;
- legacy `onMount`/`onUnmount` ordering remains unchanged;
- existing downstream binary compatibility smoke test remains green.

## Definition of modern-framework completion

OpenUI reaches this roadmap's target when a downstream developer can build a large, reactive, reusable UI without manually managing retained component identity, subscriptions/effects, resource cleanup, renderer-version differences, focus plumbing, native widgets, or redundant rebuild/layout scheduling—and can publish reusable OpenUI component libraries against a documented stable SPI.