# OpenUI MC framework guide

OpenUI MC owns screen plumbing so application code only holds state, business callbacks, and UI composition. The composition API is available for Fabric and Forge on 1.20.1, Fabric and NeoForge on 1.21.1, and NeoForge on 26.1.2. Read [Getting Started](GETTING_STARTED.md) first when integrating the library and use the [API Reference](API_REFERENCE.md) as a component catalog.

## Screens and lifecycle

Extend `UiScreen` for a normal screen or `UiContainerScreen<M>` for a menu screen, then return one root component:

```java
public final class SettingsScreen extends UiScreen {
    private final Signal<Boolean> enabled = Signals.of(true);

    public SettingsScreen() {
        super(Component.literal("Settings"));
    }

    @Override
    protected UIComponent buildUI() {
        return Ui.column(
            Ui.heading("Settings"),
            Ui.checkbox("Enabled", enabled),
            Ui.button("Done", this::onClose).primary()
        ).gap(8);
    }
}
```

`UiRuntime` mounts and unmounts the tree, measures and lays it out, renders it, dispatches input, advances animations, manages focus and overlays, and registers native widgets. Components can override `onMount()` and `onUnmount()` for resources and subscriptions. Mutating structure calls build invalidation; size-affecting changes call layout invalidation; visual-only changes call paint invalidation.

## Reactive state

```java
Signal<String> query = Signals.of("");
Signal<List<Product>> products = Signals.of(List.of());
Computed<List<Product>> visible = Signals.computed(() -> products.get().stream()
    .filter(product -> product.name().contains(query.get()))
    .toList());

Signals.batch(() -> {
    query.set("");
    products.set(newProducts);
});
```

Computed values automatically track every signal read during evaluation. `Effect` is intended for side effects and must be closed when its owner is disposed. Signal-bound controls remove their subscriptions on unmount. `StateStore.remember` retains keyed state across component rebuilding, while `AsyncValue` represents loading, success, and failure without sentinel values.

## Layout

Components measure against `Constraints` and return a `Size`. Rows and columns support gaps, cross-axis `Alignment`, main-axis `Justification`, fixed/min/max constraints, and fill/flex behavior. `Stack` and `Positioned` cover layered layouts, `ClipStack` provides nested clipping, and `Responsive` can choose a subtree from the current viewport.

```java
return Ui.padding(12,
    Ui.column(
        Ui.row(Ui.heading("Market"), Ui.spacer(), Ui.textField(search).width(120))
            .align(Alignment.CENTER)
            .justify(Justification.SPACE_BETWEEN),
        Ui.grid(results, this::productCard).minCellWidth(96).gap(6).flex()
    ).gap(8)
);
```

Use `VirtualList` for large, fixed-height collections. Its keyed cells are limited to the visible range plus configurable overscan. Use `DynamicGrid` when columns should adapt to UI scale.

## Input, focus, and native fields

Events travel capture → target → bubble and may stop propagation, prevent their default action, or capture the pointer. `preventDefault()` suppresses legacy/default handling. For compatibility with OpenUI's legacy handlers, `stopPropagation()` stops traversal and suppresses that default-handler bridge as well. Pointer capture should only be requested for a button/action the control actually begins. Standard controls are focusable and keyboard operable. Tab and Shift+Tab traverse focus; modal Escape handling is centralized. A `TextField` owns its vanilla `EditBox` through the runtime, so screens must not call `addRenderableWidget` or synchronize widget bounds themselves.

## Forms, navigation, overlays, and animation

`Form` groups typed `Field<T>` values and validators into a reactive validity result. `Navigator` provides push, replace, and pop semantics over typed `Route<T>` values. Runtime overlays have base, dropdown, popover, modal, toast, tooltip, and debug layers; `Dialog` uses the modal layer and closes on Escape. `AnimationManager` owns time-based animations and easing and cancels them when the runtime closes.

## Themes and custom components

The runtime supplies a `Theme` to every mounted component. Standard controls consume semantic colors such as primary, surface, danger, success, border, and their foreground colors. Replace the runtime theme instead of hard-coding control colors.

Custom components should implement measurement/layout only when their geometry is special. Minecraft 1.20.1 and 1.21.1 render through `GuiGraphics`; 26.1.2 uses `GuiGraphicsExtractor` and extracted render state. Prefer composing existing components. Register subscriptions in `onMount`, close them in `onUnmount`, use semantic theme colors, and call the narrowest invalidation method after mutation. Coordinate changes require layout invalidation; visual-only changes require paint invalidation.

`FadeTransition` is deliberately unavailable on 26.1.2 because the extracted renderer cannot apply scoped opacity to an arbitrary component subtree. Use `SlideTransition` or `ScaleTransition` for cross-version code.

## Testing and performance

Layout, state, event, focus, lifecycle, and navigation logic can be tested without launching Minecraft. Keep stable keys in virtual collections, avoid rebuilding trees from render methods, batch related state changes, and use computed state rather than duplicating derived values.

Run the complete matrix with the Gradle 9.1.0 wrapper before publishing:

```shell
./gradlew clean buildAll testAllVersions
```

## Migrating an existing screen

1. Replace `Screen` / `AbstractContainerScreen` with the matching OpenUI base screen.
2. Move the component tree into `buildUI()`.
3. Replace mutable duplicated display state with signals and computed values.
4. Replace coordinates with row, column, padding, grid, stack, and responsive constraints.
5. Replace raw `EditBox` registration, focus plumbing, event forwarding, scissor calls, and manual animation loops with framework controls and runtime services.
6. Leave packets, menus, domain formatting, and business decisions in the consuming mod.

The migration is complete when the screen contains composition and business callbacks but no framework plumbing.
