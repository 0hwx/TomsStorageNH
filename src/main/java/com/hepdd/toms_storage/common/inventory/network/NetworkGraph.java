package com.hepdd.toms_storage.common.inventory.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.gtnewhorizon.gtnhlib.capability.Capabilities;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.hepdd.toms_storage.ModConfig;
import com.hepdd.toms_storage.common.inventory.SidedInvWrapper;
import com.hepdd.toms_storage.common.inventory.network.interfaces.StorageNetworkEndpoint;
import com.hepdd.toms_storage.common.tile.TileEntityInventoryConnector;
import com.hepdd.toms_storage.registry.ModBlocks;
import com.hepdd.toms_storage.utils.StorageUtils;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/** Immutable snapshot of one connector's reachable inventories. Rebuilt only when the topology is dirty. */
public final class NetworkGraph {

    public static final NetworkGraph EMPTY = new NetworkGraph(
        new MemberRef[0],
        new int[0],
        0,
        new LongOpenHashSet(),
        Collections.emptyList(),
        false,
        false);

    private final MemberRef[] members;
    private final int[] offsets;
    private final int totalSlots;
    private final LongOpenHashSet positions;
    private final List<EndpointRef> endpoints;
    private final boolean cableScanLimitReached;
    private final boolean cableNetworkConflict;

    private NetworkGraph(MemberRef[] members, int[] offsets, int totalSlots, LongOpenHashSet positions,
        List<EndpointRef> endpoints, boolean cableScanLimitReached, boolean cableNetworkConflict) {
        this.members = members;
        this.offsets = offsets;
        this.totalSlots = totalSlots;
        this.positions = positions;
        this.endpoints = endpoints;
        this.cableScanLimitReached = cableScanLimitReached;
        this.cableNetworkConflict = cableNetworkConflict;
    }

    public MemberRef[] members() {
        return members;
    }

    public int[] offsets() {
        return offsets;
    }

    public int totalSlots() {
        return totalSlots;
    }

    public List<EndpointRef> endpoints() {
        return endpoints;
    }

    public boolean cableScanLimitReached() {
        return cableScanLimitReached;
    }

    public boolean cableNetworkConflict() {
        return cableNetworkConflict;
    }

    public boolean contains(long pos) {
        return positions.contains(pos);
    }

