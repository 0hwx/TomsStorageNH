package com.hepdd.toms_storage.common.inventory.network;

import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class StorageNetworkEvents {

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world == null || event.world.isRemote) return;
        StorageNetworkRegistry registry = StorageNetworkRegistry.get(event.world);
        if (registry != null) registry.tick();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        StorageNetworkRegistry.onWorldUnload(event.world);
    }
}
