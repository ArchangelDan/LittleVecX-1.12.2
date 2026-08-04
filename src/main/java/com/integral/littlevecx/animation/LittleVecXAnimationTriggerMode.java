package com.integral.littlevecx.animation;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.littletiles.common.structure.type.door.LittleDoor.DoorActivator;

public enum LittleVecXAnimationTriggerMode {

    NONE("none", "gui.littlevecx.animation_trigger.none"),
    RIGHT_CLICK("right_click", "gui.littlevecx.animation_trigger.right_click"),
    SHIFT_RIGHT_CLICK("shift_right_click", "gui.littlevecx.animation_trigger.shift_right_click");

    public final String id;
    public final String translationKey;

    LittleVecXAnimationTriggerMode(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String getCaption() {
        return CoreControl.translate(translationKey);
    }

    public static String[] captions() {
        LittleVecXAnimationTriggerMode[] values = values();
        String[] captions = new String[values.length];
        for (int i = 0; i < values.length; i++)
            captions[i] = values[i].getCaption();
        return captions;
    }

    public static LittleVecXAnimationTriggerMode fromId(String id) {
        if (id != null) {
            for (LittleVecXAnimationTriggerMode mode : values())
                if (mode.id.equals(id))
                    return mode;
        }
        return NONE;
    }

    public static LittleVecXAnimationTriggerMode fromIndex(int index) {
        LittleVecXAnimationTriggerMode[] values = values();
        if (index < 0 || index >= values.length)
            return NONE;
        return values[index];
    }

    public static LittleVecXAnimationTriggerMode fromActivator(DoorActivator activator) {
        if (activator == DoorActivator.COMMAND)
            return SHIFT_RIGHT_CLICK;
        if (activator == DoorActivator.RIGHTCLICK)
            return RIGHT_CLICK;
        return NONE;
    }
}