    /** True when the position is a member or orthogonally adjacent to one, i.e. a change there can matter. */
    public boolean touches(long pos) {
        if (positions.contains(pos)) return true;
        int x = CoordinatePacker.unpackX(pos);
        int y = CoordinatePacker.unpackY(pos);
        int z = CoordinatePacker.unpackZ(pos);
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (positions
                .contains(CoordinatePacker.pack(x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ)))
                return true;
        }
        return false;
    }

    /** Endpoints are a capability, so a tile from another mod can be one without implementing our class. */
    public static StorageNetworkEndpoint endpointAt(TileEntity tile, ForgeDirection side) {
        return tile == null ? null : Capabilities.getCapability(tile, StorageNetworkEndpoint.class, side);
    }

    public boolean hasEndpoint(StorageNetworkEndpoint endpoint) {
        for (EndpointRef ref : endpoints) {
            if (ref.endpoint == endpoint) return true;
        }
        return false;
    }

    /**
     * Whether another graph maps the same positions to the same global slot ranges. When it does, an index
     * built against that graph is still addressed correctly and does not need rebuilding.
     */
    public boolean sameLayout(NetworkGraph other) {
        if (other == null || other.totalSlots != totalSlots || other.members.length != members.length) return false;
        for (int i = 0; i < members.length; i++) {
            if (members[i].pos != other.members[i].pos || members[i].slots != other.members[i].slots) return false;
        }
        return true;
    }

    public static NetworkGraph build(TileEntityInventoryConnector owner) {
        World world = owner.getWorldObj();
        if (world == null || world.isRemote) return EMPTY;

        int originX = owner.xCoord;
        int originY = owner.yCoord;
        int originZ = owner.zCoord;
        long origin = CoordinatePacker.pack(originX, originY, originZ);

        LongArrayList toCheck = new LongArrayList();
        LongOpenHashSet checked = new LongOpenHashSet();
        // Every traversed network position (origin, cables, trims, inventories) - what touches() tests.
        LongOpenHashSet memberPositions = new LongOpenHashSet();
        List<MemberRef> members = new ArrayList<>();
        List<EndpointRef> endpoints = new ArrayList<>();
        // One backing inventory reachable from two positions must be indexed once, or the terminal doubles it.
        Set<Object> seenBacking = Collections.newSetFromMap(new IdentityHashMap<>());

        toCheck.add(origin);
        checked.add(origin);
        memberPositions.add(origin);

        int cableNodes = 0;
        boolean cableScanLimitReached = false;
        boolean cableNetworkConflict = false;
        int rangeSq = ModConfig.getInventoryConnectorRangeSquared();
        Block cableBlock = ModBlocks.INVENTORY_CABLE.get();
        Block trimBlock = ModBlocks.TRIM.get();

        int cursor = 0;
        while (cursor < toCheck.size()) {
            long current = toCheck.getLong(cursor++);
            int cx = CoordinatePacker.unpackX(current);
            int cy = CoordinatePacker.unpackY(current);
            int cz = CoordinatePacker.unpackZ(current);
            Block currentBlock = world.getBlock(cx, cy, cz);
            boolean currentIsTrim = currentBlock == trimBlock;
            boolean currentIsCable = currentBlock == cableBlock;
            boolean currentIsCableNode = currentIsCable || current == origin;

            for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
                int nx = cx + direction.offsetX;
                int ny = cy + direction.offsetY;
                int nz = cz + direction.offsetZ;
                if (!world.getChunkProvider()
                    .chunkExists(nx >> 4, nz >> 4)) continue;

                long next = CoordinatePacker.pack(nx, ny, nz);
                Block nextBlock = world.getBlock(nx, ny, nz);
                TileEntity tile = world.getTileEntity(nx, ny, nz);
                ForgeDirection access = direction.getOpposite();
                StorageNetworkEndpoint endpoint = endpointAt(tile, access);

                if (currentIsCable && tile instanceof TileEntityInventoryConnector && tile != owner) {
                    cableNetworkConflict = true;
                    continue;
                }

                if (currentIsCableNode && endpoint != null) {
                    if (endpoint.canConnectNetworkFrom(access)) {
                        endpoints.add(new EndpointRef(endpoint, access));
                    }
                    continue;
                }

                if (checked.contains(next)) continue;

                if (currentIsCableNode && nextBlock == cableBlock) {
                    if (cableNodes >= ModConfig.maxInventoryCableNodes) {
                        cableScanLimitReached = true;
                        continue;
                    }
                    checked.add(next);
                    memberPositions.add(next);
                    cableNodes++;
                    toCheck.add(next);
                    continue;
                }

                if (nextBlock == cableBlock || endpoint != null) continue;

                int dx = nx - originX;
                int dy = ny - originY;
                int dz = nz - originZ;
                if (currentIsCable || dx * dx + dy * dy + dz * dz > rangeSq) continue;
                checked.add(next);

                if (tile instanceof TileEntityInventoryConnector) continue;

                // NOTE: the TileEntityInventoryConnector skip above MUST stay above this branch -- asHandler
                // would otherwise return another connector's whole network and nest it inside this one.
                IItemHandler handler = StorageUtils.asHandler(tile, access);
                if (handler != null && (!ModConfig.onlyTrimsConnect || currentIsTrim)) {
                    Object backing = handler instanceof SidedInvWrapper wrapper ? wrapper.getInventory()
                        : handler;
                    // A duplicate still counts as a network position, so changes there still invalidate.
                    memberPositions.add(next);
                    int memberSlots = handler.getSlots();
                    // Slotless members are dropped: they contribute nothing, and an empty span would make
                    // two members share a prefix-sum offset, which a binary search cannot tell apart.
                    if (memberSlots > 0 && seenBacking.add(backing)) {
                        if (handler instanceof SidedInvWrapper wrapper) wrapper.cacheAccessibleSlots();
                        members.add(new MemberRef(next, tile, handler, memberSlots));
                    }
                    toCheck.add(next);
                    continue;
                }

                if (nextBlock == trimBlock) {
                    memberPositions.add(next);
                    toCheck.add(next);
                }
            }
        }

        MemberRef[] memberArray = members.toArray(new MemberRef[0]);
        int[] offsets = new int[memberArray.length];
        int total = 0;
        for (int i = 0; i < memberArray.length; i++) {
            offsets[i] = total;
            memberArray[i].firstSlot = total;
            total += memberArray[i].slots;
        }

        return new NetworkGraph(
            memberArray,
            offsets,
            total,
            memberPositions,
            endpoints,
            cableScanLimitReached,
            cableNetworkConflict);
    }

    public static final class MemberRef {

        public final long pos;
        public final TileEntity tile;
        public final IItemHandler handler;
        public final int slots;
        int firstSlot;

        MemberRef(long pos, TileEntity tile, IItemHandler handler, int slots) {
            this.pos = pos;
            this.tile = tile;
            this.handler = handler;
            this.slots = slots;
        }

        public int firstSlot() {
            return firstSlot;
        }

        /** A broken tile still holds its item array, so writing or reading through it would move phantom items. */
        public boolean isUsable() {
            return tile == null || !tile.isInvalid();
        }
    }

    public static final class EndpointRef {

        public final StorageNetworkEndpoint endpoint;
        public final ForgeDirection side;

        EndpointRef(StorageNetworkEndpoint endpoint, ForgeDirection side) {
            this.endpoint = endpoint;
            this.side = side;
        }
    }
}
