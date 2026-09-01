package com.hepdd.toms_storage.compat.mui.slot;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.cleanroommc.modularui.widgets.slot.InventoryCraftingWrapper;
import com.cleanroommc.modularui.widgets.slot.ModularCraftingSlot;
import com.hepdd.toms_storage.compat.mui.container.CraftingTerminalContainer;

public class TerminalCraftingSlot extends ModularCraftingSlot {

    private InventoryCraftingWrapper matrix;

    public TerminalCraftingSlot(IItemHandler itemHandler, int index) {
        super(itemHandler, index);
    }

    @Override
    public void setCraftMatrix(InventoryCraftingWrapper craftMatrix) {
        super.setCraftMatrix(craftMatrix);
        this.matrix = craftMatrix;
    }

    @Override
    public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {
        if (matrix == null) return;
        if (matrix.getContainer() instanceof CraftingTerminalContainer container) {
            ItemStack output = container.getCraftSession()
                .craftOnce(matrix, player);
            if (output != null) {
                onCrafting(output);
                output.onCrafting(player.worldObj, player, output.stackSize);
            }
        }
        // After the refill, so the result slot is recomputed against the restocked grid.
        matrix.notifyContainer();
    }
}
