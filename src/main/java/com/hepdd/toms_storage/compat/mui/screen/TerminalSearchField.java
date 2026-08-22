package com.hepdd.toms_storage.compat.mui.screen;

import java.util.function.BooleanSupplier;

import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.hepdd.toms_storage.compat.nei.NeiSearchBridge;

import cpw.mods.fml.common.Loader;

public class TerminalSearchField extends TextFieldWidget {

    private static final boolean NEI_LOADED = Loader.isModLoaded("NotEnoughItems");

    private static String originalNeiSearch;

    private final BooleanSupplier syncNei;
    private String lastTerminalSearch = "";
    private String lastNeiSearch = "";

    public TerminalSearchField(BooleanSupplier syncNei) {
        this.syncNei = syncNei;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!NEI_LOADED || !syncNei.getAsBoolean()) return;

        String neiSearch = NeiSearchBridge.getSearch();
        if (neiSearch == null) return;

        if (originalNeiSearch == null) {
            // First tick with sync on: adopt NEI's text if we have none, otherwise push ours to NEI.
            originalNeiSearch = neiSearch;
            String terminalSearch = getText();
            if (terminalSearch.isEmpty() && !neiSearch.isEmpty()) {
                setText(neiSearch);
                terminalSearch = neiSearch;
            } else if (!terminalSearch.equals(neiSearch)) {
                NeiSearchBridge.setSearch(terminalSearch);
            }
            lastTerminalSearch = terminalSearch;
            lastNeiSearch = terminalSearch;
            return;
        }

        String terminalSearch = getText();
        if (!terminalSearch.equals(lastTerminalSearch)) {
            NeiSearchBridge.setSearch(terminalSearch);
            lastTerminalSearch = terminalSearch;
            lastNeiSearch = terminalSearch;
        } else if (!neiSearch.equals(lastNeiSearch)) {
            setText(neiSearch);
            lastTerminalSearch = neiSearch;
            lastNeiSearch = neiSearch;
        }
    }

    public static void restoreNeiSearch() {
        if (!NEI_LOADED || originalNeiSearch == null) return;
        NeiSearchBridge.setSearch(originalNeiSearch);
        originalNeiSearch = null;
    }
}
