# OpenUI MC implementation plan

## Status — 2026-08-21 (Framework v0.9.0-RC)

Legend: ✅ implemented, 🟡 foundation/substantial implementation, ⬜ remaining.

Overall framework completion: **~78%** against full detailed roadmap.

### Milestones

- ✅ M1 Runtime (~95%): `UiRuntime`, `UiScreen`, `UiContainerScreen`, lifecycle, focus, centralized event dispatch, pointer capture, native widget ownership, overlay dismiss priority, and disposal.
- ✅ M2 Reactive (~90%): `Signal`, dependency-tracked `Computed`, `Effect`, batching, dirty build/layout/paint invalidation, declarative `Ui` factories, signal-bound components, keys, and remembered state.
- 🟡 M3 UI System (~85%): constraints, measurement, alignment/justification, stacks, positioning, true nested scissor clipping stack (`ClipStack`), responsive layout, runtime Theme 2.0 semantic palette, typography roles (`TextStyle`), rich Minecraft `Component` integration, forms, dialogs, cards, icons, selects, popovers, tooltips, toasts, context menus, tabs, tables, radios, badges, chips, spinners, and skeletons implemented. Pseudo-state styling and extended component variants continue.
- 🟡 M4 Polish (~75%): centralized delta-time animation runtime and easing, time-based control transitions (hover/press), responsive dynamic grids, keyed virtual lists, async state/components, progress, empty states, error boundaries, charts (`LineChart`, `BarChart`, `Sparkline`), and in-game dev tools (`UiInspector`, `UiGalleryScreen`) implemented. Generic page transitions, drag-and-drop, and deep profiling remain.
- 🟡 M5 Production (~70%): separate multi-loader/multi-version library (Fabric/Forge 1.20.1 & Fabric/NeoForge 1.21.1), automated unit/interaction/animation/overlay/leak/snapshot test matrix, graphics/platform abstractions (`UiCanvas`, `UiPlatform`), build artifacts, GitHub releases, and CurseForge publishing implemented. Full application screen migrations and final API freeze continue.

### Critical path checklist

- [x] 1. Introduce `UiRuntime` and runtime-owned root.
- [x] 2. Add `UiScreen` and `UiContainerScreen` integration.
- [x] 3. Centralize mouse and keyboard dispatch.
- [x] 4. Add `FocusManager` and pointer capture.
- [x] 5. Internalize native `EditBox` registration and synchronization.
- [x] 6. Introduce constraints-based measurement.
- [x] 7. Add alignment and justification.
- [x] 8. Add `Stack`, `Positioned`, and `ClipStack`.
- [x] 9. Introduce `Signal<T>`.
- [x] 10. Introduce `Computed<T>` dependency tracking, `Effect`, and batching.
- [x] 11. Add dirty build/layout/paint invalidation.
- [x] 12. Add declarative `Ui` DSL.
- [x] 13. Add layered `OverlayManager`.
- [x] 14. Complete Dialog/Tooltip/Popover/Select suite.
- [x] 15. Introduce runtime `Theme` and semantic `ColorScheme`.
- [x] 16. Complete semantic component theme catalogue.
- [x] 17. Add centralized animation runtime and easing.
- [x] 18. Add keyed virtual list and responsive dynamic grid.
- [x] 19. Add UI inspector, layout tree, and performance debug overlay.
- [x] 20. Decouple framework architecture from specific screen implementations.
- [x] 21. Provide container screen abstraction (`UiContainerScreen`).
- [x] 22. Complete the interaction, snapshot, overlay, animation, and leak test matrix.
- [x] 23. Complete documentation examples and in-game component gallery (`UiGalleryScreen`).
- [x] 24. Extract framework into the standalone OpenUI MC library with Fabric/Forge/NeoForge version modules.

### Additional completed foundations

- [x] Forms and typed validation (`Form`, `Field`, `Validator`).
- [x] Async state and declarative async content (`AsyncValue`, `AsyncComponent`).
- [x] Single/multiple selection model.
- [x] Stack navigation (`Navigator`, typed `Route`).
- [x] State retention (`StateStore.remember`).
- [x] Signal-bound text field, text, checkbox, switch, slider, progress, list, grid, and switcher controls.
- [x] Complete data visualization catalogue (`LineChart`, `BarChart`, `Sparkline`).
- [x] Failure isolation (`ErrorBoundary`) and zero-state handling (`EmptyState`, `LoadingOverlay`).
- [x] True nested scissor stack (`ClipStack`).
- [x] Graphics & Platform utilities (`UiCanvas`, `UiPlatform`).
- [x] Four production targets: Fabric/Forge 1.20.1 and Fabric/NeoForge 1.21.1.
- [x] CI test/build matrix, GitHub release artifacts, and CurseForge project `1661978` publishing.

