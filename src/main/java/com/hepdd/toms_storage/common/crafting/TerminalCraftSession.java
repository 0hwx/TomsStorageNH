package com.hepdd.toms_storage.common.crafting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;

import com.cleanroommc.modularui.widgets.slot.InventoryCraftingWrapper;
import com.hepdd.toms_storage.TomsConfig;
import com.hepdd.toms_storage.common.inventory.TerminalNetworkService;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * One Crafting Terminal's craft cycle: pay for the recipe out of the network, return what it gives back,
 * and bound how much a single shift-click may produce.
 */
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

    /**
     * How many crafts this click may run, decided before any of them happen - AE2's
     * {@code floor(maxStackSize / howManyPerCraft)}. A recipe yielding 3 gets 21 crafts, so it stops at 63
     * rather than overshooting a stack.
     * 
     * @param output what one craft produces, or null when the grid holds no recipe
     */
    public int plannedCrafts(ItemStack output) {
        if (output == null) return 0;
        if (!bounded) return Integer.MAX_VALUE;
        return Math.max(1, output.getMaxStackSize() / Math.max(1, output.stackSize));
    }

    public boolean isSpent() {
        return bounded && perCraft > 0 && crafted + perCraft > stackLimit;
    }

    /**
     * Runs one craft: fires the crafting event, pays for every filled slot out of the network, and puts
     * back whatever the recipe returns.
     * <p>
     * Server only, and it never half-executes: a null return means nothing was consumed. The caller owns
     * the output - the storage path pushes it, the player path had MUI move it before we got here.
     *
     * @return what this craft produced, or null when the grid holds no recipe
     */
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

    /**
     * Takes one of this ingredient from the network. Only when the network cannot supply it does the grid
     * slot itself pay
     */
    private void consumeOne(InventoryCraftingWrapper matrix, int slot, ItemStack ingredient, EntityPlayer player) {
        ItemStack consumed = ingredient.copy();
        consumed.stackSize = 1;

        boolean fromGrid = TerminalNetworkService.pull(terminal, consumed, 1) == null;
        if (fromGrid) matrix.decrStackSize(slot, 1, false);

        ItemStack remainder = CraftingRemainderHandler.getRemainder(consumed);
        if (remainder == null) {
            if (CraftingRemainderHandler.containerBreaks(consumed)) {
                MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, consumed));
            }
            return;
        }
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
