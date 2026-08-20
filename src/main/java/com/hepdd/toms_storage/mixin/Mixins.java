package com.hepdd.toms_storage.mixin;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {
    // spotless:off

    NEI_AUTOCRAFTING(new MixinBuilder()
            .addClientMixins("nei.AutoCraftingManagerMixin","nei.DefaultOverlayHandlerMixin")
            .addRequiredMod(TargetedMod.NEI)
            .setPhase(Phase.LATE)
    )
    ; // leave trailing semicolon
    // spotless:on

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Override
    public MixinBuilder getBuilder() {
        return this.builder;
    }
}
