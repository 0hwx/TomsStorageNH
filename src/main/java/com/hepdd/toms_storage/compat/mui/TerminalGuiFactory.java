package com.hepdd.toms_storage.compat.mui;

import java.util.Objects;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal;

public class TerminalGuiFactory extends AbstractUIFactory<PosGuiData> {

    public static final TerminalGuiFactory INSTANCE = new TerminalGuiFactory();

    private TerminalGuiFactory() {
        super("tomsstorage:terminal");
    }

    public static void register() {
        GuiManager.registerFactory(INSTANCE);
    }

    public void open(EntityPlayer player, TileEntityStorageTerminal tile) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(tile);
        if (tile.isInvalid()) throw new IllegalArgumentException("Can't open invalid TileEntity GUI!");
        if (player.worldObj != tile.getWorldObj()) {
            throw new IllegalArgumentException("TileEntity must be in same dimension as the player!");
        }
        GuiManager.open(this, new PosGuiData(player, tile.xCoord, tile.yCoord, tile.zCoord), (EntityPlayerMP) player);
    }

    @Override
    public IGuiHolder<PosGuiData> getGuiHolder(PosGuiData data) {
        return Objects.requireNonNull(castGuiHolder(data.getTileEntity()), "Found TileEntity is not a gui holder!");
    }

    // Here use the tile canInteractWith
    @Override
    public boolean canInteractWith(EntityPlayer player, PosGuiData guiData) {
        if (player != guiData.getPlayer()) return false;
        TileEntity tile = guiData.getTileEntity();
        return tile instanceof TileEntityStorageTerminal terminal && terminal.canInteractWith(player);
    }

    @Override
    public void writeGuiData(PosGuiData guiData, PacketBuffer buffer) {
        buffer.writeVarIntToBuffer(guiData.getX());
        buffer.writeVarIntToBuffer(guiData.getY());
        buffer.writeVarIntToBuffer(guiData.getZ());
    }

    @Override
    public PosGuiData readGuiData(EntityPlayer player, PacketBuffer buffer) {
        return new PosGuiData(
            player,
            buffer.readVarIntFromBuffer(),
            buffer.readVarIntFromBuffer(),
            buffer.readVarIntFromBuffer());
    }
}
