package com.hepdd.toms_storage.common.inventory.network.interfaces;

import java.util.List;

import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.hepdd.toms_storage.common.inventory.CachedItemStack;
import com.hepdd.toms_storage.common.inventory.network.NetworkGraph;

/**
 * A storage network, as seen by terminals and endpoints. Found as a capability, so a network is not
 * required to be one particular tile.
 *
 * <p>
 * The last three methods exist so {@link NetworkGraph} never has to leave the implementation.
 */
public interface StorageNetwork {

    IItemHandler getStorageInventory();

    /** Cached, de-duplicated contents of the whole network. */
    List<CachedItemStack> getAggregatedStacks();

    /** Increments only when the contents actually change, so readers can skip needless re-sorts. */
    int getContentsRevision();

    void markContentsDirty();

    /** False once the network is gone, so a bound endpoint can drop a dead reference. */
    boolean isNetworkValid();

    boolean hasEndpoint(StorageNetworkEndpoint endpoint);

    /** Whether a packed block position is part of this network. */
    boolean containsPosition(long packedPos);
}
