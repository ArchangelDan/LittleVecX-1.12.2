package com.integral.littlevecx.screwdriver;

import com.creativemd.creativecore.common.utils.mc.ColorUtils;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.LittleTileColored;
import com.creativemd.littletiles.common.tile.parent.IParentTileList;
import com.creativemd.littletiles.common.util.selection.selector.TileSelector;

import net.minecraft.nbt.NBTTagCompound;

public class LittleVecXColorSelector extends TileSelector {

    private int color;

    public LittleVecXColorSelector(int color) {
        this.color = color;
    }

    public LittleVecXColorSelector() {}

    @Override
    protected void saveNBT(NBTTagCompound nbt) {
        nbt.setInteger("color", color);
    }

    @Override
    protected void loadNBT(NBTTagCompound nbt) {
        color = nbt.getInteger("color");
    }

    @Override
    public boolean is(IParentTileList parent, LittleTile tile) {
        return getComparableColor(tile) == color;
    }

    private static int getComparableColor(LittleTile tile) {
        if (tile instanceof LittleTileColored)
            return ((LittleTileColored) tile).color;
        return ColorUtils.WHITE;
    }
}
