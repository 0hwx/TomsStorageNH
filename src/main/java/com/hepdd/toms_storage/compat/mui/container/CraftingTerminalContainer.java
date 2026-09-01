package com.hepdd.toms_storage.compat.mui.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.widgets.slot.InventoryCraftingWrapper;
import com.cleanroommc.modularui.widgets.slot.ModularCraftingSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.hepdd.toms_storage.TomsConfig;
import com.hepdd.toms_storage.common.crafting.TerminalCraftSession;
import com.hepdd.toms_storage.common.crafting.autocrafting.AutoCraftDebug;
import com.hepdd.toms_storage.common.crafting.autocrafting.AutoCraftExecutor;
import com.hepdd.toms_storage.common.crafting.autocrafting.AutoCraftRequest;
import com.hepdd.toms_storage.common.crafting.autocrafting.CraftOutcome;
import com.hepdd.toms_storage.common.inventory.TerminalNetworkService;
import com.hepdd.toms_storage.common.tile.TileEntityCraftingTerminal;
import com.hepdd.toms_storage.compat.nei.AutoCraftPreviewInventory;
import com.hepdd.toms_storage.utils.StorageUtils;

public class CraftingTerminalContainer extends StorageTerminalContainer {

    private static final int CRAFT_ALL_GUARD = 1024;

    protected final TileEntityCraftingTerminal terminal;
    protected final IItemHandlerModifiable craftingInventory;
    private final InventoryCraftingWrapper inventoryCrafting;
    private final AutoCraftPreviewInventory autoCraftPreview = new AutoCraftPreviewInventory();
    private ModularCraftingSlot craftingSlot;
    private final TerminalCraftSession craftSession;

    public CraftingTerminalContainer(TileEntityCraftingTerminal terminal, IItemHandlerModifiable craftingInventory) {
        super(terminal);
        this.terminal = terminal;
        this.craftingInventory = craftingInventory;
        this.inventoryCrafting = new InventoryCraftingWrapper(this, 3, 3, craftingInventory, 0);
        this.craftSession = new TerminalCraftSession(terminal);
    }

    public TerminalCraftSession getCraftSession() {
        return craftSession;
    }

    public AutoCraftPreviewInventory getAutoCraftPreview() {
        return autoCraftPreview;
    }

    public void runAutoCraft(NBTTagCompound recipeTag) {
        if (!TomsConfig.neiAutocrafting) return;
        if (!(getSyncManager().getPlayer() instanceof EntityPlayerMP serverPlayer)) return;
        AutoCraftRequest request = AutoCraftRequest.fromNbt(recipeTag, true, true);
        ItemStack output = request.getOutput();
        if (output == null || output.stackSize <= 0) return;
        CraftOutcome outcome = AutoCraftExecutor.execute(request, serverPlayer, terminal);
        if (!outcome.isSuccess()) AutoCraftDebug.log("autocraft failed: " + outcome.getReason());
        serverPlayer.inventory.markDirty();
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        this.inventoryCrafting.detectChanges();
    }

    @Override
    public void registerSlot(String panelName, ModularSlot slot) {
        super.registerSlot(panelName, slot);
        if (slot instanceof ModularCraftingSlot craftingSlot1) {
            if (this.craftingSlot != null && this.craftingSlot != craftingSlot1) {
                throw new IllegalStateException("Only one crafting output slot is supported!");
            }
            this.craftingSlot = craftingSlot1;
            craftingSlot1.setCraftMatrix(this.inventoryCrafting);
        }
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        if (getGuiData().isClient()) return;
        this.craftingSlot.updateResult(
            craftSession.isSpent() ? null
                : CraftingManager.getInstance()
                    .findMatchingRecipe(this.inventoryCrafting, getPlayer().worldObj));
    }

    @Override
    public ItemStack slotClick(int slotId, int mouseButton, int mode, EntityPlayer player) {
        boolean onResult = slotId >= 0 && slotId < this.inventorySlots.size()
            && getModularSlot(slotId) instanceof ModularCraftingSlot;
        if (!onResult) return super.slotClick(slotId, mouseButton, mode, player);

        if (terminal.isCraftToStorage()) {
            if (!player.worldObj.isRemote) craftIntoStorage(player, mode == 1);
            return null;
        }
        craftSession.begin(mode == 1);
        try {
            return super.slotClick(slotId, mouseButton, mode, player);
        } finally {
            craftSession.end();
            if (!player.worldObj.isRemote) onCraftMatrixChanged(inventoryCrafting);
        }
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!player.worldObj.isRemote) dumpMatrixToNetwork();
    }

    public void clearGrid() {
        dumpMatrixToNetwork();
        onCraftMatrixChanged(inventoryCrafting);
    }

    private void dumpMatrixToNetwork() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = craftingInventory.getStackInSlot(i);
            if (stack != null) {
                craftingInventory.setStackInSlot(i, null);
                TerminalNetworkService.pushOrDrop(terminal, stack);
            }
        }
    }

    public void fillRecipe(ItemStack[] targets) {
        EntityPlayer player = getSyncManager().getPlayer();
        dumpMatrixToNetwork();
        for (int i = 0; i < 9 && i < targets.length; i++) {
            ItemStack target = targets[i];
            if (target == null) continue;
            int want = Math.max(1, target.stackSize);
            ItemStack pulled = TerminalNetworkService.pull(terminal, target, want);
            int got = pulled == null ? 0 : pulled.stackSize;
            if (got < want) {
                ItemStack fromPlayer = takeFromPlayer(player, target, want - got);
                if (fromPlayer != null) {
                    if (pulled == null) pulled = fromPlayer;
                    else pulled.stackSize += fromPlayer.stackSize;
                }
            }
            if (pulled != null) craftingInventory.setStackInSlot(i, pulled);
        }
        onCraftMatrixChanged(inventoryCrafting);
    }

    private static ItemStack takeFromPlayer(EntityPlayer player, ItemStack target, int count) {
        ItemStack result = null;
        for (int i = 0; i < player.inventory.mainInventory.length && count > 0; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (stack != null && StorageUtils.areItemStacksEqual(stack, target, true)) {
                int take = Math.min(count, stack.stackSize);
                if (result == null) {
                    result = stack.copy();
                    result.stackSize = take;
                } else {
                    result.stackSize += take;
                }
                stack.stackSize -= take;
                count -= take;
                if (stack.stackSize <= 0) player.inventory.mainInventory[i] = null;
            }
        }
        return result;
    }

    private void craftIntoStorage(EntityPlayer player, boolean craftAll) {
        craftSession.begin(craftAll);
        int planned = craftAll ? Math.min(
            CRAFT_ALL_GUARD,
            craftSession.plannedCrafts(
                CraftingManager.getInstance()
                    .findMatchingRecipe(inventoryCrafting, player.worldObj)))
            : 1;

        boolean crafted = false;
        for (int i = 0; i < planned; i++) {
            ItemStack result = craftSession.craftOnce(inventoryCrafting, player);
            if (result == null) break;
            TerminalNetworkService.pushOrDrop(terminal, result);
            crafted = true;
        }
        craftSession.end();
        if (crafted) {
            onCraftMatrixChanged(inventoryCrafting);
            detectAndSendChanges();
        }
    }
}
