package com.nstut.openui.format;

import java.text.NumberFormat;
import java.util.Locale;

public interface ValueFormatter<T> {
    String format(T value);

    ValueFormatter<Number> INTEGER = val -> val == null ? "0" : String.format("%,d", val.longValue());
    ValueFormatter<Number> DECIMAL = val -> val == null ? "0.0" : String.format(Locale.ROOT, "%,.2f", val.doubleValue());
    ValueFormatter<Number> PERCENT = val -> val == null ? "0%" : String.format(Locale.ROOT, "%.1f%%", val.doubleValue() * 100.0);

    ValueFormatter<Number> COMPACT = val -> {
        if (val == null) return "0";
        double d = val.doubleValue();
        if (Math.abs(d) >= 1_000_000_000) return String.format(Locale.ROOT, "%.1fB", d / 1_000_000_000.0);
        if (Math.abs(d) >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", d / 1_000_000.0);
        if (Math.abs(d) >= 1_000) return String.format(Locale.ROOT, "%.1fk", d / 1_000.0);
        return String.format(Locale.ROOT, "%.0f", d);
    };

    ValueFormatter<Number> CURRENCY = val -> val == null ? "$0" : String.format(Locale.ROOT, "$%,.2f", val.doubleValue());
}
