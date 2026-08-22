# OpenUI MC

OpenUI MC is a client-side, declarative, and reactive UI framework for Minecraft mods. It replaces repeated screen plumbing with composable components, signals, managed focus and input, overlays, animation, themes, and lifecycle-aware native widgets.

It is a developer library: players install it when another mod declares OpenUI MC as a dependency.

## Supported targets

| Minecraft | Loader | Java | Gradle module |
|---|---|---:|---|
| 1.20.1 | Fabric | 17 | `fabric-1.20.1` |
| 1.20.1 | Forge | 17 | `forge-1.20.1` |
| 1.21.1 | Fabric | 21 | `fabric-1.21.1` |
| 1.21.1 | NeoForge | 21 | `neoforge-1.21.1` |
| 26.1.2 | NeoForge | 25 | `neoforge-26.1.2` |

## What it provides

- Declarative rows, columns, stacks, padding, clipping, responsive DynamicGrid layouts, VirtualList, and VirtualGrid
- Reactive `Signal`, `Computed`, `Effect`, batching, remembered state, and async state
- Buttons, text fields, checkboxes, switches, sliders, selects, tabs, tables, charts, loading and empty states
- Capture/target/bubble events, focus traversal, pointer capture, drag and drop, and native widget ownership
- Dialogs, popovers, context menus, tooltips, toasts, command palettes, and layered overlays
- Runtime themes, navigation, forms and validation, animation, error boundaries, and a UI inspector
- `UiScreen` and `UiContainerScreen` adapters that own Minecraft lifecycle and input forwarding

## Quick start

Clone OpenUI MC next to the consuming project and substitute the matching loader module:

```groovy
// settings.gradle
includeBuild('../OpenUI-MC') {
    dependencySubstitution {
        substitute module('com.nstut:openui-mc') using project(':neoforge-1.21.1')
    }
}
```

Then add the dependency in the consuming loader module:

```groovy
dependencies {
    implementation 'com.nstut:openui-mc:0.0.2'
}
```

For Forge 1.20.1, wrap the coordinate with `fg.deobf(...)`. For Fabric, use `modImplementation`. Ship the matching OpenUI MC jar with the consuming mod or declare it as a required runtime dependency.

Create a screen by returning one component tree:

```java
public final class SettingsScreen extends UiScreen {
    private final Signal<Boolean> enabled = Signals.of(true);

    public SettingsScreen() {
        super(Component.literal("Settings"));
    }

    @Override
    protected UIComponent buildUI() {
        return Ui.padding(12,
            Ui.column(
                Ui.heading("Settings"),
                Ui.checkbox("Enabled", enabled),
                Ui.button("Done", this::onClose).primary()
            ).gap(8)
        );
    }
}
```

See [Getting Started](docs/GETTING_STARTED.md) for dependency and screen integration, [Framework Guide](docs/FRAMEWORK.md) for architecture and behavior, and [API Reference](docs/API_REFERENCE.md) for the component catalog.

## Version notes

- Minecraft 1.20.1 and 1.21.1 custom rendering receives `GuiGraphics`.
- Minecraft 26.1.2 custom rendering receives `GuiGraphicsExtractor`, uses `Identifier` instead of `ResourceLocation`, and follows Minecraft's extracted render-state model.
- `FadeTransition` is unavailable on 26.1.2 because that renderer has no scoped opacity operation for an arbitrary component subtree. Use `SlideTransition` or `ScaleTransition` there.
- `stopPropagation()` intentionally also suppresses OpenUI's legacy/default-handler bridge. `preventDefault()` suppresses default handling without stopping listeners.

## Building and testing

The Gradle 9.1.0 wrapper supports the Java 25 toolchain required by Minecraft 26.1.2.

```shell
./gradlew clean testAllVersions buildAll
```

Build one target with, for example:

```shell
./gradlew :neoforge-26.1.2:build
```

Output jars are written to each loader module's `build/libs/` directory. Pull requests build all five loader targets. Pushes to `main` create a GitHub release and publish each matching artifact to CurseForge project `1661978` when `CURSEFORGE_API_TOKEN` is configured.

## Links

- [CurseForge description](curseforge.md)
- [Issue tracker](https://github.com/UpperMoon0/OpenUI-MC/issues)
- [Source](https://github.com/UpperMoon0/OpenUI-MC)
- License: MIT
