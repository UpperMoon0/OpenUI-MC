# Modern Framework API

This document describes the additive modern-framework surface introduced by `docs/MODERN_FRAMEWORK_PLAN.md`. Existing retained-mode OpenUI code remains valid; none of these APIs are required for existing screens.

## Compatibility model

Legacy code continues to use `UIComponent`, `Ui.*`, `UiScreen`, `UiContainerScreen`, signals, themes and direct tree mutation. Modern code is layered on top:

```text
signals/context
    ↓
DeclarativeHost builder
    ↓
DeclarativeChild descriptions
    ↓
KeyedReconciler
    ↓
existing retained UIComponent children
```

The retained tree is still the rendering/input/layout authority. Declarative code only manages identity and updates inside its host subtree.

## Lifecycle scopes

Extend `ScopedUIComponent` when a custom component owns reactive or asynchronous resources.

```java
final class StatusPanel extends ScopedUIComponent {
    @Override
    protected void onScopedMount(UiScope scope) {
        scope.effect(() -> updateStatus(status.get()));
        scope.subscribe(progress, this::onProgress);
        scope.own(myCloseable);
        Signal<String> query = scope.remember("query", "");
    }
}
```

Every mount receives a fresh `UiScope`. Resources are disposed once, in reverse ownership order, after `onScopedUnmount`. Existing `UIComponent.onMount/onUnmount` semantics are unchanged for legacy subclasses.

## Context providers

Context keys are identity-based and typed.

```java
static final ContextKey<MyClient> CLIENT = ContextKey.create("client");

UIComponent tree = Contexts.provide(CLIENT, client, screenContent);
```

Inside `ScopedUIComponent`:

```java
MyClient client = context(CLIENT);
Optional<MyClient> optional = findContext(CLIENT);
```

The nearest ancestor provider wins. Existing theme inheritance remains independent for compatibility.

## Functional declarative components

`DeclarativeHost` tracks signals read by its builder through a scoped effect. A dependency change schedules one coalesced rebuild before the next measurement/layout pass.

```java
Signal<Boolean> advanced = Signals.of(false);

UIComponent host = new DeclarativeHost(() -> List.of(
    new DeclarativeChild<>(
        "title",
        "title",
        () -> Ui.text("Settings"),
        component -> component.setVisible(true)
    ),
    new DeclarativeChild<>(
        "advanced-panel",
        "advanced",
        () -> buildAdvancedPanel(),
        component -> component.setVisible(advanced.get())
    )
));
```

A `DeclarativeChild` contains a logical type, optional key, factory and update function. The factory runs only when the retained child cannot be reused.

### Reconciliation rules

- same type + same key: reuse retained instance;
- keyed reorder: preserve retained identity/state;
- same key + different type: replace;
- removed node: unmount through normal retained lifecycle;
- unkeyed children: positional matching;
- duplicate sibling keys: fail fast with a diagnostic.

This is intentionally much smaller than React Fiber. OpenUI does not implement a DOM, hydration or server rendering.

## Scheduling

`FrameScheduler` provides deterministic, insertion-ordered task coalescing. Keyed tasks with the same key execute once per flush. Work scheduled by work already being flushed is processed in a subsequent pass. A configurable pass limit catches runaway update loops.

`DeclarativeHost` uses this scheduler internally so multiple reactive invalidations collapse into one retained-tree reconciliation.

## Styles

`Style` is an immutable lightweight style value rather than CSS. `StateStyle` composes hover/focus/pressed/disabled variants.

```java
StateStyle style = new StateStyle(
    Style.builder().padding(6).background(0xCC101010).radius(4).build(),
    Style.builder().background(0xDD202020).build(),
    Style.builder().border(1, 0xFFFFFFFF).build(),
    Style.EMPTY,
    Style.EMPTY
);

UIComponent styled = new StyledBox(style, content);
```

`StyledBox` is additive, so existing fluent layout/control APIs remain unchanged.

## Stable drawing SPI

Reusable component libraries should target `UiDrawContext` where possible. `UiCanvas` implements this interface on every supported Minecraft generation.

The stable surface includes fills, rounded surfaces/outlines, shadows, text and clipping. `UiCanvas.rawGraphics()` remains available for version-specific rendering that cannot be expressed through the SPI.

## Semantics and navigation

Attach renderer-independent accessibility metadata with `SemanticComponent` and `Semantics`.

```java
UIComponent buy = new SemanticComponent(
    Semantics.button().label("Buy item").disabled(false).build(),
    Ui.button("Buy", this::buy)
);
```

`SemanticNarration.describe(...)` converts role/state/value metadata into concise narration text. `FocusManager.focusDirection(...)` uses `SpatialNavigation` to move focus spatially while respecting active focus traps and overlays, providing the framework hook needed by arrow-key or controller adapters.

## Profiling

`UiProfiler` records build, reconciliation, layout, paint and event timing plus update causes. Profiling is opt-in and clears data when disabled.

Every `DeclarativeHost` exposes `profiler()` so inspector/devtools integrations can enable it and inspect snapshots without changing application code.

## Scoped async work

`ScopedAsync.submit(...)` owns a `CompletableFuture` through a `UiScope`. Unmount closes/cancels the task and suppresses stale result delivery.

The caller supplies separate background and delivery executors; Minecraft client state should only be mutated from the delivery executor configured for the client thread.

## Maven dependency

Release artifacts are published to GitHub Packages after a GitHub Release succeeds.

```gradle
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/UpperMoon0/OpenUI-MC")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull
            password = providers.gradleProperty("gpr.key").orNull
        }
    }
}

dependencies {
    modImplementation("com.nstut:openui-mc-neoforge-1.21.1:<version>")
}
```

GitHub Packages requires GitHub credentials for dependency download. Composite builds remain useful for OpenUI contributors but are no longer the only publication path.

## API stability

The annotations `@PublicApi`, `@Experimental`, `@Internal` and `@Since` document compatibility intent without moving existing classes between packages. Existing package names are grandfathered to avoid binary breakage.

The existing precompiled Simply Speakers compatibility fixture remains the strongest binary regression gate, alongside multi-version builds/tests and legacy source-parity checks.
