package com.creativemd.littletiles.common.tile.math.box;

import java.util.Arrays;
import java.util.Iterator;

import com.creativemd.creativecore.common.utils.math.RotationUtils;
import com.creativemd.creativecore.common.utils.math.VectorUtils;
import com.creativemd.creativecore.common.utils.math.box.BoxCorner;
import com.creativemd.creativecore.common.utils.math.IntegerUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.math.Vec3i;

import com.creativemd.littletiles.common.tile.math.box.slice.LittleSlice;
import com.creativemd.littletiles.common.tile.math.vec.LittleVec;
public class LittleVecXSliceFixBox extends LittleTransformableBox {

    public static final String NBT_FLAG = "littlevecx_slice_fix";
    public static final String NBT_SLICE = "littlevecx_slice_fix_slice";
    public static final String NBT_VEC = "littlevecx_slice_fix_vec";
    public static final String NBT_PROFILE_AXIS = "littlevecx_slice_fix_profile_axis";
    public static final String NBT_PROFILE_CUT = "littlevecx_slice_fix_profile_cut";

    private final Vec3i vec;
    private final Axis profileAxis;
    private final int profileCutValue;

    private static class SliceProfile {
        final Axis preferredAxis;
        final int cutValue;

        SliceProfile(Axis preferredAxis, int cutValue) {
            this.preferredAxis = preferredAxis;
            this.cutValue = cutValue;
        }
    }

    public LittleVecXSliceFixBox(int[] data, Vec3i vec, Axis profileAxis, int profileCutValue) {
        super(data);
        this.vec = vec;
        this.profileAxis = profileAxis;
        this.profileCutValue = profileCutValue;
    }

    public LittleVecXSliceFixBox(LittleBox box, int[] data, Vec3i vec, Axis profileAxis, int profileCutValue) {
        super(box, data);
        this.vec = vec;
        this.profileAxis = profileAxis;
        this.profileCutValue = profileCutValue;
    }

    public Vec3i getVec() {
        return vec;
    }

    public Axis getProfileAxis() {
        return profileAxis;
    }

    public int getProfileCutValue() {
        return profileCutValue;
    }

    public static LittleVecXSliceFixBox create(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Vec3i vec) {
        LittleTransformableBox box = new LittleTransformableBox(new LittleBox(minX, minY, minZ, maxX, maxY, maxZ), new int[1]);
        CornerCache cache = box.new CornerCache(false);

        Axis axis = getAxis(vec);
        Axis one = RotationUtils.getOne(axis);
        Axis two = RotationUtils.getTwo(axis);

        EnumFacing facingOne = EnumFacing.getFacingFromAxis(VectorUtils.get(one, vec) > 0 ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE, one);
        EnumFacing facingTwo = EnumFacing.getFacingFromAxis(VectorUtils.get(two, vec) > 0 ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE, two);

        LittleVec size = box.getSize();
        EnumFacing preferred;
        int sizeOne = size.get(one);
        int sizeTwo = size.get(two);
        if (sizeOne > sizeTwo)
            preferred = facingTwo;
        else if (sizeOne < sizeTwo)
            preferred = facingOne;
        else
            preferred = one == Axis.Y ? facingOne : facingTwo;

        BoxCorner corner = BoxCorner.getCornerUnsorted(EnumFacing.getFacingFromAxis(AxisDirection.NEGATIVE, axis), facingOne, facingTwo);
        cache.setAbsolute(corner, preferred.getAxis(), box.get(preferred.getOpposite()));
        cache.setAbsolute(corner.flip(axis), preferred.getAxis(), box.get(preferred.getOpposite()));

        box.setData(cache.getData());
        return new LittleVecXSliceFixBox(box.getArray(), vec, preferred.getAxis(), box.get(preferred.getOpposite()));
    }

    public static void writeMarker(NBTTagCompound nbt, Vec3i vec) {
        nbt.setBoolean(NBT_FLAG, true);
        nbt.setIntArray(NBT_VEC, new int[] { vec.getX(), vec.getY(), vec.getZ() });
    }

