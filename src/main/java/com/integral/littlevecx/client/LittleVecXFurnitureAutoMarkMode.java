package com.integral.littlevecx.client;

import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.place.IMarkMode;
import com.creativemd.littletiles.common.util.place.PlacementPosition;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleVecXFurnitureAutoMarkMode implements IMarkMode {

    private final PlacementPosition position;
    private final boolean allowLowResolution;

    public LittleVecXFurnitureAutoMarkMode(PlacementPosition position, boolean allowLowResolution) {
        this.position = position.copy();
        this.allowLowResolution = allowLowResolution;
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
            public void createControls() {}
        };
    }

    @Override
    public void render(LittleGridContext positionContext, double x, double y, double z) {}

    @Override
    public void move(LittleGridContext positionContext, EnumFacing facing) {}

    @Override
    public void done() {}
}
