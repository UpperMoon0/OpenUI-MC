# OpenUI MC

OpenUI MC is a client-side UI framework used by Minecraft mod developers to build polished, reactive screens without rewriting focus, input routing, layout, animation, and widget lifecycle plumbing.

![OpenUI MC logo](https://raw.githubusercontent.com/UpperMoon0/OpenUI-MC/main/logo.png)

## For players

OpenUI MC is a library mod. It does not add blocks, items, recipes, or gameplay by itself. Install it when another mod lists OpenUI MC as a required dependency, and make sure the Minecraft version and loader match that mod.

## For mod developers

- Declarative rows, columns, stacks, padding, clipping, responsive layouts, grids, and virtual lists
- Reactive signals, computed values, effects, batching, remembered state, and async state
- Buttons, text fields, toggles, sliders, selects, tabs, tables, charts, forms, and validation
- Managed focus, keyboard navigation, pointer capture, drag and drop, and native Minecraft widgets
- Dialogs, popovers, menus, tooltips, toasts, command palettes, and layered overlays
- Themes, animations, navigation, error boundaries, and UI inspection tools
- Base classes for ordinary screens and menu/container screens

OpenUI MC keeps application code focused on state and business callbacks while its runtime mounts components, computes layout, renders, dispatches input, owns focus, and cleans up resources.

## Supported platforms

| Minecraft | Loader | Required Java |
|---|---|---:|
| 1.20.1 | Fabric | 17 |
| 1.20.1 | Forge | 17 |
| 1.21.1 | Fabric | 21 |
| 1.21.1 | NeoForge | 21 |
| 26.1.2 | NeoForge | 25 |

Download the file matching both your Minecraft version and loader. Builds are client-side and are not interchangeable between Minecraft generations.

## Developer documentation

- [Quick start and integration](https://github.com/UpperMoon0/OpenUI-MC/blob/main/docs/GETTING_STARTED.md)
- [Framework guide](https://github.com/UpperMoon0/OpenUI-MC/blob/main/docs/FRAMEWORK.md)
- [API reference](https://github.com/UpperMoon0/OpenUI-MC/blob/main/docs/API_REFERENCE.md)
- [Source code](https://github.com/UpperMoon0/OpenUI-MC)

## Important 26.1.2 note

Minecraft 26.1.2 uses its extracted GUI render-state API. OpenUI supports that API, but `FadeTransition` is unavailable because Minecraft no longer exposes a safe scoped opacity operation for arbitrary UI subtrees. Slide and scale transitions remain available.

## Support and license

Report bugs and compatibility problems on the [GitHub issue tracker](https://github.com/UpperMoon0/OpenUI-MC/issues). Include the Minecraft version, loader, OpenUI version, and the consuming mod.

OpenUI MC is authored by **NsTut** and distributed under the **MIT License**.
