package com.integral.littlevecx.selection;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxes;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.place.PlacementPosition;
import com.creativemd.littletiles.common.util.shape.LittleShape;
import com.creativemd.littletiles.common.util.shape.ShapeRegistry;
import com.creativemd.littletiles.common.util.shape.ShapeSelection;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LittleVecXIndustrialChiselSelection extends ShapeSelection {

    private LittleVecXIndustrialChiselShapeSelectCache littleVexCache;

    public LittleVecXIndustrialChiselSelection(ItemStack stack, boolean inside) {
        super(stack, inside);
        syncShape();
    }

    private LittleShape resolveShape(String key) {
        return ShapeRegistry.getShape(key);
    }

    private void syncShape() {
        shapeKey = getNBT().getString("shape");
        shape = resolveShape(shapeKey);
    }

    protected boolean requiresLittleVecXCacheUpdate() {
        if (littleVexCache == null)
            return true;

        if (littleVexCache.context != context)
            return true;

        NBTTagCompound nbt = getNBT();
        if (!shapeKey.equals(nbt.getString("shape"))) {
            shapeKey = nbt.getString("shape");
            shape = resolveShape(shapeKey);
            if (!littleVexCache.shapeKey.equals(shapeKey))
                return true;
        }

        if (countPositions() != littleVexCache.positions.size())
            return true;

        int i = 0;
        for (ShapeSelectPos pos : this) {
            if (!pos.equals(littleVexCache.positions.get(i)))
                return true;
            i++;
        }

        return false;
    }

    protected LittleVecXIndustrialChiselShapeSelectCache getLittleVecXCache() {
        syncShape();
        if (requiresLittleVecXCacheUpdate()) {
            LittleBox[] pointBoxes = new LittleBox[countPositions()];
            List<ShapeSelectPos> cachedPositions = new ArrayList<>(pointBoxes.length);
            int i = 0;
            for (ShapeSelectPos pos : this) {
                pointBoxes[i] = new LittleBox(pos.pos.getRelative(LittleVecXIndustrialChiselSelection.this.pos));
                cachedPositions.add(pos.copy());
                i++;
            }

            overallBox = new LittleBox(pointBoxes);
            littleVexCache = new LittleVecXIndustrialChiselShapeSelectCache(this, context, cachedPositions, shapeKey, shape);
        }
        return littleVexCache;
    }

    @Override
    public LittleBoxes getBoxes(boolean allowLowResolution) {
        if (this.allowLowResolution && allowLowResolution)
            return getLittleVecXCache().get(true);
        return getLittleVecXCache().get(false);
    }

    @Override
    public void deleteCache() {
        super.deleteCache();
        littleVexCache = null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean addAndCheckIfPlace(EntityPlayer player, PlacementPosition position, RayTraceResult result) {
        syncShape();
        return super.addAndCheckIfPlace(player, position, result);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setLast(EntityPlayer player, ItemStack stack, PlacementPosition position, RayTraceResult result) {
        syncShape();
        super.setLast(player, stack, position, result);
    }

    @Override
    public void rotate(EntityPlayer player, ItemStack stack, com.creativemd.creativecore.common.utils.math.Rotation rotation) {
        syncShape();
        super.rotate(player, stack, rotation);
        littleVexCache = null;
    }

    @Override
    public void flip(EntityPlayer player, ItemStack stack, Axis axis) {
        syncShape();
        super.flip(player, stack, axis);
        littleVexCache = null;
    }
}
