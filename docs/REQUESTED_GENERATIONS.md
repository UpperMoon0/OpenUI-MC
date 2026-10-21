# Minecraft 1.12.2, 1.16.5 and 26.2

These native modules are independent builds so their incompatible Gradle/plugin generations do not alter the existing root build.

| Directory | Minecraft / loader pin | Build driver | Compiler / runtime |
|---|---|---|---|
| `forge-1.12.2` | Forge 14.23.5.2864, MCP stable 39 | Java 8, Gradle 5.6.4, ForgeGradle 3.0.197 | Java 17 + Jabel 0.4.2, `--release 8`; runtime Java 8 |
| `fabric-1.16.5` | Fabric Loader 0.19.5, official mappings | Java 25, Gradle 9.5.1, Loom 1.17.9 remap | Java 17 + Jabel 0.4.2, `--release 8`; runtime Java 8 |
| `forge-1.16.5` | Existing Forge 36.2.42 module | Java 21, Gradle 8.14, ForgeGradle 6.0.54 | Java 17 + Jabel 0.4.2, `--release 8`; runtime Java 8 |
| `fabric-26.2` | Fabric Loader 0.19.5 | Java 25, Gradle 9.5.1, Loom 1.17.9 no-remap | Java 25 |
| `neoforge-26.2` | NeoForge 26.2.0.93 | Java 25, Gradle 9.5.1, ModDevGradle 2.0.148 | Java 25 |

There is no official Fabric 1.12.2 lane. This port does not silently substitute Legacy Fabric. Forge also exists for 26.2, but the respective modern loader scope here is Fabric plus NeoForge.

## Build and use while the pull request is unmerged

Check out an exact commit from this pull request, not an unreleased package name assumed to exist on GitHub Packages. Install Java 8, 17, 21 and 25 as needed. For each target:

```sh
# Forge 1.12.2: run the old Gradle driver on Java 8 and the compiler on Java 17.
export JAVA_HOME=/path/to/jdk8
export JAVA17_HOME=/path/to/jdk17
./forge-1.12.2/gradlew -p forge-1.12.2 clean build publishToMavenLocal --no-daemon --max-workers=2

# Fabric 1.16.5 and both 26.2 modules: run Gradle on Java 25.
export JAVA_HOME=/path/to/jdk25
./fabric-1.16.5/gradlew -p fabric-1.16.5 clean build publishToMavenLocal --no-daemon --max-workers=2
./fabric-26.2/gradlew -p fabric-26.2 clean build publishToMavenLocal --no-daemon --max-workers=2
./neoforge-26.2/gradlew -p neoforge-26.2 clean build publishToMavenLocal --no-daemon --max-workers=2
```

Use `mavenLocal()` before remote package repositories in an explicit development build. The coordinate is `com.nstut:openui-mc-<loader>-<minecraft>:0.0.15`. Record the exact source commit in the consumer's dependency lock/documentation. Fabric 1.16.5 needs `modImplementation`; Forge needs the usual `fg.deobf(...)`. These commands install local artifacts only, not releases. The PR workflow does not publish packages or releases.

## Architecture and compatibility

- Shared reactive state, layout, focus, overlays, input, controls and lifecycle are retained, not replaced with a compatibility skeleton. Deterministic generators lower existing legacy code to Java 8 and synchronize deleted source files.
- Forge 1.12.2 uses native `GuiScreen`, `GuiContainer`, `GuiTextField`, LWJGL2 input and immediate-mode rendering. The UI key contract remains stable GLFW physical identities; typed characters and native scan codes are routed separately.
- Modern input preserves full Unicode code points through cancellable events and native EditBox insertion while retaining existing char-based API methods. Keyboard ownership blocks native inventory/drop/swap shortcuts behind focused text or blocking modals; ordinary Escape and OpenUI Tab behavior are retained. Native modern clipping transforms before intersection and preserves incoming clipping independently for each graphics extractor.
- Text-field resize preserves text, selection, focus, constraints and change callbacks. Container inventory-key handling respects text focus and modal ownership.
- Legacy clipping follows the same transform as drawn content, intersects nested and pre-existing framebuffer clipping, and restores clipping and matrix state on exceptional frames.
- Minecraft 26.2 reuses the extracted GUI render-state model through `GuiGraphicsExtractor`, without introducing raw OpenGL calls. NeoForge 26.1.2 and both 26.2 loaders compile one authoritative `shared/modern` Java implementation and test suite directly, with a build-wiring invariant that rejects cloned or shadowed classes. Native loader entrypoints and pack metadata remain separate.
- New loader entrypoints contain no client-class references. New Fabric metadata allows an inert server-side mod container, and Forge 1.12.2 is not marked client-only, so consumers may declare a required dependency on dedicated servers without forcing UI initialization.
- The existing Forge 1.16.5 implementation and its provenance/history are preserved. Its adapters and regressions are reused for Fabric's official class names rather than reopening or rewriting prior work.

API decisions were cross-checked against the source-reference snapshot `01087a3908d6e14c43de4ecc39cf1d4f5025a8bb`. No proprietary game source or binaries are included. That reference's parsing/provenance audit is not proof of game-runtime equivalence.

## Evidence and remaining acceptance

Local direct compilation used genuine mapped Minecraft classes and real loader libraries, not API stubs. Java 8 legacy tests exercise native text editing, focus, resize, key translation, clipping, records/state and isolated server-safe class discovery. Modern tests exercise the full shared framework. The compiler fixture checks source deletion/rename and removal of obsolete nested class files while preserving resources.

Native Gradle packaging, reobfuscation and production-jar metadata/bytecode checks run in the PR's five-lane native matrix, alongside the pre-existing targets and consumer checks. Inspect the exact PR commit's CI status; compilation/unit checks do not imply a real game launch.

A genuine Forge dedicated-server discovery boot, client UI smoke testing (keyboard/clipboard/drag/modal/container lifecycle), and consuming Screens/Speakers media behavior on each new target remain separate acceptance requirements. Do not label the port runtime-validated solely from passing unit tests or package builds.
