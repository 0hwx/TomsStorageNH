package com.hepdd.toms_storage.network;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.hepdd.toms_storage.client.ConnectorRangePreview;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PacketConnectorRangePreview implements IPacket {

    private int dimension;
    private int x;
    private int y;
    private int z;
    private int radius;
    private int durationTicks;

    public PacketConnectorRangePreview() {}

    public PacketConnectorRangePreview(int dimension, int x, int y, int z, int radius, int durationTicks) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.durationTicks = durationTicks;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeInt(dimension);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(radius);
        buf.writeInt(durationTicks);
    }

    @Override
    public void decode(PacketBuffer buf) {
        dimension = buf.readInt();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        radius = buf.readInt();
        durationTicks = buf.readInt();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        ConnectorRangePreview.INSTANCE.showOrToggle(this);
        return null;
    }

    public int getDimension() {
        return dimension;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getRadius() {
        return radius;
    }

    public int getDurationTicks() {
        return durationTicks;
    }
}