The detailed phases below remain the authoritative design specification. Items marked partial above should not be interpreted as fully complete merely because their architectural foundation exists.

Yes. If the target is “a genuinely polished, reusable React/Compose-quality UI framework for Minecraft”, I would stop treating it as incremental cleanup and give it a proper framework roadmap.
The end state should be:
Application
   │
   ▼
Declarative UI API
   │
   ├── State / Signals
   ├── Components
   ├── Theme
   └── Navigation / Overlays
            │
            ▼
        UiRuntime
   ┌────────┼──────────┐
   ▼        ▼          ▼
 Layout   Events    Animation
   │        │          │
   └────────┼──────────┘
            ▼
       Render Tree
            │
      Minecraft GUI
I'd define 100% as: a developer can create a complex screen without manually calculating coordinates, registering vanilla widgets, synchronizing state, handling focus, drawing standard controls, implementing scrolling, or manually propagating events.
Phase 0 — Freeze the architecture
Before adding more components, establish these layers:
ui/
├── runtime/
├── component/
├── layout/
├── state/
├── input/
├── animation/
├── theme/
├── overlay/
├── controls/
├── graphics/
└── minecraft/
Keep business-specific Economy code completely outside the framework.
The dependency direction should be:
controls
   ↓
component
   ↓
layout / state / input / animation
   ↓
runtime
   ↓
graphics + minecraft adapter
No MarketScreen concepts inside the framework.
Phase 1 — UiRuntime + UiScreen
This should be the first major change.
Currently MarketScreen still controls too much framework plumbing.
Introduce:
public abstract class UiScreen extends Screen {

    protected abstract UIComponent buildUI();

}
Or for container screens:
public abstract class UiContainerScreen<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {

    protected abstract UIComponent buildUI();

}
Then framework owns:
initialization
component mounting
layout
rendering
pre-render
mouse routing
keyboard routing
native widget registration
resizing
focus
disposal
Internally:
final class UiRuntime {

    UIComponent root;

    FocusManager focus;
    OverlayManager overlays;
    AnimationManager animations;
    NativeWidgetManager nativeWidgets;

}
Target
Application code becomes:
@Override
protected UIComponent buildUI() {
    return Ui.column(
        header(),
        content()
    );
}
Not:
root.layoutTree(...)
root.preRender(...)
root.render(...)

addRenderableWidget(...)
syncEditBoxes(...)
Done when
There is zero framework plumbing inside MarketScreen.
Phase 2 — Proper lifecycle
Give components an explicit lifecycle.
I'd use:
mount()
unmount()

build()
layout()
paint()
But avoid exposing all of that unless custom components need it.
Internally:
CREATED
   ↓
MOUNTED
   ↓
DIRTY_BUILD
   ↓
DIRTY_LAYOUT
   ↓
DIRTY_PAINT
Add invalidation:
invalidate();
invalidateLayout();
invalidatePaint();
Parent propagation should happen automatically where necessary.
For example:
text.setText("Hello");
should trigger:
text changed
↓
preferred size may change
↓
invalidate layout
↓
parent layout gets marked dirty
You should never need:
rebuildEverything();
unless the component structure itself changed.
Phase 3 — Reactive state system
This is the single biggest step toward React/Compose quality.
Implement:
Signal<T>
Computed<T>
Effect
For example:
Signal<String> query = Signals.of("");
Signal<SortMode> sort = Signals.of(SortMode.PRICE_ASC);
Binding:
Ui.text(() -> query.get())
or:
TextWidget title = Ui.text(query);
Derived state:
Computed<List<Product>> results =
    Signals.computed(() ->
        filterProducts(products.get(), query.get(), sort.get())
    );
UI:
Ui.grid(results, this::productCard);
Now:
query.set("iron");
causes dependent UI to refresh automatically.
Required API
interface Signal<T> {

    T get();

    void set(T value);

    Subscription subscribe(Consumer<T> listener);
}
Add:
computed(...)
effect(...)
batch(...)
Batch updates
Important:
Signals.batch(() -> {
    query.set("");
    sort.set(DEFAULT);
    filter.set(ALL);
});
should cause one update pass.
Not three.
Phase 4 — Dependency tracking
Don't require developers to manually subscribe everywhere.
Support:
Computed<String> title =
    computed(() -> "Items: " + products.get().size());
The framework records which signals are accessed while evaluating the computation.
Conceptually:
beginTracking()

products.get()
query.get()

endTracking()

