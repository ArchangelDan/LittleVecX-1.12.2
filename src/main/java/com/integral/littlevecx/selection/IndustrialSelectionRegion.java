package com.integral.littlevecx.selection;

import javax.annotation.Nullable;

import com.creativemd.littletiles.common.tile.math.box.LittleAbsoluteBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.vec.LittleAbsoluteVec;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public final class IndustrialSelectionRegion {

    public final LittleAbsoluteVec first;
    public final LittleAbsoluteVec second;
    public final EnumFacing facing;

    public IndustrialSelectionRegion(LittleAbsoluteVec first, LittleAbsoluteVec second, EnumFacing facing) {
        this.first = first.copy();
        this.second = second.copy();
        this.facing = facing == null ? EnumFacing.UP : facing;
    }

    public boolean isValid() {
        LittleAbsoluteBox box = toAbsoluteBox();
        return box != null && box.box.isValidBox();
    }

    @Nullable
    public LittleAbsoluteBox toAbsoluteBox() {
        LittleAbsoluteVec firstCopy = first.copy();
        LittleAbsoluteVec secondCopy = second.copy();
        LittleGridContext context = LittleGridContext.max(firstCopy.getContext(), secondCopy.getContext());
        firstCopy.convertTo(context);
        secondCopy.convertTo(context);

        long minX = Math.min(toAbsoluteGridX(firstCopy), toAbsoluteGridX(secondCopy));
        long minY = Math.min(toAbsoluteGridY(firstCopy), toAbsoluteGridY(secondCopy));
        long minZ = Math.min(toAbsoluteGridZ(firstCopy), toAbsoluteGridZ(secondCopy));
        long maxX = Math.max(toAbsoluteGridX(firstCopy), toAbsoluteGridX(secondCopy)) + 1;
        long maxY = Math.max(toAbsoluteGridY(firstCopy), toAbsoluteGridY(secondCopy)) + 1;
        long maxZ = Math.max(toAbsoluteGridZ(firstCopy), toAbsoluteGridZ(secondCopy)) + 1;

        if (maxX <= minX || maxY <= minY || maxZ <= minZ)
            return null;

        BlockPos origin = new BlockPos(context.toBlockOffset(minX), context.toBlockOffset(minY), context.toBlockOffset(minZ));
        long originX = context.toGrid(origin.getX());
        long originY = context.toGrid(origin.getY());
        long originZ = context.toGrid(origin.getZ());
        LittleBox box = new LittleBox((int) (minX - originX), (int) (minY - originY), (int) (minZ - originZ), (int) (maxX - originX), (int) (maxY - originY),
                (int) (maxZ - originZ));
        return new LittleAbsoluteBox(origin, box, context);
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        first.writeToNBT("first", nbt);
        second.writeToNBT("second", nbt);
        nbt.setInteger("face", facing.getIndex());
        return nbt;
    }

    @Nullable
    public static IndustrialSelectionRegion readFromNBT(NBTTagCompound nbt) {
        if (!nbt.hasKey("first") || !nbt.hasKey("second"))
            return null;
        LittleAbsoluteVec first = new LittleAbsoluteVec("first", nbt);
        LittleAbsoluteVec second = new LittleAbsoluteVec("second", nbt);
        EnumFacing facing = EnumFacing.byIndex(nbt.getInteger("face"));
        return new IndustrialSelectionRegion(first, second, facing == null ? EnumFacing.UP : facing);
    }

    private static long toAbsoluteGridX(LittleAbsoluteVec vec) {
        return (long) vec.getPos().getX() * vec.getContext().size + vec.getVec().x;
    }

    private static long toAbsoluteGridY(LittleAbsoluteVec vec) {
        return (long) vec.getPos().getY() * vec.getContext().size + vec.getVec().y;
    }

    private static long toAbsoluteGridZ(LittleAbsoluteVec vec) {
        return (long) vec.getPos().getZ() * vec.getContext().size + vec.getVec().z;
    }
}
