package com.hepdd.toms_storage.common.crafting.autocrafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

public final class CraftingPlan {

    private final List<CraftOperation> operations = new ArrayList<>();

    public void addOperation(Extraction[] extractions, List<ItemStack> remainders, ItemStack output, int times,
        boolean finalOutput) {
        operations.add(new CraftOperation(extractions, remainders, output, times, finalOutput));
    }

    public boolean isEmpty() {
        return operations.isEmpty();
    }

    public List<CraftOperation> getOperations() {
        return Collections.unmodifiableList(operations);
    }

    public static final class CraftOperation {

        private final Extraction[] extractions;
        private final List<ItemStack> remainders;
        private final ItemStack output;
        private final int times;
        private final boolean finalOutput;

        private CraftOperation(Extraction[] extractions, List<ItemStack> remainders, ItemStack output, int times,
            boolean finalOutput) {
            this.extractions = extractions;
            this.remainders = remainders;
            this.output = output == null ? null : output.copy();
            this.times = Math.max(1, times);
            this.finalOutput = finalOutput;
        }

        public Extraction[] getExtractions() {
            return extractions;
        }

        public List<ItemStack> getRemainders() {
            return remainders;
        }

        public ItemStack getOutput() {
            return output == null ? null : output.copy();
        }

        public int getTimes() {
            return times;
        }

        public boolean isFinalOutput() {
            return finalOutput;
        }
    }
}
