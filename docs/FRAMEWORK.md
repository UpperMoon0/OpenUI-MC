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

Root and overlay layout use separate dirty flags. A root or viewport layout request also invalidates overlays, while `requestOverlayLayout()` updates overlay geometry without measuring the application tree. Cursor-following feedback, tooltips, and other overlay-only movement should use the overlay-specific path.

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

Computed values automatically track every signal read during evaluation. `Effect` is intended for side effects and must be closed when its owner is disposed. Signal-bound controls remove their subscriptions on unmount. `StateStore.remember` retains keyed state only for the lifetime of its owning store, so code rebuilding components must preserve and reuse that store instance. `AsyncValue` represents loading, success, and failure without sentinel values.

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

Use `VirtualList` for large, fixed-height collections and `VirtualGrid` for virtualized grid collections. Their keyed cells are limited to the visible range plus configurable overscan. Use `DynamicGrid` when columns should adapt to UI scale for small/intrinsic collections.

### Overflow, clipping, and scrolling

Normal layout containers such as `VStack`, `HStack`, and `Stack` keep normal layout semantics: they do not become scroll views and do not gain surprise wheel capture or scrollbars. `ClipStack` remains the explicit low-level clipping primitive.

Bounded visual surfaces such as `Card`, `Panel`, and `StyledBox` use one protected child-content rectangle for layout, descendant painting, and descendant hit-testing. The rectangle respects padding and the actually rendered border width and, for rounded surfaces, uses the renderer-clamped radius (`min(configuredRadius, min(width,height)/2)`) before choosing a conservative rectangle whose four corners stay inside the rounded inner fill. Auto-sized surfaces solve that inset against their candidate intrinsic width/height until it converges, so a large configured pill radius does not inflate preferred size merely because the raw radius is large. Descendants therefore cannot square off a low-padding rounded corner or receive input in pixels that their clip hides. The shared legacy `childrenMouse*` forwarding helpers enforce the same child clip for click, scroll, drag, and release, so a bounded container cannot bypass clip-aware hit-testing by forwarding directly to overflowing children. `Card` permanently reserves its one-pixel state-outline gutter, so focus/selection only repaint the outline and never reflow content. This is still only an overflow safety boundary: it does not make overflowing content accessible and it does not change layout into implicit scrolling. Surface shadows are painted outside the child clip, while dropdowns, popovers, tooltips, dialogs, toasts, and drag feedback use `OverlayManager` roots and therefore are not trapped by an ancestor surface clip.

When content can legitimately exceed the available height, put the body inside an explicit `Ui.scroll(...)` and flex that viewport within the card. `ScrollView` remains a paint/pointer clip, but it is also a keyboard-focus viewport: off-screen focusable descendants stay in Tab/programmatic focus order, newly focused descendants are automatically scrolled into view, and manually scrolling a focused descendant off-screen does not invalidate its keyboard focus:

```java
return Ui.card(
    Ui.column(
        Ui.heading("Details"),
        Ui.scroll(Ui.text(longBody)).flex()
    ).gap(6)
).padding(8).height(140).fillWidth();
```

With the OpenUI inspector active, bounded non-scroll overflow emits a deduplicated development warning (`Content height exceeds bounded parent; consider Ui.scroll(...)`). The same diagnostic can be enabled without the inspector with `-Dopenui.debug.layout=true`. Constraint-managed subtrees (for example a flexed explicit scroll viewport) are conservatively excluded so a correct bounded layout is not told to add scrolling. `LayoutDiagnostics` is a public development API for tooling that needs to enable or query these diagnostics programmatically; ordinary screens can just use the inspector/property.

### Flex semantics

Flex applies from **parent → child**:

- `child.flex()` means: *"allocate leftover space to this child inside its flex parent (`HStack` or `VStack`)"*.
- Calling `parent.flex()` does **not** change how the parent distributes space to its own children; it only affects how the parent itself receives space from its grandparent.

```java
// Correct: the list child receives remaining height inside root
VStack root = new VStack();
root.addChild(header);

UIComponent content = Ui.list(...);
content.flex();
root.addChild(content);

// Incorrect: calling root.flex() does not make children expand
VStack root = new VStack();
root.flex();
root.addChild(header);
root.addChild(Ui.list(...));
```

## Input, focus, and native fields

Events travel capture → target → bubble and may stop propagation, prevent their default action, or capture the pointer. `preventDefault()` suppresses legacy/default handling. For compatibility with OpenUI's legacy handlers, `stopPropagation()` stops traversal and suppresses that default-handler bridge as well. Pointer capture should only be requested for a button/action the control actually begins. Standard controls are focusable and keyboard operable. Tab and Shift+Tab traverse focus, but descendants whose entire laid-out rectangle is removed by a bounded surface clip are excluded from traversal/programmatic focus and lose keyboard eligibility if later clipped away. Explicit `ScrollView` clips are different: they preserve off-screen descendants in keyboard focus order and reveal a target when focus moves to it. Pre-layout components remain eligible for initial focus setup, while components that have actually been laid out to zero width or zero height are treated as having no focusable area. Hover propagation follows the same ancestor clip chain, so invisible overflow cannot enter or retain hover state or fire hover transitions. Modal Escape handling is centralized. A `TextField` owns its vanilla `EditBox` through the runtime, so screens must not call `addRenderableWidget` or synchronize widget bounds themselves. `UiScreen` and `UiContainerScreen` always route mouse-down through OpenUI first; when they fall back to vanilla Screen handling, OpenUI-owned native widgets are temporarily inactive so a registered `EditBox` cannot bypass ancestor clip-aware hit-testing while unrelated vanilla screen children can still receive the fallback click.

## Forms, navigation, overlays, and animation

`Form` groups typed `Field<T>` values and validators into a reactive validity result. `Navigator` provides push, replace, and pop semantics over typed `Route<T>` values. Runtime overlays have base, dropdown, popover, modal, toast, tooltip, and debug layers; `Dialog` uses the modal layer and closes on Escape. `AnimationManager` owns time-based animations and easing and cancels them when the runtime closes.

## Themes and custom components

The runtime supplies a `Theme` to every mounted component. Standard controls consume semantic colors such as primary, surface, danger, success, border, and their foreground colors. Replace the runtime theme instead of hard-coding control colors.

Custom components should implement measurement/layout only when their geometry is special. Minecraft 1.20.1 and 1.21.1 render through `GuiGraphics`; 26.1.2 uses `GuiGraphicsExtractor` and extracted render state. Prefer composing existing components. Register subscriptions in `onMount`, close them in `onUnmount`, use semantic theme colors, and call the narrowest invalidation method after mutation. Coordinate changes require layout invalidation; visual-only changes require paint invalidation.

High-level OpenUI components are the safe path: they own layout, clipping, and semantic foreground/background contrast. `UiRender` and custom render hooks are intentional low-level escape hatches. Code using them owns text contrast, clipping, bounds, and collision avoidance.

Vanilla menu slots and other native screen regions are outside the OpenUI layout tree. OpenUI does not move or reserve those regions automatically, because doing so would change menu coordinates and network-visible behavior. Container screens must explicitly reserve their native regions or anchor custom annotations to the menu's slot constants, and should regression-test those rectangles when layouts change.

## Cross-version source layout

`shared/core/src/main/java` contains framework classes that compile unchanged for every supported Minecraft target. The 1.20.1 common, 1.21.1 common, and 26.1.2 NeoForge projects all include that source set. APIs or rendering code that differ by Minecraft version remain in their version directories. A class should move into shared core only after it is identical across targets and the complete `testAllVersions` plus `buildAll` matrix passes.

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
