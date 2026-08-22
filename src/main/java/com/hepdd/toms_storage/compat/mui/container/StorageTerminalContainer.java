package com.hepdd.toms_storage.compat.mui.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.utils.item.SlotItemHandler;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.hepdd.toms_storage.common.inventory.TerminalNetworkService;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal;
import com.hepdd.toms_storage.compat.mui.slot.TerminalGridSlot;

public class StorageTerminalContainer extends ModularContainer {

    private final TerminalClickRouter router;

    public StorageTerminalContainer(TileEntityStorageTerminal terminal) {
        this.router = new TerminalClickRouter(terminal);
    }

    @Override
    public ItemStack slotClick(int slotId, int mouseButton, int mode, EntityPlayer player) {
        if (slotId >= 0 && slotId < this.inventorySlots.size()) {
            Slot slot = getSlot(slotId);
            if (isPlayerSlot(slot) && router.handlePlayer(slotId, mouseButton, mode, slot, player)) {
                detectAndSendChanges();
                return null;
            }
        }
        return super.slotClick(slotId, mouseButton, mode, player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = getSlot(index);
        if (slot instanceof TerminalGridSlot) return null;
        if (isPlayerSlot(slot)) {
            if (player instanceof EntityPlayerMP) router.depositSlot(slot);
            return null;
        }
        return super.transferStackInSlot(player, index);
    }

    private static boolean isPlayerSlot(Slot slot) {
        return slot instanceof SlotItemHandler handler && ModularSlot.getPlayerSlotPlayer(handler) != null;
    }

    public static class TerminalClickRouter {

        private static final int MODE_QUICK_MOVE = 1;
        private static final long DOUBLE_CLICK_MS = 250L;

        private final TileEntityStorageTerminal terminal;

        private int lastShiftSlot = -1;
        private int lastShiftButton = -1;
        private long lastShiftTime;
        private ItemStack lastShiftStack;

        public TerminalClickRouter(TileEntityStorageTerminal terminal) {
            this.terminal = terminal;
        }

        public boolean handlePlayer(int slotId, int button, int mode, Slot slot, EntityPlayer player) {
            if (mode != MODE_QUICK_MOVE) return false;
            long now = System.currentTimeMillis();
            boolean doubleClick = slotId == lastShiftSlot && button == lastShiftButton
                && now - lastShiftTime < DOUBLE_CLICK_MS;
            ItemStack template = slot.getStack() != null ? slot.getStack() : lastShiftStack;
            if (doubleClick && template != null) {
                clearShiftTracking();
                if (player instanceof EntityPlayerMP mp) {
                    TerminalNetworkService.depositMatching(terminal, mp, template.copy());
                }
                return true;
            }
            lastShiftSlot = slotId;
            lastShiftButton = button;
            lastShiftTime = now;
            lastShiftStack = slot.getStack() == null ? null
                : slot.getStack()
                    .copy();
            return false;
        }

        public void depositSlot(Slot slot) {
            ItemStack stack = slot.getStack();
            if (stack == null) return;
            slot.putStack(TerminalNetworkService.push(terminal, stack));
            slot.onSlotChanged();
        }

        private void clearShiftTracking() {
            lastShiftSlot = -1;
            lastShiftButton = -1;
            lastShiftTime = 0L;
            lastShiftStack = null;
        }
    }
}
