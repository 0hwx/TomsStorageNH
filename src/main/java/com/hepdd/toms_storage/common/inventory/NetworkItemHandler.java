package com.hepdd.toms_storage.common.inventory;

import java.util.Arrays;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.hepdd.toms_storage.common.inventory.network.NetworkGraph;
import com.hepdd.toms_storage.common.inventory.network.NetworkItemIndex;
import com.hepdd.toms_storage.common.inventory.network.interfaces.StorageNetworkHost;

/** Flat IItemHandler view over a NetworkGraph. Slot -> member is a binary search over prefix sums. */
public class NetworkItemHandler implements IItemHandlerModifiable {

    private NetworkGraph graph = NetworkGraph.EMPTY;
    private NetworkItemIndex index;
    private StorageNetworkHost host;

    public void setGraph(NetworkGraph graph) {
        this.graph = graph == null ? NetworkGraph.EMPTY : graph;
    }

    public void setHost(StorageNetworkHost host) {
        this.host = host;
    }

    public void setIndex(NetworkItemIndex index) {
        this.index = index;
    }

    /**
     * The contents index backing this view, or null when there is none. Pending writes are applied first,
     * so a caller never reads slot lists that are behind the real inventories.
     */
    public NetworkItemIndex getIndex() {
        if (index != null && host != null) host.flushPendingWrites();
        return index;
    }

    public NetworkGraph getGraph() {
        return graph;
    }

    @Override
    public int getSlots() {
        return graph.totalSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        NetworkGraph.MemberRef member = memberFor(slot);
        return member == null ? null : member.handler.getStackInSlot(slot - member.firstSlot());
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        NetworkGraph.MemberRef member = memberFor(slot);
        if (member == null) return stack;
        ItemStack remainder = member.handler.insertItem(slot - member.firstSlot(), stack, simulate);
        if (!simulate) notifyWritten(slot);
        return remainder;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        NetworkGraph.MemberRef member = memberFor(slot);
        if (member == null) return null;
        ItemStack extracted = member.handler.extractItem(slot - member.firstSlot(), amount, simulate);
        if (!simulate) notifyWritten(slot);
        return extracted;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        NetworkGraph.MemberRef member = memberFor(slot);
        return member != null && member.handler.isItemValid(slot - member.firstSlot(), stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        NetworkGraph.MemberRef member = memberFor(slot);
        return member == null ? 0 : member.handler.getSlotLimit(slot - member.firstSlot());
    }

    // Throws rather than no-ops for a non-modifiable member: a silent failure here would look like item loss.
    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        NetworkGraph.MemberRef member = memberFor(slot);
        if (member == null) return;
        if (!(member.handler instanceof IItemHandlerModifiable)) {
            throw new UnsupportedOperationException("Network member does not support setStackInSlot");
        }
        ((IItemHandlerModifiable) member.handler).setStackInSlot(slot - member.firstSlot(), stack);
        notifyWritten(slot);
    }

    /** Null for an out-of-range slot or a member whose tile died; a dead tile is never touched. */
    private NetworkGraph.MemberRef memberFor(int slot) {
        int[] offsets = graph.offsets();
        if (slot < 0 || slot >= graph.totalSlots() || offsets.length == 0) return null;
        int index = Arrays.binarySearch(offsets, slot);
        if (index < 0) index = -index - 2;
        NetworkGraph.MemberRef member = graph.members()[index];
        if (!member.isUsable()) {
            if (host != null) host.markTopologyDirty();
            return null;
        }
        return member;
    }

    private void notifyWritten(int slot) {
        if (host != null) host.onNetworkSlotWritten(slot);
    }
}
