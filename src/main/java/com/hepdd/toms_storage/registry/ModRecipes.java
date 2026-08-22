package com.hepdd.toms_storage.registry;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ModRecipes {

    public static void init() {
        // spotless:off
        shaped(
            ModBlocks.INVENTORY_CONNECTOR.newItemStack(),
            "PCP",
            "cDc",
            "PEP",
            'P', "plankWood",
            'c', Blocks.chest,
            'D', "gemDiamond",
            'C', Items.comparator,
            'E', Items.ender_pearl);

        shaped(
            ModBlocks.STORAGE_TERMINAL.newItemStack(),
            "PCP",
            "cGg",
            "PCP",
            'P', "plankWood",
            'c', Blocks.chest,
            'C', Items.comparator,
            'G', Blocks.glowstone,
            'g', "blockGlass");

        shaped(
            ModBlocks.CRAFTING_TERMINAL.newItemStack(),
            "cdc",
            "dtd",
            "cdc",
            'd', "gemDiamond",
            'c', Blocks.crafting_table,
            't', orElse(ModBlocks.STORAGE_TERMINAL.get(), Blocks.chest));

        shaped(
            ModBlocks.TRIM.newItemStack(2),
            "PSP",
            "SCS",
            "PSP",
            'P', "plankWood",
            'C', Blocks.chest,
            'S', "stickWood");

        shaped(
            ModBlocks.INVENTORY_CABLE.newItemStack(6),
            "SPS",
            "TcT",
            "SPS",
            'P', "plankWood",
            'S', "stickWood",
            'c', Blocks.chest,
            'T', Blocks.trapdoor);

        shaped(
            ModBlocks.INVENTORY_HOPPER.newItemStack(),
            "PcP",
            " H ",
            'P', "plankWood",
            'c', orElse(ModBlocks.INVENTORY_CABLE.get(), "stickWood"),
            'H', Blocks.hopper);

        shaped(
            ModItems.WIRELESS_TERMINAL.newItemStack(),
            "PCP",
            "aGg",
            "PEP",
            'P', "plankWood",
            'a', Items.ender_eye,
            'C', Items.comparator,
            'E', Items.ender_pearl,
            'G', Blocks.glowstone,
            'g', "blockGlass");
    }
    // spotless:on

    private static void shaped(ItemStack result, Object... recipe) {
        if (!enabled(result)) return;
        GameRegistry.addRecipe(new ShapedOreRecipe(result, recipe));
    }

    private static Object orElse(Object... options) {
        for (Object option : options) {
            if (enabled(option)) return option;
        }
        return options.length == 0 ? null : options[options.length - 1];
    }

    private static boolean enabled(Object part) {
        if (part == null) return false;
        if (part instanceof ItemStack stack) return enabled(stack.getItem());
        if (part instanceof Block block) {
            for (ModBlocks entry : ModBlocks.VALUES) {
                if (entry.get() == block) return entry.isEnabled();
            }
        } else if (part instanceof Item item) {
            for (ModItems entry : ModItems.VALUES) {
                if (entry.get() == item) return entry.isEnabled();
            }
        }
        return true;
    }
}
