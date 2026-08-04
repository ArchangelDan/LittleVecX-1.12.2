package com.integral.littlevecx.client;

import javax.annotation.Nullable;

import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.render.overlay.PreviewRenderer;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.api.ILittlePlacer;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.util.place.Placement;
import com.creativemd.littletiles.common.util.place.PlacementHelper;
import com.creativemd.littletiles.common.util.place.PlacementMode;
import com.creativemd.littletiles.common.util.place.PlacementPosition;
import com.creativemd.littletiles.common.util.place.PlacementPreview;
import com.integral.littlevecx.furniture.LittleVecXFurniturePlacementHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class LittleVecXPlacementFeedbackHelper {

    private LittleVecXPlacementFeedbackHelper() {}

    public static boolean supportsPreview(@Nullable LittlePreviews previews) {
        return previews != null && !LittleVecXFurniturePlacementHelper.isFurniturePreview(previews);
    }

    public static boolean canPlaceCurrent(ItemStack stack, ILittlePlacer placer, LittlePreviews rawPreviews) {
        if (!supportsPreview(rawPreviews))
            return true;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null)
            return true;

        PlacementPreview preview = resolveCurrentPlacementPreview(mc.world, stack, placer, rawPreviews);
        return canPlacePreview(mc.player, stack, preview);
    }

    @Nullable
    public static PlacementPreview resolveCurrentPlacementPreview(World world, ItemStack stack, ILittlePlacer placer, LittlePreviews rawPreviews) {
        PlacementPosition position = getCurrentPosition(world, placer, stack);
        boolean markedPresent = PreviewRenderer.marked != null;
        boolean allowLowResolution = markedPresent ? PreviewRenderer.marked.allowLowResolution() : true;
        return buildPlacementPreview(world, stack, placer, rawPreviews, position, markedPresent, allowLowResolution);
    }

    public static boolean canPlacePreview(@Nullable EntityPlayer player, ItemStack stack, @Nullable PlacementPreview preview) {
        if (player == null || preview == null)
            return true;

        try {
            Placement placement = new Placement(player, preview).setStack(stack).setPlaySounds(false).setAfterNotifyPlace(false);
            return placement.canPlace();
        } catch (LittleActionException ignored) {
            return false;
        }
    }

    @Nullable
    private static PlacementPreview buildPlacementPreview(World world, ItemStack stack, ILittlePlacer placer, LittlePreviews rawPreviews, @Nullable PlacementPosition position,
            boolean markedPresent, boolean allowLowResolution) {
        Minecraft mc = Minecraft.getMinecraft();
        if (position == null || mc.player == null)
            return null;

        boolean centered = isCentered(mc.player, stack, placer, markedPresent);
        boolean fixed = isFixed(mc.player, stack, placer, markedPresent);
        PlacementMode mode = placer.getPlacementMode(stack);
        return PlacementHelper.getPreviews(world, rawPreviews, placer.getPreviewsContext(stack), stack, position.copy(), centered, fixed, allowLowResolution, mode);
    }

    @Nullable
    private static PlacementPosition getCurrentPosition(World world, ILittlePlacer placer, ItemStack stack) {
        if (PreviewRenderer.marked != null)
            return PreviewRenderer.marked.getPosition().copy();

        RayTraceResult result = Minecraft.getMinecraft().objectMouseOver;
        if (result == null || result.typeOfHit != RayTraceResult.Type.BLOCK || result.sideHit == null)
            return null;

        return PlacementHelper.getPosition(world, result, placer.getPositionContext(stack), placer, stack);
    }

    private static boolean isCentered(EntityPlayer player, ItemStack stack, ILittlePlacer placer, boolean markedPresent) {
        if (placer.snapToGridByDefault(stack))
            return LittleAction.isUsingSecondMode(player) && !markedPresent;
        return LittleTiles.CONFIG.building.invertStickToGrid == LittleAction.isUsingSecondMode(player) || markedPresent;
    }

    private static boolean isFixed(EntityPlayer player, ItemStack stack, ILittlePlacer placer, boolean markedPresent) {
        if (placer.snapToGridByDefault(stack))
            return !LittleAction.isUsingSecondMode(player) && !markedPresent;
        return LittleTiles.CONFIG.building.invertStickToGrid != LittleAction.isUsingSecondMode(player) && !markedPresent;
    }
}
