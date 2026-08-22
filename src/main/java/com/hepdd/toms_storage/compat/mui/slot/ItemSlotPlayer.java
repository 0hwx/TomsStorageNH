package com.hepdd.toms_storage.compat.mui.slot;

import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;

public class ItemSlotPlayer extends ItemSlot {

    private static final int KEY_SPACE = Keyboard.KEY_SPACE;

    public static final String DEPOSIT_MATCHING = "deposit_matching";

    @Override
    public boolean handleAsVanillaSlot() {
        return !Interactable.isKeyPressed(KEY_SPACE);
    }

    @Override
    public Interactable.Result onMousePressed(int mouseButton) {
        if (Interactable.isKeyPressed(KEY_SPACE)) {
            ItemStack stack = getSlot().getStack();
            if (stack != null) {
                getSyncHandler().getSyncManager()
                    .callSyncedAction(DEPOSIT_MATCHING, buf -> NetworkUtils.writeItemStack(buf, stack));
            }
            return Interactable.Result.SUCCESS;
        }
        return super.onMousePressed(mouseButton);
    }
}
