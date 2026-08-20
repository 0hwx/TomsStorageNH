package com.hepdd.toms_storage.registry;

import com.hepdd.toms_storage.TomsStorage;
import com.hepdd.toms_storage.tile.TileEntityCraftingTerminal;
import com.hepdd.toms_storage.tile.TileEntityInventoryConnector;
import com.hepdd.toms_storage.tile.TileEntityInventoryHopper;
import com.hepdd.toms_storage.tile.TileEntityStorageTerminal;
import net.minecraft.tileentity.TileEntity;


import cpw.mods.fml.common.registry.GameRegistry;

public enum ModTileEntities {
    // spotless:off
    // make sure to leave a trailing comma
    STORAGE_TERMINAL(TileEntityStorageTerminal.class                                                , "storage_terminal"),
    CRAFTING_TERMINAL(TileEntityCraftingTerminal.class                                              , "crafting_terminal"),
    INVENTORY_CONNECTOR(TileEntityInventoryConnector.class                                          , "inventory_connector"),
    INVENTORY_HOPPER(TileEntityInventoryHopper.class                                                , "inventory_hopper"),
    ;
    // spotless:on

    public static final ModTileEntities[] VALUES = values();

    public static void init() {
        for (ModTileEntities te : VALUES) {
            if (te.isEnabled()) {
                GameRegistry.registerTileEntity(te.clazz, te.name);
            }
        }
    }

    private final boolean isEnabled;
    private final Class<? extends TileEntity> clazz;
    private final String name;

    ModTileEntities(Class<? extends TileEntity> clazz, String name) {
        this(true, clazz, name);
    }

    ModTileEntities(Boolean enabled, Class<? extends TileEntity> clazz, String name) {
        this.isEnabled = enabled;
        this.clazz = clazz;
        this.name = TomsStorage.MODID + ":" + name;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}