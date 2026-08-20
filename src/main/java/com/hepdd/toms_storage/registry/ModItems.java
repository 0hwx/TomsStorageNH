package com.hepdd.toms_storage.registry;

import com.hepdd.toms_storage.TomsStorage;
import com.hepdd.toms_storage.item.ItemWirelessTerminal;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;

// Credit to Et Futurum (Requiem)
public enum ModItems {
    // spotless:off

    // make sure to leave a trailing comma
    WIRELESS_TERMINAL(true, new ItemWirelessTerminal(), "wireless_terminal"),

    ; // leave trailing semicolon
    // spotless:on

    public static final ModItems[] VALUES = values();

    public static void init() {
        for (ModItems item : VALUES) {
            if (item.isEnabled()) {
                item.theItem.setCreativeTab(TomsStorage.CREATIVE_TAB);
                GameRegistry.registerItem(item.get(), item.name);
            }
        }
    }

    private final boolean isEnabled;
    private final Item theItem;
    private final String name;

    ModItems(boolean enabled, Item item, String name) {
        this.isEnabled = enabled;
        theItem = item;
        this.name = name;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Item get() {
        return theItem;
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