dependencies =
[
    products,
    query
]
When either changes, invalidate the computation.
That's a huge ergonomics improvement.
Phase 5 — Declarative Java DSL
The framework should eventually make UI construction read more like JSX/Compose.
Target:
return Ui.column(
    Ui.heading("Market"),

    Ui.row(
        Ui.textField(search)
            .placeholder("Search products")
            .flex(),

        Ui.button("Search")
            .primary()
    ).gap(8),

    Ui.grid(filteredProducts, this::productCard)
        .columns(3)
        .gap(8)
        .flex()
)
.padding(12)
.gap(10);
Instead of:
VStack v = new VStack();
HStack h = new HStack();
...
v.addChild(...);
h.addChild(...);
Both APIs can remain available internally.
Useful factories
Ui.row(...)
Ui.column(...)
Ui.stack(...)
Ui.box(...)
Ui.padding(...)
Ui.scroll(...)
Ui.grid(...)
Ui.text(...)
Ui.button(...)
Ui.textField(...)
Ui.image(...)
Ui.spacer()
Ui.divider()
Phase 6 — Component props
Instead of mutable configuration scattered everywhere, standardize component properties.
Something approximately like:
Button.create("Buy")
    .variant(PRIMARY)
    .size(MEDIUM)
    .disabled(() -> balance.get() < price.get())
    .onClick(this::buy);
Or:
Ui.button("Buy", this::buy)
    .primary();
Components should expose semantic intent, not raw render values.
Bad public API:
new ButtonWidget(
    0xFF202027,
    0xFF292931,
    0xFFF4F1EE
);
Good:
Ui.button("Buy").primary();
Phase 7 — Real layout engine
Your current stack/flex system is a good base.
Now complete it.
HStack / VStack
Support:
alignItems()
justifyContent()
gap()
Values:
START
CENTER
END
STRETCH
and:
START
CENTER
END
SPACE_BETWEEN
SPACE_AROUND
SPACE_EVENLY
Example:
Ui.row(...)
    .align(CENTER)
    .justify(SPACE_BETWEEN);
Constraints
Add:
width()
height()

minWidth()
maxWidth()
minHeight()
maxHeight()
Also:
fillWidth()
fillHeight()
You want a proper constraint object internally:
record Constraints(
    int minWidth,
    int maxWidth,
    int minHeight,
    int maxHeight
) {}
This is more robust than passing:
availableWidth
availableHeight
forever.
Phase 8 — Replace preferredWidth/preferredHeight model
Eventually replace:
preferredWidth()
preferredHeight()
with:
Size measure(Constraints constraints);
Then:
parent gives constraints
↓
child measures itself
↓
child returns desired size
↓
parent positions child
This gives you something closer to Flutter/Compose.
Example:
Row
 ├── Icon 16px
 ├── Text flexible
 └── Button 60px
This becomes dramatically easier to reason about.
Phase 9 — Padding, margin and box model
Support:
padding(8)
padding(4, 8)

margin(8)

border(...)
background(...)
radius(...)
Use:
Insets
rather than four integer fields everywhere:
Insets.all(8)
Insets.symmetric(4, 8)
Insets.only(...)
Phase 10 — Stack / absolute overlays
Add:
Stack
Positioned
Example:
Ui.stack(
    background,

    Ui.positioned(
        notificationBadge
    ).top(4).right(4)
)
Needed for:
badges
floating buttons
graph labels
overlays
icon decorators
custom HUD elements
Phase 11 — Clipping and overflow
Every component should optionally support:
clip()
overflowVisible()
Implement nested clipping correctly.
Create:
ClipStack
because Minecraft's scissor API is global-ish and nested scrollable regions can otherwise break each other.
Conceptually:
push clip
 render children
pop clip
Never:
enableScissor()
...
disableScissor()
without stack semantics.
Phase 12 — Responsive layout
Eventually support:
LayoutBuilder
Example:
Ui.responsive(ctx -> {
    if (ctx.width() < 280) {
        return mobileLayout();
    }

    return desktopLayout();
});
Also useful:
Breakpoint.SM
Breakpoint.MD
Breakpoint.LG
Minecraft UI scale changes make this surprisingly valuable.
Phase 13 — Event system
Create a common event abstraction.
UiEvent
MouseEvent
KeyboardEvent
FocusEvent
ScrollEvent
Components get:
onMouseDown
onMouseUp
onClick
onDoubleClick

onHoverEnter
onHoverLeave

onScroll

onKeyDown
onKeyUp
onCharTyped
Phase 14 — Event propagation
Support DOM-style propagation:
capture
↓
target
↓
bubble
Example:
Panel
 └── Button
