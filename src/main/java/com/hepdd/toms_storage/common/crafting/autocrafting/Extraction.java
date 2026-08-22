package com.hepdd.toms_storage.common.crafting.autocrafting;

import net.minecraft.item.ItemStack;

/**
 * One reservation the planner made: what to take, and from where.
 *
 * <p>
 * The source matters at apply time. Network and player takes touch the world and have to be undone if
 * the plan fails partway; an {@link Source#INTERMEDIATE} take is a sub-craft output or a leftover the
 * plan produced itself, which never leaves the executor's hands and so needs no rollback.
 */
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

    /** What to take, with the amount already multiplied up for a batched operation. */
    public ItemStack getStack() {
        return stack;
    }

    public Source getSource() {
        return source;
    }

    /** Only meaningful for {@link Source#PLAYER}. */
    public int getPlayerSlot() {
        return playerSlot;
    }
}
