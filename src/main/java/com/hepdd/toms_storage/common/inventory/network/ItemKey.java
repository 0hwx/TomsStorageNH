package com.hepdd.toms_storage.common.inventory.network;

import java.util.Objects;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class ItemKey {

    private Item item;
    private int meta;
    private NBTTagCompound tag;
    private int hash;

    public static ItemKey scratch() {
        return new ItemKey();
    }

    public static ItemKey copyOf(ItemStack stack) {
        ItemKey key = new ItemKey();
        key.item = stack.getItem();
        key.meta = stack.getItemDamage();
        key.tag = stack.hasTagCompound() ? (NBTTagCompound) stack.getTagCompound()
            .copy() : null;
        key.hash = computeHash(key.item, key.meta, key.tag);
        return key;
    }

    public ItemKey set(ItemStack stack) {
        item = stack.getItem();
        meta = stack.getItemDamage();
        tag = stack.hasTagCompound() ? stack.getTagCompound() : null;
        hash = computeHash(item, meta, tag);
        return this;
    }

    public boolean matches(ItemStack stack) {
        if (stack == null) return false;
        if (item != stack.getItem() || meta != stack.getItemDamage()) return false;
        NBTTagCompound other = stack.hasTagCompound() ? stack.getTagCompound() : null;
        return Objects.equals(tag, other);
    }

    public ItemStack toStack() {
        ItemStack stack = new ItemStack(item, 1, meta);
        if (tag != null) stack.setTagCompound((NBTTagCompound) tag.copy());
        return stack;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ItemKey that)) return false;
        if (item != that.item || meta != that.meta) return false;
        return Objects.equals(tag, that.tag);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    private static int computeHash(Item item, int meta, NBTTagCompound tag) {
        int result = Item.getIdFromItem(item);
        result = 31 * result + meta;
        return 31 * result + (tag == null ? 0 : tag.hashCode());
    }
}
