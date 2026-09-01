package com.hepdd.toms_storage.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.gtnewhorizon.gtnhlib.capability.Capabilities;
import com.hepdd.toms_storage.common.inventory.CachedItemStack;
import com.hepdd.toms_storage.common.inventory.NetworkItemHandler;
import com.hepdd.toms_storage.common.inventory.SidedInvWrapper;
import com.hepdd.toms_storage.common.inventory.network.NetworkItemIndex;

import it.unimi.dsi.fastutil.ints.IntList;

public final class StorageUtils {

    public static boolean areItemStacksEqual(ItemStack stack, ItemStack other, boolean checkNBT) {
        if (stack == null || other == null) return stack == other;
        if (stack.getItem() != other.getItem()) return false;
        if (stack.getItemDamage() != other.getItemDamage()) return false;
        return !checkNBT || ItemStack.areItemStackTagsEqual(stack, other);
    }

    /**
     * Resolves a neighbouring tile to an item handler: its IItemHandler capability if it has one, otherwise a
     * sided wrapper over its IInventory. Null when the tile is neither.
     */
    public static IItemHandler asHandler(TileEntity tile, ForgeDirection side) {
        if (tile == null) return null;
        IItemHandler handler = Capabilities.getCapability(tile, IItemHandler.class, side);
        if (handler != null) return handler;
        return tile instanceof IInventory inventory ? new SidedInvWrapper(inventory, side) : null;
    }

    public static List<CachedItemStack> aggregate(IItemHandler inventory) {
        Map<CachedItemStack, Long> totals = new HashMap<>();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null && stack.stackSize > 0) {
                totals.merge(new CachedItemStack(stack, 0), (long) stack.stackSize, Long::sum);
            }
        }
        List<CachedItemStack> result = new ArrayList<>(totals.size());
        for (Map.Entry<CachedItemStack, Long> entry : totals.entrySet()) {
            result.add(
                entry.getKey()
                    .withCount(entry.getValue()));
        }
        return result;
    }

    public static ItemStack insertStacked(IItemHandler inventory, ItemStack stack, boolean simulate) {
        if (stack == null || stack.stackSize < 1) return null;
        NetworkItemIndex index = indexOf(inventory);
        if (index != null) return insertIndexed(inventory, index, stack, simulate);

        ItemStack remainder = stack.copy();
        int slots = inventory.getSlots();

        for (int i = 0; i < slots && remainder != null; i++) {
            ItemStack existing = inventory.getStackInSlot(i);
            if (existing != null && areItemStacksEqual(existing, remainder, true)) {
                remainder = inventory.insertItem(i, remainder, simulate);
            }
        }

        for (int i = 0; i < slots && remainder != null; i++) {
            if (inventory.getStackInSlot(i) == null) {
                remainder = inventory.insertItem(i, remainder, simulate);
            }
        }

        return remainder;
    }

    public static ItemStack extractMatching(IItemHandler inventory, ItemStack match, int amount, boolean simulate) {
        if (match == null || amount < 1) return null;
        NetworkItemIndex index = indexOf(inventory);
        if (index != null) return extractIndexed(inventory, index, match, amount, simulate);

        ItemStack result = null;
        int remaining = amount;
        int slots = inventory.getSlots();

        for (int i = 0; i < slots && remaining > 0; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null && areItemStacksEqual(stack, match, true)) {
                ItemStack extracted = inventory.extractItem(i, remaining, simulate);
                if (extracted != null) {
                    if (result == null) {
                        result = extracted;
                    } else {
                        result.stackSize += extracted.stackSize;
                    }
                    remaining -= extracted.stackSize;
                }
            }
        }

        return result;
    }

    private static NetworkItemIndex indexOf(IItemHandler inventory) {
        return inventory instanceof NetworkItemHandler itemHandler ? itemHandler.getIndex() : null;
    }

    private static ItemStack insertIndexed(IItemHandler inventory, NetworkItemIndex index, ItemStack stack,
        boolean simulate) {
        ItemStack remainder = stack.copy();

        IntList matching = index.slotsFor(remainder);
        for (int i = 0; i < matching.size() && remainder != null; i++) {
            remainder = inventory.insertItem(matching.getInt(i), remainder, simulate);
        }

        for (int slot = index.nextEmptySlot(0); slot >= 0 && remainder != null; slot = index.nextEmptySlot(slot + 1)) {
            remainder = inventory.insertItem(slot, remainder, simulate);
        }

        return remainder;
    }

    private static ItemStack extractIndexed(IItemHandler inventory, NetworkItemIndex index, ItemStack match, int amount,
        boolean simulate) {
        IntList slots = index.slotsFor(match);
        ItemStack result = null;
        int remaining = amount;

        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            ItemStack extracted = inventory.extractItem(slots.getInt(i), remaining, simulate);
            if (extracted == null) continue;
            if (result == null) {
                result = extracted;
            } else {
                result.stackSize += extracted.stackSize;
            }
            remaining -= extracted.stackSize;
        }

        return result;
    }
}
