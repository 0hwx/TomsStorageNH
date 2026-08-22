package com.hepdd.toms_storage.common.inventory.network.interfaces;

import net.minecraftforge.common.util.ForgeDirection;

/** A tile that a cable can bind to a storage network, such as the Basic Inventory Hopper. */
public interface StorageNetworkEndpoint {

    boolean canConnectNetworkFrom(ForgeDirection side);

    void attachNetwork(StorageNetwork network, ForgeDirection side);

    /** Called when a rebuild finds this endpoint is no longer on the network. */
    void detachNetwork(StorageNetwork network);

    void markNetworkConflict();
}
