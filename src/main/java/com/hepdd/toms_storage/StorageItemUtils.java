package com.hepdd.toms_storage;

import net.minecraft.item.ItemStack;

public final class StorageItemUtils {

    public static boolean areItemStacksEqual(ItemStack stack, ItemStack other, boolean checkNBT) {
        if (stack == null || other == null) return stack == other;
        if (stack.getItem() != other.getItem()) return false;
        if (stack.getItemDamage() != other.getItemDamage()) return false;
        return !checkNBT || ItemStack.areItemStackTagsEqual(stack, other);
    }
}
