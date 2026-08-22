package com.hepdd.toms_storage.compat.mui.container;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.integration.nei.INEIRecipeTransfer;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.widgets.slot.InventoryCraftingWrapper;
import com.cleanroommc.modularui.widgets.slot.ModularCraftingSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.hepdd.toms_storage.common.crafting.AutoCraftRequest;
import com.hepdd.toms_storage.common.crafting.SingleAutoCraftExecutor;
import com.hepdd.toms_storage.common.crafting.TerminalCraftSession;
import com.hepdd.toms_storage.common.inventory.TerminalNetworkService;
import com.hepdd.toms_storage.common.tile.TileEntityCraftingTerminal;
import com.hepdd.toms_storage.compat.nei.AutoCraftPreviewInventory;
import com.hepdd.toms_storage.compat.nei.NeiCompat;
import com.hepdd.toms_storage.utils.StorageUtils;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.Optional;

@Optional.Interface(iface = "com.cleanroommc.modularui.integration.nei.INEIRecipeTransfer", modid = "NotEnoughItems")
public class CraftingTerminalContainer extends StorageTerminalContainer
    implements INEIRecipeTransfer<GuiContainerWrapper> {

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

    public IItemHandlerModifiable getCraftingInventory() {
        return craftingInventory;
    }

    public TerminalCraftSession getCraftSession() {
        return craftSession;
    }

    public AutoCraftPreviewInventory getAutoCraftPreview() {
        return autoCraftPreview;
    }

    // Server side, driven by TerminalGridSyncHandler's ID_AUTOCRAFT.
    public void runAutoCraft(NBTTagCompound recipeTag) {
        if (!(getSyncManager().getPlayer() instanceof EntityPlayerMP serverPlayer)) return;
        AutoCraftRequest request = new AutoCraftRequest(recipeTag, true, true);
        ItemStack output = request.getOutput();
        if (output == null || output.stackSize <= 0) return;
        SingleAutoCraftExecutor.execute(request, serverPlayer, terminal);
        serverPlayer.inventory.markDirty();
    }

    @Optional.Method(modid = "NotEnoughItems")
    @Override
    public String[] getIdents() {
        return NeiCompat.getIdents();
    }

    @Optional.Method(modid = "NotEnoughItems")
    @Override
    public int transferRecipe(GuiContainerWrapper gui, IRecipeHandler recipe, int recipeIndex, int multiplier) {
        return NeiCompat.transferRecipe(this, gui, recipe, recipeIndex, multiplier);
    }

    @Optional.Method(modid = "NotEnoughItems")
    @Override
    public List<GuiOverlayButton.ItemOverlayState> presenceOverlay(GuiContainerWrapper gui, IRecipeHandler recipe,
        int recipeIndex) {
        return NeiCompat.presenceOverlay(this, gui, recipe, recipeIndex);
    }

    @Optional.Method(modid = "NotEnoughItems")
    @Override
    public ArrayList<PositionedStack> positionStacks(GuiContainerWrapper gui, ArrayList<PositionedStack> stacks) {
        return NeiCompat.positionStacks(this, gui, stacks);
    }

    @Optional.Method(modid = "NotEnoughItems")
    @Override
    public boolean canCraft(GuiContainerWrapper gui, IRecipeHandler recipe, int recipeIndex) {
        return NeiCompat.canCraft(this, gui, recipe, recipeIndex);
    }

    @Optional.Method(modid = "NotEnoughItems")
    @Override
    public boolean craft(GuiContainerWrapper gui, IRecipeHandler recipe, int recipeIndex, int multiplier) {
        return NeiCompat.craft(this, gui, recipe, recipeIndex, multiplier);
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
        // Blanking the result is what stops MUI's quick-move loop: it only repeats while the result slot
        // still holds something that stacks with what it just moved.
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

        // In craft-to-storage mode, clicking the result crafts straight into the network (shift = craft all).
        if (terminal.isCraftToStorage()) {
            if (!player.worldObj.isRemote) craftIntoStorage(player, mode == 1);
            return null;
        }
        // Craft-to-inventory runs MUI's own quick-move loop, so the budget has to span the whole click.
        craftSession.begin(mode == 1);
        try {
            return super.slotClick(slotId, mouseButton, mode, player);
        } finally {
            craftSession.end();
            // The burst may have blanked the result to break the loop; restore it now the click is over.
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

    /**
     * We drive this loop ourselves, so the craft count is settled up front the way AE2 does it rather than
     * re-tested after every craft. The guard still caps an unbounded craft-all.
     */
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
