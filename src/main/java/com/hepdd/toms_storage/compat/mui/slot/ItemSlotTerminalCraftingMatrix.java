package com.hepdd.toms_storage.compat.mui.slot;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.hepdd.toms_storage.compat.nei.NeiCompat;

import cpw.mods.fml.common.Loader;

/** Crafting matrix slot that paints NEI's recipe ghost itself; see NeiCompat#drawGhost for why. */
public class ItemSlotTerminalCraftingMatrix extends ItemSlot {

    private static final boolean NEI_LOADED = Loader.isModLoaded("NotEnoughItems");

    private final int index;

    public ItemSlotTerminalCraftingMatrix(int index) {
        this.index = index;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        if (NEI_LOADED && getSlot().getStack() == null) NeiCompat.drawGhost(index);
    }
}