click button:
Panel capture
Button target
Panel bubble
Allow:
event.stopPropagation();
event.preventDefault();
This makes complicated controls dramatically easier.
Phase 15 — Pointer capture
Needed for:
scrollbar drag
slider
window dragging
resize handles
drag-and-drop
When a component begins a drag:
event.capturePointer();
Then it keeps receiving mouse events even when cursor leaves its bounds.
This removes tons of edge cases.
Phase 16 — FocusManager
Create:
FocusManager
tracking:
focused
focusable components
focus order
API:
requestFocus()
clearFocus()
focusNext()
focusPrevious()
Support:
Tab
Shift+Tab
Enter
Space
Escape
arrow navigation
Phase 17 — Keyboard navigation
Standard controls should behave properly automatically.
Buttons:
Tab → focus
Enter/Space → click
Tabs:
Left/right → neighboring tab
Lists:
Up/down → selection
Enter → activate
Dialogs:
Escape → close
Tab → stays inside modal
This is one of those things users don't consciously notice until it's missing.
Phase 18 — Native widget abstraction
Completely hide:
EditBox
from application code.
Current:
searchField.getEditBox()
addRenderableWidget(...)
Target:
Ui.textField(searchState)
The framework should internally manage:
create EditBox
register with Screen
sync bounds
sync visibility
sync focus
sync value
dispose
Create something like:
NativeWidgetAdapter<T extends AbstractWidget>
Then other vanilla widgets can be wrapped cleanly too.
Phase 19 — TextField 2.0
Build a real framework-level text input.
Features:
placeholder
value binding
selection
cursor
max length
validator
disabled
read only
numeric mode
password mode if ever needed
prefix/suffix icon
clear button
error message
submit action
Example:
Ui.textField(price)
    .label("Price")
    .numeric()
    .validator(v -> ...)
    .prefix(coinIcon);
Phase 20 — Input validation/forms
Introduce:
Form
FormField<T>
Validator<T>
Example:
Form order = Form.create();

Field<Integer> quantity =
    order.field(1)
         .validate(v -> v > 0, "Must be greater than zero");

Field<BigDecimal> price =
    order.field(BigDecimal.ZERO)
         .validate(v -> v.signum() > 0, "Invalid price");
Then:
button.enabled(order::isValid);
Huge improvement for order creation/editing.
Phase 21 — Overlay system
Create a root-level:
OverlayManager
Layers:
Base
Dropdown
Popover
Modal
Toast
Tooltip
Debug
API:
Overlay.show(...)
Overlay.close(...)
or better:
context.overlays().show(...)
Phase 22 — Modal/Dialog
Reusable:
Ui.dialog()
Example:
Dialogs.confirm(
    "Cancel order?",
    this::cancelOrder
);
Framework handles:
backdrop
focus trap
Escape
close animation
input blocking
centering
z-order
No more hand-positioned modal math in MarketScreen.
Phase 23 — Dropdown / Select
Build real:
Select<T>
Example:
Ui.select(sortMode)
    .option("Price ↑", PRICE_ASC)
    .option("Price ↓", PRICE_DESC)
    .option("Activity", ACTIVITY);
No more cycling buttons if a select is more appropriate.
Phase 24 — Popover
Useful for:
filters
quick settings
context panels
date/time controls
item actions
Handle:
preferred side
viewport collision
anchor tracking
outside click
Escape
Phase 25 — Tooltips
Framework-level tooltip system:
component.tooltip("Available balance")
and rich:
component.tooltip(
    Ui.column(...)
)
Features:
delay
viewport clamping
preferred direction
rich content
dynamic content
Phase 26 — Toast notifications
Example:
toast.success("Order created");
toast.error("Insufficient funds");
toast.warning(...);
Features:
stacking
timeout
entry/exit animation
manual dismiss
action button
Phase 27 — Context menus
Example:
Ui.contextMenu(
    item("Edit", ...),
    item("Cancel", ...),
    separator(),
    item("Delete", ...)
);
Useful for advanced/power-user UI.
Phase 28 — Theme architecture 2.0
Current UiTheme constants should evolve into an object:
Theme
Example:
Theme.dark()
Theme.light()
with:
ColorScheme
Typography
Spacing
Radii
Shadows
AnimationDurations
ComponentThemes
Phase 29 — Semantic color system
Instead of components selecting raw colors:
theme.colors().primary()
theme.colors().surface()
theme.colors().surfaceVariant()
theme.colors().danger()
theme.colors().warning()
theme.colors().success()
theme.colors().onPrimary()
theme.colors().onSurface()
This allows runtime theme changes.
Phase 30 — Component themes
For example:
ButtonTheme
TextFieldTheme
CardTheme
ScrollbarTheme
TooltipTheme
DialogTheme
Then mods can customize:
Theme.builder()
    .buttonTheme(...)
    .build();
