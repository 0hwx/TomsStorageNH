package com.hepdd.toms_storage.common.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.api.IBlockModelProvider;
import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc;
import com.hepdd.toms_storage.TomsStorage;
import com.hepdd.toms_storage.client.TooltipHelper;
import com.hepdd.toms_storage.inventory.IStorageNetworkEndpoint;
import com.hepdd.toms_storage.registry.ModBlocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockInventoryCable extends Block implements IBlockModelProvider {

    private static final float MIN = 6.0F / 16.0F;
    private static final float MAX = 10.0F / 16.0F;

    @SideOnly(Side.CLIENT)
    private static BakedModel itemModel;

    public BlockInventoryCable() {
        super(Material.wood);
        setBlockName("tomsstorage.inventory_cable");
        setHardness(1.5F);
        setResistance(3.0F);
        setBlockBounds(MIN, MIN, MIN, MAX, MAX, MAX);
    }

    public boolean connectsTo(IBlockAccess world, int x, int y, int z, ForgeDirection direction) {
        int nextX = x + direction.offsetX;
        int nextY = y + direction.offsetY;
        int nextZ = z + direction.offsetZ;
        Block block = world.getBlock(nextX, nextY, nextZ);
        if (block == ModBlocks.INVENTORY_CABLE.get() || block == ModBlocks.INVENTORY_CONNECTOR.get()) return true;
        TileEntity tile = world.getTileEntity(nextX, nextY, nextZ);
        if (tile instanceof IStorageNetworkEndpoint) {
            return ((IStorageNetworkEndpoint) tile).canConnectNetworkFrom(direction.getOpposite());
        }
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BakedModel getModel(BakedModelQuadContext context) {
        if (context instanceof BakedModelQuadContext.Item) {
            if (itemModel == null) {
                itemModel = ModelRegistry
                    .getJSONModel(new ResourceLoc.ModelLoc(TomsStorage.MODID, "block/cable/cable_item"))
                    .bake();
            }
            return itemModel;
        }
        return ModelRegistry.getBakedModel(context.getBlockState());
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        float minX = connectsTo(world, x, y, z, ForgeDirection.WEST) ? 0.0F : MIN;
        float minY = connectsTo(world, x, y, z, ForgeDirection.DOWN) ? 0.0F : MIN;
        float minZ = connectsTo(world, x, y, z, ForgeDirection.NORTH) ? 0.0F : MIN;
        float maxX = connectsTo(world, x, y, z, ForgeDirection.EAST) ? 1.0F : MAX;
        float maxY = connectsTo(world, x, y, z, ForgeDirection.UP) ? 1.0F : MAX;
        float maxZ = connectsTo(world, x, y, z, ForgeDirection.SOUTH) ? 1.0F : MAX;
        setBlockBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(MIN, MIN, MIN, MAX, MAX, MAX);
    }

    public static class ItemBlockInventoryCable extends ItemBlock {

        public ItemBlockInventoryCable(Block block) {
            super(block);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
            String key = "tooltip.tomsstorage.inventory_cable";
            TooltipHelper.addLines(tooltip, key);
        }
    }
}
