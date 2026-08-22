package com.hepdd.toms_storage.common.crafting.autocrafting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.hepdd.toms_storage.common.crafting.CraftingRemainderHandler;
import com.hepdd.toms_storage.common.inventory.TerminalNetworkService;
import com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal;
import com.hepdd.toms_storage.utils.StorageUtils;

/**
 * Turns a planned auto-craft into real item movement, all of it or none of it.
 *
 * <p>
 * Nothing is written back to the network until every extraction the plan needs has succeeded. Sub-craft
 * outputs and leftovers are held in a local buffer rather than round-tripped through storage, so a plan
 * that fails halfway leaves no trace: the buffer is discarded and the real takes are handed back.
 */
public final class AutoCraftExecutor {

    private final TileEntityStorageTerminal terminal;
    private final EntityPlayerMP player;

    /** Items produced by the plan and still owed to a later operation, or to the player at the end. */
    private final List<ItemStack> buffer = new ArrayList<>();
    /** Real takes, kept so a failure can put them back exactly where they came from. */
    private final List<ItemStack> takenFromNetwork = new ArrayList<>();
    private final List<ItemStack> takenFromPlayer = new ArrayList<>();
    private final List<ItemStack> finalOutputs = new ArrayList<>();

    private AutoCraftExecutor(TileEntityStorageTerminal terminal, EntityPlayerMP player) {
        this.terminal = terminal;
        this.player = player;
    }

    public static CraftOutcome execute(AutoCraftRequest request, EntityPlayerMP player,
        TileEntityStorageTerminal terminal) {
        CraftingPattern root = request.root();
        if (root == null || root.getOutput() == null) return CraftOutcome.failure("missing_output");

        CraftingInventorySnapshot snapshot = new CraftingInventorySnapshot(
            terminal,
            player,
            request.usePlayerInventory());
        CraftOutcome planned = new CraftingPlanner(player, snapshot, request.patterns(), request.allowSubCrafting())
            .plan(root, request.count());
        if (!planned.isSuccess()) return planned;

        return new AutoCraftExecutor(terminal, player).apply(planned.getPlan());
    }

    private CraftOutcome apply(CraftingPlan plan) {
        for (CraftingPlan.CraftOperation operation : plan.getOperations()) {
            CraftOutcome taken = takeIngredients(operation);
            if (!taken.isSuccess()) {
                rollback();
                return taken;
            }
            produce(operation);
        }
        commit();
        return CraftOutcome.success();
    }

    private CraftOutcome takeIngredients(CraftingPlan.CraftOperation operation) {
        for (Extraction extraction : operation.getExtractions()) {
            if (extraction == null) continue;
            ItemStack wanted = extraction.getStack();
            ItemStack actual = take(extraction);
            if (actual == null || actual.stackSize < wanted.stackSize
                || !StorageUtils.areItemStacksEqual(wanted, actual, true)) {
                AutoCraftDebug.log(
                    "autocraft apply missing source=" + extraction.getSource()
                        + " expected="
                        + AutoCraftDebug.stack(wanted)
                        + " actual="
                        + AutoCraftDebug.stack(actual));
                if (actual != null) record(extraction.getSource(), actual);
                return CraftOutcome.failure(reasonFor(extraction.getSource()));
            }
            record(extraction.getSource(), actual);
        }
        return CraftOutcome.success();
    }

    private ItemStack take(Extraction extraction) {
        ItemStack wanted = extraction.getStack();
        switch (extraction.getSource()) {
            case NETWORK:
                return TerminalNetworkService.pull(terminal, wanted, wanted.stackSize);
            case PLAYER:
                return player.inventory.decrStackSize(extraction.getPlayerSlot(), wanted.stackSize);
            default:
                return takeFromBuffer(wanted);
        }
    }

    /** Only real takes are recorded; a buffer take was never anyone else's item to begin with. */
    private void record(Extraction.Source source, ItemStack actual) {
        if (source == Extraction.Source.NETWORK) takenFromNetwork.add(actual.copy());
        else if (source == Extraction.Source.PLAYER) takenFromPlayer.add(actual.copy());
    }

    private void produce(CraftingPlan.CraftOperation operation) {
        for (Extraction extraction : operation.getExtractions()) {
            if (extraction == null) continue;
            ItemStack remainder = CraftingRemainderHandler.getRemainder(extraction.getStack());
            if (remainder != null) addToBuffer(remainder);
        }
        ItemStack output = operation.getOutput();
        if (output == null) return;
        output.stackSize = Math.max(1, output.stackSize) * operation.getTimes();
        if (operation.isFinalOutput()) finalOutputs.add(output);
        else addToBuffer(output);
    }

    /** Everything survived: the player gets what they asked for, the rest goes to storage. */
    private void commit() {
        for (ItemStack output : finalOutputs) {
            if (!player.inventory.addItemStackToInventory(output)) TerminalNetworkService.pushOrDrop(terminal, output);
        }
        for (ItemStack leftover : buffer) {
            TerminalNetworkService.pushOrDrop(terminal, leftover);
        }
        player.inventory.markDirty();
    }

    private void rollback() {
        for (ItemStack stack : takenFromNetwork) {
            TerminalNetworkService.pushOrDrop(terminal, stack);
        }
        for (ItemStack stack : takenFromPlayer) {
            if (!player.inventory.addItemStackToInventory(stack)) TerminalNetworkService.pushOrDrop(terminal, stack);
        }
        player.inventory.markDirty();
    }

    private ItemStack takeFromBuffer(ItemStack wanted) {
        for (int i = 0; i < buffer.size(); i++) {
            ItemStack held = buffer.get(i);
            if (!StorageUtils.areItemStacksEqual(held, wanted, true) || held.stackSize < wanted.stackSize) continue;
            ItemStack taken = held.copy();
            taken.stackSize = wanted.stackSize;
            held.stackSize -= wanted.stackSize;
            if (held.stackSize <= 0) buffer.remove(i);
            return taken;
        }
        return null;
    }

    private void addToBuffer(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return;
        for (ItemStack held : buffer) {
            if (StorageUtils.areItemStacksEqual(held, stack, true)) {
                held.stackSize += stack.stackSize;
                return;
            }
        }
        buffer.add(stack.copy());
    }

    private static String reasonFor(Extraction.Source source) {
        switch (source) {
            case PLAYER:
                return "apply_missing_player_item";
            case INTERMEDIATE:
                return "apply_missing_intermediate";
            default:
                return "apply_missing_terminal_item";
        }
    }
}
