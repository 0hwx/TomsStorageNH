package com.hepdd.toms_storage.crafting;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.github.bsideup.jabel.Desugar;
import com.hepdd.toms_storage.compat.nei.RecipeNbtSerializer;

@Desugar
public record AutoCraftRequest(NBTTagCompound recipeTag, boolean allowSubCrafting, boolean usePlayerInventory) {

    public AutoCraftRequest(NBTTagCompound recipeTag, boolean allowSubCrafting, boolean usePlayerInventory) {
        this.recipeTag = recipeTag == null ? new NBTTagCompound() : recipeTag;
        this.allowSubCrafting = allowSubCrafting;
        this.usePlayerInventory = usePlayerInventory;
    }

    public ItemStack getOutput() {
        return RecipeNbtSerializer.readOutput(recipeTag);
    }

    public int getCount() {
        return Math.max(1, recipeTag.getInteger("count"));
    }
}
