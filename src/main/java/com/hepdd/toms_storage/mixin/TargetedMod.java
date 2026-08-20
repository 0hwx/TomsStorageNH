package com.hepdd.toms_storage.mixin;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

public enum TargetedMod implements ITargetMod {

    NEI("codechicken.nei.asm.NEICorePlugin", "NotEnoughItems")

    ;
    private final TargetModBuilder builder;

    TargetedMod(String coreModClass, String modId) {
        this.builder = new TargetModBuilder().setCoreModClass(coreModClass)
                .setModId(modId);
    }

    @Override
    public TargetModBuilder getBuilder() {
        return builder;
    }
}
