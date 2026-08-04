package com.integral.littlevecx.furniture;

import java.util.List;

import javax.annotation.Nullable;

import com.creativemd.littletiles.common.tile.math.vec.LittleVec;
import com.creativemd.littletiles.common.util.place.PlacementPreview;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

final class LittleVecXFurnitureMagnetHelper {

    private static final double FLOOR_EPSILON = 1.0E-4D;
    private static final double MAX_WALL_MAGNET_DROP_BLOCKS = 2.0D;

    private LittleVecXFurnitureMagnetHelper() {}

    @Nullable
    public static PlacementPreview applyWallMagnet(World world, @Nullable PlacementPreview preview) {
        if (preview == null || preview.facing == null || preview.context == null || preview.pos == null)
            return preview;
        if (preview.facing.getAxis().isVertical())
            return preview;

        return snapToFloor(world, preview);
    }

    private static PlacementPreview snapToFloor(World world, PlacementPreview preview) {
        List<AxisAlignedBB> boxes = LittleVecXFurniturePlacementHelper.collectVisibleBoxes(preview);
        double minY = LittleVecXFurniturePlacementHelper.getMinY(boxes);
        if (!Double.isFinite(minY))
            return preview;

        Double supportTop = null;
        for (AxisAlignedBB box : boxes) {
            if (Math.abs(box.minY - minY) > FLOOR_EPSILON)
                continue;

            Double candidate = LittleVecXFurniturePlacementHelper.findSupportTop(world, box, minY);
            if (candidate == null)
                candidate = findSupportTopSlightlyAbove(world, box, minY, 1D / preview.context.size);
            if (candidate != null && (supportTop == null || candidate.doubleValue() > supportTop.doubleValue()))
                supportTop = candidate;
        }

        if (supportTop == null)
            return preview;

        double dropDistance = minY - supportTop.doubleValue();
        if (dropDistance > MAX_WALL_MAGNET_DROP_BLOCKS + FLOOR_EPSILON)
            return preview;

        int shiftY = preview.context.toGrid(supportTop.doubleValue() - minY);
        return shiftPreview(preview, 0, shiftY, 0);
    }

    @Nullable
    private static Double findSupportTopSlightlyAbove(World world, AxisAlignedBB box, double minY, double tolerance) {
        List<AxisAlignedBB> collisions = world.getCollisionBoxes(null,
                new AxisAlignedBB(box.minX + 1.0E-6D, minY - 1D, box.minZ + 1.0E-6D, box.maxX - 1.0E-6D, minY + tolerance, box.maxZ - 1.0E-6D));
        Double topY = null;
        for (AxisAlignedBB collision : collisions) {
            if (collision.maxY < minY - tolerance || collision.maxY > minY + tolerance)
                continue;
            if (topY == null || collision.maxY > topY.doubleValue())
                topY = collision.maxY;
        }
        return topY;
    }

    private static PlacementPreview shiftPreview(PlacementPreview preview, int shiftX, int shiftY, int shiftZ) {
        if (shiftX == 0 && shiftY == 0 && shiftZ == 0)
            return preview;

        LittleVec offset = preview.inBlockOffset != null ? preview.inBlockOffset.copy() : preview.cachedOffset != null ? preview.cachedOffset.copy() : null;
        if (offset == null)
            return preview;

        BlockPos pos = preview.pos;
        int grid = preview.context.size;

        offset.x += shiftX;
        offset.y += shiftY;
        offset.z += shiftZ;

        while (offset.x < 0) {
            offset.x += grid;
            pos = pos.add(-1, 0, 0);
        }
        while (offset.x >= grid) {
            offset.x -= grid;
            pos = pos.add(1, 0, 0);
        }
        while (offset.y < 0) {
            offset.y += grid;
            pos = pos.add(0, -1, 0);
        }
        while (offset.y >= grid) {
            offset.y -= grid;
            pos = pos.add(0, 1, 0);
        }
        while (offset.z < 0) {
            offset.z += grid;
            pos = pos.add(0, 0, -1);
        }
        while (offset.z >= grid) {
            offset.z -= grid;
            pos = pos.add(0, 0, 1);
        }

        return new PlacementPreview(preview.world, preview.previews, preview.mode, preview.box, preview.fixed, pos, offset, preview.facing);
    }
}
