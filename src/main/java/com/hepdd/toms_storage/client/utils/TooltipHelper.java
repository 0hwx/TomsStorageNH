package com.hepdd.toms_storage.client.utils;

import java.util.List;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class TooltipHelper {

    public static void addLines(List<String> tooltip, String key) {
        String text = I18n.format(key);
        for (String line : text.split("\\\\n|\\n")) {
            tooltip.add(line);
        }
    }
}
