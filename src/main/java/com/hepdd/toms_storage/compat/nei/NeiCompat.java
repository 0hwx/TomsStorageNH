package com.hepdd.toms_storage.compat.nei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.hepdd.toms_storage.compat.mui.container.CraftingTerminalContainer;
import com.hepdd.toms_storage.compat.mui.sync.TerminalGridSyncHandler;
import com.hepdd.toms_storage.utils.StorageUtils;

import codechicken.nei.LayoutManager;
import codechicken.nei.PositionedStack;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.IRecipeHandler;

// Use MUI2 Integration with nei
public final class NeiCompat {

    private static final int GRID_X = 25;
    private static final int GRID_Y = 6;
    private static final int SLOT = 18;

    // Placement recorded by positionStacks, drawn by ItemSlotTerminalCraftingMatrix. Indexed by 3x3 matrix slot.
    private static final ItemStack[] GHOSTS = new ItemStack[9];

    private static int nextAutoCraftRequestId;

    private NeiCompat() {}

    public static String[] getIdents() {
        return new String[] { "crafting" };
    }

    // Map the recipe's ingredient positions to the 3x3 matrix and ask the server (via the grid channel) to fill.
    // Each slot's count comes from the multiplier (shift = max stack); the permutation prefers held items.
    public static int transferRecipe(CraftingTerminalContainer container, GuiContainerWrapper gui,
        IRecipeHandler recipe, int recipeIndex, int multiplier) {
        TerminalGridSyncHandler grid = grid(container);
        if (grid == null) return 0;
        List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);
        List<ItemStack> pool = availableItems(container, gui);
        ItemStack[] targets = new ItemStack[9];
        boolean any = false;
        for (PositionedStack ingredient : ingredients) {
            int index = matrixIndex(ingredient);
            if (index < 0) continue;
            ItemStack pick = choosePermutation(ingredient, pool);
            if (pick == null) continue;
            ItemStack target = pick.copy();
            target.stackSize = Math.max(1, Math.min(multiplier, pick.getMaxStackSize()));
            targets[index] = target;
            any = true;
        }
        if (!any) return 0;
        grid.requestFill(targets);
        return 1;
    }

    // Presence dots on the "+" button: an ingredient is present if it's in the player inventory OR the network.
    public static List<GuiOverlayButton.ItemOverlayState> presenceOverlay(CraftingTerminalContainer container,
        GuiContainerWrapper gui, IRecipeHandler recipe, int recipeIndex) {
        List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);
        List<ItemStack> available = availableItems(container, gui);
        List<GuiOverlayButton.ItemOverlayState> states = new ArrayList<>();
        for (PositionedStack ingredient : ingredients) {
            boolean present = false;
            for (ItemStack candidate : available) {
                if (candidate.stackSize > 0 && ingredient.contains(candidate)) {
                    candidate.stackSize--;
                    present = true;
                    break;
                }
            }
            states.add(new GuiOverlayButton.ItemOverlayState(ingredient, present));
        }
        return states;
    }

    // Ghost overlay, data half. MUI2 hands NEI every slot at a fixed (1,1) in ItemSlot#renderSlotUnderlayNEI
    // (the GL matrix is already at the widget origin), and DefaultOverlayRenderer picks its slot by comparing
    // relx/rely against that. So no coordinate we write here can ever identify one matrix slot -- writing 1,1
    // would instead draw every ingredient into every empty slot in the GUI. We therefore record the placement
    // and draw it ourselves in ItemSlotTerminalCraftingMatrix, leaving relx/rely at the recipe coords (25/43/61 by
    // 6/24/42,
    // never 1) so NEI's own renderer matches nothing.
    public static ArrayList<PositionedStack> positionStacks(CraftingTerminalContainer container,
        GuiContainerWrapper gui, ArrayList<PositionedStack> stacks) {
        Arrays.fill(GHOSTS, null);
        List<ItemStack> pool = availableItems(container, gui);
        for (PositionedStack stack : stacks) {
            int index = matrixIndex(stack);
            if (index < 0) continue;
            ItemStack pick = choosePermutation(stack, pool);
            if (pick != null) GHOSTS[index] = pick.copy();
        }
        return stacks;
    }

    // Gated on NEI's own overlay state so the lifecycle matches it: LayoutManager clears overlayRenderer on GUI
    // (re)load and on the clear-overlay hotkey.
    public static void drawGhost(int index) {
        if (index < 0 || index >= GHOSTS.length) return;
        ItemStack ghost = GHOSTS[index];
        if (ghost == null || LayoutManager.overlayRenderer == null) return;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(770, 1);
        GuiContainerManager.drawItem(1, 1, ghost);
        GL11.glPopAttrib();
        GuiDraw.drawRect(1, 1, 16, 16, 0x66555555);
    }

    public static boolean canCraft(CraftingTerminalContainer container, GuiContainerWrapper gui, IRecipeHandler recipe,
        int recipeIndex) {
        return grid(container) != null && getRecipeOutput(recipe, recipeIndex) != null;
    }

    // NEI auto-craft: serialise the recipe, simulate it against the client-side preview, then ship it
    // over the grid's C2S channel. Unlike the legacy path this needs no slot-position search -- matrixIndex
    // maps a PositionedStack to its matrix slot arithmetically.
    public static boolean craft(CraftingTerminalContainer container, GuiContainerWrapper gui, IRecipeHandler recipe,
        int recipeIndex, int multiplier) {
        TerminalGridSyncHandler grid = grid(container);
        if (grid == null) return false;

        ItemStack output = getRecipeOutput(recipe, recipeIndex);
        if (output == null) return false;

        int count = Math.max(1, multiplier);
        ItemStack[][] mapped = new ItemStack[RecipeNbtSerializer.GRID_SIZE][];
        for (int i = 0; i < mapped.length; i++) mapped[i] = new ItemStack[0];
        for (PositionedStack ingredient : recipe.getIngredientStacks(recipeIndex)) {
            int index = matrixIndex(ingredient);
            if (index < 0) return false;
            mapped[index] = ingredient.items;
        }

        NBTTagCompound recipeTag = RecipeNbtSerializer.writeRecipe(mapped, output, count);
        RecipeNbtSerializer.setPatterns(
            recipeTag,
            container.getAutoCraftPreview()
                .getPatterns());

        int requestId = nextAutoCraftRequestId++;
        if (!container.getAutoCraftPreview()
            .applyRequest(
                requestId,
                grid.clientCachedItems(),
                gui.mc.thePlayer.inventory,
                RecipeNbtSerializer.readIngredients(recipeTag),
                output,
                count)) {
            NEIDebug.log("craft abort: preview simulation failed");
            return false;
        }
        container.getAutoCraftPreview()
            .rememberPattern(recipeTag);

        grid.requestAutoCraft(recipeTag);
        return true;
    }

    // NEI handlers expose the result through getResultStack(int), which isn't on IRecipeHandler.
    private static ItemStack getRecipeOutput(IRecipeHandler recipe, int recipeIndex) {
        try {
            Object result = recipe.getClass()
                .getMethod("getResultStack", int.class)
                .invoke(recipe, recipeIndex);
            if (result instanceof PositionedStack positioned) {
                return positioned.item == null ? null : positioned.item.copy();
            }
            if (result instanceof ItemStack stack) return stack.copy();
        } catch (ReflectiveOperationException e) {
            NEIDebug.log(
                "craft output lookup failed handler=" + recipe.getClass()
                    .getName() + " error=" + e);
        }
        return null;
    }

    private static int matrixIndex(PositionedStack stack) {
        if (stack == null || stack.items == null || stack.items.length == 0) return -1;
        int col = (stack.relx - GRID_X) / SLOT;
        int row = (stack.rely - GRID_Y) / SLOT;
        return (col < 0 || col > 2 || row < 0 || row > 2) ? -1 : row * 3 + col;
    }

    // Prefer the ingredient permutation the player/network actually holds (so "any planks" uses ones you own),
    // consuming from the pool as it assigns; falls back to the first permutation if none are available.
    private static ItemStack choosePermutation(PositionedStack ingredient, List<ItemStack> pool) {
        for (ItemStack option : ingredient.items) {
            if (option == null) continue;
            for (ItemStack have : pool) {
                if (have.stackSize > 0 && StorageUtils.areItemStacksEqual(have, option, true)) {
                    have.stackSize--;
                    return option;
                }
            }
        }
        for (ItemStack option : ingredient.items) {
            if (option != null) return option;
        }
        return null;
    }

    // Player-accessible GUI slots (copies) + the network snapshot; sizes are mutable so callers can decrement.
    private static List<ItemStack> availableItems(CraftingTerminalContainer container, GuiContainerWrapper gui) {
        List<ItemStack> available = new ArrayList<>();
        for (Object object : gui.inventorySlots.inventorySlots) {
            Slot slot = (Slot) object;
            if (slot != null && slot.getStack() != null
                && slot.getStack().stackSize > 0
                && slot.isItemValid(slot.getStack())
                && slot.canTakeStack(gui.mc.thePlayer)) {
                available.add(
                    slot.getStack()
                        .copy());
            }
        }
        TerminalGridSyncHandler grid = grid(container);
        if (grid != null) available.addAll(grid.clientNetworkItems());
        return available;
    }

    public static TerminalGridSyncHandler grid(CraftingTerminalContainer container) {
        SyncHandler<?> handler = container.getSyncManager()
            .getMainPSM()
            .findSyncHandlerNullable(TerminalGridSyncHandler.SYNC_KEY, TerminalGridSyncHandler.SYNC_ID);
        return handler instanceof TerminalGridSyncHandler grid ? grid : null;
    }
}
