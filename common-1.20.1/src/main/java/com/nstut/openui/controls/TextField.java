package com.nstut.openui.controls;

import com.nstut.openui.api.EditBoxWrapper;
import com.nstut.openui.api.UiTheme;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Subscription;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.function.Predicate;

public class TextField extends EditBoxWrapper {
    private final Signal<String> value;
    private Subscription subscription = Subscription.EMPTY;
    private Predicate<String> validator = ignored -> true;
    private boolean numeric;
    private java.util.function.Consumer<String> onCommit;
    private String lastSubmitted;

    public TextField(Signal<String> value) {
        this(value, Minecraft.getInstance() != null && Minecraft.getInstance().font != null
                ? Minecraft.getInstance().font : new Font(null, false));
    }

    public TextField(Signal<String> value, Font font) {
        super(256, 0, 0, font);
        this.value = Objects.requireNonNull(value);
        setValue(value.get());
        getEditBox().setResponder(text -> {
            if ((!numeric || text.isEmpty() || text.matches("-?[0-9]*(\\.[0-9]*)?")) && validator.test(text)) value.set(text);
        });
    }

    public TextField placeholder(String placeholder) { setPlaceholder(placeholder); return this; }
    public TextField maxLength(int maxLength) { getEditBox().setMaxLength(maxLength); return this; }
    public TextField numeric() { numeric = true; return this; }
    public TextField validator(Predicate<String> validator) { this.validator = Objects.requireNonNull(validator); return this; }
    public boolean isValid() { return validator.test(getValue()); }

    /** Submit on Enter or when edited text loses focus; validation remains consumer policy. */
    public TextField onCommit(java.util.function.Consumer<String> action) {
        onCommit=Objects.requireNonNull(action); lastSubmitted=getValue(); return this;
    }
    private void submit(boolean explicit) {
        String current=getValue();
        if(onCommit!=null && (explicit || !Objects.equals(current,lastSubmitted))) {
            lastSubmitted=current; onCommit.accept(current);
        }
    }
    @Override public void onFocusLost() { super.onFocusLost(); submit(false); }
    @Override public boolean keyPressed(int key,int scanCode,int modifiers) {
        if(onCommit!=null && (key==257 || key==335)) { submit(true); return true; }
        return super.keyPressed(key,scanCode,modifiers);
    }
    @Override protected void onMount() {
        if (!Objects.equals(getValue(), value.get())) setValue(value.get());
        subscription = value.subscribe(text -> {
            if (!Objects.equals(getValue(), text)) setValue(text);
        });
    }

    @Override protected void onUnmount() {
        subscription.close();
        subscription = Subscription.EMPTY;
    }
}
