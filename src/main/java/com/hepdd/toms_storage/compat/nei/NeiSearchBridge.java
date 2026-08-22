package com.hepdd.toms_storage.compat.nei;

import codechicken.nei.LayoutManager;

public final class NeiSearchBridge {

    public static String getSearch() {
        try {
            return LayoutManager.searchField == null ? null : LayoutManager.searchField.text();
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static void setSearch(String text) {
        try {
            if (LayoutManager.searchField != null) LayoutManager.searchField.setText(text == null ? "" : text);
        } catch (RuntimeException | LinkageError ignored) {}
    }
}
