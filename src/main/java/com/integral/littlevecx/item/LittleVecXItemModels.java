package com.integral.littlevecx.item;

import com.creativemd.creativecore.client.CreativeCoreClient;
import com.creativemd.creativecore.client.rendering.model.CreativeBlockRenderHelper;
import com.creativemd.littletiles.LittleTiles;
import com.integral.littlevecx.LittleVecXMod;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = LittleVecXMod.MODID, value = Side.CLIENT)
public final class LittleVecXItemModels {

    private LittleVecXItemModels() {}

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        CreativeCoreClient.registerItemRenderer(LittleVecXItems.LITTLEVECX_PLIERS);
        CreativeCoreClient.registerItemRenderer(LittleVecXItems.LITTLEVECX_INDUSTRIAL_TOOL);
        CreativeCoreClient.registerItemRenderer(LittleVecXItems.LITTLEVECX_INDUSTRIAL_CHISEL);
        CreativeCoreClient.registerItemRenderer(LittleVecXItems.LITTLEVECX_DEBUG_BLAZE_ROD);
        CreativeBlockRenderHelper.registerCreativeRenderedItem(LittleVecXItems.LITTLEVECX_INDUSTRIAL_TOOL);
        CreativeBlockRenderHelper.registerCreativeRenderedItem(LittleVecXItems.LITTLEVECX_INDUSTRIAL_CHISEL);

        ModelLoader.setCustomModelResourceLocation(
                LittleVecXItems.LITTLEVECX_PLIERS,
                0,
                new ModelResourceLocation(LittleVecXItems.LITTLEVECX_PLIERS.getRegistryName(), "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
                LittleVecXItems.LITTLEVECX_INDUSTRIAL_TOOL,
                0,
                new ModelResourceLocation(LittleVecXItems.LITTLEVECX_INDUSTRIAL_TOOL.getRegistryName(), "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
                LittleVecXItems.LITTLEVECX_INDUSTRIAL_TOOL,
                1,
                new ModelResourceLocation(LittleVecXMod.MODID + ":industrial_recipe_background", "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
                LittleVecXItems.LITTLEVECX_INDUSTRIAL_CHISEL,
                0,
                new ModelResourceLocation(LittleVecXItems.LITTLEVECX_INDUSTRIAL_CHISEL.getRegistryName(), "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
                LittleVecXItems.LITTLEVECX_INDUSTRIAL_CHISEL,
                1,
                new ModelResourceLocation(LittleTiles.modid + ":chisel_background", "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
                LittleVecXItems.LITTLEVECX_DEBUG_BLAZE_ROD,
                0,
                new ModelResourceLocation(LittleVecXItems.LITTLEVECX_DEBUG_BLAZE_ROD.getRegistryName(), "inventory")
        );
    }
}