without rewriting widgets.
Phase 31 — Theme inheritance/context
Components should be able to override part of the theme:
Ui.theme(localTheme,
    content
)
Like React Context / Compose Local.
Useful for:
danger areas
sidebar
special screens
embedded panels
Phase 32 — Typography system
Implement:
Typography
Roles:
DISPLAY
TITLE
HEADING
BODY
LABEL
CAPTION
MONO
Minecraft's font is limited, but you can control:
scale
color
shadow
line spacing
letter spacing approximation
wrapping
alignment
Example:
Ui.text("Market")
    .style(TextStyle.TITLE);
Phase 33 — Text wrapping
This is essential.
Support:
wrap()
maxLines()
ellipsis()
Example:
Ui.text(description)
    .maxLines(2)
    .ellipsis();
No manually clipping strings everywhere.
Phase 34 — Icons
Create an abstraction:
Icon
Sources:
Minecraft item
texture
sprite
custom vector-like primitive
glyph
fluid
Example:
Ui.icon(ItemStack)
Ui.icon(ResourceLocation)
Ui.icon(Icons.SEARCH)
Phase 35 — Standard component library
At minimum:
Text
Icon
Button
IconButton
TextField
NumberField
Checkbox
Radio
Switch
Slider
ProgressBar
Spinner
Badge
Chip
Divider
Card
Panel
Tabs
Select
List
Grid
Tree
Table
Tooltip
Popover
Dialog
Toast
ContextMenu
Scrollbar
Image
ItemIcon
FluidIcon
That gets you into actual framework territory.
Phase 36 — Buttons
Variants:
PRIMARY
SECONDARY
GHOST
DANGER
SUCCESS
Sizes:
SMALL
MEDIUM
LARGE
States:
normal
hover
pressed
focus
disabled
loading
Support:
icon()
iconPosition()
Phase 37 — Cards
Reusable:
Card
Features:
hoverable
clickable
selected
elevated
outlined
This should replace Economy's repeated hand-rendered product cards.
Phase 38 — Tabs
Proper state-bound tabs:
Ui.tabs(selectedTab,
    tab(BROWSE, "Browse"),
    tab(ORDERS, "Orders"),
    tab(PORTFOLIO, "Portfolio")
)
Automatically:
selected styling
keyboard navigation
animated indicator
focus
Phase 39 — Lists
Current ScrollList is good.
Refactor into:
VirtualList<T>
API:
Ui.list(items)
    .key(Product::id)
    .render(this::productRow);
Keep only visible elements alive/rendered when appropriate.
Phase 40 — Virtualization
For very large datasets:
10 items
100 items
10,000 items
performance should stay stable.
Maintain:
visible range
overscan
reused cell components
For example:
viewport 10 rows
overscan 2
Only ~14 rows active.
Phase 41 — Dynamic grids
Current fixed-column grid should become responsive:
Ui.grid(items)
    .minCellWidth(90)
Framework determines:
available width 300
cell min 90
→ 3 columns
Like CSS:
grid-template-columns:
repeat(auto-fit, minmax(90px, 1fr));
This would be excellent for Minecraft screen scaling.
Phase 42 — Table component
Economy especially needs this.
Features:
column definitions
sortable headers
resizable columns
alignment
virtualized rows
selection
hover
sticky header
Example:
Ui.table(orders)
    .column("Item", ...)
    .column("Price", ...)
    .column("Qty", ...)
    .column("Status", ...);
Phase 43 — Selection model
Generic:
SelectionModel<T>
Supports:
none
single
multiple
For:
tables
lists
grids
trees
Phase 44 — Drag and drop
Eventually:
dragSource()
dropTarget()
Needed for:
inventory-like interfaces
reordering
dashboard customization
item assignment
Maybe not first priority, but part of a “100%” framework.
Phase 45 — Animation runtime
Replace per-component code like:
progress += (target - progress) * 0.32F;
with centralized time-based animation.
Implement:
Animation<T>
Tween<T>
Spring<T>
Support:
animateFloat()
animateColor()
animatePosition()
Phase 46 — Easing
Provide:
LINEAR
EASE_IN
EASE_OUT
EASE_IN_OUT
CUBIC_OUT
BACK_OUT
Potentially springs:
spring(stiffness, damping)
Phase 47 — Transitions
Allow:
component.animate()
    .opacity()
    .position()
    .size();
