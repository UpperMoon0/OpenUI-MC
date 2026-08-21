# OpenUI MC

OpenUI MC is a composable, texture-free UI framework for Minecraft mod developers. It supports Fabric and Forge on Minecraft 1.20.1, plus Fabric and NeoForge on Minecraft 1.21.1.

## Components

- Layout: `VStack`, `HStack`, `Padding`, `SizedBox`, `Spacer`, and `Panel`
- Content: `TextWidget`, `Divider`, and `ButtonWidget`
- Input: `EditBoxWrapper`
- Scrolling: `ScrollList` and `ScrollGrid`
- Foundation: `UIComponent`, `UiRender`, `UiTheme`, and `UiAnimationUtil`

All public API types live under `com.nstut.openui.api`.

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

Every pull request builds and uploads all four jars as workflow artifacts. Pushes to `main` additionally create a GitHub release for the version in `gradle.properties`. CurseForge publishing is intentionally disabled until the project exists there.
