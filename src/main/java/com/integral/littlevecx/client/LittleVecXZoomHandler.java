package com.integral.littlevecx.client;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleVecXZoomHandler {

    private static final float MIN_ZOOM_FACTOR = 0.10F;
    private static final float MAX_ZOOM_FACTOR = 1.00F;
    private static final float ZOOM_STEP = 0.05F;
    private static boolean registered;
    private static boolean zoomEnabled;
    /** FOV multiplier: 1 means no zoom, lower values mean a closer view. */
    private static float zoomFactor = MAX_ZOOM_FACTOR;

    public static void register() {
        if (registered)
            return;

        MinecraftForge.EVENT_BUS.register(new LittleVecXZoomHandler());
        registered = true;
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END)
            return;

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null) {
            zoomEnabled = false;
            return;
        }
    }

    @SubscribeEvent
    public void onFovUpdate(FOVUpdateEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!zoomEnabled || minecraft.currentScreen != null)
            return;

        float zoomedFov = MathHelper.clamp(event.getNewfov() * zoomFactor, 0.05F, 1.5F);
        event.setNewfov(zoomedFov);
    }

    @SubscribeEvent
    public void onMouseWheel(MouseEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.currentScreen != null)
            return;

        // Middle click is an immediate escape from zoom. Consume it only while zoom
        // is active so the normal pick-block action remains available otherwise.
        if (event.getButton() == 2 && event.isButtonstate() && zoomEnabled) {
            zoomEnabled = false;
            minecraft.player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.zoom.off"), true);
            event.setCanceled(true);
            return;
        }

        if (event.getDwheel() == 0)
            return;

        if (!isAltDown())
            return;

        // Wheel forward zooms in, wheel back zooms out. Alt makes this adjustment
        // deliberate and allows the wheel to enable zoom from any current state.
        float direction = event.getDwheel() > 0 ? -1F : 1F;
        zoomFactor = MathHelper.clamp(zoomFactor + direction * ZOOM_STEP, MIN_ZOOM_FACTOR, MAX_ZOOM_FACTOR);
        if (zoomFactor >= MAX_ZOOM_FACTOR) {
            zoomFactor = MAX_ZOOM_FACTOR;
            zoomEnabled = false;
            minecraft.player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.zoom.off"), true);
        } else {
            zoomEnabled = true;
            int percentage = Math.round((1F / zoomFactor) * 100F);
            minecraft.player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.zoom.level", percentage), true);
        }
        event.setCanceled(true);
    }

    private static boolean isAltDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

}
