package com.hepdd.toms_storage.common.crafting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;

public final class CraftingRemainderHandler {

    public static ItemStack getRemainder(ItemStack ingredient) {
        return getRemainder(ingredient, null);
    }

    public static ItemStack getRemainder(ItemStack ingredient, EntityPlayer player) {
        ItemStack container = getContainerItem(ingredient);
        if (container == null) return null;
        if (isBroken(container)) {
            if (player != null) MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, ingredient));
            return null;
        }
        return container.stackSize > 0 ? container.copy() : null;
    }

    public static boolean mayReturnRemainder(ItemStack ingredient) {
        return ingredient != null && (isGTTool(ingredient) || ingredient.getItem()
            .hasContainerItem(ingredient.copy()));
    }

    static boolean isGTTool(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound()
            .hasKey("GT.ToolStats", 10);
    }

    private static ItemStack getContainerItem(ItemStack ingredient) {
        if (ingredient == null) return null;
        ItemStack input = ingredient.copy();
        return input.getItem()
            .hasContainerItem(input)
                ? input.getItem()
                    .getContainerItem(input)
                : null;
    }

    private static boolean isBroken(ItemStack container) {
        return container.isItemStackDamageable() && container.getItemDamage() > container.getMaxDamage();
    }
}
