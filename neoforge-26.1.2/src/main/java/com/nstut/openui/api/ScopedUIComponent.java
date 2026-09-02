package com.nstut.openui.api;

import com.nstut.openui.state.UiScope;

/**
 * Additive lifecycle-scoped base class for components that want automatic
 * ownership of effects, subscriptions and closeable resources.
 *
 * <p>The existing {@link UIComponent} contract remains unchanged. New
 * framework components can extend this class while legacy components continue
 * to extend {@code UIComponent} directly.</p>
 */
public abstract class ScopedUIComponent extends UIComponent {
    private UiScope scope;

    @Override
    protected final void onMount() {
        scope = new UiScope();
        try {
            onScopedMount(scope);
        } catch (RuntimeException | Error failure) {
            UiScope failedScope = scope;
            scope = null;
            if (failedScope != null) failedScope.close();
            throw failure;
        }
    }

    @Override
    protected final void onUnmount() {
        UiScope mountedScope = scope;
        try {
            onScopedUnmount();
        } finally {
            scope = null;
            if (mountedScope != null) mountedScope.close();
        }
    }

    /** Called after a fresh scope is created for this mount. */
    protected void onScopedMount(UiScope scope) { }

    /** Called immediately before the current mount scope is disposed. */
    protected void onScopedUnmount() { }

    /** Returns the active mount scope. */
    protected final UiScope scope() {
        UiScope current = scope;
        if (current == null) throw new IllegalStateException("Component is not mounted");
        return current;
    }
}