When a state changes:
inactive → active
visuals interpolate automatically.
Phase 48 — Enter/exit transitions
For:
dialogs
toasts
dropdowns
pages
lists
Example:
Ui.animatedVisibility(showDialog,
    dialog
);
Phase 49 — Animated layout
When an element changes size or position:
old layout
↓
new layout
↓
interpolate
This is advanced but produces very polished interfaces.
Phase 50 — Navigation/router
For larger applications, introduce:
Navigator
Route
Instead of:
int viewMode = 4;
Use:
Route<Browse>
Route<ProductDetail>
Route<Orders>
Route<Portfolio>
Example:
navigator.push(new ProductDetail(productId));
navigator.pop();
Phase 51 — Nested navigation
Sidebar:
Browse
Orders
Portfolio
Containers
and within Orders:
Active
History
Can be modeled cleanly instead of viewMode integer spaghetti.
Phase 52 — State persistence
Provide optional:
remember()
rememberSaveable()
Conceptually:
Signal<String> search =
    state.remember("browse-search", "");
Then screen rebuild doesn't lose it.
Persistent config:
rememberPersisted(...)
could write client config.
Phase 53 — Component-local state
If you want Compose-like ergonomics:
Signal<Boolean> expanded =
    context.state(false);
But in Java this requires careful lifecycle handling.
I'd first implement explicit state objects before hook-like APIs.
Phase 54 — Context / dependency injection
Implement something equivalent to React Context:
UiContext
For:
Theme
Navigator
OverlayManager
Localization
Screen
Minecraft instance
user preferences
Custom components can access:
context.theme()
context.navigator()
context.overlays()
without global statics.
Phase 55 — Async state
Minecraft networking makes this useful.
Create:
AsyncValue<T>
States:
LOADING
SUCCESS
ERROR
Example:
AsyncValue<List<Order>> orders;
UI:
Ui.async(
    orders,
    loading -> spinner(),
    data -> orderList(data),
    error -> errorPanel(error)
);
This cleans up packet-driven interfaces considerably.
Phase 56 — Loading states
Standard:
Spinner
Skeleton
ProgressBar
LoadingOverlay
Instead of blank screens while packets arrive.
Phase 57 — Empty states
Reusable:
Ui.emptyState()
    .title("No orders")
    .description("Create your first order.")
    .action(...);
Very important for polished apps.
Phase 58 — Error boundaries
A broken custom component shouldn't necessarily crash an entire screen.
Possibly:
ErrorBoundary
that catches render/build exceptions in development and renders:
Component failed
<stack trace>
In production, log + safe fallback.
Phase 59 — Performance invalidation
Track separately:
BUILD_DIRTY
LAYOUT_DIRTY
PAINT_DIRTY
Changing button hover:
paint only
Changing text length:
layout + paint
Changing component subtree:
build + layout + paint
This is important once the framework grows.
Phase 60 — Render batching
Reduce unnecessary Minecraft draw calls where practical.
Particularly:
rect fills
icons
text
textures
Don't overengineer yet, but profile.
Phase 61 — Cached measurements
Text measurements and component preferred sizes shouldn't be recalculated repeatedly unless dependencies changed.
Cache:
measure result
constraints
style
text
Invalidate intelligently.
Phase 62 — Stable identity/keys
For dynamic lists:
Ui.list(products)
    .key(Product::id)
When list changes:
A B C

→

A D C
you don't want to destroy A and C unnecessarily.
Keys preserve:
selection
animation
local state
focus
This is one place where limited reconciliation makes sense.
Not a full VDOM.
Phase 63 — Minimal reconciliation
For declaratively rebuilt subtrees, compare:
component type
key
Reuse existing retained component where possible.
Something like:
new tree description
        ↓
small reconciler
        ↓
retained components
You don't need React's DOM complexity.
Phase 64 — Debug tools
Very important for framework development.
Add debug mode:
F8 UI Inspector
Click a component and show:
type
bounds
constraints
parent
children
state
style
hover
focus
dirty flags
Render outlines:
red   = layout
green = paint
blue  = hitbox
This will save you enormous debugging time.
Phase 65 — Layout inspector
Something like:
MarketScreen
 └─ Row 356×248
     ├─ Sidebar 84×248
     └─ Padding 272×248
         └─ Column ...
Include selected component tree.
This would make the framework dramatically easier to maintain.
Phase 66 — Performance profiler
Debug counters:
components mounted: 142
components painted: 38
layout passes: 1
build passes: 2
draw calls: ...
frame UI time: 0.42ms
Especially useful for very complex GUIs.
Phase 67 — Tests
Expand far beyond the current basic flex test.
Test:
layout
constraints
weighted flex
visibility
hit testing
event propagation
focus
clipping
scrolling
grid indexing
state updates
computed state
component disposal
overlay positioning
animation
Phase 68 — Snapshot-style layout tests
Represent layout results:
root [0,0 300×200]
  sidebar [0,0 80×200]
  content [80,0 220×200]
