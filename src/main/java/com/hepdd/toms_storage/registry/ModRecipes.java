package com.hepdd.toms_storage.registry;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ModRecipes {

    public static void init() {
        // spotless:off
        shaped(
            ModBlocks.INVENTORY_CONNECTOR.newItemStack(),
            "LLL",
            "LcL",
            "LLL",
            'L', "logWood",
            'c', Blocks.chest);

        shaped(
            ModBlocks.STORAGE_TERMINAL.newItemStack(),
            "SSS",
            "ScS",
            "SSS",
            'S', "slabWood",
            'c', Blocks.chest);

        shapeless(
            ModBlocks.CRAFTING_TERMINAL.newItemStack(),
            ModBlocks.STORAGE_TERMINAL.get(),
            Blocks.crafting_table);

        shaped(
            ModBlocks.TRIM.newItemStack(4),
            " P ",
            "PcP",
            " P ",
            'P', Blocks.planks,
            'c', Blocks.chest);

        shaped(
            ModBlocks.INVENTORY_CABLE.newItemStack(8),
            "PPP",
            "RRR",
            "PPP",
            'P', "slabWood",
            'R', Items.redstone);

        shapeless(
            ModBlocks.INVENTORY_HOPPER.newItemStack(),
            Blocks.hopper,
            ModBlocks.INVENTORY_CABLE.get());

        shaped(
            ModItems.WIRELESS_TERMINAL.newItemStack(),
            " E ",
            " T ",
            " D ",
            'E', Items.ender_pearl,
            'T', ModBlocks.STORAGE_TERMINAL.get(),
            'D', Items.diamond);
    }
    // spotless:on

    private static void shaped(ItemStack result, Object... recipe) {
        GameRegistry.addRecipe(new ShapedOreRecipe(result, recipe));
    }

    private static void shapeless(ItemStack result, Object... ingredients) {
        GameRegistry.addRecipe(new ShapelessOreRecipe(result, ingredients));
    }
}
