package com.hepdd.toms_storage.compat.mui.screen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.DrawableStack;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandlers;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.hepdd.toms_storage.TomsConfig;
import com.hepdd.toms_storage.common.inventory.TerminalNetworkService;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal.SortDirection;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal.SortingTypes;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal.TerminalSearchMode;
import com.hepdd.toms_storage.compat.mui.container.StorageTerminalContainer;
import com.hepdd.toms_storage.compat.mui.slot.ItemSlotPlayer;
import com.hepdd.toms_storage.compat.mui.slot.ItemSlotTerminal;
import com.hepdd.toms_storage.compat.mui.sync.TerminalGridSyncHandler;

public class StorageTerminalPanel extends ModularPanel {

    protected static final int GRID_X = 7;
    protected static final int GRID_Y = 17;
    protected static final int SLOT = 18;

    private static final int STATUS_X = 8;
    private static final int STATUS_Y = 6;

    private static final int SYNC_STATUS = 4;

    protected final TileEntityStorageTerminal tile;

    protected final String textureName;

    private TerminalSearchMode searchMode = TomsConfig.Client.terminalSearchMode;
    private final TerminalGridSyncHandler grid;

    public StorageTerminalPanel(TileEntityStorageTerminal tile, PanelSyncManager sync, UISettings settings) {
        this("storage_terminal", 194, 202, 119, tile, sync, settings);
    }

    protected StorageTerminalPanel(String name, int width, int height, int playerInvY, TileEntityStorageTerminal tile,
        PanelSyncManager sync, UISettings settings) {
        super(name);
        this.tile = tile;
        this.textureName = name;
        size(width, height);
        background(backgroundTexture(name, width, height)).disableHoverThemeBackground(true);
        wire(sync, settings);

        ItemStackHandler view = new ItemStackHandler(TerminalGridSyncHandler.SLOTS);
        TerminalGridSyncHandler grid = new TerminalGridSyncHandler(tile);
        this.grid = grid;
        sync.syncValue(TerminalGridSyncHandler.SYNC_KEY, TerminalGridSyncHandler.SYNC_ID, grid);

        addGridSlots(grid, view);
        addSearchField(grid);
        addSortButtons(sync);
        addScrollBar(grid);
        new TerminalStatusInfo(tile, sync, SYNC_STATUS).addTo(this, STATUS_X, STATUS_Y);
        child(playerInventory().pos(GRID_X, playerInvY));
    }

    private void wire(PanelSyncManager sync, UISettings settings) {
        settings.customContainer(this::createContainer);
        sync.registerServerSyncedAction(ItemSlotPlayer.DEPOSIT_MATCHING, packet -> {
            ItemStack template = NetworkUtils.readItemStack(packet);
            EntityPlayer player = sync.getPlayer();
            if (template != null && player instanceof EntityPlayerMP mp) {
                TerminalNetworkService.depositMatching(tile, mp, template);
                sync.getContainer()
                    .detectAndSendChanges();
            }
        });
    }

    protected TerminalGridSyncHandler grid() {
        return grid;
    }

    protected ModularContainer createContainer() {
        return new StorageTerminalContainer(tile);
    }

    private void addGridSlots(TerminalGridSyncHandler grid, ItemStackHandler view) {
        for (int row = 0; row < TerminalGridSyncHandler.ROWS; row++) {
            for (int col = 0; col < TerminalGridSyncHandler.COLUMNS; col++) {
                int index = row * TerminalGridSyncHandler.COLUMNS + col;
                child(
                    new ItemSlotTerminal(grid, index, view).invisible()
                        .pos(GRID_X + col * SLOT, GRID_Y + row * SLOT));
            }
        }
    }

    private void addSearchField(TerminalGridSyncHandler grid) {
        if (searchMode.keepText()) grid.setSearchText(TomsConfig.Client.terminalLastSearch);
        child(
            new TerminalSearchField(() -> searchMode.syncNei())
                .value(new StringValue.Dynamic(grid::getSearchText, grid::setSearchText))
                .autoUpdateOnChange(true)
                .setFocusOnGuiOpen(searchMode.autoFocus())
                .padding(3, 0)
                .invisible()
                .pos(79, 4)
                .size(90, 12));
    }

    private void cycleSearchMode() {
        searchMode = searchMode.next();
        saveSearchSettings();
    }

    private void saveSearchSettings() {
        String retained = searchMode.keepText() ? grid.getSearchText() : "";
        TomsConfig.Client.saveTerminalSearchSettings(searchMode, retained);
    }

    @Override
    public void onClose() {
        super.onClose();
        saveSearchSettings();
        TerminalSearchField.restoreNeiSearch();
    }

