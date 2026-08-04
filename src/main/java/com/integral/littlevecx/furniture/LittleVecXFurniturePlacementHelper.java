package com.integral.littlevecx.furniture;

import javax.annotation.Nullable;

import com.creativemd.littletiles.client.render.overlay.PreviewRenderer;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.api.ILittlePlacer;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.vec.LittleAbsoluteVec;
import com.creativemd.littletiles.common.tile.math.vec.LittleVec;
import com.creativemd.littletiles.common.tile.math.vec.LittleVecContext;
import com.creativemd.littletiles.common.tile.preview.LittlePreview;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.util.place.PlacementHelper;
import com.creativemd.littletiles.common.util.place.PlacementMode;
import com.creativemd.littletiles.common.util.place.PlacementPosition;
import com.creativemd.littletiles.common.util.place.PlacementPreview;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class LittleVecXFurniturePlacementHelper {

    private static final double FLOOR_EPSILON = 1.0E-4D;
    private static final double PIXEL_EPSILON = 1.0E-6D;
    private static final double SUPPORT_SAMPLE_RADIUS = 1.0E-4D;
    private static final int MAX_SNAP_DOWN_BLOCKS = 16;

    private LittleVecXFurniturePlacementHelper() {}

    public static boolean isFurniturePreview(@Nullable LittlePreviews previews) {
        return previews != null && StructureLittleVecXFurniture.STRUCTURE_ID.equals(previews.getStructureId());
    }

    public static boolean isFurnitureStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound() || !stack.getTagCompound().hasKey("structure"))
            return false;
        return StructureLittleVecXFurniture.STRUCTURE_ID.equals(stack.getTagCompound().getCompoundTag("structure").getString("id"));
    }

    @SideOnly(Side.CLIENT)
    public static boolean canPlaceOnFloorOnly(ItemStack stack, ILittlePlacer placer, LittlePreviews rawPreviews) {
        if (!isFurniturePreview(rawPreviews))
            return true;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null)
            return false;

        PlacementPreview preview = resolveCurrentPlacementPreview(mc.world, stack, placer, rawPreviews);
        return canPlaceOnFloorOnly(mc.world, rawPreviews, preview);
    }

    public static boolean canPlaceOnFloorOnly(World world, LittlePreviews rawPreviews, @Nullable PlacementPreview preview) {
        if (!isFurniturePreview(rawPreviews))
            return true;
        if (world == null || preview == null)
            return false;
        if (!hasBottomContact(world, preview))
            return false;
        if (!StructureLittleVecXFurniture.requiresFlatSurface(rawPreviews))
            return true;
        return hasFlatBottomSupport(world, preview);
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    public static PlacementPreview resolveCurrentPlacementPreview(World world, ItemStack stack, ILittlePlacer placer, LittlePreviews rawPreviews) {
        PlacementPosition position = getCurrentPosition(world, placer, stack, true);
        boolean markedPresent = PreviewRenderer.marked != null;
        boolean allowLowResolution = markedPresent ? PreviewRenderer.marked.allowLowResolution() : true;
        return buildPlacementPreview(world, stack, placer, rawPreviews, position, markedPresent, allowLowResolution);
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    public static PlacementPreview resolveMagnetizedPlacementPreview(World world, ItemStack stack, ILittlePlacer placer, LittlePreviews rawPreviews) {
        PlacementPosition position = getCurrentPosition(world, placer, stack, false);
        PlacementPreview preview = buildPlacementPreview(world, stack, placer, rawPreviews, position, true, true);
        return LittleVecXFurnitureMagnetHelper.applyWallMagnet(world, preview);
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    public static PlacementPosition resolveMagnetizedPlacementPosition(World world, ItemStack stack, ILittlePlacer placer, LittlePreviews rawPreviews) {
        PlacementPosition basePosition = getCurrentPosition(world, placer, stack, false);
        PlacementPreview preview = buildPlacementPreview(world, stack, placer, rawPreviews, basePosition, true, true);
        if (preview == null || preview.facing == null || preview.facing.getAxis().isVertical())
            return null;

        PlacementPreview magnetized = LittleVecXFurnitureMagnetHelper.applyWallMagnet(world, preview);
        if (!hasBottomContact(world, magnetized))
            return null;
        return applyPreviewDelta(basePosition, preview, magnetized);
    }

    @Nullable
    @SideOnly(Side.CLIENT)
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
    @SideOnly(Side.CLIENT)
    private static PlacementPosition getCurrentPosition(World world, ILittlePlacer placer, ItemStack stack, boolean useMarkedPosition) {
        if (useMarkedPosition && PreviewRenderer.marked != null)
            return PreviewRenderer.marked.getPosition().copy();

        RayTraceResult result = Minecraft.getMinecraft().objectMouseOver;
        if (result == null || result.typeOfHit != RayTraceResult.Type.BLOCK || result.sideHit == null)
            return null;

        return PlacementHelper.getPosition(world, result, placer.getPositionContext(stack), placer, stack);
    }

    @Nullable
    static PlacementPosition applyPreviewDelta(@Nullable PlacementPosition basePosition, @Nullable PlacementPreview basePreview, @Nullable PlacementPreview magnetizedPreview) {
        if (basePosition == null || basePreview == null || magnetizedPreview == null)
            return null;

        LittleAbsoluteVec baseOrigin = toAbsoluteVec(basePreview);
        LittleAbsoluteVec magnetizedOrigin = toAbsoluteVec(magnetizedPreview);
        if (baseOrigin == null || magnetizedOrigin == null)
            return null;

        LittleVecContext delta = magnetizedOrigin.getRelative(baseOrigin);
        PlacementPosition shifted = basePosition.copy();
        shifted.add(delta);
        shifted.removeInternalBlockOffset();
        return shifted;
    }

    private static boolean isCentered(net.minecraft.entity.player.EntityPlayer player, ItemStack stack, ILittlePlacer placer, boolean markedPresent) {
        if (placer.snapToGridByDefault(stack))
            return LittleAction.isUsingSecondMode(player) && !markedPresent;
        return LittleTiles.CONFIG.building.invertStickToGrid == LittleAction.isUsingSecondMode(player) || markedPresent;
    }

    private static boolean isFixed(net.minecraft.entity.player.EntityPlayer player, ItemStack stack, ILittlePlacer placer, boolean markedPresent) {
        if (placer.snapToGridByDefault(stack))
            return !LittleAction.isUsingSecondMode(player) && !markedPresent;
        return LittleTiles.CONFIG.building.invertStickToGrid != LittleAction.isUsingSecondMode(player) && !markedPresent;
    }

    @Nullable
    private static LittleAbsoluteVec toAbsoluteVec(@Nullable PlacementPreview preview) {
        if (preview == null || preview.context == null || preview.pos == null)
            return null;

        LittleVec offset = preview.inBlockOffset != null ? preview.inBlockOffset.copy()
                : preview.cachedOffset != null ? preview.cachedOffset.copy() : new LittleVec(0, 0, 0);
        return new LittleAbsoluteVec(preview.pos, preview.context, offset);
    }

    static boolean hasBottomContact(World world, @Nullable PlacementPreview preview) {
        java.util.List<AxisAlignedBB> boxes = collectVisibleBoxes(preview);
        double minY = getMinY(boxes);
        if (!Double.isFinite(minY))
            return false;

        for (AxisAlignedBB box : boxes) {
            if (Math.abs(box.minY - minY) > FLOOR_EPSILON)
                continue;

            Double supportTop = findSupportTop(world, box, minY);
            if (supportTop != null && Math.abs(supportTop.doubleValue() - minY) <= FLOOR_EPSILON)
                return true;
        }
        return false;
    }

    static boolean hasFlatBottomSupport(World world, @Nullable PlacementPreview preview) {
        if (preview == null || preview.context == null)
            return false;

        java.util.List<AxisAlignedBB> boxes = collectVisibleBoxes(preview);
        double minY = getMinY(boxes);
        if (!Double.isFinite(minY))
            return false;

        int grid = Math.max(1, preview.context.size);
        boolean foundBottomBox = false;
        for (AxisAlignedBB box : boxes) {
            if (Math.abs(box.minY - minY) > FLOOR_EPSILON)
                continue;

            foundBottomBox = true;
            if (!isBottomBoxFullySupported(world, box, minY, grid))
                return false;
        }
        return foundBottomBox;
    }

    @Nullable
    static Double findSupportTop(World world, AxisAlignedBB box, double minY) {
        double fromX = box.minX + PIXEL_EPSILON;
        double toX = box.maxX - PIXEL_EPSILON;
        double fromZ = box.minZ + PIXEL_EPSILON;
        double toZ = box.maxZ - PIXEL_EPSILON;
        if (fromX >= toX || fromZ >= toZ)
            return null;

        AxisAlignedBB query = new AxisAlignedBB(fromX, minY - MAX_SNAP_DOWN_BLOCKS - 1D, fromZ, toX, minY - PIXEL_EPSILON, toZ);
        java.util.List<AxisAlignedBB> collisions = world.getCollisionBoxes(null, query);
        Double topY = null;
        for (AxisAlignedBB collision : collisions) {
            if (collision.maxY > minY + FLOOR_EPSILON)
                continue;
            if (topY == null || collision.maxY > topY.doubleValue())
                topY = collision.maxY;
        }
        return topY;
    }

    static java.util.List<AxisAlignedBB> collectVisibleBoxes(@Nullable PlacementPreview preview) {
        java.util.List<AxisAlignedBB> boxes = new java.util.ArrayList<>();
        if (preview == null || preview.previews == null)
            return boxes;

        LittleVec offset = preview.inBlockOffset != null ? preview.inBlockOffset.copy()
                : preview.cachedOffset != null ? preview.cachedOffset.copy() : null;
        for (LittlePreview littlePreview : preview.previews.allPreviews()) {
            if (littlePreview == null || littlePreview.box == null || littlePreview.isInvisible())
                continue;

            LittleBox box = littlePreview.box.copy();
            if (offset != null)
                box.add(offset);

            AxisAlignedBB worldBox = box.getBox(preview.context, preview.pos);
            if (worldBox != null)
                boxes.add(worldBox);
        }
        return boxes;
    }

    static double getMinY(java.util.List<AxisAlignedBB> boxes) {
        double minY = Double.POSITIVE_INFINITY;
        for (AxisAlignedBB box : boxes)
            minY = Math.min(minY, box.minY);
        return minY;
    }

    private static boolean isBottomBoxFullySupported(World world, AxisAlignedBB box, double minY, int grid) {
        int minX = toGridFloor(box.minX, grid);
        int maxX = toGridCeil(box.maxX, grid);
        int minZ = toGridFloor(box.minZ, grid);
        int maxZ = toGridCeil(box.maxZ, grid);

        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                double sampleX = (x + 0.5D) / grid;
                double sampleZ = (z + 0.5D) / grid;
                if (!hasSupportAtPoint(world, sampleX, minY, sampleZ))
                    return false;
            }
        }
        return true;
    }

    private static boolean hasSupportAtPoint(World world, double x, double y, double z) {
        AxisAlignedBB query = new AxisAlignedBB(x - SUPPORT_SAMPLE_RADIUS, y - MAX_SNAP_DOWN_BLOCKS - 1D, z - SUPPORT_SAMPLE_RADIUS,
                x + SUPPORT_SAMPLE_RADIUS, y + FLOOR_EPSILON, z + SUPPORT_SAMPLE_RADIUS);
        java.util.List<AxisAlignedBB> collisions = world.getCollisionBoxes(null, query);
        for (AxisAlignedBB collision : collisions) {
            if (collision.maxY < y - FLOOR_EPSILON || collision.maxY > y + FLOOR_EPSILON)
                continue;
            if (collision.minX - PIXEL_EPSILON > x || collision.maxX + PIXEL_EPSILON < x)
                continue;
            if (collision.minZ - PIXEL_EPSILON > z || collision.maxZ + PIXEL_EPSILON < z)
                continue;
            return true;
        }
        return false;
    }

    private static int toGridFloor(double value, int grid) {
        return (int) Math.floor(value * grid + PIXEL_EPSILON);
    }

    private static int toGridCeil(double value, int grid) {
        return (int) Math.ceil(value * grid - PIXEL_EPSILON);
    }
}
