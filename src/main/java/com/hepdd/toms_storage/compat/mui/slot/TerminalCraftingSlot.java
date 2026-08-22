package com.hepdd.toms_storage.compat.mui.slot;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.cleanroommc.modularui.widgets.slot.InventoryCraftingWrapper;
import com.cleanroommc.modularui.widgets.slot.ModularCraftingSlot;
import com.hepdd.toms_storage.common.crafting.TerminalCraftSession;
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

    /**
     * MUI has already handed the output to the player by the time this runs, so all that is left is the
     * grid side of the craft - which {@link TerminalCraftSession} owns, shared with the craft-to-storage
     * path. Deliberately does not call super: ModularCraftingSlot ends by dropping the output on the
     * ground, which duplicates it because slotClick has already placed it on the cursor.
     *
     * <p>
     * The {@code stack} argument is the leftover that would not fit in the player's inventory - null on a
     * normal craft - not what was produced, so the session re-derives the real output from the grid.
     */
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
