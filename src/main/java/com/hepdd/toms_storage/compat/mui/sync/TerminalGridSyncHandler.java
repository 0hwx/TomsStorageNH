package com.hepdd.toms_storage.compat.mui.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ChatComponentTranslation;

import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.hepdd.toms_storage.common.crafting.autocrafting.CraftOutcome;
import com.hepdd.toms_storage.common.inventory.CachedItemStack;
import com.hepdd.toms_storage.common.inventory.TerminalNetworkService;
import com.hepdd.toms_storage.common.inventory.network.interfaces.StorageNetwork;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal.SortDirection;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal.SortingTypes;
import com.hepdd.toms_storage.compat.mui.container.CraftingTerminalContainer;
import com.hepdd.toms_storage.compat.mui.search.TerminalSearch;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class TerminalGridSyncHandler extends SyncHandler<TerminalGridSyncHandler> {

    public static final int COLUMNS = 9;
    public static final int ROWS = 5;
    public static final int SLOTS = COLUMNS * ROWS;

    public static final String SYNC_KEY = "terminal_grid";
    public static final int SYNC_ID = 0;

    private static final int ID_INVENTORY = 1;
    private static final int ID_CLICK = 2;
    private static final int ID_FILL = 3;
    private static final int ID_AUTOCRAFT = 4;
    private static final int ID_CLEAR = 5;
    private static final int ID_INVENTORY_RESET = 6;
    private static final int INVENTORY_BATCH_BYTES = 64 * 1024;

    private final TileEntityStorageTerminal tile;
    private final TerminalSearch search = new TerminalSearch();

    private String searchText = "";

    private final Object2IntMap<CachedItemStack> serverIds = new Object2IntOpenHashMap<>();
    private Int2ObjectMap<CachedItemStack> serverInventory = Int2ObjectMaps.emptyMap();
    private int nextId = 1;
    private int lastRevision = Integer.MIN_VALUE;
    private StorageNetwork lastNetwork;

    private final Int2ObjectMap<CachedItemStack> clientInventory = new Int2ObjectLinkedOpenHashMap<>();
    private List<Int2ObjectMap.Entry<CachedItemStack>> clientView = Collections.emptyList();
    private boolean viewDirty = true;
    private SortingTypes lastSortType;
    private SortDirection lastSortDirection;
    private int scroll;

    public TerminalGridSyncHandler(TileEntityStorageTerminal tile) {
        this.tile = tile;
        allowC2S();
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        String text = searchText == null ? "" : searchText;
        if (this.searchText.equals(text)) return;
        this.searchText = text;
        scroll = 0;
        viewDirty = true;
    }

    public int getScroll() {
        updateClientView();
        return scroll;
    }

    public int getCellCount(int cell) {
        Int2ObjectMap.Entry<CachedItemStack> entry = getCell(cell);
        return entry == null ? 0
            : (int) Math.min(
                entry.getValue()
                    .count(),
                Integer.MAX_VALUE);
    }

    public ItemStack getCellIcon(int cell) {
        Int2ObjectMap.Entry<CachedItemStack> entry = getCell(cell);
        return entry == null ? null
            : entry.getValue()
                .icon();
    }

    private Int2ObjectMap.Entry<CachedItemStack> getCell(int cell) {
        updateClientView();
        int index = scroll * COLUMNS + cell;
        return index >= 0 && index < clientView.size() ? clientView.get(index) : null;
    }

    // Client-side snapshot of the network's items (sizes capped) for NEI presence checks.
    public List<ItemStack> clientNetworkItems() {
        List<CachedItemStack> inventory = clientCachedItems();
        List<ItemStack> items = new ArrayList<>(inventory.size());
        for (CachedItemStack cached : inventory) {
            if (cached.stack() == null || cached.count() <= 0) continue;
            ItemStack stack = cached.stack()
                .copy();
            stack.stackSize = (int) Math.min(cached.count(), 64);
            items.add(stack);
        }
        return items;
    }

    public List<CachedItemStack> clientCachedItems() {
        synchronized (clientInventory) {
            return Collections.unmodifiableList(new ArrayList<>(clientInventory.values()));
        }
    }

    public int maxScroll() {
        updateClientView();
        return maxScrollForView();
    }

    private int maxScrollForView() {
        int rows = (clientView.size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, rows - ROWS);
    }

    public void scroll(int direction) {
        scrollTo(scroll + direction);
    }

    public void scrollTo(int row) {
        scroll = Math.min(maxScroll(), Math.max(0, row));
    }

    public void click(int cell, SlotAction action) {
        Int2ObjectMap.Entry<CachedItemStack> entry = getCell(cell);
        int itemId = entry == null ? 0 : entry.getIntKey();
        syncToServer(ID_CLICK, buffer -> {
            buffer.writeVarIntToBuffer(itemId);
            buffer.writeByte(action.ordinal());
        });
    }

    public void requestFill(ItemStack[] targets) {
        syncToServer(ID_FILL, buffer -> {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = i < targets.length ? targets[i] : null;
                buffer.writeBoolean(stack != null);
                if (stack != null) NetworkUtils.writeItemStack(buffer, stack);
            }
        });
    }

    public void requestAutoCraft(int requestId, NBTTagCompound recipeTag) {
        syncToServer(ID_AUTOCRAFT, buffer -> {
            buffer.writeInt(requestId);
            buffer.writeNBTTagCompoundToBuffer(recipeTag);
        });
    }

    public void requestClearGrid() {
        syncToServer(ID_CLEAR, buffer -> {});
    }

    @Override
    public void detectAndSendChanges(boolean init) {
        if (getSyncManager().isClient()) return;
        StorageNetwork network = tile.getAttachedAccess();
        int revision = network == null ? tile.getRevision() : network.getContentsRevision();
        if (!init && network == lastNetwork && revision == lastRevision) return;

        if (init) sync(ID_INVENTORY_RESET);
        Int2ObjectMap<CachedItemStack> next = new Int2ObjectOpenHashMap<>();
        PacketBuffer batch = new PacketBuffer(Unpooled.buffer());
        try {
            List<CachedItemStack> inventory = network == null ? tile.getCachedStacks() : network.getAggregatedStacks();
            for (CachedItemStack stack : inventory) {
                int itemId = serverIds.computeIfAbsent(stack, ignored -> nextId++);
                CachedItemStack previous = serverInventory.get(itemId);
                next.put(itemId, stack);
                if (init || previous == null || previous.count() != stack.count()) {
                    writeChange(batch, itemId, stack.count(), init || previous == null ? stack.stack() : null);
                }
            }
            for (Int2ObjectMap.Entry<CachedItemStack> entry : Int2ObjectMaps.fastIterable(serverInventory)) {
                if (!next.containsKey(entry.getIntKey())) {
                    serverIds.removeInt(entry.getValue());
                    if (!init) writeChange(batch, entry.getIntKey(), 0, null);
                }
            }
            if (batch.isReadable()) sendBatch(batch);
        } finally {
            batch.release();
        }
        serverInventory = next;
        lastNetwork = network;
        lastRevision = revision;
    }

    private void writeChange(PacketBuffer batch, int itemId, long count, ItemStack stack) {
        batch.writeVarIntToBuffer(itemId);
        batch.writeLong(count);
        if (count > 0) {
            batch.writeBoolean(stack != null);
            if (stack != null) NetworkUtils.writeItemStack(batch, stack);
        }
        if (batch.readableBytes() >= INVENTORY_BATCH_BYTES) sendBatch(batch);
    }

    private void sendBatch(PacketBuffer batch) {
        sync(ID_INVENTORY, buffer -> buffer.writeBytes(batch));
        batch.clear();
    }

    @Override
    public void readOnClient(int id, PacketBuffer buf) {
        if (id == ID_INVENTORY_RESET) {
            synchronized (clientInventory) {
                clientInventory.clear();
            }
            search.clear();
            viewDirty = true;
        } else if (id == ID_INVENTORY) {
            readInventoryChanges(buf);
            viewDirty = true;
        } else
            if (id == ID_AUTOCRAFT && getSyncManager().getContainer() instanceof CraftingTerminalContainer container) {
                int requestId = buf.readInt();
                String reason = NetworkUtils.readStringSafe(buf);
                container.getAutoCraftPreview()
                    .clearIfMatches(requestId);
                if (reason != null && !reason.isEmpty()) {
                    getSyncManager().getPlayer()
                        .addChatMessage(new ChatComponentTranslation("message.tomsstorage.autocraft." + reason));
                }
            }
    }

    private void readInventoryChanges(PacketBuffer buf) {
        synchronized (clientInventory) {
            while (buf.isReadable()) {
                int itemId = buf.readVarIntFromBuffer();
                long count = buf.readLong();
                if (count == 0) {
                    clientInventory.remove(itemId);
                    search.forget(itemId);
                } else if (buf.readBoolean()) {
                    ItemStack stack = NetworkUtils.readItemStack(buf);
                    if (stack != null) clientInventory.put(itemId, new CachedItemStack(stack, count));
                } else {
                    CachedItemStack stack = clientInventory.get(itemId);
                    if (stack != null) clientInventory.put(itemId, stack.withCount(count));
                }
            }
        }
    }

    private void updateClientView() {
        if (!isValid() || !getSyncManager().isClient()) return;
        SortingTypes sortType = tile.getSortType();
        SortDirection sortDirection = tile.getSortDirection();
        if (!viewDirty && sortType == lastSortType && sortDirection == lastSortDirection) return;
        clientView = search.filter(clientInventory, sortType, sortDirection, searchText, getSyncManager().getPlayer());
        scroll = Math.min(scroll, maxScrollForView());
        lastSortType = sortType;
        lastSortDirection = sortDirection;
        viewDirty = false;
    }

    @Override
    public void readOnServer(int id, PacketBuffer buf) throws IOException {
        if (id == ID_CLICK) {
            int itemId = buf.readVarIntFromBuffer();
            int actionId = buf.readUnsignedByte();
            if (actionId >= SlotAction.VALUES.length) return;
            SlotAction action = SlotAction.VALUES[actionId];
            CachedItemStack stack = serverInventory.get(itemId);
            if (itemId != 0 && stack == null) return;
            ItemStack clicked = stack == null ? null : stack.stack();
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
            int requestId = buf.readInt();
            NBTTagCompound recipeTag = buf.readNBTTagCompoundFromBuffer();
            if (getSyncManager().getContainer() instanceof CraftingTerminalContainer container) {
                CraftOutcome outcome = container.runAutoCraft(recipeTag);
                container.detectAndSendChanges();
                sync(ID_AUTOCRAFT, buffer -> {
                    buffer.writeInt(requestId);
                    NetworkUtils.writeStringSafe(buffer, outcome.getReason());
                });
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
                TerminalNetworkService.giveToPlayerOrStore(tile, player, pulled);
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
