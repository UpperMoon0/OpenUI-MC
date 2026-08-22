# Getting started with OpenUI MC

This guide takes a consuming mod from dependency setup to a working reactive screen. Match the OpenUI module to the exact Minecraft loader and version used by the mod.

## 1. Add OpenUI MC

Until a public Maven coordinate is published, use a Gradle composite build. Keep both repositories beside each other:

```text
projects/
  OpenUI-MC/
  Your-Mod/
```

In the consuming root `settings.gradle`:

```groovy
includeBuild('../OpenUI-MC') {
    dependencySubstitution {
        substitute module('com.nstut:openui-mc') using project(':fabric-1.21.1')
    }
}
```

Choose one of `fabric-1.20.1`, `forge-1.20.1`, `fabric-1.21.1`, `neoforge-1.21.1`, or `neoforge-26.1.2`. Add `com.nstut:openui-mc:0.0.4` to the consuming module with the loader's normal mod dependency configuration. The version participates in Gradle resolution even though the composite substitution selects the local project.

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

## 5. Input and focus

OpenUI dispatches capture, target, and bubble listeners. Pointer capture keeps drag and release events routed to the component that began a left-button interaction.

```java
component.on(EventType.MOUSE_DOWN, event -> {
    if (event instanceof PointerEvent pointer && pointer.button() == 0) {
        event.capturePointer();
    }
});
```

`preventDefault()` suppresses legacy/default handling. In the current OpenUI compatibility contract, `stopPropagation()` stops listener traversal and also suppresses the legacy/default-handler bridge. Focus automatically selects the nearest focusable ancestor of the hit component. Tab and Shift+Tab traverse focus.

Do not register an OpenUI `TextField`'s `EditBox` yourself. The runtime owns its mounting, bounds, focus, and removal.

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
