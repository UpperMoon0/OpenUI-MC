# OpenUI MC API reference

This is a practical catalog of the public framework surface. The canonical signatures remain the Java sources for the selected version module.

## Entry points

- `Ui`: factories for layout and controls.
- `UIComponent`: base class for custom components.
- `UiScreen`: base for ordinary screens.
- `UiContainerScreen<M>`: base for menu-backed screens.
- `UiRuntime`: lifecycle, layout, painting, input, focus, overlays, animation, and native widget ownership.
- `UiTheme` and `Theme`: component sizing constants and runtime semantic styling.

## Layout factories

- `Ui.row(...)` / `HStack`: horizontal layout with gap, alignment, and justification.
- `Ui.column(...)` / `VStack`: vertical layout.
- `Ui.stack(...)` / `Stack`: layered children.
- `Ui.padding(...)`: uniform, axis-based, or `Insets` padding.
- `Ui.positioned(...)`: explicit positioning within a stack.
- `Ui.clip(...)`: nested scissor clipping.
- `Ui.responsive(builder)`: selects content using viewport context.
- `Ui.spacer()`, `Ui.divider()`, and sized/flex constraints.
- `Ui.grid(items, renderer)` / `DynamicGrid`: responsive repeated cards.
- `Ui.list(items, renderer)` / `VirtualList`: virtualized, keyed list rows.
- `ScrollList` and `ScrollGrid`: lower-level scrollable collections.

## Content and input controls

- Text: `text`, `heading`, `title`, and signal-bound `SignalText`.
- Actions: `button`, semantic button variants, and icons from items, textures, glyphs, or custom renderers.
- Fields: `textField`, `checkbox`, `toggle`, `slider`, `select`, and `radio`.
- Navigation/content: `tabs`, `switcher`, `card`, `badge`, and `chip`.
- Data: `table`, `lineChart`, `areaChart`, `barChart`, and `sparkline`.
- State feedback: `progress`, `spinner`, `skeleton`, `loadingOverlay`, `emptyState`, `async`, and `errorBoundary`.
- Floating UI: `tooltip`, `popover`, `contextMenu`, `Dialog`, `Toast`, and `CommandPalette`.
- Drag and drop: `draggable(data, child)` and `dragTarget(type, child)` with acceptance and drop callbacks.

Stable keys are important for retained tables, grids, and lists. When a table signal emits a new item instance for the same logical key, OpenUI recreates that retained cell so updated display data is not stale.

## Reactive state

- `Signal<T>` is readable and writable.
- `ReadableSignal<T>` exposes reads/subscriptions without mutation.
- `Computed<T>` tracks signals read during evaluation.
- `Effect` reruns side effects and must be closed by its owner.
- `Signals.batch` groups writes.
- `SelectionModel<T>` manages single/multiple selection.
- `StateStore.remember` retains keyed state during rebuilding.
- `AsyncValue<T>` models loading, success, and failure.
- `Subscription` is closeable; use `Subscription.EMPTY` for a safe placeholder.

## Forms and formatting

`Form` groups `Field<T>` instances and validators. A field owns its value, validation state, and error result. `Validator` supplies reusable checks, while `ValueFormatter` converts values for display. Keep server validation authoritative even when client forms validate eagerly.

## Events

Events traverse ancestors in capture order, execute target listeners, then bubble upward. Register with `component.onCapture(type, listener)` or `component.on(type, listener)`.

- `PointerEvent`: mouse coordinates, button, drag delta, scroll delta, and pointer capture.
- `KeyboardEvent`: key, scan code, modifiers, and typed character.
- `UiEvent.preventDefault()`: suppresses the compatibility/default handler.
- `UiEvent.stopPropagation()`: stops traversal and, by OpenUI contract, suppresses the compatibility/default handler.
- `UiEvent.capturePointer()`: routes later drag/release to the requesting target; controls should request it only for interactions they actually begin.

## Focus and native widgets

`FocusManager` tracks the focused component, traverses focusable components, traps focus for modal overlays, and synchronizes `NativeWidgetOwner` controls. A click focuses the nearest focusable ancestor, which allows non-focusable labels/icons inside a button or card.

`NativeWidgetHost` is implemented by the screen adapter. Consumer screens should not separately add OpenUI-owned widgets to Minecraft.

## Overlays

`OverlayManager.show` mounts a component into an `OverlayLayer` and returns an `OverlayHandle`. The handle closes and unmounts it. Layers include base UI, dropdown/popover, modal, toast, tooltip, and debug content. Blocking overlays participate in input targeting and may trap focus.

## Animation and navigation

`AnimationManager` advances time-based `Animation` objects with an `Easing` curve. `SlideTransition` and `ScaleTransition` are cross-version. `FadeTransition` is supported on 1.20.1 and 1.21.1 but explicitly throws on 26.1.2.

`Navigator` manages typed `Route<T>` values with push, replace, and pop operations. Route builders return component trees; application state stays outside the navigator.

## Custom component checklist

1. Extend `UIComponent` and implement `render` with the version's GUI rendering type.
2. Override preferred size or layout only when composition cannot express the geometry.
3. Render visible children with `renderChildren` when the component owns a subtree.
4. Use semantic colors from `theme()`.
5. Acquire subscriptions/resources in `onMount()` and release them in `onUnmount()`.
6. Call `invalidatePaint`, `invalidateLayout`, or structural invalidation at the narrowest correct level.
7. Return `true` from a legacy handler only when it actually handled the input.
8. Avoid mutating the tree during rendering.

## Compatibility warning

Do not compile a jar against one OpenUI Minecraft module and run it with another. Minecraft GUI types and method signatures differ between generations even where OpenUI's composition API looks the same.
