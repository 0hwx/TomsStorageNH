package com.hepdd.toms_storage.common.inventory.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.IItemHandler;
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
    // Running sum of every slot's stack size, so the terminal's fill bar costs nothing to read.
    private long totalCount;

    private ItemKey[] shadowKey = new ItemKey[0];
    private int[] shadowCount = new int[0];
    // Where each slot sits inside its item's slot list, so removal is O(1) instead of a scan.
    private int[] slotListPos = new int[0];

    private int revision;
    private List<CachedItemStack> aggregate = Collections.emptyList();
    private int aggregateRevision = -1;

    // Set by verifyAgainst so the caller can name the member that owns the offending slot.
    private int lastBadSlot = -1;

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
        totalCount = 0L;
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

    /** Every item in the network counted individually, not per stack. */
    public long totalCount() {
        return totalCount;
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
            totalCount += stack.stackSize - oldCount;
            shadowCount[slot] = stack.stackSize;
            revision++;
            return;
        }

        if (oldKey == null) {
            setEmpty(slot, false);
        } else {
            // Driven off the shadow arrays, not the entry, so a missing entry cannot drift the total.
            totalCount -= oldCount;
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
            totalCount += stack.stackSize;
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

    /**
     * Re-reads every slot and reports where the index disagrees with reality. Debug only, O(total slots).
     *
     * <p>
     * Worth having because updates are relative (`count += new - old`): the sweep repairs a wrong shadow,
     * but a drifted running total survives until the next {@link #resize}, and nothing else would notice.
     *
     * @return an empty string when consistent, otherwise a short summary
     */
    /** The first slot {@link #verifyAgainst} found wrong, or -1. */
    public int lastBadSlot() {
        return lastBadSlot;
    }

    public String verifyAgainst(IItemHandler live) {
        lastBadSlot = -1;
        int slots = shadowKey.length;
        if (live.getSlots() != slots) return "slots " + live.getSlots() + "!=" + slots;

        Object2ObjectOpenHashMap<ItemKey, long[]> expected = new Object2ObjectOpenHashMap<>();
        int expectedEmpty = 0;
        long expectedTotal = 0L;
        int shadowBad = 0;
        int bitmapBad = 0;
        int listBad = 0;
        int firstBadSlot = -1;
        String firstBadDetail = "";

        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = live.getStackInSlot(slot);
            if (stack != null && stack.stackSize <= 0) stack = null;
            ItemKey key = shadowKey[slot];

            boolean slotOk = stack == null ? key == null
                : key != null && key.matches(stack) && shadowCount[slot] == stack.stackSize;
            if (!slotOk) {
                shadowBad++;
                if (firstBadSlot < 0) {
                    firstBadSlot = slot;
                    lastBadSlot = slot;
                    firstBadDetail = describe(key, shadowCount[slot]) + "->" + describe(stack);
                }
            }

            // The bitmap must agree with the shadow regardless of what the live inventory says.
            boolean bitSet = (emptyBits[slot >>> 6] & 1L << slot) != 0L;
            if (bitSet != (key == null)) bitmapBad++;

            if (key != null) {
                Entry entry = entries.get(key);
                if (entry == null || slotListPos[slot] >= entry.slots.size()
                    || entry.slots.getInt(slotListPos[slot]) != slot) listBad++;
            }

            if (stack == null) {
                expectedEmpty++;
            } else {
                long[] total = expected.get(probe.set(stack));
                if (total == null) {
                    total = new long[1];
                    expected.put(ItemKey.copyOf(stack), total);
                }
                total[0] += stack.stackSize;
                expectedTotal += stack.stackSize;
            }
        }

        int countBad = 0;
        for (Map.Entry<ItemKey, long[]> mapping : expected.entrySet()) {
            Entry entry = entries.get(mapping.getKey());
            if (entry == null || entry.count != mapping.getValue()[0]) countBad++;
        }
        // Entries the live read never produced. Counted explicitly rather than by size difference, which
        // would cancel out when the index is both missing one item and holding a stale one.
        int orphans = 0;
        for (ItemKey key : entries.keySet()) {
            if (!expected.containsKey(key)) orphans++;
        }

        if (shadowBad == 0 && bitmapBad == 0
            && listBad == 0
            && countBad == 0
            && orphans == 0
            && expectedEmpty == emptyCount
            && expectedTotal == totalCount) return "";
        return "shadow=" + shadowBad
            + " bitmap="
            + bitmapBad
            + " lists="
            + listBad
            + " counts="
            + countBad
            + " orphans="
            + orphans
            + " empty="
            + emptyCount
            + "/"
            + expectedEmpty
            + " items="
            + totalCount
            + "/"
            + expectedTotal
            + " firstSlot="
            + firstBadSlot
            + (firstBadDetail.isEmpty() ? "" : " " + firstBadDetail);
    }

    private static String describe(ItemKey key, int count) {
        if (key == null) return "empty";
        ItemStack stack = key.toStack();
        return stack.getUnlocalizedName() + "@" + stack.getItemDamage() + "x" + count;
    }

    private static String describe(ItemStack stack) {
        if (stack == null) return "empty";
        return stack.getUnlocalizedName() + "@" + stack.getItemDamage() + "x" + stack.stackSize;
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
