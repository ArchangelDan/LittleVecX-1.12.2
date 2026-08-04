package com.integral.littlevecx.client;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleVecXScrewdriverProgressClientHandler extends Gui {

    private static final int VISIBLE_TICKS_AFTER_DONE = 30;
    private static final int Y_OFFSET_FROM_BOTTOM = 82;

    private static boolean registered;
    private static UUID activeJobId;
    private static int processed;
    private static int total;
    private static int hideAfterTicks;

    public static void register() {
        if (registered)
            return;
        MinecraftForge.EVENT_BUS.register(new LittleVecXScrewdriverProgressClientHandler());
        registered = true;
    }

    public static void update(UUID jobId, int processedBatches, int totalBatches, boolean done) {
        activeJobId = jobId;
        processed = Math.max(0, processedBatches);
        total = Math.max(1, totalBatches);
        hideAfterTicks = done ? VISIBLE_TICKS_AFTER_DONE : -1;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT)
            return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null || activeJobId == null)
            return;

        if (hideAfterTicks == 0) {
            clear();
            return;
        }
        if (hideAfterTicks > 0)
            hideAfterTicks--;

        ScaledResolution resolution = event.getResolution();
        int width = 162;
        int height = 6;
        int x = (resolution.getScaledWidth() - width) / 2;
        int y = resolution.getScaledHeight() - Y_OFFSET_FROM_BOTTOM;
        float progress = Math.min(1F, Math.max(0F, processed / (float) total));
        int filled = Math.round(width * progress);

        drawRect(x - 1, y - 1, x + width + 1, y + height + 1, 0xAA000000);
        drawRect(x, y, x + width, y + height, 0xAA1B1B1B);
        drawRect(x, y, x + filled, y + height, hideAfterTicks > 0 ? 0xFF65C96A : 0xFF4AA3FF);

        String label = hideAfterTicks > 0 ? I18n.format("message.littlevecx.screwdriver.progress_done")
                : I18n.format("message.littlevecx.screwdriver.progress", Math.round(progress * 100F));
        int labelX = (resolution.getScaledWidth() - mc.fontRenderer.getStringWidth(label)) / 2;
        mc.fontRenderer.drawStringWithShadow(label, labelX, y - 11, 0xFFFFFF);
    }

    private static void clear() {
        activeJobId = null;
        processed = 0;
        total = 0;
        hideAfterTicks = 0;
    }
}
