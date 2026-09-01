package com.hepdd.toms_storage.mixin.late.nei;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hepdd.toms_storage.TomsConfig;
import com.hepdd.toms_storage.common.crafting.autocrafting.AutoCraftDebug;
import com.hepdd.toms_storage.common.inventory.CachedItemStack;
import com.hepdd.toms_storage.compat.mui.container.CraftingTerminalContainer;
import com.hepdd.toms_storage.compat.mui.sync.TerminalGridSyncHandler;
import com.hepdd.toms_storage.compat.nei.NeiCompat;

import codechicken.nei.ItemStackAmount;
import codechicken.nei.recipe.AutoCraftingManager;

/** Makes NEI's auto-craft planner see the storage network as if it were inventory. */
@Mixin(value = AutoCraftingManager.class, remap = false)
public abstract class AutoCraftingManagerMixin {

    @Inject(method = "getInventoryItems", at = @At("RETURN"))
    private static void tomsstorage$getInventoryItems(GuiContainer guiContainer,
        CallbackInfoReturnable<ItemStackAmount> cir) {
        if (!TomsConfig.neiAutocrafting) return;
        if (!(guiContainer.inventorySlots instanceof CraftingTerminalContainer container)) return;

        TerminalGridSyncHandler grid = NeiCompat.grid(container);
        if (grid == null) return;

        ItemStackAmount inventory = cir.getReturnValue();
        if (container.getAutoCraftPreview()
            .isActive()) {
            inventory.clear();
            for (ItemStack stack : container.getAutoCraftPreview()
                .getPlayerStacks(guiContainer.mc.thePlayer.inventory)) {
                inventory.add(stack);
            }
        }

        int added = 0;
        List<CachedItemStack> stacks = container.getAutoCraftPreview()
            .getStacks(grid.clientCachedItems());
        for (CachedItemStack stack : stacks) {
            ItemStack actual = stack.actualStack();
            if (actual != null && actual.stackSize > 0) {
                inventory.add(actual);
                added++;
            }
        }
        AutoCraftDebug.log("AutoCraftingManager.getInventoryItems added terminal stacks=" + added);
    }
}
