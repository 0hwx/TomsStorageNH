package com.hepdd.toms_storage.common.tile;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.cleanroommc.modularui.utils.item.ItemHandlerHelper;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.hepdd.toms_storage.common.inventory.NetworkItemHandler;
import com.hepdd.toms_storage.common.inventory.SidedInvWrapper;
import com.hepdd.toms_storage.common.inventory.network.NetworkItemIndex;
import com.hepdd.toms_storage.common.inventory.network.interfaces.StorageNetwork;
import com.hepdd.toms_storage.common.inventory.network.interfaces.StorageNetworkEndpoint;
import com.hepdd.toms_storage.utils.StorageUtils;

import it.unimi.dsi.fastutil.ints.IntList;

public class TileEntityInventoryHopper extends TileEntity implements StorageNetworkEndpoint {

    private static final int TRANSFER_COOLDOWN = 10;

    private ItemStack filter;
    private int cooldown;
    private int roundRobinSlot;
    private StorageNetwork network;
    private ForgeDirection networkSide = ForgeDirection.UNKNOWN;
    private boolean connectionConflict;

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;
        if (cooldown > 0) cooldown--;
        if (cooldown > 0 || worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord)) return;
        cooldown = TRANSFER_COOLDOWN;
        if (!hasCurrentNetwork() || connectionConflict) return;

        ForgeDirection facing = getFacing();
        if (networkSide != facing && networkSide != facing.getOpposite()) return;
        ForgeDirection externalSide = networkSide.getOpposite();
        TileEntity externalTile = worldObj
            .getTileEntity(xCoord + externalSide.offsetX, yCoord + externalSide.offsetY, zCoord + externalSide.offsetZ);
        if (!(externalTile instanceof IInventory)) return;
        IInventory externalInventory = (IInventory) externalTile;
        // Pushing into an inventory that is already on the network would just shuffle items in place.
        if (network.containsPosition(
            CoordinatePacker
                .pack(xCoord + externalSide.offsetX, yCoord + externalSide.offsetY, zCoord + externalSide.offsetZ)))
            return;

        IItemHandler networkInventory = network.getStorageInventory();
        SidedInvWrapper external = new SidedInvWrapper(externalInventory, externalSide.getOpposite());
        if (isImportMode()) {
            transferOne(external, networkInventory, false);
        } else if (filter != null) {
            transferFiltered(networkInventory, external);
        }
    }

    @Override
    public boolean canConnectNetworkFrom(ForgeDirection side) {
        ForgeDirection facing = getFacing();
        return side == facing || side == facing.getOpposite();
    }

    @Override
    public void attachNetwork(StorageNetwork newNetwork, ForgeDirection side) {
        if (worldObj == null || newNetwork == null || !canConnectNetworkFrom(side)) return;
        // Two live networks reaching the same hopper is the conflict case the cable rules forbid.
        if (network != null && network != newNetwork && network.isNetworkValid() && network.hasEndpoint(this)) {
            connectionConflict = true;
            return;
        }
        network = newNetwork;
        networkSide = side;
        connectionConflict = false;
    }

    @Override
    public void detachNetwork(StorageNetwork oldNetwork) {
        if (network != oldNetwork) return;
        clearNetwork();
    }

    @Override
    public void markNetworkConflict() {
        clearNetwork();
        connectionConflict = true;
    }

    public void markNetworkDirty() {
        clearNetwork();
    }

    private void clearNetwork() {
        network = null;
        networkSide = ForgeDirection.UNKNOWN;
        connectionConflict = false;
    }

    public void setFilter(ItemStack stack) {
        filter = stack == null ? null : ItemHandlerHelper.copyStackWithSize(stack, 1);
        markDirty();
    }

    public ItemStack getFilter() {
        return filter == null ? null : filter.copy();
    }

    public String getModeTranslationKey() {
        if (!hasCurrentNetwork() || connectionConflict) return "message.tomsstorage.inventory_hopper.mode_unknown";
        return isImportMode() ? "message.tomsstorage.inventory_hopper.mode_import"
            : "message.tomsstorage.inventory_hopper.mode_export";
    }

    public String getStatusTranslationKey() {
        if (connectionConflict) return "message.tomsstorage.inventory_hopper.conflict";
        if (!hasCurrentNetwork()) return "message.tomsstorage.inventory_hopper.disconnected";
        if (!isImportMode() && filter == null) return "message.tomsstorage.inventory_hopper.filter_required";
        if (worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord))
            return "message.tomsstorage.inventory_hopper.redstone_disabled";
        return "message.tomsstorage.inventory_hopper.active";
    }

    private boolean isImportMode() {
        return networkSide == getFacing();
    }

    private boolean hasCurrentNetwork() {
        if (network == null) return false;
        if (!network.isNetworkValid()) {
            clearNetwork();
            return false;
        }
        return true;
    }

    private ForgeDirection getFacing() {
        ForgeDirection facing = ForgeDirection.getOrientation(getBlockMetadata());
        return facing == ForgeDirection.UNKNOWN ? ForgeDirection.NORTH : facing;
    }

    /** Export: the filter names exactly one item, so ask the index which slots hold it. */
    private boolean transferFiltered(IItemHandler source, IItemHandler destination) {
        NetworkItemIndex index = source instanceof NetworkItemHandler ? ((NetworkItemHandler) source).getIndex() : null;
        if (index == null) return transferOne(source, destination, true);

        IntList slots = index.slotsFor(filter);
        int count = slots.size();
        if (count == 0) return false;

        for (int checked = 0; checked < count; checked++) {
            int slot = slots.getInt(Math.floorMod(roundRobinSlot + checked, count));
            ItemStack candidate = source.getStackInSlot(slot);
            if (candidate == null || candidate.stackSize <= 0 || !matchesFilter(candidate, true)) continue;
            ItemStack simulatedExtract = source.extractItem(slot, 1, true);
            if (simulatedExtract == null || simulatedExtract.stackSize != 1) continue;
            ItemStack simulatedRemainder = StorageUtils.insertStacked(destination, simulatedExtract, true);
            if (simulatedRemainder != null && simulatedRemainder.stackSize > 0) continue;

            ItemStack extracted = source.extractItem(slot, 1, false);
            if (extracted == null || extracted.stackSize <= 0) continue;
            if (extracted.stackSize != 1 || !StorageUtils.areItemStacksEqual(extracted, simulatedExtract, true)) {
                rollbackOrDrop(source, slot, extracted);
                return false;
            }
            ItemStack remainder = StorageUtils.insertStacked(destination, extracted, false);
            if (remainder != null && remainder.stackSize > 0) rollbackOrDrop(source, slot, remainder);
            // Indexes into the candidate list, not the global slot space - only used to spread the load.
            roundRobinSlot = checked + 1;
            markDirty();
            return remainder == null || remainder.stackSize <= 0;
        }
        roundRobinSlot++;
        return false;
    }

    private boolean transferOne(IItemHandler source, IItemHandler destination, boolean requireFilter) {
        int slots = source.getSlots();
        if (slots <= 0) return false;
        for (int checked = 0; checked < slots; checked++) {
            int slot = Math.floorMod(roundRobinSlot + checked, slots);
            ItemStack candidate = source.getStackInSlot(slot);
            if (candidate == null || candidate.stackSize <= 0 || !matchesFilter(candidate, requireFilter)) continue;
            ItemStack simulatedExtract = source.extractItem(slot, 1, true);
            if (simulatedExtract == null || simulatedExtract.stackSize != 1) continue;
            ItemStack simulatedRemainder = StorageUtils.insertStacked(destination, simulatedExtract, true);
            if (simulatedRemainder != null && simulatedRemainder.stackSize > 0) continue;

            ItemStack extracted = source.extractItem(slot, 1, false);
            if (extracted == null || extracted.stackSize <= 0) continue;
            if (extracted.stackSize != 1 || !StorageUtils.areItemStacksEqual(extracted, simulatedExtract, true)) {
                rollbackOrDrop(source, slot, extracted);
                return false;
            }
            ItemStack remainder = StorageUtils.insertStacked(destination, extracted, false);
            if (remainder != null && remainder.stackSize > 0) rollbackOrDrop(source, slot, remainder);
            roundRobinSlot = (slot + 1) % slots;
            markDirty();
            if (network != null) network.markContentsDirty();
            return remainder == null || remainder.stackSize <= 0;
        }
        roundRobinSlot = (roundRobinSlot + 1) % slots;
        return false;
    }

    private boolean matchesFilter(ItemStack stack, boolean requireFilter) {
        if (filter == null) return !requireFilter;
        return StorageUtils.areItemStacksEqual(filter, stack, true);
    }

    private void rollbackOrDrop(IItemHandler source, int sourceSlot, ItemStack stack) {
        ItemStack remainder = source.insertItem(sourceSlot, stack, false);
        if (remainder != null) remainder = StorageUtils.insertStacked(source, remainder, false);
        if (remainder == null || remainder.stackSize <= 0) return;
        EntityItem dropped = new EntityItem(worldObj, xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, remainder.copy());
        worldObj.spawnEntityInWorld(dropped);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        cooldown = tag.getInteger("Cooldown");
        roundRobinSlot = tag.getInteger("RoundRobinSlot");
        filter = tag.hasKey("Filter") ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Filter")) : null;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Cooldown", cooldown);
        tag.setInteger("RoundRobinSlot", roundRobinSlot);
        if (filter != null) {
            NBTTagCompound filterTag = new NBTTagCompound();
            filter.writeToNBT(filterTag);
            tag.setTag("Filter", filterTag);
        } else {
            tag.removeTag("Filter");
        }
    }
}
