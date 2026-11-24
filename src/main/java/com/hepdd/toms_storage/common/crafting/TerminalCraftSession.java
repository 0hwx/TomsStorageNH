package com.hepdd.toms_storage.common.crafting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;

import com.cleanroommc.modularui.widgets.slot.InventoryCraftingWrapper;
import com.hepdd.toms_storage.TomsConfig;
import com.hepdd.toms_storage.common.inventory.TerminalNetworkService;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal;

import cpw.mods.fml.common.FMLCommonHandler;

public final class TerminalCraftSession {

    private final TileEntityStorageTerminal terminal;

    private int crafted;
    private int perCraft = -1;
    private int stackLimit = -1;
    private boolean bounded;

    public TerminalCraftSession(TileEntityStorageTerminal terminal) {
        this.terminal = terminal;
    }

    public void begin(boolean shiftClick) {
        crafted = 0;
        perCraft = -1;
        stackLimit = -1;
        bounded = shiftClick && TomsConfig.shiftCraftLimit == ShiftCraftLimit.ONE_STACK;
    }

    public void end() {
        crafted = 0;
        perCraft = -1;
        stackLimit = -1;
        bounded = false;
    }

    public int plannedCrafts(ItemStack output) {
        if (output == null) return 0;
        if (!bounded) return Integer.MAX_VALUE;
        return Math.max(1, output.getMaxStackSize() / Math.max(1, output.stackSize));
    }

    public boolean isSpent() {
        return bounded && perCraft > 0 && crafted + perCraft > stackLimit;
    }

    public ItemStack craftOnce(InventoryCraftingWrapper matrix, EntityPlayer player) {
        if (player.worldObj.isRemote) return null;
        ItemStack output = CraftingManager.getInstance()
            .findMatchingRecipe(matrix, player.worldObj);
        if (output == null) return null;

        FMLCommonHandler.instance()
            .firePlayerCraftingEvent(player, output.copy(), matrix);
        account(output);

        for (int slot = 0, size = ingredientSlots(matrix); slot < size; slot++) {
            ItemStack ingredient = matrix.getStackInSlot(slot);
            if (ingredient == null || ingredient.getItem() == null) continue;
            consumeOne(matrix, slot, ingredient, player);
        }
        return output.copy();
    }

    private static int ingredientSlots(InventoryCraftingWrapper matrix) {
        return matrix.getSizeInventory() - 1;
    }

    private void consumeOne(InventoryCraftingWrapper matrix, int slot, ItemStack ingredient, EntityPlayer player) {
        ItemStack consumed = ingredient.copy();
        consumed.stackSize = 1;

        boolean fromGrid = TerminalNetworkService.pull(terminal, consumed, 1) == null;
        if (fromGrid) matrix.decrStackSize(slot, 1, false);

        ItemStack remainder = CraftingRemainderHandler.getRemainder(consumed, player);
        if (remainder == null) return;
        // A slot the fallback just emptied takes the leftover back, so a bucket stays where the recipe
        // wants it. Otherwise the grid is untouched and the leftover belongs in storage.
        if (fromGrid && matrix.getStackInSlot(slot) == null) matrix.setSlot(slot, remainder, false);
        else TerminalNetworkService.pushOrDrop(terminal, remainder);
    }

    private void account(ItemStack output) {
        // The result is only known once a craft has happened, so the cap is learned on the first one.
        if (perCraft < 0) {
            perCraft = Math.max(1, output.stackSize);
            stackLimit = output.getMaxStackSize();
        }
        crafted += output.stackSize;
    }
}
