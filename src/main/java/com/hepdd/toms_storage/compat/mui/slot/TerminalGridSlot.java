package com.hepdd.toms_storage.compat.mui.slot;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.hepdd.toms_storage.compat.mui.sync.TerminalGridSyncHandler;

public class TerminalGridSlot extends ModularSlot {

    private final TerminalGridSyncHandler grid;
    private final int cell;

    public TerminalGridSlot(TerminalGridSyncHandler grid, int cell, IItemHandler view) {
        super(view, cell);
        this.grid = grid;
        this.cell = cell;
        accessibility(false, false);
    }

    // Read through to the sync handler's client list so the icon is always derived from live data.
    @Override
    public ItemStack getStack() {
        return grid.getCellIcon(cell);
    }
}