and assert expected tree geometry.
No GPU required.
Phase 69 — Interaction tests
Simulate:
click
drag
scroll
Tab
Enter
typing
Example:
click(button);
assertEquals(true, modal.isOpen());
Phase 70 — Memory/leak tests
Important because signals can retain subscribers.
After a screen closes:
all subscriptions removed
all native widgets unregistered
all animations cancelled
all overlays disposed
Do not allow screen objects to remain referenced.
Phase 71 — Public API cleanup
Decide what users are allowed to touch.
Public:
Ui
Component
Signal
Theme
Controls
UiScreen
Internal:
layout engine internals
render node internals
subscription trackers
native widget registry
reconciler implementation
Use:
internal package
even if Java cannot enforce perfectly.
Phase 72 — Separate framework from Economy
Eventually extract:
common-ui
into a dedicated module.
Potentially:
economy-ui-core
first, then genericize.
Ideal:
ui-core
ui-forge-1.20.1
economy
Then Economy consumes the framework like any other mod.
Phase 73 — Loader abstraction
If you want this to become genuinely reusable:
common UI code
        │
 ┌──────┴──────┐
 ▼             ▼
Forge       NeoForge
Do not let Forge-specific stuff leak into component/layout/state packages.
Phase 74 — Version independence
Minecraft GUI APIs change.
Create a small platform abstraction:
UiPlatform
for:
draw text
draw texture
draw item
scissor
native widget registration
keyboard clipboard
sound
Then supporting another Minecraft version doesn't require rewriting the framework.
Phase 75 — Documentation
Framework quality heavily depends on documentation.
You need:
Getting Started
Layout
State
Signals
Components
Themes
Events
Focus
Overlays
Animation
Custom Components
Performance
Minecraft Integration
Migration Guide
Phase 76 — Examples
Create screens demonstrating:
login/settings style form
market dashboard
inventory-style grid
large virtual list
modal dialog
dropdowns
tabs
responsive layout
theming
animations
Example code should be tiny and attractive.
Phase 77 — Component gallery / Storybook equivalent
Make an in-game dev screen:
UI Gallery
├─ Buttons
├─ Inputs
├─ Cards
├─ Tabs
├─ Lists
├─ Modals
├─ Tooltips
├─ Colors
└─ Typography
Exactly like Storybook.
This should show:
normal
hover
pressed
focus
disabled
error
loading
for each component.
This would massively improve visual consistency.
Phase 78 — Accessibility-adjacent support
Minecraft isn't a browser, but still implement:
keyboard navigation
focus visibility
high contrast theme
scalable UI
semantic labels
screen-reader-compatible Component text where possible
reduced-motion option
At minimum:
AccessibilitySettings.reducedMotion()
should turn long animations off.
Phase 79 — Localization
Never make framework components dependent on raw String.
Support:
Component
Supplier<Component>
where useful.
So:
Ui.button(Component.translatable("economy.buy"));
is first-class.
Phase 80 — Styling API
Eventually introduce:
Style
Example:
Style.builder()
    .background(theme.surface())
    .border(...)
    .radius(6)
    .padding(8)
    .build();
But don't recreate CSS unnecessarily.
Use it primarily for custom components.
Phase 81 — Hover/focus/pressed pseudo-state styles
Allow:
Style normal
Style hover
Style pressed
Style focused
Style disabled
Potential API:
component.style(
    styles -> styles
        .base(...)
        .hover(...)
        .focused(...)
);
Standard controls should still use semantic themes.
Phase 82 — Better graphics abstraction
UiRender is currently utility methods.
Eventually wrap rendering:
UiCanvas
Example:
canvas.roundRect(...)
canvas.text(...)
canvas.item(...)
canvas.line(...)
canvas.clip(...)
Advantages:
testability
render state safety
clip stack
transform stack
future platform adaptation
Phase 83 — Transform stack
Support:
translate
scale
rotate
opacity
Useful for animation.
Keep transformations scoped.
Phase 84 — Z ordering
Explicit:
zIndex
or preferably structural stacking through Stack.
Overlay manager should handle high-level z order.
Phase 85 — Charts
Since Economy already uses charts, turn those into reusable framework components:
LineChart
AreaChart
BarChart
Sparkline
Features:
axis
tooltip
pan
zoom maybe
live mode
selection
formatters
Economy should no longer draw charts manually.
Phase 86 — Data formatting hooks
Not framework business logic, but generic:
ValueFormatter<T>
Useful for:
numbers
percentages
dates
money
quantities
Don't bake Economy's coin formatting into core UI.
Phase 87 — Command palette
Optional power-user feature:
Ctrl+K
Framework component:
CommandPalette
Can search:
actions
pages
items
settings
Not necessary for 1.0 but very nice.
Phase 88 — Animation polish pass
Once architecture is stable:
Standard timings approximately:
hover        100–140 ms
press         70–100 ms
tooltip      100–150 ms
dropdown     120–180 ms
modal        160–220 ms
page         180–250 ms
toast        180–220 ms
Don't animate everything.
Animation should communicate:
state
hierarchy
continuity
not just movement for its own sake.
Phase 89 — Visual polish pass
Audit all components for:
spacing consistency
radius consistency
border contrast
hover contrast
disabled contrast
text hierarchy
alignment
icon size
touch/click target
empty states
loading states
error states
This is where the last 10–15% of perceived quality comes from.
Phase 90 — Migrate Economy completely
This is the proof test.
MarketScreen should eventually contain mostly:
state
network integration
business callbacks
screen composition
No:
x/y positioning
manual scissor
manual modal rendering
manual scrollbar
manual button drawing
manual input registration
manual focus logic
manual tooltip positioning
manual animation math
If those still exist, framework work remains.
What the final MarketScreen should roughly look like
Something like:
public final class MarketScreen extends UiContainerScreen<MarketMenu> {

