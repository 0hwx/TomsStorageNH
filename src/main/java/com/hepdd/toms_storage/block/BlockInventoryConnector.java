package com.hepdd.toms_storage.block;

import com.hepdd.toms_storage.client.TooltipHelper;
import com.hepdd.toms_storage.registry.ModBlocks;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import com.hepdd.toms_storage.Config;
import com.hepdd.toms_storage.ModNetwork;
import com.hepdd.toms_storage.inventory.IStorageInventory;
import com.hepdd.toms_storage.network.PacketConnectorRangePreview;
import com.hepdd.toms_storage.tile.TileEntityInventoryConnector;

import java.util.List;

public class BlockInventoryConnector extends BlockContainer {

    public BlockInventoryConnector() {
        super(Material.wood);
        setBlockName("tomsstorage.inventory_connector");
        setHardness(3.0F);
        setResistance(5.0F);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileEntityInventoryConnector();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        ItemStack held = player.getHeldItem();
        if (held != null && held.getItem() == Item.getItemFromBlock(ModBlocks.INVENTORY_CABLE.get())) {
            return false;
        }
        if (!world.isRemote) {
            if (player.isSneaking() && player.getHeldItem() == null && player instanceof EntityPlayerMP) {
                ModNetwork.channel.sendTo(
                    new PacketConnectorRangePreview(
                        world.provider.dimensionId,
                        x,
                        y,
                        z,
                        Config.inventoryConnectorRange,
                        200),
                    (EntityPlayerMP) player);
                return true;
            }
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileEntityInventoryConnector) {
                IStorageInventory inventory = ((TileEntityInventoryConnector) tile).getStorageInventory();
                int total = inventory.getSlots();
                int free = 0;
                for (int i = 0; i < total; i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (stack == null || stack.stackSize <= 0) free++;
                }
                player.addChatMessage(
                    new ChatComponentTranslation("message.tomsstorage.inventory_connector.slots", free, total));
                if (((TileEntityInventoryConnector) tile).isCableScanLimitReached()) {
                    player.addChatMessage(
                        new ChatComponentTranslation(
                            "message.tomsstorage.inventory_connector.cable_limit",
                            Config.maxInventoryCableNodes));
                }
                if (((TileEntityInventoryConnector) tile).hasCableNetworkConflict()) {
                    player.addChatMessage(
                        new ChatComponentTranslation("message.tomsstorage.inventory_connector.cable_conflict"));
                }
            }
        }
        return true;
    }

    public static class ItemBlockInventoryConnector extends ItemBlock {

        public ItemBlockInventoryConnector(Block block) {
            super(block);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
            String key = "tooltip.tomsstorage.inventory_connector";
            TooltipHelper.addLines(tooltip, key);
            tooltip.add(I18n.format("tooltip.tomsstorage.inventory_connector.range", Config.inventoryConnectorRange));
            tooltip.add(I18n.format("tooltip.tomsstorage.inventory_connector.range_cable"));
        }
    }
}
