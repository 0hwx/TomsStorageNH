package com.hepdd.toms_storage.block;

import com.hepdd.toms_storage.client.TooltipHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import java.util.List;

public class BlockTrim extends Block {

    public BlockTrim() {
        super(Material.wood);
        setBlockName("tomsstorage.trim");
        setHardness(3.0F);
        setResistance(5.0F);
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
