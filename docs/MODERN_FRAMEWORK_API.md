# Modern Framework API

This document describes the additive modern-framework surface introduced by `docs/MODERN_FRAMEWORK_PLAN.md`. Existing retained-mode OpenUI code remains valid; none of these APIs are required for existing screens.

## Compatibility model

Legacy code continues to use `UIComponent`, `Ui.*`, `UiScreen`, `UiContainerScreen`, signals, themes and direct tree mutation. Modern code is layered on top:

```text
signals/context
    ↓
Ui.component(scope -> ...)
    ↓
DeclarativeChild description tree
    ↓
DeclarativeTree + KeyedReconciler
    ↓
existing retained UIComponent tree
```

The retained tree remains the rendering/input/layout authority. Declarative code owns identity and updates only inside declaratively-described subtrees. A declarative leaf may therefore wrap an existing retained component that manages its own children.

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

Every mount receives a fresh `UiScope`. Resources are disposed once, in reverse ownership order. Keyed ownership with `scope.own(key, factory)` is useful for work created from rerunnable declarative builds because the same resource is reused for the lifetime of the mount.

## Context providers

Context keys are identity-based and typed.

```java
static final ContextKey<MyClient> CLIENT = ContextKey.create("client");

UIComponent tree = Ui.provide(CLIENT, client, screenContent);
```

Inside a scoped component:

```java
MyClient client = context(CLIENT);
Optional<MyClient> optional = findContext(CLIENT);
```

Inside a functional builder:

```java
Ui.component(scope -> {
    MyClient client = scope.context(CLIENT);
    return List.of(/* descriptions */);
});
```

The nearest ancestor provider wins. Provider values are reactive: declarative builders that read a context value are rebuilt when that provider changes. Retained consumers still receive layout invalidation as a compatibility fallback.

## Functional declarative components

Use `Ui.component(...)` for a rerunnable component builder. The builder receives a `UiBuildScope` containing remembered state, context lookup and scoped async composition.

```java
Signal<Boolean> advanced = Signals.of(false);

UIComponent host = Ui.component(scope -> List.of(
    Ui.node("title", "title", () -> Ui.text("Settings")),
    Ui.node(
        "advanced-panel",
        "advanced",
        () -> Ui.column(),
        component -> component.setVisible(advanced.get()),
        Ui.node("body", "body", () -> Ui.text("Advanced settings"))
    )
));
```

`DeclarativeChild` is immutable and contains a logical type, optional key, retained-node factory, update callback and child descriptions. The factory runs only when reconciliation cannot reuse an existing retained node.

### Reconciliation rules

- same type + same key: reuse the retained instance;
- keyed reorder: preserve retained identity/state;
- same key + different type: detach the old subtree before attaching the replacement;
- removed nodes: deterministic unmount/scope cleanup;
- unkeyed children: positional matching;
- duplicate sibling keys: fail fast;
- externally mutating a declaratively-owned direct child list fails closed with a diagnostic;
- declarative leaves can embed legacy retained components and leave their internal subtree untouched.

This is intentionally much smaller than React Fiber. OpenUI does not implement a DOM, hydration or server rendering.

## Scheduling

`FrameScheduler` provides deterministic, insertion-ordered task coalescing. Keyed tasks with the same key execute once per flush. Work scheduled by work already being flushed is processed in a subsequent pass. A configurable pass limit catches runaway update loops.

Mounted declarative hosts share a scheduler per `UiRuntime`, so multiple signal/context invalidations in one frame coalesce before retained reconciliation/layout. Overlay-only layout and legacy direct invalidation retain their existing paths.

## Styles

`Style` is an immutable lightweight style value rather than CSS. `StateStyle` composes hover/focus/pressed/disabled variants.

```java
StateStyle style = new StateStyle(
    Style.builder()
        .padding(6)
        .background(0xCC101010)
        .radius(4)
        .minWidth(120)
        .typography(TextStyle.BODY)
        .build(),
    Style.builder().background(0xDD202020).build(),
    Style.builder().border(1, 0xFFFFFFFF).build(),
    Style.EMPTY,
    Style.builder().padding(0).noBorder().build()
);

UIComponent styled = Ui.styled(style, content);
```

Styles support margin/padding, background/border/radius, exact/min/max dimensions, typography intent, state variants and explicit reset values such as zero padding or no border. `StyledBox` applies the size constraints during measurement/layout while existing fluent component APIs remain unchanged.

## Stable drawing SPI

Reusable component libraries should target `UiDrawContext` where possible. `UiCanvas` implements this interface on every supported Minecraft generation.

The stable surface covers:

- text/components;
- fills, rounded rectangles, borders, surfaces and shadows;
- namespaced texture regions through `UiTexture`;
- item rendering;
- tooltip rendering;
- clipping;
- push/translate/scale/pop transforms.

`UiCanvas.rawGraphics()` remains the explicit low-level escape hatch when a component needs version-specific `GuiGraphics`/`GuiGraphicsExtractor` behavior.

## Semantics and navigation

Attach renderer-independent accessibility metadata with `SemanticComponent` and `Semantics`.

```java
UIComponent buy = Ui.semantic(
    Semantics.button().label("Buy item").disabled(false).build(),
    Ui.button("Buy", this::buy)
);
```

`SemanticNarration.describe(...)` converts role/state/value metadata into concise narration text. `describeNearest(...)` resolves the nearest semantic wrapper for a retained/focused leaf and `describeTree(...)` exposes visible semantic descriptions in retained-tree order. `FocusManager.focusedNarration()` connects focused UI to that semantic layer.

`FocusManager.focusDirection(...)` uses `SpatialNavigation` for directional keyboard/controller movement while respecting active focus traps and overlays. Existing Tab traversal remains unchanged.

## Profiling / Devtools 2.0 foundation

`UiProfiler` records build, reconciliation, layout, paint and event timing plus update causes. It also keeps a bounded chronological trace and exposes default-safe slow-sample warnings.

Every `DeclarativeHost` exposes `profiler()`. Event dispatch through the host is traced with capture/target/bubble phase information, and update counts represent declarative rebuilds rather than every render sample. Instrumentation is disabled by default and clears when disabled.

## Scoped async work

`ScopedAsync.submit(...)` owns a `CompletableFuture` through a `UiScope`. Unmount closes/cancels the task and suppresses stale result delivery.

For rerunnable builders, use a key so repeated builds do not duplicate work:

```java
ReadableSignal<AsyncValue<User>> user = scope.async(
    "user:" + userId,
    () -> client.loadUser(userId),
    backgroundExecutor,
    minecraftClientExecutor
);
```

Changing the key intentionally creates distinct scope-owned work/state. `AsyncValue` and `AsyncComponent` remain compatible facades for loading/error/success UI.

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

Published modules include runtime and sources artifacts plus Gradle/POM metadata. Composite builds remain useful for OpenUI contributors but are no longer the only dependency path.

## API compatibility gates

The annotations `@PublicApi`, `@Experimental`, `@Internal` and `@Since` document compatibility intent without moving existing public classes between packages.

CI protects the compatibility contract with:

- legacy 1.20.1/1.21.1 source-parity checks;
- unit tests;
- all supported loader/Minecraft builds;
- a public JVM binary API comparison against the released 0.0.7 Fabric baseline;
- the precompiled Simply Speakers downstream boot fixture.

The binary API check emits a compatibility report into the GitHub Actions summary and fails when a released public class/member disappears or changes JVM descriptor.
