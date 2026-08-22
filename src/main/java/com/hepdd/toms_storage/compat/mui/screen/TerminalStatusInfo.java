package com.hepdd.toms_storage.compat.mui.screen;

import java.util.function.DoubleSupplier;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ProgressWidget;
import com.hepdd.toms_storage.TomsStorage;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal;

public class TerminalStatusInfo {

    public static final int WIDTH = 32;
    public static final int HEIGHT = 4;
    public static final int SPACING = HEIGHT + 1;

    public static final UITexture SLOT_PROGRESS = UITexture.builder()
        .location(TomsStorage.MODID, "gui/terminal_slots")
        .imageSize(WIDTH, HEIGHT * 2)
        .build();

    public static final UITexture STORAGE_PROGRESS = UITexture.builder()
        .location(TomsStorage.MODID, "gui/terminal_storage")
        .imageSize(WIDTH, HEIGHT * 2)
        .build();

    private final IntSyncValue usedSlots;
    private final IntSyncValue totalSlots;
    private final LongSyncValue storedItems;
    private final LongSyncValue itemCapacity;

    public TerminalStatusInfo(TileEntityStorageTerminal tile, PanelSyncManager sync, int firstSyncId) {
        this.usedSlots = new IntSyncValue(tile::getUsedSlots);
        this.totalSlots = new IntSyncValue(tile::getTotalSlots);
        this.storedItems = new LongSyncValue(tile::getStoredItems);
        this.itemCapacity = new LongSyncValue(tile::getItemCapacity);
        sync.syncValue("terminal_used_slots", firstSyncId, usedSlots);
        sync.syncValue("terminal_total_slots", firstSyncId + 1, totalSlots);
        sync.syncValue("terminal_stored_items", firstSyncId + 2, storedItems);
        sync.syncValue("terminal_item_capacity", firstSyncId + 3, itemCapacity);
    }

    public void addTo(ModularPanel panel, int x, int y) {
        panel.child(
            bar(SLOT_PROGRESS, this::slotFraction).pos(x, y)
                .tooltipAutoUpdate(true)
                .tooltipDynamic(
                    tooltip -> tooltip.addLine(
                        IKey.lang(
                            "tooltip.tomsstorage.status.slots",
                            usedSlots.getIntValue(),
                            totalSlots.getIntValue()))));

        panel.child(
            bar(STORAGE_PROGRESS, this::storageFraction).pos(x, y + SPACING)
                .tooltipAutoUpdate(true)
                .tooltipDynamic(
                    tooltip -> tooltip.addLine(
                        IKey.lang(
                            "tooltip.tomsstorage.status.storage",
                            storedItems.getLongValue(),
                            itemCapacity.getLongValue()))));
    }

    private static ProgressWidget bar(UITexture texture, DoubleSupplier fraction) {
        return new ProgressWidget().value(new DoubleValue.Dynamic(fraction, null))
            .texture(texture, WIDTH)
            .direction(ProgressWidget.Direction.RIGHT)
            .size(WIDTH, HEIGHT);
    }

    private double slotFraction() {
        int total = totalSlots.getIntValue();
        return total <= 0 ? 0.0D : (double) usedSlots.getIntValue() / total;
    }

    private double storageFraction() {
        long capacity = itemCapacity.getLongValue();
        return capacity <= 0L ? 0.0D : (double) storedItems.getLongValue() / capacity;
    }
}
