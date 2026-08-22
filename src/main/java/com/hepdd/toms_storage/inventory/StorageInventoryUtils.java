package com.hepdd.toms_storage.inventory;

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
import com.hepdd.toms_storage.StorageItemUtils;

public final class StorageInventoryUtils {

    private StorageInventoryUtils() {}

    /**
     * Resolves a neighbouring tile to an item handler: its IItemHandler capability if it has one, otherwise a
     * sided wrapper over its IInventory. Null when the tile is neither.
     *
     * <p>
     * Capabilities.getCapability tries a direct cast before consulting CapabilityProvider, so a tile that
     * simply implements IItemHandler is picked up without being a CapabilityProvider.
     */
    public static IItemHandler asHandler(TileEntity tile, ForgeDirection side) {
        if (tile == null) return null;
        IItemHandler handler = Capabilities.getCapability(tile, IItemHandler.class, side);
        if (handler != null) return handler;
        return tile instanceof IInventory inventory ? new SidedInvWrapper(inventory, side) : null;
    }

    /** Aggregates every slot into de-duplicated {@link CachedItemStack}s (identity + summed count). */
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
        ItemStack remainder = stack.copy();

        for (int i = 0; i < inventory.getSlots() && remainder != null; i++) {
            ItemStack existing = inventory.getStackInSlot(i);
            if (existing != null && StorageItemUtils.areItemStacksEqual(existing, remainder, true)) {
                remainder = inventory.insertItem(i, remainder, simulate);
            }
        }

        for (int i = 0; i < inventory.getSlots() && remainder != null; i++) {
            if (inventory.getStackInSlot(i) == null) {
                remainder = inventory.insertItem(i, remainder, simulate);
            }
        }

        return remainder;
    }

    public static ItemStack extractMatching(IItemHandler inventory, ItemStack match, int amount, boolean simulate) {
        if (match == null || amount < 1) return null;
        ItemStack result = null;
        int remaining = amount;

        for (int i = 0; i < inventory.getSlots() && remaining > 0; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null && StorageItemUtils.areItemStacksEqual(stack, match, true)) {
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
}
