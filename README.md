# OpenUI MC

OpenUI MC is a composable, texture-free UI framework for Minecraft mod developers. It supports Fabric and Forge on Minecraft 1.20.1, plus Fabric and NeoForge on Minecraft 1.21.1.

## Framework

- Runtime-owned screens, lifecycle, invalidation, focus, input routing, pointer capture, overlays, animation, and native widgets
- Reactive `Signal`, dependency-tracked `Computed`, `Effect`, batching, remembered state, async state, and selection models
- Constraint-based rows, columns, stacks, positioning, clipping, responsive layout, dynamic grids, and virtual lists
- Declarative `Ui` factories and signal-bound text, fields, checkboxes, switches, sliders, progress, async content, and switchers
- Runtime themes, semantic button variants, forms/validation, dialogs, and stack navigation

Start with the [framework guide](docs/FRAMEWORK.md). Public entry points are `Ui`, `UiScreen` / `UiContainerScreen`, signals, themes, controls, forms, and navigation. Minecraft widget ownership stays inside the runtime.

## Development dependency

Until a Maven repository is configured, clone OpenUI MC next to the consuming project and select the matching loader module through a Gradle composite build:

```groovy
// settings.gradle
includeBuild('../OpenUI-MC') {
    dependencySubstitution {
        substitute module('com.nstut:openui-mc') using project(':forge-1.20.1')
    }
}

// build.gradle
implementation fg.deobf('com.nstut:openui-mc:0.2.0')
```

Available loader modules are `fabric-1.20.1`, `forge-1.20.1`, `fabric-1.21.1`, and `neoforge-1.21.1`. At runtime, include the matching OpenUI MC jar as a required client-side library mod.

## Building

```shell
./gradlew buildAll testAllVersions
```

Minecraft 1.20.1 targets Java 17; Minecraft 1.21.1 targets Java 21.

## Releases

Every pull request builds and uploads all four jars as workflow artifacts. Pushes to `main` create a GitHub release for the version in `gradle.properties` and publish the matching artifacts to CurseForge project `1661978` when `CURSEFORGE_TOKEN` is configured.