    private final Signal<Page> page =
            signal(Page.BROWSE);

    private final Signal<String> search =
            signal("");

    private final Signal<List<Product>> products =
            signal(List.of());

    private final Computed<List<Product>> visibleProducts =
            computed(() ->
                    filterProducts(
                            products.get(),
                            search.get()
                    )
            );

    @Override
    protected UIComponent buildUI() {
        return Ui.row(
            sidebar(),
            content()
        );
    }

    private UIComponent sidebar() {
        return Ui.sidebar(
            Ui.heading("ECONOMY"),
            Ui.navItem("Browse", Page.BROWSE, page),
            Ui.navItem("Orders", Page.ORDERS, page),
            Ui.navItem("Portfolio", Page.PORTFOLIO, page),
            Ui.navItem("Containers", Page.CONTAINERS, page)
        );
    }

    private UIComponent content() {
        return Ui.switcher(page,
            Page.BROWSE, this::browsePage,
            Page.ORDERS, this::ordersPage,
            Page.PORTFOLIO, this::portfolioPage,
            Page.CONTAINERS, this::containersPage
        );
    }

    private UIComponent browsePage() {
        return Ui.column(
            Ui.row(
                Ui.heading("Market"),
                Ui.spacer(),
                Ui.textField(search)
                    .placeholder("Search")
                    .width(120)
            ),

            Ui.grid(visibleProducts, this::productCard)
                .minCellWidth(105)
                .gap(6)
                .flex()
        ).gap(8);
    }
}
That's when I would genuinely call it React/Compose-like.
Recommended implementation order
Do not implement the 90 sections literally one after another. They fall into five real milestones.
Milestone
Major work
Framework maturity
M1 Runtime
UiRoot, UiScreen, lifecycle, focus/event runtime, native widget ownership
~65%
M2 Reactive
Signal, Computed, invalidation, declarative DSL, keyed components
~75%
M3 UI System
full constraints/layout, overlays, themes, typography, controls
~85%
M4 Polish
animation, responsive UI, virtualization, async states, charts, inspector
~93%
M5 Production
migration, tests, docs, platform abstraction, gallery, performance
~100%
The critical path
If I were implementing this repo, my next commits would be approximately:
1. refactor: introduce UiRuntime and UiRoot
2. refactor: add UiScreen integration
3. refactor: centralize input dispatch
4. feat: add FocusManager and pointer capture
5. refactor: internalize native EditBox registration
6. feat: introduce constraints-based measurement
7. feat: add alignment and justification
8. feat: add Stack and ClipStack
9. feat: introduce Signal<T>
10. feat: introduce Computed<T> dependency tracking
11. feat: add dirty build/layout/paint invalidation
12. feat: add declarative Ui DSL
13. feat: add OverlayManager
14. feat: add Dialog/Tooltip/Popover/Select
15. feat: introduce runtime Theme
16. feat: semantic Button/Text/Card/Input themes
17. feat: centralized animation runtime
18. feat: virtualized generic List/Grid
19. feat: UI inspector/debug overlay
20. refactor: migrate MarketScreen completely
21. refactor: migrate VaultScreen and TankScreen
22. test: full framework interaction/layout suite
23. docs: framework guide and component gallery
24. refactor: extract framework module
That is the road I would actually follow.
The most important thing is do not start by adding 30 more widgets. Your current widgets are good enough to continue. First fix the runtime, state model, layout constraints, focus system, native integration, and overlays. Once those foundations are right, adding polished components becomes straightforward rather than piling more features on top of the current manual screen architecture.
