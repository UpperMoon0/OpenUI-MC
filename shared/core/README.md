# Shared framework core

This source set contains OpenUI classes whose implementation is identical across every supported Minecraft target. Each common/version project compiles these sources alongside its version-specific adapters.

Keep Minecraft APIs that differ between versions in the version source trees. Move a class here only when it compiles unchanged for every target; the full `testAllVersions` and `buildAll` matrix is the compatibility gate.
