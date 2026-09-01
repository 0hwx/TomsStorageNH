package com.hepdd.toms_storage.common.crafting.autocrafting;

import net.minecraft.item.ItemStack;

public final class Extraction {

    public enum Source {
        NETWORK,
        PLAYER,
        INTERMEDIATE
    }

    private final ItemStack stack;
    private final Source source;
    private final int playerSlot;

    Extraction(ItemStack stack, Source source, int playerSlot) {
        this.stack = stack;
        this.source = source;
        this.playerSlot = playerSlot;
    }

    public ItemStack getStack() {
        return stack;
    }

    public Source getSource() {
        return source;
    }

    public int getPlayerSlot() {
        return playerSlot;
    }
}
