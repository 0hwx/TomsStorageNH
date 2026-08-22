package com.hepdd.toms_storage.client;

import net.minecraft.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.DirectionBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.hepdd.toms_storage.TomsStorage;
import com.hepdd.toms_storage.client.model.CableStateProperties;
import com.hepdd.toms_storage.client.model.TerminalStateProperties;
import com.hepdd.toms_storage.common.CommonProxy;
import com.hepdd.toms_storage.common.block.BlockStorageTerminal.MountPos;
import com.hepdd.toms_storage.common.tile.TileEntityCraftingTerminal;
import com.hepdd.toms_storage.compat.mui.container.NeiCraftingTerminalContainer;
import com.hepdd.toms_storage.registry.ModBlocks;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(ConnectorRangePreview.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(ConnectorRangePreview.INSTANCE);
        super.preInit(event);
        registerJsonModels();
    }

    @Override
    public ModularContainer craftingTerminalContainer(TileEntityCraftingTerminal terminal,
        IItemHandlerModifiable craftingInventory) {
        return new NeiCraftingTerminalContainer(terminal, craftingInventory);
    }

    private void registerJsonModels() {
        ModelRegistry.registerModid(TomsStorage.MODID);

        BlockProperty<ForgeDirection> terminalFacing = TerminalStateProperties.facing();
        BlockProperty<String> terminalPos = TerminalStateProperties.pos();
        for (ModBlocks terminal : new ModBlocks[] { ModBlocks.STORAGE_TERMINAL, ModBlocks.CRAFTING_TERMINAL }) {
            if (!terminal.isEnabled()) continue;
            Block block = terminal.get();
            BlockPropertyRegistry.registerBlockItemProperty(block, terminalFacing, ForgeDirection.NORTH);
            BlockPropertyRegistry.registerBlockItemProperty(block, terminalPos, MountPos.CENTER.variant());
        }
        if (ModBlocks.INVENTORY_HOPPER.isEnabled()) {
            BlockPropertyRegistry.registerBlockItemProperty(
                ModBlocks.INVENTORY_HOPPER.get(),
                DirectionBlockProperty.facing(),
                ForgeDirection.DOWN);
        }
        if (ModBlocks.INVENTORY_CABLE.isEnabled()) {
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                BlockPropertyRegistry
                    .registerProperty(ModBlocks.INVENTORY_CABLE.get(), CableStateProperties.connection(side));
            }
        }
    }
}
