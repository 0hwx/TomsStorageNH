package com.hepdd.toms_storage.common.inventory.network.interfaces;

import java.util.List;

import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.hepdd.toms_storage.common.inventory.CachedItemStack;

public interface StorageNetwork {

    IItemHandler getStorageInventory();

    List<CachedItemStack> getAggregatedStacks();

    int getContentsRevision();

    int getTotalSlots();

    int getUsedSlots();

    long getStoredItems();

    long getItemCapacity();

    void markContentsDirty();

    boolean isNetworkValid();

    boolean hasEndpoint(StorageNetworkEndpoint endpoint);

    boolean containsPosition(long packedPos);
}
