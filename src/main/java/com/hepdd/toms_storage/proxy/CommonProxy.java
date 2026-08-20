package com.hepdd.toms_storage.proxy;

import com.hepdd.toms_storage.Config;
import com.hepdd.toms_storage.GuiHandler;
import com.hepdd.toms_storage.ModNetwork;
import com.hepdd.toms_storage.Tags;
import com.hepdd.toms_storage.TomsStorage;
import com.hepdd.toms_storage.network.PacketAutoCraftResult;
import com.hepdd.toms_storage.network.PacketConnectorRangePreview;
import com.hepdd.toms_storage.network.PacketTerminalData;

import com.hepdd.toms_storage.registry.ModBlocks;
import com.hepdd.toms_storage.registry.ModItems;
import com.hepdd.toms_storage.registry.ModRecipes;
import com.hepdd.toms_storage.registry.ModTileEntities;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        ModNetwork.init();
        ModBlocks.init();
        ModItems.init();
        TomsStorage.LOG.info("Starting " + TomsStorage.NAME + " " + Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        ModTileEntities.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(TomsStorage.instance, new GuiHandler());
        ModRecipes.init();
    }

    public void postInit(FMLPostInitializationEvent event) {}

    public void handleTerminalData(PacketTerminalData message) {}

    public void handleAutoCraftResult(PacketAutoCraftResult message) {}

    public void handleConnectorRangePreview(PacketConnectorRangePreview message) {}

    public void serverStarting(FMLServerStartingEvent event) {}
}
