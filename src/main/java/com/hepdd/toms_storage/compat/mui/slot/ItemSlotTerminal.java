package com.hepdd.toms_storage.compat.mui.slot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.hepdd.toms_storage.compat.mui.search.TerminalFormat;
import com.hepdd.toms_storage.compat.mui.sync.TerminalGridSyncHandler;
import com.hepdd.toms_storage.gui.SlotAction;

public class ItemSlotTerminal extends ItemSlot {

    private final TerminalGridSyncHandler grid;
    private final int index;

    public ItemSlotTerminal(TerminalGridSyncHandler grid, int index, IItemHandlerModifiable view) {
        this.grid = grid;
        this.index = index;
        slot(new TerminalGridSlot(grid, index, view));
    }

    @Override
    public boolean handleAsVanillaSlot() {
        return false;
    }

    @Override
    public Interactable.Result onMousePressed(int mouseButton) {
        boolean shift = Interactable.hasShiftDown();
        SlotAction action = mouseButton == 1 ? (shift ? SlotAction.PULL_ONE : SlotAction.GET_HALF)
            : (shift ? SlotAction.SHIFT_PULL : SlotAction.PULL_OR_PUSH_STACK);
        grid.click(index, action);
        return Interactable.Result.SUCCESS;
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        return true;
    }

    @Override
    public void onMouseDrag(int mouseButton, long timeSinceClick) {}

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        grid.scroll(scrollDirection == UpOrDown.UP ? -1 : 1);
        return true;
    }

    @Override
    protected void drawSlotAmountText(int amount, String format) {
        int count = grid.getCellCount(index);
        if (count <= 0) return;

        String text = TerminalFormat.compact(count);
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        float scale = 0.6f;
        float inverse = 1f / scale;

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1f);
        int x = (int) ((1f + 16f - fr.getStringWidth(text) * scale) * inverse);
        int y = (int) ((1f + 16f - 7f * scale) * inverse);
        fr.drawStringWithShadow(text, x, y, 0xFFFFFF);
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}
