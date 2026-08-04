package com.integral.littlevecx.client.gui.controls;

import org.lwjgl.util.Color;

import com.creativemd.creativecore.common.gui.GuiRenderHelper;
import com.creativemd.creativecore.common.gui.client.style.Style;
import com.creativemd.creativecore.common.gui.controls.gui.GuiSteppedSlider;

public class GuiLittleVecXColoredSteppedSlider extends GuiSteppedSlider {

    public GuiLittleVecXColorPicker picker;
    public LittleVecXColorParts.ColorPart part;

    public GuiLittleVecXColoredSteppedSlider(String name, int x, int y, int width, int height, GuiLittleVecXColorPicker picker, LittleVecXColorParts.ColorPart part) {
        super(name, x, y, width, height, part.getColor(picker.color), 0, 255);
        this.picker = picker;
        this.part = part;
    }

    @Override
    public void setValue(double value) {
        super.setValue((int) value);
        if (part != null) {
            part.setColor(picker.color, (int) this.value);
            picker.onColorChanged();
        }
    }

    @Override
    protected void renderContent(GuiRenderHelper helper, Style style, int width, int height) {
        if (part == LittleVecXColorParts.ColorPart.ALPHA) {
            Color startColor = new Color(picker.color);
            startColor.setAlpha(0);
            Color endColor = new Color(picker.color);
            endColor.setAlpha(255);
            helper.drawHorizontalGradientRect(0, 0, width, height, LittleVecXColorParts.RGBAToInt(startColor), LittleVecXColorParts.RGBAToInt(endColor));
        } else
            helper.drawHorizontalChannelMaskGradientRect(0, 0, width, height, LittleVecXColorParts.RGBAToInt(picker.color), part.getBrightest());

        super.renderContent(helper, style, width, height);
    }
}
