package com.integral.littlevecx.preview;

import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.math.box.LittleVecXSliceFixBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.preview.LittlePreview;
import com.integral.littlevecx.tile.LittleVecXSliceFixTile;

import net.minecraft.nbt.NBTTagCompound;

public class LittleVecXSliceFixPreview extends LittlePreview {

    public LittleVecXSliceFixPreview(NBTTagCompound nbt) {
        super(nbt);
        ensureTileType();
        LittleVecXSliceFixBox restored = LittleVecXSliceFixBox.restoreFromArray(nbt.getIntArray("bBox"), nbt);
        if (restored != null)
            this.box = restored;
    }

    public LittleVecXSliceFixPreview(LittleVecXSliceFixBox box, NBTTagCompound tileData) {
        super(box, tileData);
        ensureTileType();
    }

    private void ensureTileType() {
        tileData.setString("tID", LittleVecXSliceFixTile.TILE_ID);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        ensureTileType();
        super.writeToNBT(nbt);
        if (box instanceof LittleVecXSliceFixBox)
            LittleVecXSliceFixBox.writeMarker(nbt, ((LittleVecXSliceFixBox) box).getVec(), ((LittleVecXSliceFixBox) box).getProfileAxis(),
                    ((LittleVecXSliceFixBox) box).getProfileCutValue());
    }

    @Override
    public LittleTile getLittleTile() {
        ensureTileType();
        return super.getLittleTile();
    }
    
    @Override
    public boolean canBeNBTGrouped(LittlePreview preview) {
        if (!(preview instanceof LittleVecXSliceFixPreview) || !super.canBeNBTGrouped(preview))
            return false;
        return hasMatchingSliceState(this.box, preview.getBox());
    }
    
    private static boolean hasMatchingSliceState(LittleBox first, LittleBox second) {
        if (!(first instanceof LittleVecXSliceFixBox) || !(second instanceof LittleVecXSliceFixBox))
            return false;
        LittleVecXSliceFixBox left = (LittleVecXSliceFixBox) first;
        LittleVecXSliceFixBox right = (LittleVecXSliceFixBox) second;
        return left.getVec().equals(right.getVec()) && left.getProfileAxis() == right.getProfileAxis()
                && left.getProfileCutValue() == right.getProfileCutValue();
    }
}
