package com.hepdd.toms_storage.compat.mui.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.hepdd.toms_storage.common.inventory.CachedItemStack;
import com.hepdd.toms_storage.common.inventory.TerminalNetworkService;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal;
import com.hepdd.toms_storage.compat.mui.container.CraftingTerminalContainer;
import com.hepdd.toms_storage.compat.mui.search.TerminalSearch;

public class TerminalGridSyncHandler extends SyncHandler<TerminalGridSyncHandler> {

    public static final int COLUMNS = 9;
    public static final int ROWS = 5;
    public static final int SLOTS = COLUMNS * ROWS;

    public static final String SYNC_KEY = "terminal_grid";
    public static final int SYNC_ID = 0;

    private static final int ID_LIST = 1;
    private static final int ID_CLICK = 2;
    private static final int ID_FILL = 3;
    private static final int ID_AUTOCRAFT = 4;
    private static final int ID_CLEAR = 5;

    private final TileEntityStorageTerminal tile;
    private final TerminalSearch search = new TerminalSearch();

    private String searchText = "";

    private List<CachedItemStack> filtered = Collections.emptyList();
    private int lastRevision = Integer.MIN_VALUE;
    private TileEntityStorageTerminal.SortingTypes lastSortType;
    private TileEntityStorageTerminal.SortDirection lastSortDirection;
    private String lastSearchApplied;

    private List<CachedItemStack> clientList = Collections.emptyList();
    private int scroll;

    public TerminalGridSyncHandler(TileEntityStorageTerminal tile) {
        this.tile = tile;
        allowC2S();
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText == null ? "" : searchText;
    }

    public int getScroll() {
        return scroll;
    }

    public int getCellCount(int cell) {
        int index = scroll * COLUMNS + cell;
        return index >= 0 && index < clientList.size() ? (int) Math.min(
            clientList.get(index)
                .count(),
            Integer.MAX_VALUE) : 0;
    }

    // Display icon for a grid cell, derived live from clientList (+ scroll). The slot reads through to this
    // instead of a cached view handler, so the display can't go stale if MUI blanks the view on re-init.
    public ItemStack getCellIcon(int cell) {
        int index = scroll * COLUMNS + cell;
        return index >= 0 && index < clientList.size() ? clientList.get(index)
            .icon() : null;
    }

    // Client-side snapshot of the network's items (sizes capped) for NEI presence checks.
    public List<ItemStack> clientNetworkItems() {
        List<ItemStack> items = new ArrayList<>(clientList.size());
        for (CachedItemStack cached : clientList) {
            if (cached.stack() == null || cached.count() <= 0) continue;
            ItemStack stack = cached.stack()
                .copy();
            stack.stackSize = (int) Math.min(cached.count(), 64);
            items.add(stack);
        }
        return items;
    }

    // Raw client-side snapshot with true counts. clientNetworkItems() caps sizes at 64 for NEI presence
    // dots; the auto-craft preview needs the real amounts.
    public List<CachedItemStack> clientCachedItems() {
        return Collections.unmodifiableList(clientList);
    }

    public int maxScroll() {
        int rows = (clientList.size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, rows - ROWS);
    }

    public void scroll(int direction) {
        scrollTo(scroll + direction);
    }

    public void scrollTo(int row) {
        scroll = Math.min(maxScroll(), Math.max(0, row));
    }

    public void click(int cell, SlotAction action) {
        int index = scroll * COLUMNS + cell;
        syncToServer(ID_CLICK, buffer -> {
            buffer.writeInt(index);
            buffer.writeByte(action.ordinal());
        });
    }

