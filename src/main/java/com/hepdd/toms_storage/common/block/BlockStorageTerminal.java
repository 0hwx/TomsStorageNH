package com.hepdd.toms_storage.common.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.hepdd.toms_storage.client.utils.TooltipHelper;
import com.hepdd.toms_storage.common.inventory.network.StorageNetworkRegistry;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal;
import com.hepdd.toms_storage.compat.mui.TerminalGuiFactory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockStorageTerminal extends BlockContainer {

    public enum MountPos {

        CENTER(0, null, "center"), // wall mount, screen upright; attach direction comes from the facing
        UP(1, ForgeDirection.DOWN, "up"), // floor mount, screen tilted up
        DOWN(2, ForgeDirection.UP, "down"); // ceiling mount, screen tilted down

        private final int index;
        private final ForgeDirection attach;
        private final String variant;

        MountPos(int index, ForgeDirection attach, String variant) {
            this.index = index;
            this.attach = attach;
            this.variant = variant;
        }

        public int toMeta() {
            return index << 2;
        }

        public ForgeDirection attachDirection() {
            return attach;
        }

        public String variant() {
            return variant;
        }

        public static MountPos fromMeta(int metadata) {
            return switch ((metadata >> 2) & 3) {
                case 1 -> UP;
                case 2 -> DOWN;
                default -> CENTER;
            };
        }

        public static MountPos fromVariant(String variant) {
            for (MountPos pos : values()) {
                if (pos.variant.equals(variant)) return pos;
            }
            return CENTER;
        }
    }

    private static final ForgeDirection[] HORIZONTAL = { ForgeDirection.NORTH, ForgeDirection.EAST,
        ForgeDirection.SOUTH, ForgeDirection.WEST };

    public BlockStorageTerminal() {
        super(Material.wood);
        setBlockName("tomsstorage.storage_terminal");
        setBlockTextureName("tomsstorage:inventory_block");
        setHardness(3.0F);
        setResistance(5.0F);
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
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        setBoundsForAttach(getAttachDirection(world.getBlockMetadata(x, y, z)));
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBoundsForAttach(ForgeDirection.NORTH);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileEntityStorageTerminal();
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

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (!world.isRemote) {
            if (world.getTileEntity(x, y, z) instanceof TileEntityStorageTerminal terminal) {
                TerminalGuiFactory.INSTANCE.open(player, terminal);
            }
        }
        return true;
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
        int metadata) {
        ForgeDirection clickedSide = ForgeDirection.getOrientation(side);
        ForgeDirection attach = clickedSide.getOpposite();
        if (attach == ForgeDirection.DOWN) return MountPos.UP.toMeta();
        if (attach == ForgeDirection.UP) return MountPos.DOWN.toMeta();
        if (clickedSide.offsetY == 0 && clickedSide != ForgeDirection.UNKNOWN) return facingIndex(clickedSide);
        return metadata;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack stack) {
        int metadata = world.getBlockMetadata(x, y, z);
        MountPos pos = MountPos.fromMeta(metadata);
        if (pos != MountPos.CENTER) {
            world.setBlockMetadataWithNotify(x, y, z, pos.toMeta() | horizontalIndex(entity), 2);
        }
    }

    public static ForgeDirection getFacing(int metadata) {
        return HORIZONTAL[metadata & 3];
    }

    public static int facingIndex(ForgeDirection direction) {
        for (int i = 0; i < HORIZONTAL.length; i++) {
            if (HORIZONTAL[i] == direction) return i;
        }
        return 0;
    }

    public static ForgeDirection getAttachDirection(int metadata) {
        MountPos pos = MountPos.fromMeta(metadata);
        return pos == MountPos.CENTER ? getFacing(metadata).getOpposite() : pos.attachDirection();
    }

    private static int horizontalIndex(EntityLivingBase entity) {
        return (int) Math.floor(entity.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
    }

    private void setBoundsForAttach(ForgeDirection attach) {
        switch (attach) {
            case SOUTH:
                setBlockBounds(0.0F, 0.0F, 0.625F, 1.0F, 1.0F, 1.0F);
                break;
            case EAST:
                setBlockBounds(0.625F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                break;
            case WEST:
                setBlockBounds(0.0F, 0.0F, 0.0F, 0.375F, 1.0F, 1.0F);
                break;
            case UP:
                setBlockBounds(0.0F, 0.625F, 0.0F, 1.0F, 1.0F, 1.0F);
                break;
            case DOWN:
                setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.375F, 1.0F);
                break;
            case NORTH:
            default:
                setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.375F);
                break;
        }
    }

    public static class ItemBlockStorageTerminal extends ItemBlock {

        public ItemBlockStorageTerminal(Block block) {
            super(block);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
            String key = "tooltip.tomsstorage.storage_terminal";
            TooltipHelper.addLines(tooltip, key);
        }
    }
}
