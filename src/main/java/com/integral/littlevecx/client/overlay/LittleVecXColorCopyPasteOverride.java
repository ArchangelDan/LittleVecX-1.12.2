package com.integral.littlevecx.client.overlay;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.Color;

import com.creativemd.creativecore.common.gui.GuiControl;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiColorPicker;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;

import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleVecXColorCopyPasteOverride<K extends SubGui> extends LittleVecXSubGuiOverride<K> {

    private static final String COPY_PREFIX = "littlevecx_color_copy_";
    private static final String PASTE_PREFIX = "littlevecx_color_paste_";
    private static final int BUTTON_SIZE = 4;
    private static final int BUTTON_GAP = 5;
    private static final int BUTTON_GROUP_OFFSET_X = -52;
    private static final int BUTTON_GROUP_OFFSET_Y = 12;

    private static Color copiedColor;

    public LittleVecXColorCopyPasteOverride(Class<K> key) {
        super(key);
    }

    @Override
    protected void overrideGui(K gui) {
        List<GuiControl> controls = new ArrayList<GuiControl>(gui.controls);
        for (GuiControl control : controls) {
            if (!(control instanceof GuiColorPicker))
                continue;

            GuiColorPicker picker = (GuiColorPicker) control;
            String copyName = COPY_PREFIX + picker.name;
            String pasteName = PASTE_PREFIX + picker.name;
            if (gui.get(copyName) != null || gui.get(pasteName) != null)
                continue;

            int x = picker.posX + Math.max(0, picker.width - (BUTTON_SIZE * 2) - BUTTON_GAP - 4) + BUTTON_GROUP_OFFSET_X;
            int y = picker.posY + Math.max(0, picker.height - BUTTON_SIZE - 4) + BUTTON_GROUP_OFFSET_Y;

            gui.addControl(new ColorCopyPasteButton(copyName, "c", x, y, "copy color") {
                @Override
                public void onClicked(int mouseX, int mouseY, int button) {
                    copiedColor = copyColor(picker.color);
                    playSound(SoundEvents.UI_BUTTON_CLICK);
                }
            });

            gui.addControl(new ColorCopyPasteButton(pasteName, "p", x + BUTTON_SIZE + BUTTON_GAP, y, "paste color") {
                @Override
                public void onClicked(int mouseX, int mouseY, int button) {
                    if (copiedColor == null)
                        return;
                    picker.setColor(copyColor(copiedColor));
                    picker.onColorChanged();
                    gui.raiseEvent(new GuiControlChangedEvent(picker));
                    playSound(SoundEvents.UI_BUTTON_CLICK);
                }
            });
        }
    }

    private static Color copyColor(Color source) {
        return new Color(source.getRed(), source.getGreen(), source.getBlue(), source.getAlpha());
    }

    private static abstract class ColorCopyPasteButton extends GuiButton {

        private ColorCopyPasteButton(String name, String title, int x, int y, String tooltip) {
            super(name, title, x, y, BUTTON_SIZE, BUTTON_SIZE);
            setCustomTooltip(tooltip);
        }
    }
}
