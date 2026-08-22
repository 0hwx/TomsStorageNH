package com.hepdd.toms_storage.common.tile;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.hepdd.toms_storage.ModConfig;
import com.hepdd.toms_storage.TomsStorage;
import com.hepdd.toms_storage.common.block.BlockStorageTerminal;
import com.hepdd.toms_storage.common.item.ItemWirelessTerminal;
import com.hepdd.toms_storage.common.sort.SortDirection;
import com.hepdd.toms_storage.common.sort.SortingTypes;
import com.hepdd.toms_storage.compat.mui.screen.StorageTerminalPanel;
import com.hepdd.toms_storage.inventory.CachedItemStack;
import com.hepdd.toms_storage.inventory.IStorageNetworkAccess;
import com.hepdd.toms_storage.inventory.StorageInventoryUtils;
import com.hepdd.toms_storage.inventory.TerminalNetworkService;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityStorageTerminal extends TileEntity implements IGuiHolder<PosGuiData> {

    private SortingTypes sortType = SortingTypes.AMOUNT;
    private SortDirection sortDirection = SortDirection.ASCENDING;
    private String lastSearch = "";

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new StorageTerminalPanel(this, sync, settings);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PosGuiData data, ModularPanel mainPanel) {
        return new ModularScreen(TomsStorage.MODID, mainPanel);
    }

    /** MUI2 terminal contents: the network's shared, cached aggregate (no per-terminal scan). */
    public List<CachedItemStack> getCachedStacks() {
        IStorageNetworkAccess access = getAttachedAccess();
        if (access != null) return access.getAggregatedStacks();
        IItemHandler inventory = getAttachedInventory();
        return inventory == null ? Collections.emptyList() : StorageInventoryUtils.aggregate(inventory);
    }

    /** Bumps when the network contents change; the sync handler re-sorts only when this changes. */
    public int getRevision() {
        IStorageNetworkAccess access = getAttachedAccess();
        // No connector to cache against: force a recompute each tick (single small inventory, cheap).
        return access != null ? access.getContentsRevision() : (int) worldObj.getTotalWorldTime();
    }

    /** Marks the network aggregate + this tile dirty after a contents change. */
    public void markContentsChanged() {
        IStorageNetworkAccess access = getAttachedAccess();
        if (access != null) access.markContentsDirty();
        markDirty();
    }

    public ItemStack pushStack(ItemStack stack) {
        return TerminalNetworkService.push(this, stack);
    }

    public void pushOrDrop(ItemStack stack) {
        TerminalNetworkService.pushOrDrop(this, stack);
    }

    public boolean canInteractWith(EntityPlayer player) {
        if (worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) return false;
        double maxDistance = ItemWirelessTerminal.isPlayerHolding(player)
            ? ModConfig.wirelessReach * ModConfig.wirelessReach
            : 64.0D;
        return player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= maxDistance;
    }

    public SortingTypes getSortType() {
        return sortType;
    }

    public void setSortType(SortingTypes sortType) {
        this.sortType = sortType;
        markDirty();
    }

    public SortDirection getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(SortDirection sortDirection) {
        this.sortDirection = sortDirection;
        markDirty();
    }

    public String getLastSearch() {
        return lastSearch;
    }

    public void setLastSearch(String lastSearch) {
        this.lastSearch = lastSearch == null ? "" : lastSearch;
        markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString("sortType", sortType.name());
        tag.setString("sortDirection", sortDirection.name());
        tag.setString("lastSearch", lastSearch);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        sortType = readEnum(tag.getString("sortType"), SortingTypes.class, SortingTypes.AMOUNT);
        sortDirection = readEnum(tag.getString("sortDirection"), SortDirection.class, SortDirection.ASCENDING);
        lastSearch = tag.getString("lastSearch");
    }

    private static <E extends Enum<E>> E readEnum(String name, Class<E> type, E fallback) {
        if (name == null || name.isEmpty()) return fallback;
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private ForgeDirection getAttachDirection() {
        return BlockStorageTerminal.getAttachDirection(worldObj.getBlockMetadata(xCoord, yCoord, zCoord));
    }

    private TileEntity getAttachedTile(ForgeDirection attach) {
        return worldObj.getTileEntity(xCoord + attach.offsetX, yCoord + attach.offsetY, zCoord + attach.offsetZ);
    }

    public IItemHandler getAttachedInventory() {
        ForgeDirection attach = getAttachDirection();
        TileEntity tile = getAttachedTile(attach);
        // Direct call for the connector: skips a capability dispatch on the hot path and keeps intent obvious.
        if (tile instanceof TileEntityInventoryConnector connector) {
            return connector.getStorageInventory();
        }
        return StorageInventoryUtils.asHandler(tile, attach.getOpposite());
    }

    private IStorageNetworkAccess getAttachedAccess() {
        TileEntity tile = getAttachedTile(getAttachDirection());
        return tile instanceof IStorageNetworkAccess ? (IStorageNetworkAccess) tile : null;
    }
}
