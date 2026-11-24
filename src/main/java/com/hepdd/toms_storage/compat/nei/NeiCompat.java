package com.hepdd.toms_storage.compat.nei;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.utils.item.ItemHandlerHelper;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.hepdd.toms_storage.TomsConfig;
import com.hepdd.toms_storage.common.crafting.autocrafting.AutoCraftDebug;
import com.hepdd.toms_storage.common.crafting.autocrafting.CraftingPattern;
import com.hepdd.toms_storage.common.crafting.autocrafting.RecipeNbtSerializer;
import com.hepdd.toms_storage.compat.mui.container.CraftingTerminalContainer;
import com.hepdd.toms_storage.compat.mui.sync.TerminalGridSyncHandler;
import com.hepdd.toms_storage.mixin.late.nei.AutoCraftingManagerAccessor;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.LayoutManager;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.recipe.AutoCraftingManager;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.Recipe.RecipeId;
import codechicken.nei.recipe.RecipeHandlerRef;
import codechicken.nei.recipe.chain.RecipeChainMath;

// Use MUI2 Integration with nei
public final class NeiCompat {

    private static final int GRID_X = 25;
    private static final int GRID_Y = 6;
    private static final int SLOT = 18;
    private static final int RECIPE_PACKET_BUDGET = 16 * 1024 - 512;

    private static final ItemStack[] GHOSTS = new ItemStack[9];

    private static int nextAutoCraftRequestId;

    public static String[] getIdents() {
        return new String[] { "crafting" };
    }

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

    public static void drawGhost(int index) {
        if (index < 0 || index >= GHOSTS.length) return;
        ItemStack ghost = GHOSTS[index];
        if (ghost == null || LayoutManager.overlayRenderer == null) return;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
        Platform.setupDrawItem();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(770, 1);
        GuiContainerManager.drawItem(1, 1, ghost);
        GlStateManager.disableRescaleNormal();
        Platform.endDrawItem();
        NEIClientUtils.gl2DRenderContext(() -> GuiDraw.drawRect(1, 1, 16, 16, 0x66555555));
        GL11.glPopAttrib();
    }

    public static boolean canCraft(CraftingTerminalContainer container, IRecipeHandler recipe, int recipeIndex) {
        return TomsConfig.neiAutocrafting && grid(container) != null && getRecipeOutput(recipe, recipeIndex) != null;
    }

    public static boolean craft(CraftingTerminalContainer container, GuiContainerWrapper gui, IRecipeHandler recipe,
        int recipeIndex, int multiplier) {
        if (!TomsConfig.neiAutocrafting) return false;
        TerminalGridSyncHandler grid = grid(container);
        if (grid == null) return false;
        AutoCraftPreviewInventory preview = container.getAutoCraftPreview();
        if (preview.isWaitingForServer()) return false;

        ItemStack output = getRecipeOutput(recipe, recipeIndex);
        if (output == null) return false;

        int count = Math.max(1, multiplier);
        ItemStack[][] mapped = mapIngredients(recipe, recipeIndex);
        if (mapped == null) return false;

        NBTTagCompound recipeTag = RecipeNbtSerializer.writeRecipe(mapped, output, count);
        int requestId = nextAutoCraftRequestId++;
        boolean previewed = preview.applyRequest(
            requestId,
            grid.clientCachedItems(),
            gui.mc.thePlayer.inventory,
            RecipeNbtSerializer.readIngredients(recipeTag),
            output,
            count);
        if (!previewed && AutoCraftingManager.processing()) addBookmarkDependencies(recipeTag, recipe, recipeIndex);
        grid.requestAutoCraft(requestId, recipeTag);
        // NEI repeats immediately while this returns true.
        return previewed;
    }

    private static ItemStack[][] mapIngredients(IRecipeHandler recipe, int recipeIndex) {
        ItemStack[][] mapped = new ItemStack[CraftingPattern.GRID_SIZE][];
        Arrays.fill(mapped, new ItemStack[0]);
        for (PositionedStack ingredient : recipe.getIngredientStacks(recipeIndex)) {
            int index = matrixIndex(ingredient);
            if (index < 0) return null;
            mapped[index] = ingredient.items;
        }
        return mapped;
    }

    private static void addBookmarkDependencies(NBTTagCompound recipeTag, IRecipeHandler handler, int recipeIndex) {
        RecipeChainMath chain = AutoCraftingManagerAccessor.tomsstorage$getMath();
        if (chain == null) return;
        RecipeId root = Recipe.of(handler, recipeIndex)
            .getRecipeId();
        Set<RecipeId> visited = new HashSet<>();
        visited.add(root);
        Deque<RecipeId> pending = new ArrayDeque<>();
        pending.add(root);

        NBTTagList dependencies = new NBTTagList();
        recipeTag.setTag(RecipeNbtSerializer.DEPENDENCIES_TAG, dependencies);
        while (!pending.isEmpty() && dependencies.tagCount() < RecipeNbtSerializer.MAX_DEPENDENCIES) {
            RecipeId parent = pending.removeFirst();
            for (Map.Entry<BookmarkItem, BookmarkItem> link : chain.preferredItems.entrySet()) {
                RecipeId child = link.getValue().recipeId;
                if (!parent.equals(link.getKey().recipeId) || child == null || !visited.add(child)) continue;

                RecipeHandlerRef ref = RecipeHandlerRef.of(child);
                if (ref == null) continue;
                ItemStack output = getRecipeOutput(ref.handler, ref.recipeIndex);
                ItemStack[][] ingredients = mapIngredients(ref.handler, ref.recipeIndex);
                if (output == null || ingredients == null) continue;

                dependencies.appendTag(RecipeNbtSerializer.writeRecipe(ingredients, output, 1));
                if (!fitsRecipePacket(recipeTag)) {
                    dependencies.removeTag(dependencies.tagCount() - 1);
                    break;
                }
                pending.addLast(child);
                if (dependencies.tagCount() >= RecipeNbtSerializer.MAX_DEPENDENCIES) break;
            }
        }
    }

    private static boolean fitsRecipePacket(NBTTagCompound recipeTag) {
        try {
            return CompressedStreamTools.compress(recipeTag).length <= RECIPE_PACKET_BUDGET;
        } catch (IOException ignored) {
            return false;
        }
    }

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
            AutoCraftDebug.log(
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

    private static ItemStack choosePermutation(PositionedStack ingredient, List<ItemStack> pool) {
        for (ItemStack option : ingredient.items) {
            if (option == null) continue;
            for (ItemStack have : pool) {
                if (have.stackSize > 0 && NEIServerUtils.areStacksSameTypeCraftingWithNBT(option, have)) {
                    have.stackSize--;
                    return ItemHandlerHelper.copyStackWithSize(have, 1);
                }
            }
        }
        for (ItemStack option : ingredient.items) {
            if (option != null) return option;
        }
        return null;
    }

    private static List<ItemStack> availableItems(CraftingTerminalContainer container, GuiContainerWrapper gui) {
        List<ItemStack> available = new ArrayList<>();
        for (Slot slot : gui.inventorySlots.inventorySlots) {
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
