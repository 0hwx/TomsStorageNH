package com.hepdd.toms_storage.block;

import com.hepdd.toms_storage.client.TooltipHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hepdd.toms_storage.tile.TileEntityCraftingTerminal;

import java.util.List;

public class BlockCraftingTerminal extends BlockStorageTerminal {

    public BlockCraftingTerminal() {
        setBlockName("tomsstorage.crafting_terminal");
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileEntityCraftingTerminal();
    }

    public static class ItemBlockCraftingTerminal extends ItemBlock {

        public ItemBlockCraftingTerminal(Block block) {
            super(block);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
            String key = "tooltip.tomsstorage.crafting_terminal";
            TooltipHelper.addLines(tooltip, key);
        }
    }
}
