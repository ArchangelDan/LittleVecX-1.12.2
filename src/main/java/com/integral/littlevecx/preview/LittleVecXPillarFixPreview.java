package com.integral.littlevecx.preview;

import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.math.box.LittleVecXPillarFixBox;
import com.creativemd.littletiles.common.tile.preview.LittlePreview;
import com.integral.littlevecx.tile.LittleVecXPillarFixTile;

import net.minecraft.nbt.NBTTagCompound;

public class LittleVecXPillarFixPreview extends LittlePreview {

    public LittleVecXPillarFixPreview(NBTTagCompound nbt) {
        super(nbt);
        ensureTileType();
        LittleVecXPillarFixBox restored = LittleVecXPillarFixBox.restoreFromArray(nbt.getIntArray("bBox"), nbt);
        if (restored != null)
            this.box = restored;
    }

    public LittleVecXPillarFixPreview(LittleVecXPillarFixBox box, NBTTagCompound tileData) {
        super(box, tileData);
        ensureTileType();
    }

    private void ensureTileType() {
        tileData.setString("tID", LittleVecXPillarFixTile.TILE_ID);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        ensureTileType();
        super.writeToNBT(nbt);
        if (box instanceof LittleVecXPillarFixBox)
            LittleVecXPillarFixBox.writeMarker(nbt);
    }

    @Override
    public LittleTile getLittleTile() {
        ensureTileType();
        return super.getLittleTile();
    }
    
    @Override
    public boolean canBeNBTGrouped(LittlePreview preview) {
        return preview instanceof LittleVecXPillarFixPreview && super.canBeNBTGrouped(preview);
    }
}
