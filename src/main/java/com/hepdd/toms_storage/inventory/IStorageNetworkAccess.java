package com.hepdd.toms_storage.inventory;

import java.util.List;

import com.cleanroommc.modularui.utils.item.IItemHandler;

public interface IStorageNetworkAccess {

    IItemHandler getStorageInventory();

    List<CachedItemStack> getAggregatedStacks();

    int getContentsRevision();

    void markContentsDirty();
}
