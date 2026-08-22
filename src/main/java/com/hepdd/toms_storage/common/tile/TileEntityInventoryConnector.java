package com.hepdd.toms_storage.common.tile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.gtnewhorizon.gtnhlib.capability.CapabilityProvider;
import com.hepdd.toms_storage.ModConfig;
import com.hepdd.toms_storage.inventory.CachedItemStack;
import com.hepdd.toms_storage.inventory.IStorageNetworkAccess;
import com.hepdd.toms_storage.inventory.IStorageNetworkEndpoint;
import com.hepdd.toms_storage.inventory.NetworkItemHandler;
import com.hepdd.toms_storage.inventory.StorageInventoryUtils;
import com.hepdd.toms_storage.registry.ModBlocks;

public class TileEntityInventoryConnector extends TileEntity implements IStorageNetworkAccess, CapabilityProvider {

    // How often the aggregate is re-scanned for externally-changed items when nobody has dirtied it.
    private static final int CONTENT_REBUILD_INTERVAL = 10;

    private final NetworkItemHandler inventory = new NetworkItemHandler();
    private long lastScanTick = -1L;
    private boolean cableScanLimitReached;
    private boolean cableNetworkConflict;

    private List<CachedItemStack> aggregated = Collections.emptyList();
    private long lastAggregateTick = Long.MIN_VALUE;
    private boolean contentsDirty = true;
    private long contentSignature;
    private int contentRevision;

    @Override
    public IItemHandler getStorageInventory() {
        if (!worldObj.isRemote && lastScanTick != worldObj.getTotalWorldTime()) {
            scanInventories();
        }
        return inventory;
    }

    @Override
    public List<CachedItemStack> getAggregatedStacks() {
        ensureAggregated();
        return aggregated;
    }

    @Override
    public int getContentsRevision() {
        ensureAggregated();
        return contentRevision;
    }

    @Override
    public void markContentsDirty() {
        contentsDirty = true;
    }

    @Override
    public <T> T getCapability(Class<T> capability, ForgeDirection side) {
        if (capability == IItemHandler.class) return capability.cast(getStorageInventory());
        return null;
    }

    private void ensureAggregated() {
        if (worldObj == null || worldObj.isRemote) return;
        long now = worldObj.getTotalWorldTime();
        if (!contentsDirty && now - lastAggregateTick < CONTENT_REBUILD_INTERVAL) return;
        lastAggregateTick = now;
        contentsDirty = false;

        List<CachedItemStack> list = StorageInventoryUtils.aggregate(getStorageInventory());
        long signature = 0L;
        for (CachedItemStack cached : list) {
            long count = cached.count();
            signature += cached.hashCode() * 31L + (count ^ (count >>> 32));
        }
        if (signature != contentSignature) {
            contentSignature = signature;
            contentRevision++;
        }
        aggregated = list;
    }

    @Override
    public void updateEntity() {
        if (!worldObj.isRemote && worldObj.getTotalWorldTime() % 20L == 0L) {
            scanInventories();
        }
    }

    private void scanInventories() {
        lastScanTick = worldObj.getTotalWorldTime();
        inventory.clear();
        cableScanLimitReached = false;
        cableNetworkConflict = false;

        Deque<ChunkCoordinates> toCheck = new ArrayDeque<>();
        Set<String> checked = new HashSet<>();
        ChunkCoordinates start = new ChunkCoordinates(xCoord, yCoord, zCoord);
        toCheck.addLast(start);
        checked.add(getKey(start));
        int cableNodes = 0;
        List<EndpointRef> cableEndpoints = new ArrayList<>();

        while (!toCheck.isEmpty()) {
            ChunkCoordinates current = toCheck.removeFirst();
            Block currentBlock = worldObj.getBlock(current.posX, current.posY, current.posZ);
            boolean currentIsTrim = currentBlock == ModBlocks.TRIM.get();
            boolean currentIsCableNode = currentBlock == ModBlocks.INVENTORY_CABLE.get()
                || current.posX == xCoord && current.posY == yCoord && current.posZ == zCoord;

            for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
                ChunkCoordinates next = new ChunkCoordinates(
                    current.posX + direction.offsetX,
                    current.posY + direction.offsetY,
                    current.posZ + direction.offsetZ);
                String key = getKey(next);
                if (!worldObj.getChunkProvider()
                    .chunkExists(next.posX >> 4, next.posZ >> 4)) continue;

                Block nextBlock = worldObj.getBlock(next.posX, next.posY, next.posZ);
                TileEntity tile = worldObj.getTileEntity(next.posX, next.posY, next.posZ);

                if (currentBlock == ModBlocks.INVENTORY_CABLE.get() && tile instanceof TileEntityInventoryConnector
                    && tile != this) {
                    cableNetworkConflict = true;
                    continue;
                }

                if (currentIsCableNode && tile instanceof IStorageNetworkEndpoint) {
                    IStorageNetworkEndpoint endpoint = (IStorageNetworkEndpoint) tile;
                    if (endpoint.canConnectNetworkFrom(direction.getOpposite())) {
                        cableEndpoints.add(new EndpointRef(endpoint, direction.getOpposite()));
                    }
                    continue;
                }

                if (checked.contains(key)) continue;

                if (currentIsCableNode && nextBlock == ModBlocks.INVENTORY_CABLE.get()) {
                    if (cableNodes >= ModConfig.maxInventoryCableNodes) {
                        cableScanLimitReached = true;
                        continue;
                    }
                    checked.add(key);
                    cableNodes++;
                    toCheck.addLast(next);
                    continue;
                }

                if (nextBlock == ModBlocks.INVENTORY_CABLE.get() || tile instanceof IStorageNetworkEndpoint) continue;
                if (currentBlock == ModBlocks.INVENTORY_CABLE.get()
                    || getDistanceSq(next) > ModConfig.getInventoryConnectorRangeSquared()) continue;
                checked.add(key);

                if (tile instanceof TileEntityInventoryConnector) {
                    continue;
                }

                // NOTE: the TileEntityInventoryConnector skip above MUST stay above this branch -- asHandler
                // would otherwise return another connector's whole network and nest it inside this one.
                IItemHandler handler = StorageInventoryUtils.asHandler(tile, direction.getOpposite());
                if (handler != null && (!ModConfig.onlyTrimsConnect || currentIsTrim)) {
                    inventory.add(handler);
                    toCheck.addLast(next);
                    continue;
                }

                if (nextBlock == ModBlocks.TRIM.get()) {
                    toCheck.addLast(next);
                }
            }
        }

        for (EndpointRef endpoint : cableEndpoints) {
            if (cableNetworkConflict) {
                endpoint.endpoint.markNetworkConflict();
            } else {
                endpoint.endpoint.attachNetwork(this, endpoint.side);
            }
        }
    }

    public boolean isCableScanLimitReached() {
        return cableScanLimitReached;
    }

    public boolean hasCableNetworkConflict() {
        return cableNetworkConflict;
    }

    private double getDistanceSq(ChunkCoordinates coordinates) {
        double dx = coordinates.posX - xCoord;
        double dy = coordinates.posY - yCoord;
        double dz = coordinates.posZ - zCoord;
        return dx * dx + dy * dy + dz * dz;
    }

    private String getKey(ChunkCoordinates coordinates) {
        return coordinates.posX + ":" + coordinates.posY + ":" + coordinates.posZ;
    }

    private static final class EndpointRef {

        private final IStorageNetworkEndpoint endpoint;
        private final ForgeDirection side;

        private EndpointRef(IStorageNetworkEndpoint endpoint, ForgeDirection side) {
            this.endpoint = endpoint;
            this.side = side;
        }
    }
}
