package com.hepdd.toms_storage.compat.mui.slot;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;

import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.cleanroommc.modularui.widgets.slot.InventoryCraftingWrapper;
import com.cleanroommc.modularui.widgets.slot.ModularCraftingSlot;

import cpw.mods.fml.common.FMLCommonHandler;

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

    // ModularCraftingSlot.onPickupFromSlot ends by dropping the crafted output on the ground
    // but MUI's slotClick has ALREADY placed that output on the cursor
    // the item is duplicated (one on the cursor, one on the ground)
    // This fix it
    @Override
    public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {
        if (matrix == null) return;
        FMLCommonHandler.instance()
            .firePlayerCraftingEvent(player, stack, matrix);
        onCrafting(stack);
        if (stack != null) stack.onCrafting(player.worldObj, player, stack.stackSize);
        for (int i = 0; i < matrix.getSizeInventory(); i++) {
            ItemStack ingredient = matrix.getStackInSlot(i);
            if (ingredient == null || ingredient.getItem() == null) continue;
            matrix.decrStackSize(i, 1, false);
            if (ingredient.getItem()
                .hasContainerItem(ingredient)) {
                ItemStack container = ingredient.getItem()
                    .getContainerItem(ingredient);
                if (container != null) {
                    if (container.isItemStackDamageable() && container.getItemDamage() > container.getMaxDamage()) {
                        MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, container));
                        continue;
                    }
                    if (!ingredient.getItem()
                        .doesContainerItemLeaveCraftingGrid(ingredient)
                        || !player.inventory.addItemStackToInventory(container)) {
                        if (matrix.getStackInSlot(i) == null) matrix.setSlot(i, container, false);
                        else player.dropPlayerItemWithRandomChoice(container, false);
                    }
                }
            }
        }
        matrix.notifyContainer();
    }
}
