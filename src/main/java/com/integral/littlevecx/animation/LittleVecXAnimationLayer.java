package com.integral.littlevecx.animation;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.creativemd.littletiles.common.structure.animation.event.AnimationEvent;
import com.creativemd.littletiles.common.structure.animation.ValueTimeline;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class LittleVecXAnimationLayer {

    public String name = "";
    public LittleVecXAnimationTriggerMode trigger = LittleVecXAnimationTriggerMode.RIGHT_CLICK;
    public LittleVecXAnimationLayerDoorType doorType = LittleVecXAnimationLayerDoorType.AXIS;
    public int duration = LittleVecXAnimationLayerCompiler.DEFAULT_DURATION;
    public int interpolation = 0;
    public int offGrid = LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID;
    @Nullable
    public int[] axisData;
    @Nullable
    public int[] axisLocalData;
    public NBTTagCompound doorData = new NBTTagCompound();
    public List<AnimationEvent> events = new ArrayList<>();

    @Nullable
    public ValueTimeline rotX;
    @Nullable
    public ValueTimeline rotY;
    @Nullable
    public ValueTimeline rotZ;
    @Nullable
    public ValueTimeline offX;
    @Nullable
    public ValueTimeline offY;
    @Nullable
    public ValueTimeline offZ;

    public LittleVecXAnimationLayer copy() {
        LittleVecXAnimationLayer copy = new LittleVecXAnimationLayer();
        copy.applyFrom(this);
        return copy;
    }

    public void applyFrom(LittleVecXAnimationLayer other) {
        name = other.name;
        trigger = other.trigger;
        doorType = other.doorType;
        duration = other.getSafeDuration();
        interpolation = other.interpolation;
        offGrid = other.getSafeOffGrid();
        axisData = other.axisData == null ? null : other.axisData.clone();
        axisLocalData = other.axisLocalData == null ? null : other.axisLocalData.clone();
        doorData = other.doorData == null ? new NBTTagCompound() : other.doorData.copy();
        events = copyEvents(other.events);
        rotX = copyTimeline(other.rotX);
        rotY = copyTimeline(other.rotY);
        rotZ = copyTimeline(other.rotZ);
        offX = copyTimeline(other.offX);
        offY = copyTimeline(other.offY);
        offZ = copyTimeline(other.offZ);
    }

    public int getSafeDuration() {
        return duration <= 0 ? LittleVecXAnimationLayerCompiler.DEFAULT_DURATION : duration;
    }

    public int getSafeOffGrid() {
        return offGrid <= 0 ? LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID : offGrid;
    }

    public String getDisplayName(int index) {
        String trimmed = name == null ? "" : name.trim();
        return trimmed.isEmpty() ? "Animation " + index : trimmed;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("name", name == null ? "" : name);
        nbt.setString("trigger", trigger.id);
        nbt.setString("doorType", doorType.id);
        nbt.setInteger("duration", getSafeDuration());
        nbt.setInteger("interpolation", interpolation);
        nbt.setInteger("offGrid", getSafeOffGrid());
        if (axisData != null && axisData.length == 7)
            nbt.setIntArray("axisData", axisData.clone());
        if (axisLocalData != null && axisLocalData.length == 7)
            nbt.setIntArray("axisLocalData", axisLocalData.clone());
        if (doorData != null && !doorData.isEmpty())
            nbt.setTag("doorData", doorData.copy());

        NBTTagList eventsList = new NBTTagList();
        for (AnimationEvent event : events)
            eventsList.appendTag(event.writeToNBT(new NBTTagCompound()));
        nbt.setTag("events", eventsList);

        NBTTagCompound animation = new NBTTagCompound();
        if (rotX != null)
            animation.setIntArray("rotX", rotX.write());
        if (rotY != null)
            animation.setIntArray("rotY", rotY.write());
        if (rotZ != null)
            animation.setIntArray("rotZ", rotZ.write());
        if (offX != null)
            animation.setIntArray("offX", offX.write());
        if (offY != null)
            animation.setIntArray("offY", offY.write());
        if (offZ != null)
            animation.setIntArray("offZ", offZ.write());
        nbt.setTag("animation", animation);
        return nbt;
    }

    public static LittleVecXAnimationLayer readFromNBT(NBTTagCompound nbt) {
        LittleVecXAnimationLayer layer = new LittleVecXAnimationLayer();
        layer.name = nbt.getString("name");
        layer.trigger = LittleVecXAnimationTriggerMode.fromId(nbt.getString("trigger"));
        layer.doorType = nbt.hasKey("doorType") ? LittleVecXAnimationLayerDoorType.fromId(nbt.getString("doorType")) : LittleVecXAnimationLayerDoorType.ADVANCED;
        layer.duration = nbt.hasKey("duration") ? nbt.getInteger("duration") : LittleVecXAnimationLayerCompiler.DEFAULT_DURATION;
        layer.interpolation = nbt.hasKey("interpolation") ? nbt.getInteger("interpolation") : 0;
        layer.offGrid = nbt.hasKey("offGrid") ? nbt.getInteger("offGrid") : LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID;
        layer.axisData = nbt.hasKey("axisData", 11) ? nbt.getIntArray("axisData") : null;
        layer.axisLocalData = nbt.hasKey("axisLocalData", 11) ? nbt.getIntArray("axisLocalData") : null;
        layer.doorData = nbt.hasKey("doorData", 10) ? nbt.getCompoundTag("doorData").copy() : new NBTTagCompound();
        layer.events = readEvents(nbt.getTagList("events", 10));

        NBTTagCompound animation = nbt.getCompoundTag("animation");
        layer.rotX = tryReadTimeline(animation, "rotX");
        layer.rotY = tryReadTimeline(animation, "rotY");
        layer.rotZ = tryReadTimeline(animation, "rotZ");
        layer.offX = tryReadTimeline(animation, "offX");
        layer.offY = tryReadTimeline(animation, "offY");
        layer.offZ = tryReadTimeline(animation, "offZ");
        return layer;
    }

    @Nullable
    private static ValueTimeline tryReadTimeline(NBTTagCompound nbt, String key) {
        if (nbt == null || !nbt.hasKey(key))
            return null;
        try {
            return ValueTimeline.read(nbt.getIntArray(key));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static ValueTimeline copyTimeline(@Nullable ValueTimeline timeline) {
        return timeline == null ? null : timeline.copy();
    }

    private static List<AnimationEvent> readEvents(NBTTagList list) {
        List<AnimationEvent> events = new ArrayList<>();
        for (int i = 0; i < list.tagCount(); i++) {
            AnimationEvent event = AnimationEvent.loadFromNBT(list.getCompoundTagAt(i));
            if (event != null)
                events.add(event);
        }
        return events;
    }

    private static List<AnimationEvent> copyEvents(List<AnimationEvent> source) {
        List<AnimationEvent> copy = new ArrayList<>();
        if (source == null)
            return copy;

        for (AnimationEvent event : source) {
            AnimationEvent cloned = AnimationEvent.loadFromNBT(event.writeToNBT(new NBTTagCompound()));
            if (cloned != null)
                copy.add(cloned);
        }
        return copy;
    }
}
