package com.hepdd.toms_storage.common.crafting.autocrafting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record AutoCraftRequest(CraftingPattern root, List<CraftingPattern> patterns, int count,
    boolean allowSubCrafting, boolean usePlayerInventory) {

    public static AutoCraftRequest fromNbt(NBTTagCompound recipeTag, Collection<CraftingPattern> knownPatterns,
        boolean allowSubCrafting, boolean usePlayerInventory) {
        NBTTagCompound tag = recipeTag == null ? new NBTTagCompound() : recipeTag;
        CraftingPattern root = RecipeNbtSerializer.readPattern(tag);
        List<CraftingPattern> patterns = new ArrayList<>();
        RecipeNbtSerializer.addDependencies(tag, patterns);
        patterns.addAll(knownPatterns);
        patterns.add(root);
        return new AutoCraftRequest(
            root,
            patterns,
            Math.max(1, tag.getInteger("count")),
            allowSubCrafting,
            usePlayerInventory);
    }

    public ItemStack getOutput() {
        return root == null ? null : root.getOutput();
    }
}
