package com.hepdd.toms_storage.client.model;

import java.lang.reflect.Type;

import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.MetaBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.DirectionBlockProperty;
import com.hepdd.toms_storage.block.BlockStorageTerminal;
import com.hepdd.toms_storage.block.BlockStorageTerminal.MountPos;

public final class TerminalStateProperties {

    public static BlockProperty<ForgeDirection> facing() {
        return DirectionBlockProperty.facing(3, BlockStorageTerminal::facingIndex, BlockStorageTerminal::getFacing);
    }

    public static BlockProperty<String> pos() {
        return new PosProperty();
    }

    private static final class PosProperty implements MetaBlockProperty<String> {

        @Override
        public String getName() {
            return "pos";
        }

        @Override
        public Type getType() {
            return String.class;
        }

        @Override
        public boolean hasTrait(BlockPropertyTrait trait) {
            return trait == BlockPropertyTrait.SupportsWorld || trait == BlockPropertyTrait.OnlyNeedsMeta;
        }

        @Override
        public int getMeta(String value, int existing) {
            return (existing & 3) | MountPos.fromVariant(value)
                .toMeta();
        }

        @Override
        public String getValue(int meta) {
            return MountPos.fromMeta(meta)
                .variant();
        }

        @Override
        public String stringify(String value) {
            return value;
        }
    }
}
