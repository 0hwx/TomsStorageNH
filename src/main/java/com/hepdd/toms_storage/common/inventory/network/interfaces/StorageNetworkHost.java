package com.hepdd.toms_storage.common.inventory.network.interfaces;

/**
 * The owner of a storage network: driven by {@code StorageNetworkRegistry} and called back by its own
 * {@code NetworkItemHandler}.
 *
 * <p>
 * Deliberately separate from {@link StorageNetwork}: this is lifecycle, not contents. Terminals hold a
 * network and must not be able to force rebuilds; the host drives rebuilds and the registry never reads items.
 */
public interface StorageNetworkHost {

    /** Whether a change at this packed position can affect the network, i.e. it is a member or next to one. */
    boolean touchesPosition(long packedPos);

    /** Rebuild the graph on the next tick. Also how the item view reports a member whose tile died. */
    void markTopologyDirty();

    /** Called once per tick with this network's share of the world sweep budget. */
    void tickNetwork(int slotBudget);

    /** A real (non-simulated) write landed on this global slot; re-read it. */
    void onNetworkSlotWritten(int globalSlot);

    /**
     * Applies writes recorded so far. Callers about to read the index must do this first, and must not do
     * it again while iterating one of its slot lists - the deferral is what keeps those lists stable.
     */
    void flushPendingWrites();
}
