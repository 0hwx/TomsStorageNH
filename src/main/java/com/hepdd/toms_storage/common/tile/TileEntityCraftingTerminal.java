package com.hepdd.toms_storage.common.tile;

import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.hepdd.toms_storage.compat.mui.screen.CraftingTerminalPanel;

public class TileEntityCraftingTerminal extends TileEntityStorageTerminal {

    // When true, taking the crafting result pushes it into the network instead of the cursor/inventory.
    private boolean craftToStorage;

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new CraftingTerminalPanel(this, sync, settings);
    }

    public boolean isCraftToStorage() {
        return craftToStorage;
    }

    public void setCraftToStorage(boolean craftToStorage) {
        this.craftToStorage = craftToStorage;
        markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("craftToStorage", craftToStorage);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        craftToStorage = tag.getBoolean("craftToStorage");
    }
}
