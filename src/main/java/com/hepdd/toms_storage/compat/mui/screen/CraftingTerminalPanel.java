package com.hepdd.toms_storage.compat.mui.screen;

import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.hepdd.toms_storage.common.tile.TileEntityCraftingTerminal;
import com.hepdd.toms_storage.compat.mui.container.CraftingTerminalContainer;
import com.hepdd.toms_storage.compat.mui.slot.ItemSlotTerminalCraftingMatrix;
import com.hepdd.toms_storage.compat.mui.slot.TerminalCraftingSlot;
import com.hepdd.toms_storage.compat.mui.sync.TerminalGridSyncHandler;

public class CraftingTerminalPanel extends StorageTerminalPanel {

    // 0-8 = 3x3 matrix, 9 = result. Transient: dumped back to the network on container close.
    private final ItemStackHandler craftingInventory = new ItemStackHandler(10);
    private final TileEntityCraftingTerminal craftingTile;

    public CraftingTerminalPanel(TileEntityCraftingTerminal tile, PanelSyncManager sync, UISettings settings) {
        super("crafting_terminal", 194, 256, 173, tile, sync, settings);
        this.craftingTile = tile;
        addCraftingWidgets(sync);
    }

    @Override
    protected ModularContainer createContainer() {
        return new CraftingTerminalContainer(craftingTile, craftingInventory);
    }

    private void addCraftingWidgets(PanelSyncManager sync) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                child(
                    new ItemSlotTerminalCraftingMatrix(index).slot(new ModularSlot(craftingInventory, index))
                        .invisible()
                        .pos(25 + col * SLOT, 110 + row * SLOT));
            }
        }
        child(
            new ItemSlot().slot(new TerminalCraftingSlot(craftingInventory, 9))
                .invisible()
                .pos(119, 128));

        BooleanSyncValue craftMode = new BooleanSyncValue(
            craftingTile::isCraftToStorage,
            craftingTile::setCraftToStorage);
        craftMode.allowC2S(true);
        sync.syncValue("craft_to_storage", 3, craftMode);
        child(new ButtonWidget<>().onMousePressed(mouseButton -> {
            craftMode.setBoolValue(!craftMode.getBoolValue(), true, true);
            return true;
        })
            .pos(150, 129)
            .size(16, 16));

        addClearGridButton();
    }

    private void addClearGridButton() {
        ButtonWidget<?> clear = new ButtonWidget<>();
        clear.onMousePressed(mouseButton -> {
            TerminalGridSyncHandler grid = grid();
            if (grid != null) grid.requestClearGrid();
            return true;
        })
            .background(new DynamicDrawable(() -> buttonTexture(clear.isHovering() ? 216 : 205, 10, 11, 11)))
            .pos(80, 110)
            .size(11, 11);
        child(clear);
    }
}