    // NEI recipe transfer: client asks the server (over the grid's own C2S channel) to fill the crafting matrix
    // with these 9 slot targets. Server pulls them via the same TerminalNetworkService.pull the grid clicks use.
    public void requestFill(ItemStack[] targets) {
        syncToServer(ID_FILL, buffer -> {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = i < targets.length ? targets[i] : null;
                buffer.writeBoolean(stack != null);
                if (stack != null) NetworkUtils.writeItemStack(buffer, stack);
            }
        });
    }

    // NEI auto-craft: the serialised recipe rides the grid's own C2S channel, the same transport
    // requestFill uses, so the crafting terminal has exactly one client-to-server path.
    public void requestAutoCraft(NBTTagCompound recipeTag) {
        syncToServer(ID_AUTOCRAFT, buffer -> buffer.writeNBTTagCompoundToBuffer(recipeTag));
    }

    // Clear-grid button: return the crafting matrix to the network.
    public void requestClearGrid() {
        syncToServer(ID_CLEAR, buffer -> {});
    }

    @Override
    public void detectAndSendChanges(boolean init) {
        if (getSyncManager().isClient()) return;
        int revision = tile.getRevision();
        TileEntityStorageTerminal.SortingTypes sortType = tile.getSortType();
        TileEntityStorageTerminal.SortDirection sortDirection = tile.getSortDirection();
        if (init || revision != lastRevision
            || sortType != lastSortType
            || sortDirection != lastSortDirection
            || !searchText.equals(lastSearchApplied)) {
            EntityPlayer player = getSyncManager().getPlayer();
            filtered = search.filter(tile.getCachedStacks(), sortType, sortDirection, searchText, player);
            lastRevision = revision;
            lastSortType = sortType;
            lastSortDirection = sortDirection;
            lastSearchApplied = searchText;
            sync(ID_LIST, this::writeList);
        }
    }

    private void writeList(PacketBuffer buffer) {
        buffer.writeVarIntToBuffer(filtered.size());
        for (CachedItemStack stack : filtered) {
            NetworkUtils.writeItemStack(buffer, stack.icon());
            buffer.writeLong(stack.count());
        }
    }

    @Override
    public void readOnClient(int id, PacketBuffer buf) throws IOException {
        if (id != ID_LIST) return;
        int size = buf.readVarIntFromBuffer();
        List<CachedItemStack> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack icon = NetworkUtils.readItemStack(buf);
            list.add(new CachedItemStack(icon, buf.readLong()));
        }
        clientList = list;
        if (scroll > maxScroll()) scroll = maxScroll();
        // The auto-craft preview only exists to mask the gap between requesting a craft and the server's
        // updated list arriving. This list IS that update, so the simulation is now stale either way.
        if (getSyncManager().getContainer() instanceof CraftingTerminalContainer container) {
            container.getAutoCraftPreview()
                .clear();
        }
    }

    @Override
    public void readOnServer(int id, PacketBuffer buf) throws IOException {
        if (id == ID_CLICK) {
            int index = buf.readInt();
            SlotAction action = SlotAction.VALUES[buf.readByte() & 0xFF];
            ItemStack clicked = index >= 0 && index < filtered.size() ? filtered.get(index)
                .stack() : null;
            if (getSyncManager().getPlayer() instanceof EntityPlayerMP player) {
                execute(action, clicked, player);
            }
        } else if (id == ID_FILL) {
            ItemStack[] targets = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                if (buf.readBoolean()) targets[i] = NetworkUtils.readItemStack(buf);
            }
            if (getSyncManager().getContainer() instanceof CraftingTerminalContainer container) {
                container.fillRecipe(targets);
                getSyncManager().getContainer()
                    .detectAndSendChanges();
            }
        } else if (id == ID_AUTOCRAFT) {
            NBTTagCompound recipeTag = buf.readNBTTagCompoundFromBuffer();
            if (getSyncManager().getContainer() instanceof CraftingTerminalContainer container) {
                container.runAutoCraft(recipeTag);
                container.detectAndSendChanges();
            }
        } else if (id == ID_CLEAR) {
            if (getSyncManager().getContainer() instanceof CraftingTerminalContainer container) {
                container.clearGrid();
                container.detectAndSendChanges();
            }
        }
    }

    private void execute(SlotAction action, ItemStack clicked, EntityPlayerMP player) {
        ItemStack held = player.inventory.getItemStack();
        if (action == SlotAction.PULL_OR_PUSH_STACK && held != null) {
            getSyncManager().setCursorItem(TerminalNetworkService.push(tile, held));
        } else if (clicked != null) {
            ItemStack pulled = TerminalNetworkService.pull(tile, clicked, amount(action, clicked));
            if (pulled == null) return;
            if (action == SlotAction.SHIFT_PULL) {
                if (!TerminalNetworkService.giveToPlayer(player, pulled))
                    TerminalNetworkService.pushOrDrop(tile, pulled);
            } else if (held == null) {
                getSyncManager().setCursorItem(pulled);
            } else {
                TerminalNetworkService.pushOrDrop(tile, pulled);
            }
        } else {
            return;
        }
        player.inventory.markDirty();
        getSyncManager().getContainer()
            .detectAndSendChanges();
    }

    private static int amount(SlotAction action, ItemStack clicked) {
        int max = clicked.getMaxStackSize();
        return switch (action) {
            case PULL_ONE -> 1;
            case GET_HALF -> Math.max(1, max / 2);
            case GET_QUARTER -> Math.max(1, max / 4);
            default -> max;
        };
    }

    public enum SlotAction {

        PULL_OR_PUSH_STACK,
        PULL_ONE,
        SHIFT_PULL,
        GET_HALF,
        GET_QUARTER,
        QUICK_DEPOSIT,
        PUSH_MATCHING_FROM_PLAYER,
        CLEAR_GRID;

        public static final SlotAction[] VALUES = values();
    }
}
