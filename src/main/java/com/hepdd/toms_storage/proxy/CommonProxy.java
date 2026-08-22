package com.hepdd.toms_storage.proxy;

import com.hepdd.toms_storage.ModConfig;
import com.hepdd.toms_storage.ModNetwork;
import com.hepdd.toms_storage.Tags;
import com.hepdd.toms_storage.TomsStorage;
import com.hepdd.toms_storage.compat.mui.TerminalGuiFactory;
import com.hepdd.toms_storage.network.PacketConnectorRangePreview;
import com.hepdd.toms_storage.registry.ModBlocks;
import com.hepdd.toms_storage.registry.ModItems;
import com.hepdd.toms_storage.registry.ModRecipes;
import com.hepdd.toms_storage.registry.ModTileEntities;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.register();
        ModNetwork.init();
        TerminalGuiFactory.register();
        ModBlocks.init();
        ModItems.init();
        TomsStorage.LOG.info("Starting " + TomsStorage.NAME + " " + Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        ModTileEntities.init();
        ModRecipes.init();
    }

    public void postInit(FMLPostInitializationEvent event) {}

    public void handleConnectorRangePreview(PacketConnectorRangePreview message) {}

    public void serverStarting(FMLServerStartingEvent event) {}
}
