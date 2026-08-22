package com.hepdd.toms_storage.common.search;

import java.util.Locale;

public enum TerminalSearchMode {

    STANDARD(false, false, false),
    AUTO(true, false, false),
    AUTO_KEEP(true, true, false),
    AUTO_NEI(true, true, true);

    public static final TerminalSearchMode[] VALUES = values();

    private final boolean autoFocus;
    private final boolean keepText;
    private final boolean syncNei;
    private final String key;

    TerminalSearchMode(boolean autoFocus, boolean keepText, boolean syncNei) {
        this.autoFocus = autoFocus;
        this.keepText = keepText;
        this.syncNei = syncNei;
        this.key = name().toLowerCase(Locale.ROOT);
    }

    public boolean autoFocus() {
        return autoFocus;
    }

    public boolean keepText() {
        return keepText;
    }

    public boolean syncNei() {
        return syncNei;
    }

    /** Lowercase name, used as the tooltip translation key suffix. */
    public String key() {
        return key;
    }

    public TerminalSearchMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }
}
