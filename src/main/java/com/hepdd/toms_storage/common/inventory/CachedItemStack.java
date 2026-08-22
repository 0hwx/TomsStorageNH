package com.hepdd.toms_storage.common.inventory;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.ItemHandlerHelper;
import com.github.bsideup.jabel.Desugar;
import com.hepdd.toms_storage.utils.StorageUtils;

/**
 * Immutable (identity, count) pair for the storage network's aggregated contents and the MUI2 terminal.
 */
@Desugar
public record CachedItemStack(ItemStack stack, long count) {

    public CachedItemStack(ItemStack stack, long count) {
        this.stack = stack == null ? null : ItemHandlerHelper.copyStackWithSize(stack, 1);
        this.count = count;
    }

    public CachedItemStack withCount(long newCount) {
        return new CachedItemStack(stack, newCount);
    }

    public ItemStack icon() {
        return ItemHandlerHelper.copyStackWithSize(stack, 1);
    }

    public ItemStack actualStack() {
        return ItemHandlerHelper.copyStackWithSize(stack, (int) Math.min(count, Integer.MAX_VALUE));
    }

    public String displayName() {
        return stack == null ? "" : stack.getDisplayName();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CachedItemStack that)) return false;
        return StorageUtils.areItemStacksEqual(stack, that.stack, true);
    }

    @Override
    public int hashCode() {
        if (stack == null) return 0;
        int result = Item.getIdFromItem(stack.getItem());
        result = 31 * result + stack.getItemDamage();
        result = 31 * result + (stack.hasTagCompound() ? stack.getTagCompound()
            .hashCode() : 0);
        return result;
    }
}
