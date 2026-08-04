package com.integral.littlevecx.animation;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.utils.type.Pair;
import com.creativemd.creativecore.common.utils.type.PairList;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.animation.AnimationKey;
import com.creativemd.littletiles.common.structure.animation.AnimationTimeline;
import com.creativemd.littletiles.common.structure.animation.ValueTimeline;
import com.creativemd.littletiles.common.structure.animation.event.AnimationEvent;
import com.creativemd.littletiles.common.structure.registry.LittleStructureRegistry;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType;
import com.creativemd.littletiles.common.structure.relative.StructureRelative;
import com.creativemd.littletiles.common.structure.type.door.LittleAdvancedDoor;
import com.creativemd.littletiles.common.structure.type.door.LittleAxisDoor;
import com.creativemd.littletiles.common.structure.type.door.LittleDoorBase;
import com.creativemd.littletiles.common.structure.type.door.LittleSlidingDoor;
import com.creativemd.littletiles.common.tile.math.vec.LittleVecContext;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

public final class LittleVecXAnimationLayerDoorHelper {

    private LittleVecXAnimationLayerDoorHelper() {}

    @Nullable
    public static LittleDoorBase createDoorForEdit(LittleVecXAnimationLayer layer, @Nullable LittlePreviews previews) {
        StructureRelative explicitAxis = resolveAxisCenter(layer, previews);
        if (layer.doorData != null && !layer.doorData.isEmpty()) {
            NBTTagCompound editData = layer.doorData.copy();
            if (explicitAxis != null)
                editData.setIntArray("axisCenter", explicitAxis.write());

            LittleDoorBase loaded = loadDoor(layer.doorType, editData);
            if (loaded != null) {
                loaded.events = copyEvents(layer.events);
                loaded.duration = layer.getSafeDuration();
                loaded.interpolation = layer.interpolation;
                if (loaded instanceof LittleAdvancedDoor) {
                    LittleAdvancedDoor advanced = (LittleAdvancedDoor) loaded;
                    advanced.offGrid = safeGrid(layer.getSafeOffGrid());
                    advanced.rotX = copyTimeline(layer.rotX);
                    advanced.rotY = copyTimeline(layer.rotY);
                    advanced.rotZ = copyTimeline(layer.rotZ);
                    advanced.offX = copyTimeline(layer.offX);
                    advanced.offY = copyTimeline(layer.offY);
                    advanced.offZ = copyTimeline(layer.offZ);
                }
                applyAxisToDoor(loaded, explicitAxis);
                return loaded;
            }
        }

        LittleDoorBase door;
        switch (layer.doorType) {
        case AXIS:
            door = newDoor(LittleAxisDoor.class);
            break;
        case SLIDING:
            door = newDoor(LittleSlidingDoor.class);
            break;
        case ADVANCED:
        default:
            door = newDoor(LittleAdvancedDoor.class);
            break;
        }
        if (door == null)
            return null;

        door.duration = layer.getSafeDuration();
        door.interpolation = layer.interpolation;
        door.events = copyEvents(layer.events);
        if (door instanceof LittleAdvancedDoor) {
            LittleAdvancedDoor advanced = (LittleAdvancedDoor) door;
            advanced.offGrid = safeGrid(layer.getSafeOffGrid());
            advanced.rotX = copyTimeline(layer.rotX);
            advanced.rotY = copyTimeline(layer.rotY);
            advanced.rotZ = copyTimeline(layer.rotZ);
            advanced.offX = copyTimeline(layer.offX);
            advanced.offY = copyTimeline(layer.offY);
            advanced.offZ = copyTimeline(layer.offZ);
        }
        applyAxisToDoor(door, explicitAxis);
        return door;
    }

