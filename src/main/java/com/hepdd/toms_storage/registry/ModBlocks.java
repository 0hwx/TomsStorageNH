package com.hepdd.toms_storage.registry;

import java.util.function.BooleanSupplier;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import com.hepdd.toms_storage.TomsConfig;
import com.hepdd.toms_storage.TomsStorage;
import com.hepdd.toms_storage.common.block.BlockCraftingTerminal;
import com.hepdd.toms_storage.common.block.BlockInventoryCable;
import com.hepdd.toms_storage.common.block.BlockInventoryConnector;
import com.hepdd.toms_storage.common.block.BlockInventoryHopper;
import com.hepdd.toms_storage.common.block.BlockStorageTerminal;
import com.hepdd.toms_storage.common.block.BlockTrim;

import cpw.mods.fml.common.registry.GameRegistry;

// Credit to Et Futurum (Requiem)
public enum ModBlocks {

    // spotless:off
    INVENTORY_CONNECTOR(() -> TomsConfig.Content.inventoryConnector, new BlockInventoryConnector(), BlockInventoryConnector.ItemBlockInventoryConnector.class, "inventory_connector"),
    CRAFTING_TERMINAL(() -> TomsConfig.Content.craftingTerminal, new BlockCraftingTerminal(), BlockCraftingTerminal.ItemBlockCraftingTerminal.class, "crafting_terminal"),
    STORAGE_TERMINAL(() -> TomsConfig.Content.storageTerminal, new BlockStorageTerminal(), BlockStorageTerminal.ItemBlockStorageTerminal.class, "storage_terminal"),
    TRIM(() -> TomsConfig.Content.trim, new BlockTrim(), BlockTrim.ItemBlockTrim.class, "trim"),
    INVENTORY_CABLE(() -> TomsConfig.Content.inventoryCable, new BlockInventoryCable(), BlockInventoryCable.ItemBlockInventoryCable.class, "inventory_cable" ),
    INVENTORY_HOPPER(() -> TomsConfig.Content.inventoryHopper, new BlockInventoryHopper(), BlockInventoryHopper.ItemInventoryHopper.class, "inventory_hopper"),

    ; // leave trailing semicolon
    // spotless:on

    public static final ModBlocks[] VALUES = values();

    public static void init() {
        for (ModBlocks block : VALUES) {
            if (block.isEnabled()) {
                block.theBlock.setCreativeTab(TomsStorage.CREATIVE_TAB);
                if (block.getItemBlock() != null || !block.getHasItemBlock()) {
                    GameRegistry.registerBlock(block.get(), block.getItemBlock(), block.name);
                    // This part is used if the getItemBlock() is not ItemBlock.class, so we register a custom ItemBlock
                    // class as the ItemBlock
                    // It is also used if the getItemBlock() == null and getHasItemBlock() is false, meaning we WANT to
                    // register it as null, making the block have no inventory item.
                } else {
                    GameRegistry.registerBlock(block.get(), block.name);
                    // Used if getItemBlock() == null but getHasItemBlock() is true, registering it with a default
                    // inventory item.
                }
            }
        }
    }

    private final BooleanSupplier isEnabled;
    private final Block theBlock;
    /**
     * null == default ItemBlock
     */
    private final Class<? extends ItemBlock> itemBlock;
    /**
     * Determines if we should register the block with an ItemBlock.
     * Set to false when the constructor that specifies the ItemBlock is specifically set to false.
     */
    private boolean hasItemBlock;
    private final String name;

    ModBlocks(BooleanSupplier enabled, Block block, String name) {
        this(enabled, block, null, name);
        hasItemBlock = true;
    }

    ModBlocks(BooleanSupplier enabled, Block block, Class<? extends ItemBlock> iblock, String name) {
        isEnabled = enabled;
        theBlock = block;
        itemBlock = iblock;
        hasItemBlock = iblock != null;
        this.name = name;
    }

    /**
     * If this is false, the block is initialized without an inventory item, or ItemBlock.
     */
    public boolean getHasItemBlock() {
        return hasItemBlock;
    }

    public Block get() {
        return theBlock;
    }

    public Class<? extends ItemBlock> getItemBlock() {
        return itemBlock;
    }

    public Item getItem() {
        return Item.getItemFromBlock(get());
    }

    public boolean isEnabled() {
        return isEnabled.getAsBoolean();
    }

    public ItemStack newItemStack() {
        return newItemStack(1);
    }

    public ItemStack newItemStack(int count) {
        return newItemStack(count, 0);
    }

    public ItemStack newItemStack(int count, int meta) {
        return new ItemStack(this.get(), count, meta);
    }
}
