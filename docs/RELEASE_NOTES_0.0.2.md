# OpenUI MC 0.0.2

## Added

- VirtualGrid with row virtualization and stable-key support.
- Theme-aware UiRender helpers and expanded responsive-grid coverage.

## Fixed

- DynamicGrid multi-row measurement and clipping.
- VirtualList and VirtualGrid stale-cell, duplicate-key, and scroll-boundary behavior.
- Runtime and component-local theme relayout.
- Theme-aware divider, reactive text, Card, and Panel rendering.
- Card multi-child composition and mutable component invalidation.

## Changed

- `Ui.card(a, b, c)` stacks multiple children vertically.
- Virtual collections recreate a cell when the keyed item value changes.
