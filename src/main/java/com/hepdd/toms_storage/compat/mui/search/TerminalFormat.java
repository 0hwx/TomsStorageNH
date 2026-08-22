package com.hepdd.toms_storage.compat.mui.search;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public final class TerminalFormat {

    private static final int BASE = 1000;
    private static final char[] POSTFIX = "KMGTPE".toCharArray();
    private static final DecimalFormat FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        FORMAT = new DecimalFormat(".#;0.#", symbols);
    }

    public static String compact(long number) {
        if (number < 0) return Long.toString(number);
        String plain = Long.toString(number);
        if (plain.length() <= 4) return plain;

        long base = number;
        double last = base;
        int exponent = -1;
        String postfix = "";
        int size = plain.length();
        while (size > 4) {
            last = base;
            base /= BASE;
            exponent++;
            size = Long.toString(base)
                .length() + 1;
            postfix = String.valueOf(POSTFIX[exponent]);
        }

        String withPrecision = FORMAT.format(last / BASE) + postfix;
        String withoutPrecision = base + postfix;
        return withPrecision.length() <= 4 ? withPrecision : withoutPrecision;
    }
}
