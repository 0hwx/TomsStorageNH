package com.hepdd.toms_storage.client.model;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.properties.BooleanBlockProperty;
import com.hepdd.toms_storage.block.BlockInventoryCable;

public final class CableStateProperties {

    public static BooleanBlockProperty connection(ForgeDirection side) {
        return new ConnectionProperty(side);
    }

    private static final class ConnectionProperty implements BooleanBlockProperty {

        private final ForgeDirection side;
        private final String name;

        ConnectionProperty(ForgeDirection side) {
            this.side = side;
            this.name = side.name()
                .toLowerCase();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean hasTrait(BlockPropertyTrait trait) {
            return trait == BlockPropertyTrait.SupportsWorld;
        }

        @Override
        public Boolean getValue(IBlockAccess world, int x, int y, int z) {
            Block block = world.getBlock(x, y, z);
            return block instanceof BlockInventoryCable cable && cable.connectsTo(world, x, y, z, side);
        }
    }
}
