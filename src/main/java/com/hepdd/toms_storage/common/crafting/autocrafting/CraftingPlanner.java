package com.hepdd.toms_storage.common.crafting.autocrafting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;

import com.hepdd.toms_storage.TomsConfig;
import com.hepdd.toms_storage.common.crafting.CraftingRemainderHandler;
import com.hepdd.toms_storage.common.crafting.CraftingStackMatcher;

public final class CraftingPlanner {

    private static final int MAX_DEPTH = 8;

    private final EntityPlayerMP player;
    private final CraftingInventorySnapshot snapshot;
    private final List<CraftingPattern> patterns;
    private final boolean allowSubCrafting;

    private final InventoryCrafting scratch = new InventoryCrafting(new Container() {

        @Override
        public boolean canInteractWith(EntityPlayer ignored) {
            return false;
        }
    }, 3, 3);

    public CraftingPlanner(EntityPlayerMP player, CraftingInventorySnapshot snapshot, List<CraftingPattern> patterns,
        boolean allowSubCrafting) {
        this.player = player;
        this.snapshot = snapshot;
        this.patterns = patterns;
        this.allowSubCrafting = allowSubCrafting;
    }

    public CraftOutcome plan(CraftingPattern root, int times) {
        CraftingPlan plan = new CraftingPlan();
        CraftOutcome outcome = planPattern(root, plan, Math.max(1, times), new ArrayDeque<>(), true);
        if (!outcome.isSuccess()) return outcome;
        return plan.isEmpty() ? CraftOutcome.failure("nothing_to_craft") : CraftOutcome.planned(plan);
    }

    private CraftOutcome planPattern(CraftingPattern pattern, CraftingPlan plan, int times, Deque<String> resolving,
        boolean finalOutput) {
        ItemStack output = pattern.getOutput();
        if (output == null || output.stackSize <= 0) return CraftOutcome.failure("missing_output");

        String key = pattern.getKey();
        if (resolving.contains(key)) return CraftOutcome.failure("recipe_cycle");
        if (resolving.size() >= MAX_DEPTH) return CraftOutcome.failure("recipe_too_deep");

        resolving.push(key);
        try {
            // A pattern that hands something back has to be run one craft at a time: the leftover appears
            // once per craft, and a tool's leftover differs each time.
            int batch = pattern.hasRemainders() ? 1 : times;
            for (int done = 0; done < times;) {
                int chunk = Math.min(batch, times - done);
                CraftOutcome operation = planOperation(pattern, plan, chunk, resolving, finalOutput);
                if (!operation.isSuccess()) {
                    // The snapshot could not fund a whole batch; a single craft may still fit.
                    if (chunk == 1) return operation;
                    batch = 1;
                    continue;
                }
                done += chunk;
            }
            return CraftOutcome.success();
        } finally {
            resolving.pop();
        }
    }

    private CraftOutcome planOperation(CraftingPattern pattern, CraftingPlan plan, int chunk, Deque<String> resolving,
        boolean finalOutput) {
        ItemStack[][] ingredients = pattern.getIngredients();
        Extraction[] extracted = new Extraction[CraftingPattern.GRID_SIZE];

        for (int slot = 0; slot < ingredients.length; slot++) {
            ItemStack[] candidates = ingredients[slot];
            if (candidates == null || candidates.length == 0) continue;

            int need = chunk * perCraftAmount(candidates);
            Extraction extraction = snapshot.reserve(candidates, need);
            if (extraction == null && chunk == 1 && allowSubCrafting) {
                if (TomsConfig.debugNeiAutocrafting) {
                    AutoCraftDebug.log(
                        "plan " + pattern.getOutput()
                            + " slot="
                            + slot
                            + " needs="
                            + need
                            + " candidates="
                            + Arrays.toString(candidates)
                            + " available "
                            + snapshot.describeAvailable(candidates));
                }
                CraftOutcome subCraft = craftMissingCandidate(candidates, plan, need, resolving);
                if (!subCraft.isSuccess()) {
                    release(extracted);
                    return subCraft;
                }
                extraction = snapshot.reserve(candidates, need);
            }
            if (extraction == null) {
                release(extracted);
                return CraftOutcome.failure("missing_ingredients");
            }
            extracted[slot] = extraction;
            if (chunk > 1 && CraftingRemainderHandler.mayReturnRemainder(extraction.getStack())) {
                release(extracted);
                return CraftOutcome.failure("requires_single_craft");
            }
        }

        ItemStack result = validate(extracted);
        if (!CraftingStackMatcher.matchesOutput(pattern.getOutput(), result)) {
            release(extracted);
            return CraftOutcome.failure("recipe_mismatch");
        }

        List<ItemStack> remainders = new ArrayList<>();
        for (Extraction extraction : extracted) {
            if (extraction == null) continue;
            ItemStack remainder = CraftingRemainderHandler.getRemainder(extraction.getStack());
            if (remainder != null) {
                remainders.add(remainder);
                snapshot.addIntermediate(remainder);
            }
        }
        if (!finalOutput) {
            ItemStack produced = result.copy();
            produced.stackSize = result.stackSize * chunk;
            snapshot.addIntermediate(produced);
        }
        plan.addOperation(extracted, remainders, result, chunk, finalOutput);
        return CraftOutcome.success();
    }

    private CraftOutcome craftMissingCandidate(ItemStack[] candidates, CraftingPlan plan, int need,
        Deque<String> resolving) {
        for (ItemStack candidate : candidates) {
            if (candidate == null) continue;
            CraftingPattern pattern = findPattern(candidate);
            if (pattern == null) continue;
            int runs = (int) Math.ceil((double) need / pattern.getOutputCount());
            return planPattern(pattern, plan, runs, resolving, false);
        }
        return CraftOutcome.failure("missing_subrecipe");
    }

    private CraftingPattern findPattern(ItemStack target) {
        for (CraftingPattern pattern : patterns) {
            ItemStack output = pattern.getOutput();
            if (output != null && CraftingStackMatcher.matchesIngredient(target, output)) return pattern;
        }
        return null;
    }

    private void release(Extraction[] extracted) {
        for (int i = 0; i < extracted.length; i++) {
            snapshot.release(extracted[i]);
            extracted[i] = null;
        }
    }

    private ItemStack validate(Extraction[] extracted) {
        for (int i = 0; i < CraftingPattern.GRID_SIZE; i++) {
            ItemStack single = null;
            if (extracted[i] != null) {
                single = extracted[i].getStack()
                    .copy();
                single.stackSize = 1;
            }
            scratch.setInventorySlotContents(i, single);
        }
        return CraftingManager.getInstance()
            .findMatchingRecipe(scratch, player.worldObj);
    }

    private static int perCraftAmount(ItemStack[] candidates) {
        for (ItemStack candidate : candidates) {
            if (candidate != null) return Math.max(1, candidate.stackSize);
        }
        return 1;
    }
}
