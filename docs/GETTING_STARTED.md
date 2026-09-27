# Getting started with OpenUI MC

This guide takes a consuming mod from dependency setup to a working reactive screen. Match the OpenUI module to the exact Minecraft loader and version used by the mod.

## 1. Add OpenUI MC

Released OpenUI artifacts are published to GitHub Packages. Add the repository in the consuming build (GitHub Packages requires credentials with package-read access):

```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/UpperMoon0/OpenUI-MC")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull
            password = providers.gradleProperty("gpr.key").orNull
        }
    }
}
```

Then depend on the artifact matching the exact loader/Minecraft target. For example:

```groovy
dependencies {
    modImplementation "com.nstut:openui-mc-fabric-1.21.1:<version>"
}
```

Available artifact ids are `openui-mc-fabric-1.20.1`, `openui-mc-forge-1.20.1`, `openui-mc-fabric-1.21.1`, `openui-mc-neoforge-1.21.1`, and `openui-mc-neoforge-26.1.2`. Use the loader's normal dependency configuration (`fg.deobf(...)` for Forge 1.20.1, `modImplementation` for Fabric, and the normal NeoForge mod dependency form).

For OpenUI development, a Gradle composite build is still useful: keep `OpenUI-MC/` beside the consuming project and substitute the matching loader module in `settings.gradle`.

Declare `openui_mc` as a required client-side runtime dependency in the consuming mod metadata. End users must install the matching OpenUI jar unless the consuming mod legally and technically bundles it.

## 2. Create a screen

Use `UiScreen` for an ordinary screen:

```java
public final class ProfileScreen extends UiScreen {
    private final Signal<String> name = Signals.of("");
    private final Signal<Boolean> notifications = Signals.of(true);

    public ProfileScreen() {
        super(Component.literal("Profile"));
    }

    @Override
    protected UIComponent buildUI() {
        return Ui.padding(16,
            Ui.column(
                Ui.title("Profile"),
                Ui.textField(name).placeholder("Display name"),
                Ui.checkbox("Notifications", notifications),
                Ui.row(
                    Ui.button("Cancel", this::onClose),
                    Ui.button("Save", this::save).primary()
                ).gap(6)
            ).gap(8)
        );
    }

    private void save() {
        // Send a packet or update application state, then close.
        onClose();
    }
}
```

Open it with the normal client call:

```java
Minecraft.getInstance().setScreen(new ProfileScreen());
```

Use `UiContainerScreen<M>` for a menu-backed screen. Keep slot/menu authority and packets in the consuming mod; OpenUI owns the visual component tree and input routing.

## 3. Model state reactively

Use `Signal<T>` for mutable state and `Computed<T>` for derived state:

```java
Signal<String> query = Signals.of("");
Signal<List<Product>> products = Signals.of(List.of());
Computed<List<Product>> visible = Signals.computed(() -> products.get().stream()
    .filter(product -> product.name().toLowerCase().contains(query.get().toLowerCase()))
    .toList());
```

Batch related writes to avoid redundant invalidation:

```java
Signals.batch(() -> {
    query.set("");
    products.set(response.products());
});
```

Close manually created `Effect` and subscription objects during `onUnmount()`. Signal-bound OpenUI controls clean up their own subscriptions.

## 4. Layout without fixed screen coordinates

Compose `Ui.row`, `Ui.column`, `Ui.stack`, `Ui.padding`, and `Ui.responsive`. Apply `gap`, alignment, justification, width/height constraints, and flex behavior to the returned component. Use `DynamicGrid` for small responsive grids, `VirtualGrid` for large scrollable grids with row virtualization, and `VirtualList` for large fixed-height lists.

Every screen has a viewport. On `UiScreen`, override `uiLeft`, `uiTop`, `uiWidth`, or `uiHeight` when the UI should occupy a smaller region. `UiContainerScreen` instead derives its viewport directly from the menu bounds: `leftPos`, `topPos`, `imageWidth`, and `imageHeight`.

Rows, columns, and stacks do not implicitly clip or scroll. Bounded visual surfaces (`Card`, `Panel`, `StyledBox`) use one protected content rectangle for child layout, painting, and descendant input. It preserves padding/borders, stays inside rounded inner corners even at low padding, and auto-sized high-radius/pill surfaces converge on the same dimension-clamped geometry used by rendering instead of inflating from the raw radius. These surfaces still do not scroll. If overflow must remain reachable, use an explicit `Ui.scroll(body).flex()` inside the bounded surface; off-screen controls inside that ScrollView remain keyboard-reachable and are automatically revealed when focus moves to them. Floating controls should use OpenUI's overlay APIs rather than relying on child paint escaping an ancestor.

## 5. Input and focus

OpenUI dispatches capture, target, and bubble listeners. Pointer capture keeps drag and release events routed to the component that began a left-button interaction.

```java
component.on(EventType.MOUSE_DOWN, event -> {
    if (event instanceof PointerEvent pointer && pointer.button() == 0) {
        event.capturePointer();
    }
});
```

`preventDefault()` suppresses legacy/default handling. In the current OpenUI compatibility contract, `stopPropagation()` stops listener traversal and also suppresses the legacy/default-handler bridge. Focus automatically selects the nearest focusable ancestor of the hit component. Tab and Shift+Tab exclude controls fully hidden by bounded-surface overflow clips, but explicit ScrollView clipping does not remove off-screen controls from keyboard order; focusing one scrolls it into view. A control fully clipped by a non-scroll surface after layout loses keyboard eligibility. A real 0-width or 0-height layout is not considered pre-layout and is skipped by focus as well. Hover uses the same ancestor clip boundary, so invisible overflow does not receive hover state or hover events.

Do not register an OpenUI `TextField`'s `EditBox` yourself. The runtime owns its mounting, bounds, focus, and removal. Screen mouse-down fallback also suppresses direct dispatch to those OpenUI-owned native widgets, so clipped-away portions of a field cannot remain interactive through Minecraft's vanilla child list.

## 6. Overlays and dialogs

Access the current runtime from the screen with `uiRuntime()`. Its overlay manager supports dropdown, popover, modal, toast, tooltip, and debug layers. Use the supplied `Dialog`, `Popover`, `ContextMenu`, `Tooltip`, `Toast`, and `CommandPalette` controls instead of hand-forwarding input to temporary widgets.

## 7. Version-specific code

Most application composition is identical across supported versions. Custom render components differ at the Minecraft boundary:

| Version | Render argument | Resource identifier |
|---|---|---|
| 1.20.1 / 1.21.1 | `GuiGraphics` | `ResourceLocation` |
| 26.1.2 | `GuiGraphicsExtractor` | `Identifier` |

Minecraft 26.1.2 extracts render state rather than issuing the older immediate GUI calls. `FadeTransition` is intentionally unavailable there; use slide or scale transitions.

## 8. Verify the integration

- Open and close the screen repeatedly to catch leaked subscriptions/widgets.
- Verify keyboard-only focus and Escape behavior.
- Test clicks at UI-scale boundaries and scroll-wheel direction in game.
- Test drag feedback movement, rejecting drop targets, and release outside the source.
- Run the consuming mod on the exact published loader/version pair.

For framework internals and custom component rules, continue with the [Framework Guide](FRAMEWORK.md). For available factories and systems, see the [API Reference](API_REFERENCE.md).
