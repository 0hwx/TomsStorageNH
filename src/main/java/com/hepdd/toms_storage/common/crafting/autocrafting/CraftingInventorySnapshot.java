package com.hepdd.toms_storage.common.crafting.autocrafting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.hepdd.toms_storage.common.crafting.CraftingStackMatcher;
import com.hepdd.toms_storage.common.inventory.CachedItemStack;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal;
import com.hepdd.toms_storage.utils.StorageUtils;

/**
 * What the planner is allowed to spend: the network's contents, optionally the player's inventory, and
 * whatever the plan produces along the way.
 *
 * <p>
 * Reserving only marks the snapshot, never the world. Nothing here is spent until the executor applies
 * the finished plan, which is what lets a plan be abandoned for free.
 */
public final class CraftingInventorySnapshot {

    private final List<Entry> terminalStacks = new ArrayList<>();
    private final List<Entry> playerStacks = new ArrayList<>();
    private final List<Entry> intermediates = new ArrayList<>();
    private final boolean usePlayerInventory;

    public CraftingInventorySnapshot(TileEntityStorageTerminal terminal, EntityPlayerMP player,
        boolean usePlayerInventory) {
        this.usePlayerInventory = usePlayerInventory;

        for (CachedItemStack stored : terminal.getCachedStacks()) {
            ItemStack stack = stored.stack();
            if (stack != null && stored.count() > 0) {
                terminalStacks.add(new Entry(stack, (int) Math.min(Integer.MAX_VALUE, stored.count()), -1));
            }
        }

        if (usePlayerInventory) {
            for (int i = 0; i < player.inventory.mainInventory.length; i++) {
                ItemStack stack = player.inventory.mainInventory[i];
                if (stack != null && stack.stackSize > 0) playerStacks.add(new Entry(stack, stack.stackSize, i));
            }
        }
    }

    /**
     * Reserves {@code amount} of whichever candidate the snapshot can cover. Intermediates go first: a
     * sub-craft was planned to be spent here, and leaving it unspent would make the plan produce items
     * nobody asked for.
     */
    public Extraction reserve(ItemStack[] candidates, int amount) {
        if (candidates == null || amount < 1) return null;
        for (ItemStack candidate : candidates) {
            if (candidate == null) continue;
            Extraction extraction = reserveFrom(intermediates, candidate, amount, Extraction.Source.INTERMEDIATE);
            if (extraction != null) return extraction;
            extraction = reserveFrom(terminalStacks, candidate, amount, Extraction.Source.NETWORK);
            if (extraction != null) return extraction;
            if (usePlayerInventory) {
                extraction = reserveFrom(playerStacks, candidate, amount, Extraction.Source.PLAYER);
                if (extraction != null) return extraction;
            }
        }
        return null;
    }

    /**
     * Hands a reservation back, so the planner can abandon a half-built operation and retry it smaller.
     * Without this a failed attempt would leave the snapshot claiming items nothing will ever spend.
     */
    public void release(Extraction extraction) {
        if (extraction == null) return;
        ItemStack stack = extraction.getStack();
        for (Entry entry : entriesFor(extraction.getSource())) {
            if (entry.playerSlot == extraction.getPlayerSlot()
                && StorageUtils.areItemStacksEqual(entry.stack, stack, true)) {
                entry.amount += stack.stackSize;
                return;
            }
        }
    }

    /** Records something the plan will make: a sub-craft output, or a leftover a craft hands back. */
    public void addIntermediate(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return;
        for (Entry entry : intermediates) {
            if (StorageUtils.areItemStacksEqual(entry.stack, stack, true)) {
                entry.amount += stack.stackSize;
                return;
            }
        }
        intermediates.add(new Entry(stack, stack.stackSize, -1));
    }

    public boolean hasPlayerInventory() {
        return usePlayerInventory;
    }

    private List<Entry> entriesFor(Extraction.Source source) {
        switch (source) {
            case INTERMEDIATE:
                return intermediates;
            case PLAYER:
                return playerStacks;
            default:
                return terminalStacks;
        }
    }

    private static Extraction reserveFrom(List<Entry> entries, ItemStack candidate, int amount,
        Extraction.Source source) {
        for (Entry entry : entries) {
            if (entry.amount >= amount && CraftingStackMatcher.matchesIngredient(candidate, entry.stack)) {
                entry.amount -= amount;
                ItemStack reserved = entry.stack.copy();
                reserved.stackSize = amount;
                return new Extraction(reserved, source, entry.playerSlot);
            }
        }
        return null;
    }

    private static final class Entry {

        private final ItemStack stack;
        private int amount;
        private final int playerSlot;

        private Entry(ItemStack stack, int amount, int playerSlot) {
            this.stack = stack.copy();
            this.stack.stackSize = 1;
            this.amount = amount;
            this.playerSlot = playerSlot;
        }
    }
}
