package com.integral.littlevecx.client;

import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiCheckBox;
import com.creativemd.littletiles.common.tile.math.vec.LittleVec;
import com.creativemd.littletiles.common.tile.math.vec.LittleVecContext;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.place.IMarkMode;
import com.creativemd.littletiles.common.util.place.PlacementPosition;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleVecXFurnitureMarkMode implements IMarkMode {

    private final PlacementPosition position;
    private boolean allowLowResolution;
    private final boolean validAtStart;

    public LittleVecXFurnitureMarkMode(IMarkMode original, boolean validAtStart) {
        this.position = original.getPosition().copy();
        this.allowLowResolution = original.allowLowResolution();
        this.validAtStart = validAtStart;
    }

    public boolean isValidAtStart() {
        return validAtStart;
    }

    @Override
    public boolean allowLowResolution() {
        return allowLowResolution;
    }

    @Override
    public PlacementPosition getPosition() {
        return position.copy();
    }

    @Override
    public SubGui getConfigurationGui() {
        return new SubGui() {
            @Override
            public void createControls() {
                controls.add(new GuiCheckBox("resolution", I18n.translateToLocal("markmode.gui.allowlowresolution"), 0, 0, allowLowResolution));
            }

            @Override
            public void onClosed() {
                super.onClosed();
                GuiCheckBox box = (GuiCheckBox) get("resolution");
                allowLowResolution = box.value;
            }
        };
    }

    @Override
    public void render(LittleGridContext positionContext, double x, double y, double z) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        AxisAlignedBB box = new AxisAlignedBB(x, y, z, x + positionContext.pixelSize, y + positionContext.pixelSize, z + positionContext.pixelSize).grow(0.002).offset(-x, -y, -z);

        GlStateManager.glLineWidth(4.0F);
        RenderGlobal.drawSelectionBoundingBox(box, 0.0F, 0.0F, 0.0F, 1F);

        GlStateManager.disableDepth();
        GlStateManager.glLineWidth(1.0F);
        RenderGlobal.drawSelectionBoundingBox(box, 1F, 0.3F, 0.0F, 1F);
        GlStateManager.enableDepth();

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    @Override
    public void move(LittleGridContext positionContext, EnumFacing facing) {
        if (facing.getAxis().isVertical())
            return;

        LittleVec vec = new LittleVec(facing.getOpposite());
        vec.scale(GuiScreen.isCtrlKeyDown() ? positionContext.size : 1);
        position.sub(new LittleVecContext(vec, positionContext));
    }

    @Override
    public void done() {}
}