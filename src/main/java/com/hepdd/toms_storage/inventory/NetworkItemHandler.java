package com.hepdd.toms_storage.inventory;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;

public class NetworkItemHandler implements IItemHandlerModifiable {

    private final List<IItemHandler> inventories = new ArrayList<>();

    public void clear() {
        inventories.clear();
    }

    public void add(IItemHandler inventory) {
        if (inventory != null) {
            inventories.add(inventory);
        }
    }

    public List<IItemHandler> getInventories() {
        return inventories;
    }

    public boolean containsInventory(IInventory target) {
        if (target == null) return false;
        for (IItemHandler inventory : inventories) {
            if (inventory instanceof SidedInvWrapper && ((SidedInvWrapper) inventory).getInventory() == target)
                return true;
            if (inventory instanceof NetworkItemHandler && ((NetworkItemHandler) inventory).containsInventory(target))
                return true;
        }
        return false;
    }

    @Override
    public int getSlots() {
        int slots = 0;
        for (IItemHandler inventory : inventories) {
            slots += inventory.getSlots();
        }
        return slots;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        SlotRef ref = getSlotRef(slot);
        return ref == null ? null : ref.inventory.getStackInSlot(ref.slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        SlotRef ref = getSlotRef(slot);
        return ref == null ? stack : ref.inventory.insertItem(ref.slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        SlotRef ref = getSlotRef(slot);
        return ref == null ? null : ref.inventory.extractItem(ref.slot, amount, simulate);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        SlotRef ref = getSlotRef(slot);
        return ref != null && ref.inventory.isItemValid(ref.slot, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        SlotRef ref = getSlotRef(slot);
        return ref == null ? 0 : ref.inventory.getSlotLimit(ref.slot);
    }

    // Throws rather than no-ops for a non-modifiable member: a silent failure here would look like item loss.
    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        SlotRef ref = getSlotRef(slot);
        if (ref == null) return;
        if (!(ref.inventory instanceof IItemHandlerModifiable modifiable)) {
            throw new UnsupportedOperationException("Network member does not support setStackInSlot");
        }
        modifiable.setStackInSlot(ref.slot, stack);
    }

    private SlotRef getSlotRef(int slot) {
        int index = slot;
        for (IItemHandler inventory : inventories) {
            int size = inventory.getSlots();
            if (index < size) return new SlotRef(inventory, index);
            index -= size;
        }
        return null;
    }

    private static class SlotRef {

        private final IItemHandler inventory;
        private final int slot;

        private SlotRef(IItemHandler inventory, int slot) {
            this.inventory = inventory;
            this.slot = slot;
        }
    }
}
