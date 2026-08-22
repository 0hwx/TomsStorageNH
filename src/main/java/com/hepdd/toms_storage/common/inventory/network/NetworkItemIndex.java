package com.hepdd.toms_storage.common.inventory.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.hepdd.toms_storage.common.inventory.CachedItemStack;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * Contents of one network: item identity -> (count, slots holding it), plus the empty slots and a per-slot
 * shadow of what was last seen there.
 *
 * <p>
 * Updated by deltas. The sweep re-reads slots and is authoritative, so a missed delta self-corrects
 * within one sweep instead of accumulating.
 */
public final class NetworkItemIndex {

    private final Object2ObjectOpenHashMap<ItemKey, Entry> entries = new Object2ObjectOpenHashMap<>();
    private final ItemKey probe = ItemKey.scratch();

    // Empty slots are a bitmap rather than a list: O(1) to set and clear, and iterating it yields slots in
    // ascending order, so a new item fills the lowest empty slot the way a plain slot walk used to.
    private long[] emptyBits = new long[0];
    private int emptyCount;

    private ItemKey[] shadowKey = new ItemKey[0];
    private int[] shadowCount = new int[0];
    // Where each slot sits inside its item's slot list, so removal is O(1) instead of a scan.
    private int[] slotListPos = new int[0];

    private int revision;
    private List<CachedItemStack> aggregate = Collections.emptyList();
    private int aggregateRevision = -1;

    /** Resets everything and declares every slot empty. Call on graph rebuild, then re-read all slots. */
    public void resize(int totalSlots) {
        entries.clear();
        shadowKey = new ItemKey[totalSlots];
        shadowCount = new int[totalSlots];
        slotListPos = new int[totalSlots];
        emptyBits = new long[(totalSlots + 63) >>> 6];
        for (int word = 0; word < emptyBits.length; word++) {
            emptyBits[word] = -1L;
        }
        // Clear the padding bits past the last slot so nextEmptySlot cannot hand out one that does not exist.
        int tail = totalSlots & 63;
        if (tail != 0) emptyBits[emptyBits.length - 1] = (1L << tail) - 1L;
        emptyCount = totalSlots;
        revision++;
    }

    public int revision() {
        return revision;
    }

    public int distinctItems() {
        return entries.size();
    }

    public int emptySlotCount() {
        return emptyCount;
    }

    /** The lowest empty slot at or after {@code from}, or -1 when there is none. */
    public int nextEmptySlot(int from) {
        if (from < 0) from = 0;
        int word = from >>> 6;
        if (word >= emptyBits.length) return -1;

        long bits = emptyBits[word] & -1L << from;
        while (bits == 0L) {
            if (++word >= emptyBits.length) return -1;
            bits = emptyBits[word];
        }
        return (word << 6) + Long.numberOfTrailingZeros(bits);
    }

    public IntList slotsFor(ItemStack stack) {
        if (stack == null) return IntLists.EMPTY_LIST;
        Entry entry = entries.get(probe.set(stack));
        return entry == null ? IntLists.EMPTY_LIST : entry.slots;
    }

    public long countOf(ItemStack stack) {
        if (stack == null) return 0L;
        Entry entry = entries.get(probe.set(stack));
        return entry == null ? 0L : entry.count;
    }

    /** Records what a slot actually holds now. No-op when nothing changed. */
    public void applySlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= shadowKey.length) return;
        if (stack != null && stack.stackSize <= 0) stack = null;

        ItemKey oldKey = shadowKey[slot];
        int oldCount = shadowCount[slot];

        if (oldKey == null && stack == null) return;

        // Same item, different amount: adjust the running total and leave both lists alone.
        if (oldKey != null && oldKey.matches(stack)) {
            if (oldCount == stack.stackSize) return;
            Entry entry = entries.get(oldKey);
            if (entry != null) entry.count += stack.stackSize - oldCount;
            shadowCount[slot] = stack.stackSize;
            revision++;
            return;
        }

        if (oldKey == null) {
            setEmpty(slot, false);
        } else {
            Entry entry = entries.get(oldKey);
            if (entry != null) {
                entry.count -= oldCount;
                removeFrom(entry.slots, slot);
                if (entry.slots.isEmpty()) entries.remove(oldKey);
            }
        }

        if (stack == null) {
            shadowKey[slot] = null;
            shadowCount[slot] = 0;
            setEmpty(slot, true);
        } else {
            Entry entry = entries.get(probe.set(stack));
            if (entry == null) {
                entry = new Entry(ItemKey.copyOf(stack));
                entries.put(entry.key, entry);
            }
            entry.count += stack.stackSize;
            addTo(entry.slots, slot);
            shadowKey[slot] = entry.key;
            shadowCount[slot] = stack.stackSize;
        }
        revision++;
    }

    /** One CachedItemStack per distinct item, rebuilt only when the contents actually moved. */
    public List<CachedItemStack> aggregate() {
        if (aggregateRevision == revision) return aggregate;
        List<CachedItemStack> list = new ArrayList<>(entries.size());
        for (Map.Entry<ItemKey, Entry> mapping : entries.entrySet()) {
            Entry entry = mapping.getValue();
            if (entry.count > 0L) list.add(new CachedItemStack(entry.key.toStack(), entry.count));
        }
        aggregate = list;
        aggregateRevision = revision;
        return list;
    }

    private void setEmpty(int slot, boolean isEmpty) {
        int word = slot >>> 6;
        if (word >= emptyBits.length) return;
        long mask = 1L << slot;
        boolean wasEmpty = (emptyBits[word] & mask) != 0L;
        if (wasEmpty == isEmpty) return;
        if (isEmpty) {
            emptyBits[word] |= mask;
            emptyCount++;
        } else {
            emptyBits[word] &= ~mask;
            emptyCount--;
        }
    }

    private void addTo(IntArrayList list, int slot) {
        slotListPos[slot] = list.size();
        list.add(slot);
    }

    private void removeFrom(IntArrayList list, int slot) {
        int position = slotListPos[slot];
        // Defensive: a wrong position would corrupt the list, and the sweep could not repair that.
        if (position < 0 || position >= list.size() || list.getInt(position) != slot) {
            position = list.indexOf(slot);
            if (position < 0) return;
        }
        int last = list.size() - 1;
        int moved = list.getInt(last);
        list.set(position, moved);
        slotListPos[moved] = position;
        list.removeInt(last);
    }

    private static final class Entry {

        private final ItemKey key;
        private final IntArrayList slots = new IntArrayList();
        private long count;

        private Entry(ItemKey key) {
            this.key = key;
        }
    }
}
