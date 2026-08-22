package com.hepdd.toms_storage.common;

import net.minecraftforge.common.MinecraftForge;

import com.hepdd.toms_storage.Tags;
import com.hepdd.toms_storage.TomsConfig;
import com.hepdd.toms_storage.TomsStorage;
import com.hepdd.toms_storage.common.inventory.network.StorageNetworkEvents;
import com.hepdd.toms_storage.compat.mui.TerminalGuiFactory;
import com.hepdd.toms_storage.network.TomsNetwork;
import com.hepdd.toms_storage.registry.ModBlocks;
import com.hepdd.toms_storage.registry.ModItems;
import com.hepdd.toms_storage.registry.ModRecipes;
import com.hepdd.toms_storage.registry.ModTileEntities;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        TomsConfig.register();
        TomsNetwork.init();
        TerminalGuiFactory.register();
        ModBlocks.init();
        ModItems.init();
        TomsStorage.LOG.info("Starting " + TomsStorage.NAME + " " + Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        ModTileEntities.init();
        ModRecipes.init();

        StorageNetworkEvents networkEvents = new StorageNetworkEvents();
        FMLCommonHandler.instance()
            .bus()
            .register(networkEvents);
        MinecraftForge.EVENT_BUS.register(networkEvents);
    }

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {}
}
