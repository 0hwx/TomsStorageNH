package com.hepdd.toms_storage.common.sort;

public enum SortDirection {

    ASCENDING,
    DESCENDING;

    public static final SortDirection[] VALUES = values();

    public SortDirection toggle() {
        return this == ASCENDING ? DESCENDING : ASCENDING;
    }
}
