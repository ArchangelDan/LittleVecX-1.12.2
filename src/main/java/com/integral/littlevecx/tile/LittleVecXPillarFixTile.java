package com.integral.littlevecx.tile;

import com.creativemd.creativecore.common.utils.mc.ColorUtils;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.LittleTileColored;
import com.creativemd.littletiles.common.tile.math.box.LittleVecXPillarFixBox;
import com.creativemd.littletiles.common.tile.preview.LittlePreview;
import com.integral.littlevecx.preview.LittleVecXPillarFixPreview;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class LittleVecXPillarFixTile extends LittleTileColored {

    public static final String TILE_ID = "pillar_fix_tile";

    public LittleVecXPillarFixTile() {
        super();
        color = ColorUtils.WHITE;
    }

    @Override
    public void saveTileCore(NBTTagCompound nbt) {
        super.saveTileCore(nbt);
        if (getBox() instanceof LittleVecXPillarFixBox)
            LittleVecXPillarFixBox.writeMarker(nbt);
    }

    @Override
    public void loadTileCore(NBTTagCompound nbt) {
        super.loadTileCore(nbt);
        int[] savedBox = getSavedBoxArray(nbt);
        LittleVecXPillarFixBox restored = LittleVecXPillarFixBox.restoreFromArray(savedBox, nbt);
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
        if (getBox() instanceof LittleVecXPillarFixBox) {
            NBTTagCompound nbt = new NBTTagCompound();
            saveTileExtra(nbt);
            nbt.setString("tID", TILE_ID);
            return new LittleVecXPillarFixPreview((LittleVecXPillarFixBox) getBox().copy(), nbt);
        }
        return super.getPreviewTile();
    }

    @Override
    public boolean canBeNBTGrouped(LittleTile tile) {
        return tile instanceof LittleVecXPillarFixTile && super.canBeNBTGrouped(tile);
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
}
