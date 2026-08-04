package com.creativemd.littletiles.common.tile.math.box;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

public class LittleVecXPillarFixBox extends LittleTransformableBox {

    public static final String NBT_FLAG = "littlevecx_pillar_fix";

    public LittleVecXPillarFixBox(int[] data) {
        super(data);
    }

    public LittleVecXPillarFixBox(LittleBox box, int[] data) {
        super(box, data);
    }

    public static LittleVecXPillarFixBox fromBox(LittleTransformableBox box) {
        return new LittleVecXPillarFixBox(box.getArray());
    }

    public static void writeMarker(NBTTagCompound nbt) {
        nbt.setBoolean(NBT_FLAG, true);
    }

    public static boolean hasMarker(NBTTagCompound nbt) {
        return nbt != null && nbt.getBoolean(NBT_FLAG);
    }

    public static LittleVecXPillarFixBox restoreFromArray(int[] array, NBTTagCompound nbt) {
        if (array == null || array.length < 7 || !hasMarker(nbt))
            return null;
        return new LittleVecXPillarFixBox(array);
    }

    @Override
    public LittleVecXPillarFixBox copy() {
        return new LittleVecXPillarFixBox(getArray());
    }

    @Override
    public LittleBox combineBoxes(LittleBox box) {
        LittleBox combined = super.combineBoxes(box);
        if (combined instanceof LittleTransformableBox)
            return new LittleVecXPillarFixBox(((LittleTransformableBox) combined).getArray());
        return combined;
    }

    @Override
    public LittleBox extractBox(int x, int y, int z, @Nullable LittleBoxReturnedVolume volume) {
        LittleBox extracted = super.extractBox(x, y, z, volume);
        if (extracted instanceof LittleTransformableBox)
            return new LittleVecXPillarFixBox(((LittleTransformableBox) extracted).getArray());
        return extracted;
    }

    @Override
    public LittleBox extractBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, @Nullable LittleBoxReturnedVolume volume) {
        LittleBox extracted = super.extractBox(minX, minY, minZ, maxX, maxY, maxZ, volume);
        if (extracted instanceof LittleTransformableBox)
            return new LittleVecXPillarFixBox(((LittleTransformableBox) extracted).getArray());
        return extracted;
    }

    @Override
    public LittleBox grow(EnumFacing facing) {
        LittleBox box = super.grow(facing);
        if (box instanceof LittleTransformableBox)
            return new LittleVecXPillarFixBox(((LittleTransformableBox) box).getArray());
        return box;
    }

    @Override
    public LittleBox shrink(EnumFacing facing, boolean toLimit) {
        LittleBox box = super.shrink(facing, toLimit);
        if (box instanceof LittleTransformableBox)
            return new LittleVecXPillarFixBox(((LittleTransformableBox) box).getArray());
        return box;
    }
}
