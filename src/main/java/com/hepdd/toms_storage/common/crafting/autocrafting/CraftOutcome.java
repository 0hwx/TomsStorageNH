package com.hepdd.toms_storage.common.crafting.autocrafting;

/**
 * The result of planning or applying an auto-craft. One type for both halves: they fail for the same
 * kinds of reason and the caller treats them the same way.
 *
 * <p>
 * {@code reason} is a stable identifier, not a message - it goes to {@code AutoCraftDebug} and could back a
 * lang key later.
 */
public final class CraftOutcome {

    private final boolean success;
    private final String reason;
    private final CraftingPlan plan;

    private CraftOutcome(boolean success, String reason, CraftingPlan plan) {
        this.success = success;
        this.reason = reason;
        this.plan = plan;
    }

    public static CraftOutcome success() {
        return new CraftOutcome(true, "", null);
    }

    public static CraftOutcome planned(CraftingPlan plan) {
        return new CraftOutcome(true, "", plan);
    }

    public static CraftOutcome failure(String reason) {
        return new CraftOutcome(false, reason, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReason() {
        return reason;
    }

    /** The plan, on a successful plan outcome; null otherwise. */
    public CraftingPlan getPlan() {
        return plan;
    }
}
