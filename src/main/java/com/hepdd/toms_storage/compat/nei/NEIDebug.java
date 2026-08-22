package com.hepdd.toms_storage.compat.nei;

import net.minecraft.item.ItemStack;

import com.hepdd.toms_storage.TomsConfig;
import com.hepdd.toms_storage.TomsStorage;

public final class NEIDebug {

    public static void log(String message) {
        if (TomsConfig.debugNeiAutocrafting) TomsStorage.LOG.info("[NEI autocraft] " + message);
    }

    public static String stack(ItemStack stack) {
        if (stack == null) return "null";
        return stack.stackSize + "x" + stack.getDisplayName() + "@" + stack.getItemDamage();
    }
}
