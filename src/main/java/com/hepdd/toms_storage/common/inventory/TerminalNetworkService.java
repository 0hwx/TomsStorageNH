package com.hepdd.toms_storage.common.inventory;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal;
import com.hepdd.toms_storage.utils.StorageUtils;

public final class TerminalNetworkService {

    public static ItemStack pull(TileEntityStorageTerminal terminal, ItemStack template, int amount) {
        IItemHandler inventory = terminal.getAttachedInventory();
        if (inventory == null || template == null || amount < 1) return null;
        ItemStack extracted = StorageUtils.extractMatching(inventory, template, amount, false);
        if (extracted != null) terminal.markContentsChanged();
        return extracted;
    }

    public static ItemStack push(TileEntityStorageTerminal terminal, ItemStack stack) {
        IItemHandler inventory = terminal.getAttachedInventory();
        if (inventory == null || stack == null) return stack;
        ItemStack remainder = StorageUtils.insertStacked(inventory, stack, false);
        terminal.markContentsChanged();
        return remainder;
    }

    public static void pushOrDrop(TileEntityStorageTerminal terminal, ItemStack stack) {
        if (stack == null) return;
        ItemStack remainder = push(terminal, stack);
        if (remainder != null && remainder.stackSize > 0) {
            EntityItem entityItem = new EntityItem(
                terminal.getWorldObj(),
                terminal.xCoord + 0.5D,
                terminal.yCoord + 0.5D,
                terminal.zCoord + 0.5D,
                remainder);
            terminal.getWorldObj()
                .spawnEntityInWorld(entityItem);
        }
    }

    public static boolean giveToPlayer(EntityPlayerMP player, ItemStack stack) {
        return player.inventory.addItemStackToInventory(stack);
    }

    public static void depositAll(TileEntityStorageTerminal terminal, EntityPlayerMP player) {
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (stack != null) {
                player.inventory.mainInventory[i] = push(terminal, stack);
            }
        }
        player.inventory.markDirty();
    }

    public static void depositMatching(TileEntityStorageTerminal terminal, EntityPlayerMP player, ItemStack template) {
        if (template == null) return;
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (StorageUtils.areItemStacksEqual(template, stack, true)) {
                player.inventory.mainInventory[i] = push(terminal, stack);
            }
        }
        player.inventory.markDirty();
    }
}