    private void addSortButtons(PanelSyncManager sync) {
        EnumSyncValue<SortingTypes, ?> sortType = SyncHandlers
            .enumValue(SortingTypes.class, tile::getSortType, tile::setSortType);
        sortType.allowC2S(true);
        sync.syncValue("terminal_sort_type", 1, sortType);

        EnumSyncValue<SortDirection, ?> sortDirection = SyncHandlers
            .enumValue(SortDirection.class, tile::getSortDirection, tile::setSortDirection);
        sortDirection.allowC2S(true);
        sync.syncValue("terminal_sort_direction", 2, sortDirection);

        child(new ButtonWidget<>().onMousePressed(mouseButton -> {
            SortingTypes[] values = SortingTypes.VALUES;
            sortType.setValue(
                values[(sortType.getValue()
                    .ordinal() + 1) % values.length],
                true,
                true);
            return true;
        })
            .background(
                new DynamicDrawable(
                    () -> buttonTexture(
                        194 + sortType.getValue()
                            .ordinal() * 16,
                        30,
                        16,
                        16)))
            .tooltipDynamic(
                t -> t.addLine(
                    IKey.lang(
                        "tooltip.tomsstorage.sorting." + sortType.getValue()
                            .ordinal())))
            .tooltipAutoUpdate(true)
            .pos(-18, 5)
            .size(16, 16));

        child(new ButtonWidget<>().onMousePressed(mouseButton -> {
            sortDirection.setValue(
                sortDirection.getValue()
                    .toggle(),
                true,
                true);
            return true;
        })
            .background(
                new DynamicDrawable(
                    () -> buttonTexture(
                        194 + (sortDirection.getValue() == SortDirection.DESCENDING ? 16 : 0),
                        46,
                        16,
                        16)))
            .tooltipDynamic(
                t -> t.addLine(
                    IKey.lang(
                        "tooltip.tomsstorage.direction."
                            + (sortDirection.getValue() == SortDirection.DESCENDING ? 1 : 0))))
            .tooltipAutoUpdate(true)
            .pos(-18, 23)
            .size(16, 16));

        child(new ButtonWidget<>().onMousePressed(mouseButton -> {
            cycleSearchMode();
            return true;
        })
            .background(new DynamicDrawable(this::searchModeFace))
            .tooltipDynamic(t -> t.addLine(IKey.lang("tooltip.tomsstorage.search_mode." + searchMode.key())))
            .tooltipAutoUpdate(true)
            .pos(-18, 41)
            .size(16, 16));
    }

    private IDrawable searchModeFace() {
        List<IDrawable> layers = new ArrayList<>(4);
        layers.add(buttonTexture(194, 62, 16, 16));
        if (searchMode.autoFocus()) layers.add(searchIcon(210));
        if (searchMode.keepText()) layers.add(searchIcon(224));
        if (searchMode.syncNei()) layers.add(searchIcon(238));
        return new DrawableStack(layers.toArray(new IDrawable[0]));
    }

    private IDrawable searchIcon(int x) {
        return buttonTexture(x, 62, 14, 14).asIcon()
            .size(14, 14);
    }

    private void addScrollBar(TerminalGridSyncHandler grid) {
        child(new SliderWidget() {

            @Override
            public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
                grid.scroll(scrollDirection == UpOrDown.UP ? -1 : 1);
                return true;
            }
        }.setAxis(GuiAxis.Y)
            .sliderTexture(SCROLL_HANDLE)
            .sliderSize(12, 15)
            .bounds(0.0, 1.0)
            .value(
                new DoubleValue.Dynamic(
                    () -> grid.maxScroll() == 0 ? 0.0 : (double) grid.getScroll() / grid.maxScroll(),
                    fraction -> grid.scrollTo((int) Math.round(fraction * grid.maxScroll()))))
            .invisible()
            .pos(174, GRID_Y + 1)
            .size(12, 72 + 16));
    }

    private static SlotGroupWidget playerInventory() {
        SlotGroupWidget group = new SlotGroupWidget();
        group.name("player_inventory");
        for (int i = 0; i < 9; i++) {
            group.child(
                new ItemSlotPlayer().syncHandler("player", i)
                    .invisible()
                    .pos(i * SLOT, 3 * SLOT + 4));
        }
        for (int i = 0; i < 27; i++) {
            group.child(
                new ItemSlotPlayer().syncHandler("player", i + 9)
                    .invisible()
                    .pos(i % 9 * SLOT, i / 9 * SLOT));
        }
        return group.coverChildren();
    }

    private static final UITexture SCROLL_HANDLE = UITexture.builder()
        .location(new ResourceLocation("textures/gui/container/creative_inventory/tabs.png"))
        .imageSize(256, 256)
        .subAreaXYWH(232, 0, 12, 15)
        .build();

    protected UITexture buttonTexture(int x, int y, int w, int h) {
        return UITexture.builder()
            .location(new ResourceLocation("tomsstorage", "textures/gui/" + textureName + ".png"))
            .imageSize(256, 256)
            .subAreaXYWH(x, y, w, h)
            .build();
    }

    protected static UITexture backgroundTexture(String name, int width, int height) {
        return UITexture.builder()
            .location(new ResourceLocation("tomsstorage", "textures/gui/" + name + ".png"))
            .imageSize(256, 256)
            .subAreaXYWH(0, 0, width, height)
            .build();
    }
}