    public static void applyParsedDoor(LittleVecXAnimationLayer layer, LittleDoorBase door, @Nullable LittlePreviews previews) {
        layer.doorType = resolveType(door);
        layer.duration = Math.max(1, door.duration);
        layer.interpolation = door.interpolation;
        layer.events = copyEvents(door.events);
        StructureRelative axis = extractAxisFromDoor(door);
        layer.axisData = axis == null ? null : axis.write();
        StructureRelative axisLocal = toLocalAxis(axis, previews);
        layer.axisLocalData = axisLocal == null ? null : axisLocal.write();
        layer.doorData = writeDoor(door);

        layer.rotX = null;
        layer.rotY = null;
        layer.rotZ = null;
        layer.offX = null;
        layer.offY = null;
        layer.offZ = null;
        layer.offGrid = LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID;

        if (door instanceof LittleAdvancedDoor) {
            LittleAdvancedDoor advanced = (LittleAdvancedDoor) door;
            layer.offGrid = advanced.offGrid != null ? advanced.offGrid.size : LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID;
            layer.rotX = copyTimeline(advanced.rotX);
            layer.rotY = copyTimeline(advanced.rotY);
            layer.rotZ = copyTimeline(advanced.rotZ);
            layer.offX = copyTimeline(advanced.offX);
            layer.offY = copyTimeline(advanced.offY);
            layer.offZ = copyTimeline(advanced.offZ);
            return;
        }

        if (door instanceof LittleSlidingDoor) {
            LittleSlidingDoor sliding = (LittleSlidingDoor) door;
            EnumFacing direction = sliding.direction == null ? EnumFacing.NORTH : sliding.direction;
            int distance = direction.getAxisDirection().getOffset() * sliding.moveDistance;
            layer.offGrid = sliding.moveContext != null ? sliding.moveContext.size : LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID;
            ValueTimeline timeline = ValueTimeline.create(door.interpolation).addPoint(0, 0D).addPoint(layer.duration, (double) distance);
            switch (direction.getAxis()) {
            case X:
                layer.offX = timeline;
                break;
            case Y:
                layer.offY = timeline;
                break;
            case Z:
                layer.offZ = timeline;
                break;
            default:
                break;
            }
            return;
        }

        if (door instanceof LittleAxisDoor) {
            LittleAxisDoor axisDoor = (LittleAxisDoor) door;
            AnimationTimeline timeline = new AnimationTimeline(layer.duration, new PairList<>());
            if (axisDoor.doorRotation != null && axisDoor.axis != null)
                axisDoor.doorRotation.populateTimeline(timeline, layer.duration, door.interpolation, AnimationKey.getRotation(axisDoor.axis));

            layer.rotX = getTimeline(timeline, AnimationKey.rotX);
            layer.rotY = getTimeline(timeline, AnimationKey.rotY);
            layer.rotZ = getTimeline(timeline, AnimationKey.rotZ);
            layer.offX = getTimeline(timeline, AnimationKey.offX);
            layer.offY = getTimeline(timeline, AnimationKey.offY);
            layer.offZ = getTimeline(timeline, AnimationKey.offZ);
        }
    }

    @Nullable
    public static StructureRelative extractAxisCenter(@Nullable LittleVecXAnimationLayer layer) {
        if (layer != null && layer.axisLocalData != null && layer.axisLocalData.length == 7)
            return new StructureRelative(layer.axisLocalData);
        if (layer != null && layer.axisData != null && layer.axisData.length == 7)
            return new StructureRelative(layer.axisData);

        LittleDoorBase door = null;
        if (layer != null && layer.doorData != null && !layer.doorData.isEmpty())
            door = loadDoor(layer.doorType, layer.doorData);
        return extractAxisFromDoor(door);
    }

    @Nullable
    public static StructureRelative resolveAxisCenter(@Nullable LittleVecXAnimationLayer layer, @Nullable LittlePreviews previews) {
        if (layer == null)
            return null;

        if (layer.axisLocalData != null && layer.axisLocalData.length == 7 && previews != null) {
            StructureRelative axis = new StructureRelative(layer.axisLocalData);
            axis.add(getPreviewsMin(previews));
            return axis;
        }

        if (layer.axisData != null && layer.axisData.length == 7)
            return new StructureRelative(layer.axisData);

        LittleDoorBase door = null;
        if (layer.doorData != null && !layer.doorData.isEmpty())
            door = loadDoor(layer.doorType, layer.doorData);
        return extractAxisFromDoor(door);
    }

