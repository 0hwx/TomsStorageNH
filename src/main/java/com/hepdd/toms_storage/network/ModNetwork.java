package com.hepdd.toms_storage.network;

import com.hepdd.toms_storage.TomsStorage;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class ModNetwork {

    public static SimpleNetworkWrapper channel;

    private ModNetwork() {}

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(TomsStorage.NETWORK_CHANNEL);
        channel.registerMessage(
            PacketConnectorRangePreview.Handler.class,
            PacketConnectorRangePreview.class,
            0,
            Side.CLIENT);
    }
}
