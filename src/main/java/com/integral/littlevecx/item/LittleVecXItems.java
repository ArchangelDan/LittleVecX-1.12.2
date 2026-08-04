package com.integral.littlevecx.item;

import com.integral.littlevecx.LittleVecXMod;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = LittleVecXMod.MODID)
public final class LittleVecXItems {

    public static final ItemLittleVecXPliers LITTLEVECX_PLIERS = new ItemLittleVecXPliers();
    public static final ItemLittleVecXIndustrialTool LITTLEVECX_INDUSTRIAL_TOOL = new ItemLittleVecXIndustrialTool();
    public static final ItemLittleVecXIndustrialChisel LITTLEVECX_INDUSTRIAL_CHISEL = new ItemLittleVecXIndustrialChisel();
    public static final ItemLittleVecXDebugBlazeRod LITTLEVECX_DEBUG_BLAZE_ROD = new ItemLittleVecXDebugBlazeRod();

    private LittleVecXItems() {}

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(LITTLEVECX_PLIERS);
        event.getRegistry().register(LITTLEVECX_INDUSTRIAL_TOOL);
        event.getRegistry().register(LITTLEVECX_DEBUG_BLAZE_ROD);
    }
}
