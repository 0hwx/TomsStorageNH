package com.hepdd.toms_storage.common.inventory.network.interfaces;

import net.minecraftforge.common.util.ForgeDirection;

public interface StorageNetworkEndpoint {

    boolean canConnectNetworkFrom(ForgeDirection side);

    void attachNetwork(StorageNetwork network, ForgeDirection side);

    void detachNetwork(StorageNetwork network);

    void markNetworkConflict();
}