    public static void writeMarker(NBTTagCompound nbt, Vec3i vec, Axis profileAxis, int profileCutValue) {
        writeMarker(nbt, vec);
        if (profileAxis != null)
            nbt.setInteger(NBT_PROFILE_AXIS, profileAxis.ordinal());
        nbt.setInteger(NBT_PROFILE_CUT, profileCutValue);
    }

    public static boolean hasMarker(NBTTagCompound nbt) {
        return nbt != null && nbt.getBoolean(NBT_FLAG) && (nbt.hasKey(NBT_VEC) || nbt.hasKey(NBT_SLICE));
    }

    public static LittleVecXSliceFixBox fromBox(LittleBox box, Vec3i vec) {
        if (box instanceof LittleTransformableBox) {
            SliceProfile profile = inferDefaultProfile(((LittleTransformableBox) box).getArray(), vec);
            return new LittleVecXSliceFixBox(((LittleTransformableBox) box).getArray(), vec, profile.preferredAxis, profile.cutValue);
        }
        return create(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, vec);
    }

    public static LittleVecXSliceFixBox tryConvertVanillaSlice(LittleBox box) {
        if (box instanceof LittleVecXSliceFixBox)
            return ((LittleVecXSliceFixBox) box).copy();
        if (!(box instanceof LittleTransformableBox))
            return null;

        LittleTransformableBox transformable = (LittleTransformableBox) box;
        LittleBox bounds = new LittleBox(box);
        for (LittleSlice slice : LittleSlice.values()) {
            LittleVecXSliceFixBox canonical = create(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ, slice.sliceVec);
            if (Arrays.equals(transformable.getArray(), canonical.getArray()))
                return fromBox(transformable, slice.sliceVec);
        }
        return null;
    }

    public static LittleVecXSliceFixBox restoreFromArray(int[] array, NBTTagCompound nbt) {
        if (array == null || array.length < 6 || !hasMarker(nbt))
            return null;
        Vec3i vec = readVec(nbt);
        if (vec == null)
            return null;
        SliceProfile profile = readStoredProfile(nbt);
        if (profile == null)
            profile = inferDefaultProfile(array, vec);
        return new LittleVecXSliceFixBox(array, vec, profile.preferredAxis, profile.cutValue);
    }

    @Override
    public LittleVecXSliceFixBox copy() {
        return new LittleVecXSliceFixBox(getArray(), vec, profileAxis, profileCutValue);
    }

    @Override
    public LittleBox combineBoxes(LittleBox box) {
        LittleVecXSliceFixBox sameTypeBox = box instanceof LittleVecXSliceFixBox ? (LittleVecXSliceFixBox) box : null;

        if (sameTypeBox != null && !sameVec(sameTypeBox.vec, this.vec)) {
            return null;
        }

        LittleBox nativeResult = null;
        if (sameTypeBox != null)
            nativeResult = tryFaceAwareNativeMerge(sameTypeBox);

        if (nativeResult == null)
            nativeResult = super.combineBoxes(box);

        if (nativeResult instanceof LittleTransformableBox) {
            return new LittleVecXSliceFixBox(((LittleTransformableBox) nativeResult).getArray(), vec, profileAxis, profileCutValue);
        }

        return nativeResult;
    }

    @Override
    public LittleBox grow(EnumFacing facing) {
        LittleBox box = super.grow(facing);
        if (box instanceof LittleTransformableBox)
            return new LittleVecXSliceFixBox(((LittleTransformableBox) box).getArray(), vec, profileAxis, profileCutValue);
        return box;
    }

    @Override
    public LittleBox shrink(EnumFacing facing, boolean toLimit) {
        LittleBox box = super.shrink(facing, toLimit);
        if (box instanceof LittleTransformableBox)
            return new LittleVecXSliceFixBox(((LittleTransformableBox) box).getArray(), vec, profileAxis, profileCutValue);
        return box;
    }

