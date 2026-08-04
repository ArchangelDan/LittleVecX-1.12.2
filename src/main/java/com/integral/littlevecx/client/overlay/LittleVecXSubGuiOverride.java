package com.integral.littlevecx.client.overlay;

import java.util.HashMap;
import java.util.Map;

import com.creativemd.creativecore.common.gui.container.SubGui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class LittleVecXSubGuiOverride<K extends SubGui> {

    private static final Map<Class<? extends SubGui>, LittleVecXSubGuiOverride<?>> OVERRIDES = new HashMap<>();

    protected LittleVecXSubGuiOverride(Class<K> key) {
        OVERRIDES.put(key, this);
    }

    public static void apply(SubGui gui) {
        if (gui == null)
            return;

        LittleVecXSubGuiOverride<?> override = OVERRIDES.get(gui.getClass());
        if (override != null) {
            override.applyTyped(gui);
            return;
        }

        for (Map.Entry<Class<? extends SubGui>, LittleVecXSubGuiOverride<?>> entry : OVERRIDES.entrySet()) {
            if (entry.getKey().isAssignableFrom(gui.getClass())) {
                entry.getValue().applyTyped(gui);
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void applyTyped(SubGui gui) {
        overrideGui((K) gui);
    }

    protected abstract void overrideGui(K gui);
}
