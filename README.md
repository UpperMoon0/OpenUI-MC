# OpenUI MC

OpenUI MC is a composable, texture-free UI framework for Minecraft mod developers. The initial Forge 1.20.1 API was extracted from the Economy mod so it can be reused by any mod.

## Components

- Layout: `VStack`, `HStack`, `Padding`, `SizedBox`, `Spacer`, and `Panel`
- Content: `TextWidget`, `Divider`, and `ButtonWidget`
- Input: `EditBoxWrapper`
- Scrolling: `ScrollList` and `ScrollGrid`
- Foundation: `UIComponent`, `UiRender`, `UiTheme`, and `UiAnimationUtil`

All public API types live under `com.nstut.openui.api`.

## Development dependency

Until a Maven repository is configured, clone OpenUI MC next to the consuming project and use a Gradle composite build:

```groovy
// settings.gradle
includeBuild('../OpenUI-MC')

// build.gradle
implementation fg.deobf('com.nstut:openui-mc:0.1.0')
```

At runtime, include the OpenUI MC jar as a required client-side library mod.

## Building

```shell
./gradlew build
```

Requires Java 17.

