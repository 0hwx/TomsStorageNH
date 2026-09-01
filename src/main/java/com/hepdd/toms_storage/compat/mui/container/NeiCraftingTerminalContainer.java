package com.hepdd.toms_storage.compat.mui.container;

import java.util.ArrayList;
import java.util.List;

import com.cleanroommc.modularui.integration.nei.INEIRecipeTransfer;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.hepdd.toms_storage.common.tile.TileEntityCraftingTerminal;
import com.hepdd.toms_storage.compat.nei.NeiCompat;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The crafting terminal container with NEI's recipe transfer bolted on.
 */
@SideOnly(Side.CLIENT)
@Optional.Interface(iface = "com.cleanroommc.modularui.integration.nei.INEIRecipeTransfer", modid = "NotEnoughItems")
public class NeiCraftingTerminalContainer extends CraftingTerminalContainer
    implements INEIRecipeTransfer<GuiContainerWrapper> {

    public NeiCraftingTerminalContainer(TileEntityCraftingTerminal terminal, IItemHandlerModifiable craftingInventory) {
        super(terminal, craftingInventory);
    }

    @Optional.Method(modid = "NotEnoughItems")
    @Override
    public String[] getIdents() {
        return NeiCompat.getIdents();
    }

    @Optional.Method(modid = "NotEnoughItems")
    @Override
    public int transferRecipe(GuiContainerWrapper gui, IRecipeHandler recipe, int recipeIndex, int multiplier) {
        return NeiCompat.transferRecipe(this, gui, recipe, recipeIndex, multiplier);
    }

    @Optional.Method(modid = "NotEnoughItems")
    @Override
    public List<GuiOverlayButton.ItemOverlayState> presenceOverlay(GuiContainerWrapper gui, IRecipeHandler recipe,
        int recipeIndex) {
        return NeiCompat.presenceOverlay(this, gui, recipe, recipeIndex);
    }

    @Optional.Method(modid = "NotEnoughItems")
    @Override
    public ArrayList<PositionedStack> positionStacks(GuiContainerWrapper gui, ArrayList<PositionedStack> stacks) {
        return NeiCompat.positionStacks(this, gui, stacks);
    }

    @Optional.Method(modid = "NotEnoughItems")
    @Override
    public boolean canCraft(GuiContainerWrapper gui, IRecipeHandler recipe, int recipeIndex) {
        return NeiCompat.canCraft(this, recipe, recipeIndex);
    }

    @Optional.Method(modid = "NotEnoughItems")
    @Override
    public boolean craft(GuiContainerWrapper gui, IRecipeHandler recipe, int recipeIndex, int multiplier) {
        return NeiCompat.craft(this, gui, recipe, recipeIndex, multiplier);
    }
}
