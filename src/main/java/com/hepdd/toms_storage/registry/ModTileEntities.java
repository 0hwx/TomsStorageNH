package com.hepdd.toms_storage.registry;

import net.minecraft.tileentity.TileEntity;

import com.hepdd.toms_storage.TomsStorage;
import com.hepdd.toms_storage.common.tile.TileEntityCraftingTerminal;
import com.hepdd.toms_storage.common.tile.TileEntityInventoryConnector;
import com.hepdd.toms_storage.common.tile.TileEntityInventoryHopper;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal;

import cpw.mods.fml.common.registry.GameRegistry;

public enum ModTileEntities {

    // spotless:off
    // make sure to leave a trailing comma
    STORAGE_TERMINAL(ModBlocks.STORAGE_TERMINAL    , TileEntityStorageTerminal.class    , "storage_terminal"),
    CRAFTING_TERMINAL(ModBlocks.CRAFTING_TERMINAL  , TileEntityCraftingTerminal.class   , "crafting_terminal"),
    INVENTORY_CONNECTOR(ModBlocks.INVENTORY_CONNECTOR, TileEntityInventoryConnector.class, "inventory_connector"),
    INVENTORY_HOPPER(ModBlocks.INVENTORY_HOPPER    , TileEntityInventoryHopper.class    , "inventory_hopper"),
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

    private final ModBlocks block;
    private final Class<? extends TileEntity> clazz;
    private final String name;

    ModTileEntities(ModBlocks block, Class<? extends TileEntity> clazz, String name) {
        this.block = block;
        this.clazz = clazz;
        this.name = TomsStorage.MODID + ":" + name;
    }

    public boolean isEnabled() {
        return block.isEnabled();
    }
}
