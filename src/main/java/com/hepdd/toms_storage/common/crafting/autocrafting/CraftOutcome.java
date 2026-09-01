package com.hepdd.toms_storage.common.crafting.autocrafting;

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

    public CraftingPlan getPlan() {
        return plan;
    }
}