    private static LittleVecXAnimationLayerDoorType resolveType(LittleDoorBase door) {
        if (door instanceof LittleAxisDoor)
            return LittleVecXAnimationLayerDoorType.AXIS;
        if (door instanceof LittleSlidingDoor)
            return LittleVecXAnimationLayerDoorType.SLIDING;
        return LittleVecXAnimationLayerDoorType.ADVANCED;
    }

    @Nullable
    private static LittleDoorBase loadDoor(LittleVecXAnimationLayerDoorType type, NBTTagCompound nbt) {
        LittleDoorBase door = null;
        switch (type) {
        case AXIS:
            door = newDoor(LittleAxisDoor.class);
            break;
        case SLIDING:
            door = newDoor(LittleSlidingDoor.class);
            break;
        case ADVANCED:
        default:
            door = newDoor(LittleAdvancedDoor.class);
            break;
        }

        if (door == null)
            return null;

        door.loadFromNBT(nbt.copy());
        return door;
    }

    @Nullable
    private static <T extends LittleDoorBase> T newDoor(Class<T> typeClass) {
        LittleStructureType type = LittleStructureRegistry.getStructureType(typeClass);
        if (type == null)
            return null;
        LittleStructure structure = type.createStructure(null);
        if (!typeClass.isInstance(structure))
            return null;
        return typeClass.cast(structure);
    }

    private static NBTTagCompound writeDoor(LittleDoorBase door) {
        NBTTagCompound nbt = new NBTTagCompound();
        door.writeToNBT(nbt);
        nbt.removeTag("axisCenter");
        return nbt;
    }

    @Nullable
    private static StructureRelative extractAxisFromDoor(@Nullable LittleDoorBase door) {
        if (door instanceof LittleAdvancedDoor) {
            StructureRelative axisCenter = ((LittleAdvancedDoor) door).axisCenter;
            return axisCenter == null ? null : new StructureRelative(axisCenter.write());
        }
        if (door instanceof LittleAxisDoor) {
            StructureRelative axisCenter = ((LittleAxisDoor) door).axisCenter;
            return axisCenter == null ? null : new StructureRelative(axisCenter.write());
        }
        return null;
    }

    private static void applyAxisToDoor(@Nullable LittleDoorBase door, @Nullable StructureRelative axisCenter) {
        if (door == null || axisCenter == null)
            return;

        if (door instanceof LittleAdvancedDoor)
            ((LittleAdvancedDoor) door).axisCenter = new StructureRelative(axisCenter.write());
        else if (door instanceof LittleAxisDoor)
            ((LittleAxisDoor) door).axisCenter = new StructureRelative(axisCenter.write());
    }

    @Nullable
    private static StructureRelative toLocalAxis(@Nullable StructureRelative axis, @Nullable LittlePreviews previews) {
        if (axis == null)
            return null;

        StructureRelative local = new StructureRelative(axis.write());
        if (previews != null)
            local.sub(getPreviewsMin(previews));
        return local;
    }

    private static LittleVecContext getPreviewsMin(LittlePreviews previews) {
        return new LittleVecContext(previews.getMinVec(), previews.getContext());
    }

    private static LittleGridContext safeGrid(int grid) {
        try {
            return LittleGridContext.get(grid);
        } catch (RuntimeException e) {
            return LittleGridContext.get(LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID);
        }
    }

    @Nullable
    private static ValueTimeline getTimeline(AnimationTimeline timeline, AnimationKey key) {
        for (Pair<AnimationKey, ValueTimeline> pair : timeline.values) {
            if (pair.key == key)
                return copyTimeline(pair.value);
        }
        return null;
    }

    @Nullable
    private static ValueTimeline copyTimeline(@Nullable ValueTimeline timeline) {
        return timeline == null ? null : timeline.copy();
    }

    private static List<AnimationEvent> copyEvents(@Nullable List<AnimationEvent> events) {
        List<AnimationEvent> copy = new ArrayList<>();
        if (events == null)
            return copy;
        for (AnimationEvent event : events) {
            AnimationEvent cloned = AnimationEvent.loadFromNBT(event.writeToNBT(new NBTTagCompound()));
            if (cloned != null)
                copy.add(cloned);
        }
        return copy;
    }
}
