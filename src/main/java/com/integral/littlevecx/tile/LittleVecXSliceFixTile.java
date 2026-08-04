package com.integral.littlevecx.tile;

import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.LittleTileColored;
import com.creativemd.littletiles.common.tile.math.box.LittleVecXSliceFixBox;
import com.creativemd.littletiles.common.tile.preview.LittlePreview;
import com.integral.littlevecx.preview.LittleVecXSliceFixPreview;
import com.creativemd.creativecore.common.utils.mc.ColorUtils;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class LittleVecXSliceFixTile extends LittleTileColored {

    public static final String TILE_ID = "slice_fix_tile";

    public LittleVecXSliceFixTile() {
        super();
        color = ColorUtils.WHITE;
    }

    @Override
    public void saveTileCore(NBTTagCompound nbt) {
        super.saveTileCore(nbt);
        if (getBox() instanceof LittleVecXSliceFixBox)
            LittleVecXSliceFixBox.writeMarker(nbt, ((LittleVecXSliceFixBox) getBox()).getVec(), ((LittleVecXSliceFixBox) getBox()).getProfileAxis(),
                    ((LittleVecXSliceFixBox) getBox()).getProfileCutValue());
    }

    @Override
    public void loadTileCore(NBTTagCompound nbt) {
        super.loadTileCore(nbt);
        int[] savedBox = getSavedBoxArray(nbt);
        LittleVecXSliceFixBox restored = LittleVecXSliceFixBox.restoreFromArray(savedBox, nbt);
        if (restored != null)
            setBox(restored);
    }

    @Override
    public void loadTileExtra(NBTTagCompound nbt) {
        super.loadTileExtra(nbt);
        if (!nbt.hasKey("color"))
            color = ColorUtils.WHITE;
    }

    @Override
    public LittlePreview getPreviewTile() {
        if (getBox() instanceof LittleVecXSliceFixBox) {
            NBTTagCompound nbt = new NBTTagCompound();
            saveTileExtra(nbt);
            nbt.setString("tID", TILE_ID);
            return new LittleVecXSliceFixPreview((LittleVecXSliceFixBox) getBox().copy(), nbt);
        }
        return super.getPreviewTile();
    }

    @Override
    public boolean canBeNBTGrouped(LittleTile tile) {
        if (!(tile instanceof LittleVecXSliceFixTile) || !super.canBeNBTGrouped(tile))
            return false;
        return hasMatchingSliceState(getBox(), tile.getBox());
    }

    @Override
    public boolean canBeCombined(LittleTile tile) {
        return false;
    }

    private static int[] getSavedBoxArray(NBTTagCompound nbt) {
        if (nbt.hasKey("box"))
            return nbt.getIntArray("box");
        if (nbt.hasKey("boxes")) {
            NBTTagList boxes = nbt.getTagList("boxes", 11);
            if (boxes.tagCount() > 0)
                return boxes.getIntArrayAt(0);
        }
        return null;
    }
    
    private static boolean hasMatchingSliceState(com.creativemd.littletiles.common.tile.math.box.LittleBox first,
            com.creativemd.littletiles.common.tile.math.box.LittleBox second) {
        if (!(first instanceof LittleVecXSliceFixBox) || !(second instanceof LittleVecXSliceFixBox))
            return false;
        LittleVecXSliceFixBox left = (LittleVecXSliceFixBox) first;
        LittleVecXSliceFixBox right = (LittleVecXSliceFixBox) second;
        return left.getVec().equals(right.getVec()) && left.getProfileAxis() == right.getProfileAxis()
                && left.getProfileCutValue() == right.getProfileCutValue();
    }
}
