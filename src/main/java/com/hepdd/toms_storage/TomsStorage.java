package com.hepdd.toms_storage;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hepdd.toms_storage.common.CommonProxy;
import com.hepdd.toms_storage.registry.ModBlocks;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = TomsStorage.MODID,
    version = Tags.VERSION,
    name = TomsStorage.NAME,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:NotEnoughItems;required-after:unimixins;required-after:gtnhlib")
public class TomsStorage {

    public static final String MODID = "tomsstorage";
    public static final String NAME = "Tom's Simple Storage";
    public static final Logger LOG = LogManager.getLogger(MODID);
    public static final String NETWORK_CHANNEL = "TomsStorage";

    @SidedProxy(
        clientSide = "com.hepdd.toms_storage.client.ClientProxy",
        serverSide = "com.hepdd.toms_storage.common.CommonProxy")
    public static CommonProxy proxy;

    @Mod.Instance(MODID)
    public static TomsStorage instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(TomsStorage.MODID) {

        @Override
        public Item getTabIconItem() {
            return ModBlocks.INVENTORY_CONNECTOR.getItem();
        }
    };
}
