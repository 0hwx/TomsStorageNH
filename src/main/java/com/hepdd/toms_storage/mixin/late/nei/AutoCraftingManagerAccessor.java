package com.hepdd.toms_storage.mixin.late.nei;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import codechicken.nei.recipe.AutoCraftingManager;
import codechicken.nei.recipe.chain.RecipeChainMath;

@Mixin(value = AutoCraftingManager.class, remap = false)
public interface AutoCraftingManagerAccessor {

    @Accessor("math")
    static RecipeChainMath tomsstorage$getMath() {
        throw new AssertionError();
    }
}
