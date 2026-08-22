package com.hepdd.toms_storage.common.tile;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
import com.gtnewhorizon.gtnhlib.capability.Capabilities;
import com.hepdd.toms_storage.ModConfig;
import com.hepdd.toms_storage.TomsStorage;
import com.hepdd.toms_storage.common.block.BlockStorageTerminal;
import com.hepdd.toms_storage.common.inventory.CachedItemStack;
import com.hepdd.toms_storage.common.inventory.TerminalNetworkService;
import com.hepdd.toms_storage.common.inventory.network.interfaces.StorageNetwork;
import com.hepdd.toms_storage.common.item.ItemWirelessTerminal;
import com.hepdd.toms_storage.compat.mui.screen.StorageTerminalPanel;
import com.hepdd.toms_storage.utils.StorageUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityStorageTerminal extends TileEntity implements IGuiHolder<PosGuiData> {

    private SortingTypes sortType = SortingTypes.AMOUNT;
    private SortDirection sortDirection = SortDirection.ASCENDING;

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
        StorageNetwork access = getAttachedAccess();
        if (access != null) return access.getAggregatedStacks();
        IItemHandler inventory = getAttachedInventory();
        return inventory == null ? Collections.emptyList() : StorageUtils.aggregate(inventory);
    }

    /** Bumps when the network contents change; the sync handler re-sorts only when this changes. */
    public int getRevision() {
        StorageNetwork access = getAttachedAccess();
        // No connector to cache against: force a recompute each tick (single small inventory, cheap).
        return access != null ? access.getContentsRevision() : (int) worldObj.getTotalWorldTime();
    }

    /** Marks the network aggregate + this tile dirty after a contents change. */
    public void markContentsChanged() {
        StorageNetwork access = getAttachedAccess();
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

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString("sortType", sortType.name());
        tag.setString("sortDirection", sortDirection.name());
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        sortType = readEnum(tag.getString("sortType"), SortingTypes.class, SortingTypes.AMOUNT);
        sortDirection = readEnum(tag.getString("sortDirection"), SortDirection.class, SortDirection.ASCENDING);
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
        StorageNetwork network = getAttachedAccess();
        if (network != null) return network.getStorageInventory();
        return StorageUtils.asHandler(getAttachedTile(attach), attach.getOpposite());
    }

    /** A whole network if one is attached; null when the terminal sits on a plain inventory. */
    private StorageNetwork getAttachedAccess() {
        ForgeDirection attach = getAttachDirection();
        TileEntity tile = getAttachedTile(attach);
        return tile == null ? null : Capabilities.getCapability(tile, StorageNetwork.class, attach.getOpposite());
    }

    public enum TerminalSearchMode {

        STANDARD(false, false, false),
        AUTO(true, false, false),
        AUTO_KEEP(true, true, false),
        AUTO_NEI(true, true, true);

        public static final TerminalSearchMode[] VALUES = values();

        private final boolean autoFocus;
        private final boolean keepText;
        private final boolean syncNei;
        private final String key;

        TerminalSearchMode(boolean autoFocus, boolean keepText, boolean syncNei) {
            this.autoFocus = autoFocus;
            this.keepText = keepText;
            this.syncNei = syncNei;
            this.key = name().toLowerCase(Locale.ROOT);
        }

        public boolean autoFocus() {
            return autoFocus;
        }

        public boolean keepText() {
            return keepText;
        }

        public boolean syncNei() {
            return syncNei;
        }

        /** Lowercase name, used as the tooltip translation key suffix. */
        public String key() {
            return key;
        }

        public TerminalSearchMode next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }
    }

    public enum SortingTypes {

        AMOUNT,
        NAME;

        public static final SortingTypes[] VALUES = values();
    }

    public enum SortDirection {

        ASCENDING,
        DESCENDING;

        public static final SortDirection[] VALUES = values();

        public SortDirection toggle() {
            return this == ASCENDING ? DESCENDING : ASCENDING;
        }
    }
}
