package com.hepdd.toms_storage.network;

import com.gtnewhorizon.gtnhlib.network.base.NetworkChannel;
import com.hepdd.toms_storage.TomsStorage;

public final class TomsNetwork {

    public static NetworkChannel channel = new NetworkChannel(TomsStorage.NETWORK_CHANNEL);

    public static void init() {
        channel.toClient(new PacketConnectorRangePreview());
    }
}