    private LittleTransformableBox tryFaceAwareNativeMerge(LittleVecXSliceFixBox other) {
        EnumFacing facing = other.sharedBoxFaceWithoutBounds(this);
        if (facing == null) {
            return null;
        }

        Iterator<TransformableVec> points = corners();
        Iterator<TransformableVec> otherPoints = other.corners();

        TransformableVec point = null;
        TransformableVec otherPoint = null;

        Axis one = RotationUtils.getOne(facing.getAxis());
        Axis two = RotationUtils.getTwo(facing.getAxis());

        while (points.hasNext() || otherPoints.hasNext()) {
            point = points.hasNext() ? points.next() : null;
            otherPoint = otherPoints.hasNext() ? otherPoints.next() : null;

            while (point != null && (otherPoint == null || point.corner.ordinal() < otherPoint.corner.ordinal())) {
                if (other.get(point.corner, one) != point.getAbsolute(one) || other.get(point.corner, two) != point.getAbsolute(two)) {
                    return null;
                }

                point = points.hasNext() ? points.next() : null;
            }

            while (otherPoint != null && (point == null || point.corner.ordinal() > otherPoint.corner.ordinal())) {
                if (get(otherPoint.corner, one) != otherPoint.getAbsolute(one) || get(otherPoint.corner, two) != otherPoint.getAbsolute(two)) {
                    return null;
                }

                otherPoint = otherPoints.hasNext() ? otherPoints.next() : null;
            }

            if (point != null && otherPoint != null && point.corner == otherPoint.corner
                    && (point.getAbsolute(one) != otherPoint.getAbsolute(one) || point.getAbsolute(two) != otherPoint.getAbsolute(two))) {
                return null;
            }
        }

        if (!requestCache().get(facing).equalAxisStrip(other.requestCache().get(facing.getOpposite()), facing.getAxis())) {
            return null;
        }

        CornerCache cornerCache = new CornerCache(false);
        setAbsoluteCorners(cornerCache);
        CornerCache otherCornerCache = other.new CornerCache(false);
        other.setAbsoluteCorners(otherCornerCache);

        com.creativemd.littletiles.common.tile.math.vec.LittleRay ray = new com.creativemd.littletiles.common.tile.math.vec.LittleRay(new LittleVec(0, 0, 0), new LittleVec(0, 0, 0));
        com.creativemd.littletiles.common.tile.math.vec.LittleRay ray2 = new com.creativemd.littletiles.common.tile.math.vec.LittleRay(new LittleVec(0, 0, 0), new LittleVec(0, 0, 0));
        BoxCorner[] corners = BoxCorner.faceCorners(facing);
        for (int i = 0; i < corners.length; i++) {
            BoxCorner corner = corners[i];
            BoxCorner otherCorner = corner.flip(facing.getAxis());
            ray.set(cornerCache.getOrCreate(corner), cornerCache.getOrCreate(otherCorner));
            ray2.set(otherCornerCache.getOrCreate(corner), otherCornerCache.getOrCreate(otherCorner));

            if (isNoDirection(ray)) {
                if (!isNoDirection(ray2)) {
                    return null;
                }

                BoxCorner newCorner = otherCorner.flip(one);
                ray.set(cornerCache.getOrCreate(corner), cornerCache.getOrCreate(newCorner));
                ray2.set(otherCornerCache.getOrCreate(corner), otherCornerCache.getOrCreate(newCorner));
                if (!sameRay(ray, ray2)) {
                    return null;
                }

                newCorner = otherCorner.flip(two);
                ray.set(cornerCache.getOrCreate(corner), cornerCache.getOrCreate(newCorner));
                ray2.set(otherCornerCache.getOrCreate(corner), otherCornerCache.getOrCreate(newCorner));
                if (!sameRay(ray, ray2)) {
                    return null;
                }
            } else if (isNoDirection(ray2) || !sameRay(ray, ray2)) {
                return null;
            }
        }

        LittleTransformableBox result = new LittleTransformableBox(new LittleBox(this, other), getTransformableDataCopy());
        CornerCache cache = result.new CornerCache(false);
        other.setAbsoluteCornersTakeBoundsFacing(cache, facing);
        setAbsoluteCornersTakeBoundsFacing(cache, facing.getOpposite());
        result.setData(cache.getData());
        return result;
    }

