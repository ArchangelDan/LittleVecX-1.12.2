package com.integral.littlevecx.client;

import javax.annotation.Nullable;

import com.creativemd.littletiles.client.LittleTilesClient;
import com.creativemd.littletiles.client.render.overlay.PreviewRenderer;
import com.creativemd.littletiles.client.render.tile.LittleRenderBox;
import com.creativemd.littletiles.common.api.ILittlePlacer;
import com.creativemd.littletiles.common.tile.place.PlacePreview;
import com.creativemd.littletiles.common.tile.preview.LittlePreview;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.util.place.IMarkMode;
import com.creativemd.littletiles.common.util.place.PlacementPosition;
import com.creativemd.littletiles.common.util.place.PlacementPreview;
import com.integral.littlevecx.furniture.LittleVecXFurniturePlacementHelper;
import com.integral.littlevecx.furniture.StructureLittleVecXFurniture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleVecXFurnitureClientHandler {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final int INVALID_PREVIEW_COLOR = 0xFF5050;
    private static final int INVALID_PREVIEW_ALPHA = 180;

    private static boolean registered;

    public static void register() {
        if (registered)
            return;
        MinecraftForge.EVENT_BUS.register(new LittleVecXFurnitureClientHandler());
        registered = true;
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END || MC.player == null || MC.world == null) {
            clearAutoFurnitureMark();
            return;
        }

        ItemStack current = MC.player.getHeldItemMainhand();
        if (!(current.getItem() instanceof ILittlePlacer)) {
            clearAutoFurnitureMark();
            return;
        }
        if (!LittleVecXFurniturePlacementHelper.isFurnitureStack(current)) {
            clearAutoFurnitureMark();
            return;
        }

        LittlePreviews previews = getBasePreviews(current, false);
        if (!LittleVecXFurniturePlacementHelper.isFurniturePreview(previews)) {
            clearAutoFurnitureMark();
            return;
        }

        while (LittleTilesClient.flip.isPressed()) {
            // Furniture recipes are not meant to be mirrored.
        }

        updateAutoFurnitureMagnet(current, (ILittlePlacer) current.getItem(), previews.copy());
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
        if (!LittleVecXFurniturePlacementHelper.isFurnitureStack(stack))
            return;

        LittlePreviews previews = getBasePreviews(stack, false);
        if (!LittleVecXFurniturePlacementHelper.isFurniturePreview(previews))
            return;

        updateAutoFurnitureMagnet(stack, (ILittlePlacer) stack.getItem(), previews.copy());

        PlacementPreview preview = getCurrentPreview(stack, (ILittlePlacer) stack.getItem(), previews.copy());
        if (preview == null)
            return;

        boolean validSurface = LittleVecXFurniturePlacementHelper.canPlaceOnFloorOnly(event.getWorld(), previews, preview);
        boolean validPlacement = validSurface && LittleVecXPlacementFeedbackHelper.canPlacePreview(player, stack, preview);
        if (!validPlacement) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.FAIL);
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentTranslation(
                    validSurface ? "message.littlevecx.invalid_placement" : "message.littlevecx.invalid_surface"), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (MC.player == null || MC.world == null || MC.gameSettings.hideGUI)
            return;

        ItemStack stack = MC.player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ILittlePlacer))
            return;
        if (!LittleVecXFurniturePlacementHelper.isFurnitureStack(stack))
            return;

        LittlePreviews previews = getBasePreviews(stack, false);
        if (!LittleVecXFurniturePlacementHelper.isFurniturePreview(previews))
            return;

        PlacementPreview preview = getCurrentPreview(stack, (ILittlePlacer) stack.getItem(), previews.copy());
        if (preview == null)
            return;
        if (LittleVecXFurniturePlacementHelper.canPlaceOnFloorOnly(MC.world, previews, preview)
                && LittleVecXPlacementFeedbackHelper.canPlacePreview(MC.player, stack, preview))
            return;

        LittlePreviews renderPreviews = getBasePreviews(stack, shouldAllowLowResolution());
        PlacementPreview renderPreview = renderPreviews == null ? null : getCurrentPreview(stack, (ILittlePlacer) stack.getItem(), renderPreviews.copy());
        if (renderPreview != null)
            preview = renderPreview;

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

    @Nullable
    private static PlacementPreview getCurrentPreview(ItemStack stack, ILittlePlacer placer, LittlePreviews previews) {
        if (MC.player == null || MC.world == null)
            return null;
        return previews == null ? null : LittleVecXFurniturePlacementHelper.resolveCurrentPlacementPreview(MC.world, stack, placer, previews);
    }

    @Nullable
    private static LittlePreviews getBasePreviews(ItemStack stack, boolean allowLowResolution) {
        return stack.isEmpty() ? null : LittlePreview.getPreview(stack, allowLowResolution);
    }

    private static boolean shouldAllowLowResolution() {
        return PreviewRenderer.marked == null || PreviewRenderer.marked.allowLowResolution();
    }

    private static void updateAutoFurnitureMagnet(ItemStack stack, ILittlePlacer placer, LittlePreviews previews) {
        if (MC.player == null || MC.world == null) {
            clearAutoFurnitureMark();
            return;
        }

        if (!StructureLittleVecXFurniture.isMagnetEnabled(previews)) {
            clearAutoFurnitureMark();
            return;
        }

        IMarkMode marked = PreviewRenderer.marked;
        if (marked != null && !isAutoFurnitureMark(marked))
            return;

        if (LittleTilesClient.mark.isKeyDown()) {
            clearAutoFurnitureMark();
            return;
        }

        boolean allowLowResolution = marked != null ? marked.allowLowResolution() : true;
        if (isAutoFurnitureMark(marked))
            PreviewRenderer.marked = null;

        PlacementPosition magnetizedPosition = LittleVecXFurniturePlacementHelper.resolveMagnetizedPlacementPosition(MC.world, stack, placer, previews);
        if (magnetizedPosition == null) {
            clearAutoFurnitureMark();
            return;
        }

        PreviewRenderer.marked = new LittleVecXFurnitureAutoMarkMode(magnetizedPosition, allowLowResolution);
    }

    private static void clearAutoFurnitureMark() {
        if (isAutoFurnitureMark(PreviewRenderer.marked))
            PreviewRenderer.marked = null;
    }

    private static boolean isAutoFurnitureMark(@Nullable IMarkMode marked) {
        return marked instanceof LittleVecXFurnitureAutoMarkMode;
    }
}
