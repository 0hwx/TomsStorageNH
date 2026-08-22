package com.hepdd.toms_storage.common.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.hepdd.toms_storage.client.utils.TooltipHelper;
import com.hepdd.toms_storage.common.inventory.network.StorageNetworkRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockTrim extends Block {

    public BlockTrim() {
        super(Material.wood);
        setBlockName("tomsstorage.trim");
        setHardness(3.0F);
        setResistance(5.0F);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        StorageNetworkRegistry.onBlockChanged(world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        StorageNetworkRegistry.onBlockChanged(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        super.onNeighborBlockChange(world, x, y, z, neighbor);
        StorageNetworkRegistry.onBlockChanged(world, x, y, z);
    }

    public static class ItemBlockTrim extends ItemBlock {

        public ItemBlockTrim(Block block) {
            super(block);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
            String key = "tooltip.tomsstorage.trim";
            TooltipHelper.addLines(tooltip, key);
        }
    }
}
