package com.integral.littlevecx.client.overlay;

import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.mc.ContainerSub;
import com.creativemd.creativecore.common.gui.mc.IVanillaGUI;
import com.creativemd.littletiles.client.gui.SubGuiChisel;
import com.creativemd.littletiles.client.gui.SubGuiColorTube;
import com.creativemd.littletiles.client.gui.SubGuiGrabber;
import com.creativemd.littletiles.client.gui.SubGuiScrewdriver;
import com.integral.littlevecx.client.gui.SubGuiLittleVecXIndustrialChisel;
import com.integral.littlevecx.client.gui.SubGuiLittleVecXIndustrialScrewdriver;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleVecXSubGuiOverlayHandler {

    private static boolean registered = false;
    private static boolean overridesInitialized = false;
    private static boolean opened = false;

    private static SubGui previousTopLayer;

    public static void register() {
        if (registered)
            return;

        if (!overridesInitialized) {
            new LittleVecXSignalDialogOverride();
            new LittleVecXColorCopyPasteOverride<SubGuiScrewdriver>(SubGuiScrewdriver.class);
            new LittleVecXColorCopyPasteOverride<SubGuiChisel>(SubGuiChisel.class);
            new LittleVecXColorCopyPasteOverride<SubGuiColorTube>(SubGuiColorTube.class);
            new LittleVecXColorCopyPasteOverride<SubGuiGrabber>(SubGuiGrabber.class);
            new LittleVecXColorCopyPasteOverride<SubGuiLittleVecXIndustrialChisel>(SubGuiLittleVecXIndustrialChisel.class);
            new LittleVecXColorCopyPasteOverride<SubGuiLittleVecXIndustrialScrewdriver>(SubGuiLittleVecXIndustrialScrewdriver.class);
            overridesInitialized = true;
        }

        MinecraftForge.EVENT_BUS.register(new LittleVecXSubGuiOverlayHandler());
        registered = true;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.side != Side.CLIENT || event.phase != TickEvent.Phase.START)
            return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null) {
            resetState();
            return;
        }

        SubGui currentTopLayer = resolveTopLayer();
        if (currentTopLayer == null) {
            resetState();
            return;
        }

        if (!opened) {
            LittleVecXSubGuiOverride.apply(currentTopLayer);
            opened = true;
            previousTopLayer = currentTopLayer;
        } else if (currentTopLayer != previousTopLayer) {
            LittleVecXSubGuiOverride.apply(currentTopLayer);
            previousTopLayer = currentTopLayer;
        }
    }

    private static SubGui resolveTopLayer() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null)
            return null;
        if (!(mc.currentScreen instanceof IVanillaGUI))
            return null;
        if (!(mc.player.openContainer instanceof ContainerSub))
            return null;

        ContainerSub container = (ContainerSub) mc.player.openContainer;
        if (container.gui == null)
            return null;
        return container.gui.getTopLayer();
    }

    private static void resetState() {
        opened = false;
        previousTopLayer = null;
    }
}
