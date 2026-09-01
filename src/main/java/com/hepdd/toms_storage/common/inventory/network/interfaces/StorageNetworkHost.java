package com.hepdd.toms_storage.common.inventory.network.interfaces;

public interface StorageNetworkHost {

    boolean touchesPosition(long packedPos);

    void markTopologyDirty();

    void tickNetwork(int slotBudget);

    void onNetworkSlotWritten(int globalSlot);

    void flushPendingWrites();
}
