package com.hepdd.toms_storage.proxy;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.DirectionBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.hepdd.toms_storage.TomsStorage;
import com.hepdd.toms_storage.client.ConnectorRangePreview;
import com.hepdd.toms_storage.client.model.CableStateProperties;
import com.hepdd.toms_storage.client.model.TerminalStateProperties;
import com.hepdd.toms_storage.common.block.BlockStorageTerminal.MountPos;
import com.hepdd.toms_storage.network.PacketConnectorRangePreview;
import com.hepdd.toms_storage.registry.ModBlocks;

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
            BlockPropertyRegistry
                .registerProperty(ModBlocks.INVENTORY_CABLE.get(), CableStateProperties.connection(side));
        }
    }

    @Override
    public void handleConnectorRangePreview(PacketConnectorRangePreview message) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> ConnectorRangePreview.INSTANCE.showOrToggle(message));
    }
}
