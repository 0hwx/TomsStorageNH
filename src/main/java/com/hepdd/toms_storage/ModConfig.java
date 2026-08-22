package com.hepdd.toms_storage;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.hepdd.toms_storage.common.search.TerminalSearchMode;

@Config(modid = TomsStorage.MODID, filename = TomsStorage.MODID, category = "general")
public class ModConfig {

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

    public static void register() {
        ConfigurationManager.registerConfig(ModConfig.class);
        ConfigurationManager.registerConfig(Client.class);
    }

    public static int getInventoryConnectorRangeSquared() {
        return inventoryConnectorRange * inventoryConnectorRange;
    }

    /** Client-local terminal state. Written back at runtime when a terminal closes. */
    @Config(modid = TomsStorage.MODID, filename = TomsStorage.MODID, category = "client")
    public static final class Client {

        @Config.Comment("Terminal search preset: STANDARD, AUTO, AUTO_KEEP or AUTO_NEI.")
        @Config.DefaultEnum("AUTO_KEEP")
        public static TerminalSearchMode terminalSearchMode;

        @Config.Comment("Last locally retained terminal search.")
        @Config.DefaultString("")
        public static String terminalLastSearch;

        @Config.Comment("Whether the old per-terminal search value has been migrated to the local setting.")
        @Config.DefaultBoolean(false)
        public static boolean terminalSearchMigrated;

        public static void saveTerminalSearchSettings(TerminalSearchMode mode, String search, boolean migrated) {
            terminalSearchMode = mode == null ? TerminalSearchMode.STANDARD : mode;
            terminalLastSearch = search == null ? "" : search;
            terminalSearchMigrated = migrated;
            ConfigurationManager.save(Client.class);
        }
    }
}
