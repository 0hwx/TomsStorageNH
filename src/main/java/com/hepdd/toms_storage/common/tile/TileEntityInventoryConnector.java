package com.hepdd.toms_storage.common.tile;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.gtnewhorizon.gtnhlib.capability.CapabilityProvider;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.hepdd.toms_storage.TomsConfig;
import com.hepdd.toms_storage.common.inventory.CachedItemStack;
import com.hepdd.toms_storage.common.inventory.NetworkItemHandler;
import com.hepdd.toms_storage.common.inventory.network.NetworkGraph;
import com.hepdd.toms_storage.common.inventory.network.NetworkItemIndex;
import com.hepdd.toms_storage.common.inventory.network.StorageNetworkRegistry;
import com.hepdd.toms_storage.common.inventory.network.interfaces.StorageNetwork;
import com.hepdd.toms_storage.common.inventory.network.interfaces.StorageNetworkEndpoint;
import com.hepdd.toms_storage.common.inventory.network.interfaces.StorageNetworkHost;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class TileEntityInventoryConnector extends TileEntity
    implements StorageNetwork, StorageNetworkHost, CapabilityProvider {

    // Floor between dirty-driven rebuilds, so repeated neighbour changes cannot thrash the BFS.
    private static final int MIN_REBUILD_INTERVAL = 5;

    private final NetworkItemHandler inventory = new NetworkItemHandler();
    private final NetworkItemIndex index = new NetworkItemIndex();
    // Writes are recorded and applied in a batch, so the index's slot lists stay stable while an
    // insert or extract is iterating them.
    private final IntArrayList pendingSlots = new IntArrayList();
    private NetworkGraph graph = NetworkGraph.EMPTY;
    // Not Long.MIN_VALUE: "now - lastScanTick" would overflow. The initial build comes from topologyDirty.
    private long lastScanTick = -1L;
    private boolean topologyDirty = true;
    private boolean registered;
    private List<NetworkGraph.EndpointRef> attachedEndpoints = Collections.emptyList();
    private int sweepCursor;
    private long lastRebuildNanos;
    private long totalCapacity;

    @Override
    public IItemHandler getStorageInventory() {
        return inventory;
    }

    @Override
    public List<CachedItemStack> getAggregatedStacks() {
        flushPendingWrites();
        return index.aggregate();
    }

    @Override
    public int getContentsRevision() {
        flushPendingWrites();
        return index.revision();
    }

    /**
     * Writes we cause already update the index exactly, so this only asks the sweep to start over - a
     * cheap way for a caller that suspects a foreign change to get an early full pass.
     */
    @Override
    public void markContentsDirty() {
        sweepCursor = 0;
    }

    // No StorageNetwork branch: this tile implements it, so Capabilities' direct cast answers first.
    @Override
    public <T> T getCapability(Class<T> capability, ForgeDirection side) {
        if (capability == IItemHandler.class) return capability.cast(getStorageInventory());
        return null;
    }

    @Override
    public boolean isNetworkValid() {
        return !isInvalid();
    }

    @Override
    public boolean hasEndpoint(StorageNetworkEndpoint endpoint) {
        return graph.hasEndpoint(endpoint);
    }

    @Override
    public boolean containsPosition(long packedPos) {
        return graph.contains(packedPos);
    }

    public NetworkItemIndex getIndex() {
        flushPendingWrites();
        return index;
    }

    /** Applies writes recorded since the last flush. Cheap: one slot re-read each. */
    @Override
    public void flushPendingWrites() {
        if (pendingSlots.isEmpty()) return;
        for (int i = 0; i < pendingSlots.size(); i++) {
            int slot = pendingSlots.getInt(i);
            index.applySlot(slot, inventory.getStackInSlot(slot));
        }
        pendingSlots.clear();
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;
        if (!registered) {
            // Registering here rather than in validate(): worldObj is not reliably usable that early.
            StorageNetworkRegistry registry = StorageNetworkRegistry.get(worldObj);
            if (registry != null) registry.register(this);
            registered = true;
        }
        long now = worldObj.getTotalWorldTime();
        long sinceLast = now - lastScanTick;
        // Coalesce dirty bursts: a redstone clock beside a hopper fires onNeighborBlockChange every tick,
        // and rebuilding the whole graph that often would undo the point of this rework.
        if (topologyDirty && sinceLast >= MIN_REBUILD_INTERVAL
            || sinceLast >= TomsConfig.networkTopologyRescanInterval) {
            rebuildGraph();
        }
    }

    /** One tick's share of the world sweep budget: re-read slots so foreign changes are picked up. */
    @Override
    public void tickNetwork(int slotBudget) {
        if (worldObj == null || worldObj.isRemote) return;
        flushPendingWrites();
        int total = graph.totalSlots();
        if (total <= 0 || slotBudget <= 0) return;

        int budget = Math.min(slotBudget, total);
        int end = sweepCursor + budget;
        for (int i = sweepCursor; i < end; i++) {
            int slot = i >= total ? i - total : i;
            ItemStack stack = inventory.getStackInSlot(slot);
            // That read hit a dead member and asked for a rebuild; recording nulls now would blank
            // the member's items until the rebuild lands.
            if (topologyDirty) return;
            index.applySlot(slot, stack);
        }

        if (end >= total) {
            // A member that changed size invalidates the prefix sums, so a full pass also checks them.
            for (NetworkGraph.MemberRef member : graph.members()) {
                if (!member.isUsable() || member.handler.getSlots() != member.slots) {
                    topologyDirty = true;
                    break;
                }
            }
        }
        sweepCursor = end % total;
    }

    @Override
    public void markTopologyDirty() {
        topologyDirty = true;
    }

    @Override
    public boolean touchesPosition(long packedPos) {
        return graph.touches(packedPos);
    }

    @Override
    public void onNetworkSlotWritten(int globalSlot) {
        pendingSlots.add(globalSlot);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        unregister();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        unregister();
    }

    private void unregister() {
        if (!registered || worldObj == null) return;
        for (NetworkGraph.EndpointRef endpoint : attachedEndpoints) {
            endpoint.endpoint.detachNetwork(this);
        }
        attachedEndpoints = Collections.emptyList();
        StorageNetworkRegistry registry = StorageNetworkRegistry.get(worldObj);
        if (registry != null) registry.unregister(this);
        registered = false;
    }

    private void rebuildGraph() {
        long start = System.nanoTime();
        topologyDirty = false;
        lastScanTick = worldObj.getTotalWorldTime();
        NetworkGraph previousGraph = graph;
        graph = NetworkGraph.build(this);
        inventory.setGraph(graph);
        inventory.setHost(this);
        inventory.setIndex(index);

        // Only repopulate when the slot layout actually moved. The periodic rescan usually finds an
        // identical layout, and rebuilding the index there would bump the revision every time - making
        // every open terminal re-sort and resend its whole list on a timer, which is the cost this
        // rework exists to remove. A layout match keeps every global slot id valid; if a member's tile
        // was swapped for an identical one, the sweep corrects its contents within one pass.
        if (!graph.sameLayout(previousGraph)) {
            pendingSlots.clear();
            int total = graph.totalSlots();
            index.resize(total);
            for (int slot = 0; slot < total; slot++) {
                index.applySlot(slot, inventory.getStackInSlot(slot));
            }
            sweepCursor = 0;
        }

        List<NetworkGraph.EndpointRef> current = graph.endpoints();
        for (NetworkGraph.EndpointRef previous : attachedEndpoints) {
            if (!graph.hasEndpoint(previous.endpoint)) previous.endpoint.detachNetwork(this);
        }
        for (NetworkGraph.EndpointRef endpoint : current) {
            if (graph.cableNetworkConflict()) {
                endpoint.endpoint.markNetworkConflict();
            } else {
                endpoint.endpoint.attachNetwork(this, endpoint.side);
            }
        }
        attachedEndpoints = current;
        recalculateCapacity();
        lastRebuildNanos = System.nanoTime() - start;
    }

    /**
     * Sums every member's own slot limit, so a drawer or barrel contributes what it can really hold rather
     * than a flat 64. Refreshed on rebuild rather than on read: the periodic rescan bounds how long a
     * capacity upgrade takes to show up, and this walks every slot.
     */
    private void recalculateCapacity() {
        long capacity = 0L;
        for (NetworkGraph.MemberRef member : graph.members()) {
            for (int slot = 0; slot < member.slots; slot++) {
                capacity += member.handler.getSlotLimit(slot);
            }
        }
        totalCapacity = capacity;
    }

    /** Names the member owning a global slot, so a drift report says which inventory is involved. */
    private String describeMember(int globalSlot) {
        if (globalSlot < 0) return "?";
        for (NetworkGraph.MemberRef member : graph.members()) {
            if (globalSlot >= member.firstSlot() && globalSlot < member.firstSlot() + member.slots) {
                return member.tile.getClass()
                    .getSimpleName() + "("
                    + CoordinatePacker.unpackX(member.pos)
                    + ","
                    + CoordinatePacker.unpackY(member.pos)
                    + ","
                    + CoordinatePacker.unpackZ(member.pos)
                    + ")";
            }
        }
        return "?";
    }

    /** Total slots across the network, for the connector's chat readout and the terminal's status bar. */
    @Override
    public int getTotalSlots() {
        return graph.totalSlots();
    }

    @Override
    public int getUsedSlots() {
        flushPendingWrites();
        return graph.totalSlots() - index.emptySlotCount();
    }

    @Override
    public long getStoredItems() {
        flushPendingWrites();
        return index.totalCount();
    }

    @Override
    public long getItemCapacity() {
        return totalCapacity;
    }

    /** Debug readout, so the rework's effect can be measured rather than asserted. */
    public String getNetworkStats() {
        flushPendingWrites();
        String drift = index.verifyAgainst(inventory);
        if (!drift.isEmpty()) drift += " at=" + describeMember(index.lastBadSlot());
        return String.format(
            Locale.ROOT,
            "members=%d slots=%d free=%d items=%d rebuild=%.2fms sweep=%d check=%s",
            graph.members().length,
            graph.totalSlots(),
            index.emptySlotCount(),
            index.distinctItems(),
            lastRebuildNanos / 1_000_000.0D,
            sweepCursor,
            drift.isEmpty() ? "ok" : drift);
    }

    public boolean isCableScanLimitReached() {
        return graph.cableScanLimitReached();
    }

    public boolean hasCableNetworkConflict() {
        return graph.cableNetworkConflict();
    }
}