    private void setAbsoluteCornersTakeBoundsFacing(CornerCache cache, EnumFacing facing) {
        int indicator = getIndicator();

        int activeBits = 0;
        for (int i = 0; i < BoxCorner.values().length; i++) {
            BoxCorner corner = BoxCorner.values()[i];

            int index = i * 3;
            if (IntegerUtils.bitIs(indicator, index)) {
                if (corner.isFacing(facing))
                    cache.setAbsolute(corner, Axis.X, getData(activeBits) + get(corner.x));
                activeBits++;
            }

            if (IntegerUtils.bitIs(indicator, index + 1)) {
                if (corner.isFacing(facing))
                    cache.setAbsolute(corner, Axis.Y, getData(activeBits) + get(corner.y));
                activeBits++;
            }

            if (IntegerUtils.bitIs(indicator, index + 2)) {
                if (corner.isFacing(facing))
                    cache.setAbsolute(corner, Axis.Z, getData(activeBits) + get(corner.z));
                activeBits++;
            }
        }
    }

    private int[] getTransformableDataCopy() {
        int[] array = getArray();
        return Arrays.copyOfRange(array, 6, array.length);
    }

    private static boolean isNoDirection(com.creativemd.littletiles.common.tile.math.vec.LittleRay ray) {
        return ray.direction.x == 0 && ray.direction.y == 0 && ray.direction.z == 0;
    }

    private static boolean sameRay(com.creativemd.littletiles.common.tile.math.vec.LittleRay ray, com.creativemd.littletiles.common.tile.math.vec.LittleRay other) {
        return ray.parallel(other);
    }

    private static boolean sameVec(Vec3i a, Vec3i b) {
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ();
    }

    private static Vec3i readVec(NBTTagCompound nbt) {
        if (nbt != null && nbt.hasKey(NBT_VEC)) {
            int[] array = nbt.getIntArray(NBT_VEC);
            if (array.length == 3)
                return new Vec3i(array[0], array[1], array[2]);
        }

        if (nbt != null && nbt.hasKey(NBT_SLICE)) {
            int ordinal = nbt.getInteger(NBT_SLICE);
            LittleSlice[] values = LittleSlice.values();
            if (ordinal >= 0 && ordinal < values.length)
                return values[ordinal].sliceVec;
        }

        return null;
    }

    private static SliceProfile readStoredProfile(NBTTagCompound nbt) {
        if (nbt != null && nbt.hasKey(NBT_PROFILE_AXIS) && nbt.hasKey(NBT_PROFILE_CUT)) {
            int axisOrdinal = nbt.getInteger(NBT_PROFILE_AXIS);
            Axis[] values = Axis.values();
            if (axisOrdinal >= 0 && axisOrdinal < values.length)
                return new SliceProfile(values[axisOrdinal], nbt.getInteger(NBT_PROFILE_CUT));
        }
        return null;
    }

    private static Axis getAxis(Vec3i vec) {
        if (vec.getX() == 0)
            return Axis.X;
        if (vec.getY() == 0)
            return Axis.Y;
        return Axis.Z;
    }

    private static SliceProfile inferDefaultProfile(int[] array, Vec3i vec) {
        Axis axis = getAxis(vec);
        Axis one = RotationUtils.getOne(axis);
        Axis two = RotationUtils.getTwo(axis);
        EnumFacing facingOne = EnumFacing.getFacingFromAxis(VectorUtils.get(one, vec) > 0 ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE, one);
        EnumFacing facingTwo = EnumFacing.getFacingFromAxis(VectorUtils.get(two, vec) > 0 ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE, two);

        LittleBox bounds = new LittleBox(array[0], array[1], array[2], array[3], array[4], array[5]);
        LittleVec size = bounds.getSize();
        EnumFacing preferred;
        int sizeOne = size.get(one);
        int sizeTwo = size.get(two);
        if (sizeOne > sizeTwo)
            preferred = facingTwo;
        else if (sizeOne < sizeTwo)
            preferred = facingOne;
        else
            preferred = one == Axis.Y ? facingOne : facingTwo;
        return new SliceProfile(preferred.getAxis(), bounds.get(preferred.getOpposite()));
    }

}
