package com.hepdd.toms_storage;

import static com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal.TerminalSearchMode;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.hepdd.toms_storage.common.crafting.ShiftCraftLimit;

@Config(modid = TomsStorage.MODID, filename = TomsStorage.MODID, category = "general")
public class TomsConfig {

    @Config.Comment("Inventory Connector scan range.")
    @Config.DefaultInt(16)
    @Config.RangeInt(min = 4, max = 256)
    public static int inventoryConnectorRange;

    @Config.Comment("Maximum cable blocks scanned by one Inventory Connector.")
    @Config.DefaultInt(1024)
    @Config.RangeInt(min = 16, max = 65536)
    public static int maxInventoryCableNodes;

    @Config.Comment("Only allow trims to connect inventories.")
    @Config.DefaultBoolean(false)
    public static boolean onlyTrimsConnect;

    @Config.Comment("Log NEI autocrafting integration debug output.")
    @Config.DefaultBoolean(false)
    public static boolean debugNeiAutocrafting;

    @Config.Comment("Wireless terminal reach.")
    @Config.DefaultInt(16)
    @Config.RangeInt(min = 4, max = 64)
    public static int wirelessReach;

    @Config.Comment("Ticks between full Inventory Connector topology rescans. Lower reacts faster to blocks placed by other mods, higher costs less.")
    @Config.DefaultInt(100)
    @Config.RangeInt(min = 20, max = 1200)
    public static int networkTopologyRescanInterval;

    @Config.Comment("Network slots re-read per tick per world while looking for changes made by other mods.")
    @Config.DefaultInt(512)
    @Config.RangeInt(min = 32, max = 8192)
    public static int networkSweepSlotsPerTick;

    @Config.Comment("Show storage network statistics when right-clicking an Inventory Connector.")
    @Config.DefaultBoolean(false)
    public static boolean debugNetworkStats;

    @Config.Comment({ "How far one shift-click on the crafting result may drain the storage network.",
        "ONE_STACK stops after a stack of the result; ALL_AVAILABLE crafts until the ingredients run out." })
    @Config.DefaultEnum("ONE_STACK")
    public static ShiftCraftLimit shiftCraftLimit;

    public static void register() {
        ConfigurationManager.registerConfig(TomsConfig.class);
        ConfigurationManager.registerConfig(Client.class);
    }

    public static int getInventoryConnectorRangeSquared() {
        return inventoryConnectorRange * inventoryConnectorRange;
    }

    /** Client-local terminal state. The search preset is written back when a terminal closes. */
    @Config(modid = TomsStorage.MODID, filename = TomsStorage.MODID, category = "client")
    public static final class Client {

        @Config.Comment("Terminal search preset: STANDARD, AUTO, AUTO_KEEP or AUTO_NEI.")
        @Config.DefaultEnum("AUTO_KEEP")
        public static TerminalSearchMode terminalSearchMode;

        @Config.Ignore
        public static String terminalLastSearch = "";

        public static void saveTerminalSearchSettings(TerminalSearchMode mode, String search) {
            terminalSearchMode = mode == null ? TerminalSearchMode.STANDARD : mode;
            terminalLastSearch = search == null ? "" : search;
            ConfigurationManager.save(Client.class);
        }
    }
}
