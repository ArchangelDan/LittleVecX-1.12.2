package com.integral.littlevecx.client;

import javax.annotation.Nullable;

import com.creativemd.littletiles.client.render.overlay.PreviewRenderer;
import com.creativemd.littletiles.client.render.tile.LittleRenderBox;
import com.creativemd.littletiles.common.api.ILittlePlacer;
import com.creativemd.littletiles.common.tile.place.PlacePreview;
import com.creativemd.littletiles.common.tile.preview.LittlePreview;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.util.place.PlacementPreview;
import com.integral.littlevecx.LittleVecXConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleVecXPlacementFeedbackClientHandler {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final int INVALID_PREVIEW_COLOR = 0xFF5050;
    private static final int INVALID_PREVIEW_ALPHA = 180;

    private static boolean registered;

    public static void register() {
        if (registered)
            return;
        MinecraftForge.EVENT_BUS.register(new LittleVecXPlacementFeedbackClientHandler());
        registered = true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld() == null || !event.getWorld().isRemote)
            return;
        if (event.getHand() != EnumHand.MAIN_HAND)
            return;

        EntityPlayer player = event.getEntityPlayer();
        if (player == null)
            return;

        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ILittlePlacer))
            return;

        LittlePreviews previews = getBasePreviews(stack, false);
        if (!LittleVecXPlacementFeedbackHelper.supportsPreview(previews))
            return;

        PlacementPreview preview = LittleVecXPlacementFeedbackHelper.resolveCurrentPlacementPreview(event.getWorld(), stack, (ILittlePlacer) stack.getItem(), previews.copy());
        if (preview == null || LittleVecXPlacementFeedbackHelper.canPlacePreview(player, stack, preview))
            return;

        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.FAIL);
        player.sendStatusMessage(new net.minecraft.util.text.TextComponentTranslation("message.littlevecx.invalid_placement"), true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (MC.player == null || MC.world == null || MC.gameSettings.hideGUI)
            return;

        ItemStack stack = MC.player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ILittlePlacer))
            return;
        if (shouldUseNativeLowResolution(stack))
            return;

        LittlePreviews previews = getBasePreviews(stack, false);
        if (!LittleVecXPlacementFeedbackHelper.supportsPreview(previews))
            return;

        PlacementPreview preview = LittleVecXPlacementFeedbackHelper.resolveCurrentPlacementPreview(MC.world, stack, (ILittlePlacer) stack.getItem(), previews.copy());
        if (preview == null || LittleVecXPlacementFeedbackHelper.canPlacePreview(MC.player, stack, preview))
            return;

        LittlePreviews renderPreviews = getBasePreviews(stack, shouldAllowLowResolution());
        PlacementPreview renderPreview = renderPreviews == null ? null
                : LittleVecXPlacementFeedbackHelper.resolveCurrentPlacementPreview(MC.world, stack, (ILittlePlacer) stack.getItem(), renderPreviews.copy());
        renderInvalidPreview(renderPreview == null ? preview : renderPreview);
    }

    @Nullable
    private static LittlePreviews getBasePreviews(ItemStack stack, boolean allowLowResolution) {
        return stack.isEmpty() ? null : LittlePreview.getPreview(stack, allowLowResolution);
    }

    private static boolean shouldAllowLowResolution() {
        return PreviewRenderer.marked == null || PreviewRenderer.marked.allowLowResolution();
    }

    private static boolean shouldUseNativeLowResolution(ItemStack stack) {
        return LittleVecXConfig.optimizeLargeBlueprintPreviews && shouldAllowLowResolution() && stack.hasTagCompound()
                && stack.getTagCompound().hasKey("pos") && LittlePreview.getTotalCount(stack.getTagCompound()) >= LittlePreview.lowResolutionMode;
    }

    private static void renderInvalidPreview(PlacementPreview preview) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.enableTexture2D();
        MC.renderEngine.bindTexture(PreviewRenderer.WHITE_TEXTURE);
        GlStateManager.depthMask(false);

        double posX = preview.pos.getX() - TileEntityRendererDispatcher.staticPlayerX;
        double posY = preview.pos.getY() - TileEntityRendererDispatcher.staticPlayerY;
        double posZ = preview.pos.getZ() - TileEntityRendererDispatcher.staticPlayerZ;

        for (PlacePreview placePreview : preview.getPreviews()) {
            for (LittleRenderBox cube : placePreview.getPreviews(preview.context)) {
                cube.setColor(INVALID_PREVIEW_COLOR);
                cube.renderPreview(posX, posY, posZ, INVALID_PREVIEW_ALPHA);
            }
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
