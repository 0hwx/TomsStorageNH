package com.hepdd.toms_storage.common.crafting.autocrafting;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hepdd.toms_storage.common.crafting.CraftingRemainderHandler;

public final class CraftingPattern {

    public static final int GRID_SIZE = 9;

    private final ItemStack output;
    private final ItemStack[][] ingredients;
    private final String key;
    private final boolean hasRemainders;

    public CraftingPattern(ItemStack output, ItemStack[][] ingredients) {
        this.output = output == null ? null : output.copy();
        this.ingredients = copyIngredients(ingredients);
        this.key = output == null ? "" : identity(output);
        this.hasRemainders = anyRemainder(this.ingredients);
    }

    public ItemStack getOutput() {
        return output == null ? null : output.copy();
    }

    public int getOutputCount() {
        return output == null ? 0 : Math.max(1, output.stackSize);
    }

    public ItemStack[][] getIngredients() {
        return copyIngredients(ingredients);
    }

    public String getKey() {
        return key;
    }

    public boolean hasRemainders() {
        return hasRemainders;
    }

    static String identity(ItemStack stack) {
        return Item.getIdFromItem(stack.getItem()) + ":"
            + stack.getItemDamage()
            + ":"
            + (stack.hasTagCompound() ? stack.getTagCompound()
                .toString() : "");
    }

    private static boolean anyRemainder(ItemStack[][] ingredients) {
        for (ItemStack[] candidates : ingredients) {
            for (ItemStack candidate : candidates) {
                if (candidate == null) continue;
                if (CraftingRemainderHandler.getRemainder(candidate) != null
                    || CraftingRemainderHandler.containerBreaks(candidate)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ItemStack[][] copyIngredients(ItemStack[][] source) {
        ItemStack[][] copy = new ItemStack[GRID_SIZE][];
        for (int i = 0; i < copy.length; i++) {
            ItemStack[] candidates = source == null || i >= source.length || source[i] == null ? new ItemStack[0]
                : source[i];
            copy[i] = new ItemStack[candidates.length];
            for (int j = 0; j < candidates.length; j++) {
                copy[i][j] = candidates[j] == null ? null : candidates[j].copy();
            }
        }
        return copy;
    }
}
