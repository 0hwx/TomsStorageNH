package com.hepdd.toms_storage.proxy;

import com.hepdd.toms_storage.TomsStorage;
import com.hepdd.toms_storage.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.DirectionBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.hepdd.toms_storage.block.BlockStorageTerminal.MountPos;
import com.hepdd.toms_storage.client.ConnectorRangePreview;
import com.hepdd.toms_storage.client.model.CableStateProperties;
import com.hepdd.toms_storage.client.model.TerminalStateProperties;
import com.hepdd.toms_storage.gui.ContainerStorageTerminal;
import com.hepdd.toms_storage.nei.NEIDebug;
import com.hepdd.toms_storage.network.PacketAutoCraftResult;
import com.hepdd.toms_storage.network.PacketConnectorRangePreview;
import com.hepdd.toms_storage.network.PacketTerminalData;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(ConnectorRangePreview.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(ConnectorRangePreview.INSTANCE);
        super.preInit(event);
        registerJsonModels();
    }

    private void registerJsonModels() {
        ModelRegistry.registerModid(TomsStorage.MODID);

        BlockProperty<ForgeDirection> terminalFacing = TerminalStateProperties.facing();
        BlockProperty<String> terminalPos = TerminalStateProperties.pos();
        for (Block terminal : new Block[] { ModBlocks.STORAGE_TERMINAL.get(), ModBlocks.CRAFTING_TERMINAL.get() }) {
            BlockPropertyRegistry.registerBlockItemProperty(terminal, terminalFacing, ForgeDirection.NORTH);
            BlockPropertyRegistry.registerBlockItemProperty(terminal, terminalPos, MountPos.CENTER.variant());
        }
        BlockPropertyRegistry.registerBlockItemProperty(
                ModBlocks.INVENTORY_HOPPER.get(),
            DirectionBlockProperty.facing(),
            ForgeDirection.DOWN);
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            BlockPropertyRegistry.registerProperty(ModBlocks.INVENTORY_CABLE.get(), CableStateProperties.connection(side));
        }
    }

    @Override
    public void handleTerminalData(PacketTerminalData message) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> applyTerminalData(message));
    }

    @Override
    public void handleAutoCraftResult(PacketAutoCraftResult message) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> applyAutoCraftResult(message));
    }

    private void applyTerminalData(PacketTerminalData message) {
        if (Minecraft.getMinecraft().thePlayer == null) return;
        Container container = Minecraft.getMinecraft().thePlayer.openContainer;
        if (!(container instanceof ContainerStorageTerminal)) return;
        ContainerStorageTerminal terminal = (ContainerStorageTerminal) container;
        terminal.setClientStacks(message.getStacks());
        terminal.getAutoCraftPreview()
            .clear();
        terminal.sorting = message.getSorting();
        terminal.search = message.getSearch();
    }

    private void applyAutoCraftResult(PacketAutoCraftResult message) {
        if (Minecraft.getMinecraft().thePlayer == null) return;
        Container container = Minecraft.getMinecraft().thePlayer.openContainer;
        if (container instanceof ContainerStorageTerminal) {
            if (!message.isSuccess()) NEIDebug
                .log("autocraft result failed request=" + message.getRequestId() + " reason=" + message.getReason());
            ((ContainerStorageTerminal) container).getAutoCraftPreview()
                .clearIfMatches(message.getRequestId());
        }
    }

    @Override
    public void handleConnectorRangePreview(PacketConnectorRangePreview message) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> ConnectorRangePreview.INSTANCE.showOrToggle(message));
    }
}
