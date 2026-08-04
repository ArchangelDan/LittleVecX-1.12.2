package com.integral.littlevecx.client.gui;

import com.creativemd.creativecore.common.gui.GuiRenderHelper;
import com.creativemd.creativecore.common.gui.client.style.Style;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.integral.littlevecx.LittleVecXMod;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class GuiLittleVecXSearchIconButton extends GuiButton {

    private static final ResourceLocation SEARCH_ICON = new ResourceLocation(LittleVecXMod.MODID, "textures/gui/search.png");
    public static final int BUTTON_SIZE = 14;
    private static final int ICON_SIZE = 12;

    public GuiLittleVecXSearchIconButton(String name, int x, int y) {
        super(name, "", x, y, BUTTON_SIZE, BUTTON_SIZE);
        setLangTooltip("gui.signal.configuration.search");
    }

    @Override
    protected void renderContent(GuiRenderHelper helper, Style style, int width, int height) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.renderEngine.bindTexture(SEARCH_ICON);

        int iconX = (width - ICON_SIZE) / 2;
        int iconY = (height - ICON_SIZE) / 2;
        Gui.drawScaledCustomSizeModalRect(iconX, iconY, 0, 0, 16, 16, ICON_SIZE, ICON_SIZE, 16, 16);
    }
}
