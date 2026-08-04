package com.integral.littlevecx.animation;

import com.creativemd.creativecore.common.gui.CoreControl;

public enum LittleVecXAnimationLayerDoorType {

    AXIS("axis"),
    ADVANCED("advanced"),
    SLIDING("sliding");

    public final String id;

    LittleVecXAnimationLayerDoorType(String id) {
        this.id = id;
    }

    public String getCaption() {
        return CoreControl.translate("gui.littlevecx.animation_door_type." + id);
    }

    public static String[] captions() {
        LittleVecXAnimationLayerDoorType[] values = values();
        String[] captions = new String[values.length];
        for (int i = 0; i < values.length; i++)
            captions[i] = values[i].getCaption();
        return captions;
    }

    public static LittleVecXAnimationLayerDoorType fromIndex(int index) {
        LittleVecXAnimationLayerDoorType[] values = values();
        if (index < 0 || index >= values.length)
            return AXIS;
        return values[index];
    }

    public static LittleVecXAnimationLayerDoorType fromId(String id) {
        for (LittleVecXAnimationLayerDoorType value : values()) {
            if (value.id.equals(id))
                return value;
        }
        return ADVANCED;
    }
}
