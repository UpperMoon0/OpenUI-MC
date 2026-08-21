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

    @Override protected void onMount() {
        subscription = value.subscribe(text -> {
            if (!Objects.equals(getValue(), text)) setValue(text);
        });
    }

    @Override protected void onUnmount() {
        subscription.close();
        subscription = Subscription.EMPTY;
    }
}
