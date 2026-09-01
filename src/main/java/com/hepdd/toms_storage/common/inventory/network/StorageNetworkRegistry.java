package com.hepdd.toms_storage.common.inventory.network;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.hepdd.toms_storage.TomsConfig;
import com.hepdd.toms_storage.common.inventory.network.interfaces.StorageNetworkHost;

public final class StorageNetworkRegistry {

    private static final Map<World, StorageNetworkRegistry> INSTANCES = new IdentityHashMap<>();

    private final List<StorageNetworkHost> hosts = new ArrayList<>();
    private int sweepCursor;

    public static StorageNetworkRegistry get(World world) {
        if (world == null || world.isRemote) return null;
        return INSTANCES.computeIfAbsent(world, ignored -> new StorageNetworkRegistry());
    }

    public static void onBlockChanged(World world, int x, int y, int z) {
        StorageNetworkRegistry registry = get(world);
        if (registry == null) return;
        long pos = CoordinatePacker.pack(x, y, z);
        for (int i = 0; i < registry.hosts.size(); i++) {
            StorageNetworkHost host = registry.hosts.get(i);
            if (host.touchesPosition(pos)) host.markTopologyDirty();
        }
    }

    public static void onWorldUnload(World world) {
        INSTANCES.remove(world);
    }

    public void register(StorageNetworkHost host) {
        if (!hosts.contains(host)) hosts.add(host);
    }

    public void unregister(StorageNetworkHost host) {
        hosts.remove(host);
    }

    /** Spreads one world-wide slot budget across this world's networks, resuming where the last tick stopped. */
    public void tick() {
        int count = hosts.size();
        if (count == 0) return;
        int perNetwork = Math.max(1, TomsConfig.networkSweepSlotsPerTick / count);
        for (int i = 0; i < count; i++) {
            hosts.get(Math.floorMod(sweepCursor + i, count))
                .tickNetwork(perNetwork);
        }
        sweepCursor = Math.floorMod(sweepCursor + 1, count);
    }
}
