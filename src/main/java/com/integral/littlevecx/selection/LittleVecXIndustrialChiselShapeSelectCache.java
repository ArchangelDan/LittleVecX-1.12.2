package com.integral.littlevecx.selection;

import java.util.List;

import com.creativemd.littletiles.common.tile.math.box.LittleBoxes;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.shape.LittleShape;
import com.creativemd.littletiles.common.util.shape.ShapeSelection.ShapeSelectPos;

class LittleVecXIndustrialChiselShapeSelectCache {

    protected final List<ShapeSelectPos> positions;
    protected final LittleGridContext context;
    protected final LittleShape shape;
    protected final String shapeKey;
    protected final LittleBoxes cachedBoxesLowRes;
    protected LittleBoxes cachedBoxes;

    private final LittleVecXIndustrialChiselSelection selection;

    public LittleVecXIndustrialChiselShapeSelectCache(LittleVecXIndustrialChiselSelection selection, LittleGridContext context,
            List<ShapeSelectPos> positions, String shapeKey, LittleShape shape) {
        this.selection = selection;
        this.context = context;
        this.positions = positions;
        this.shapeKey = shapeKey;
        this.shape = shape;
        this.cachedBoxesLowRes = shape.getBoxes(selection, true);
    }

    public LittleBoxes get(boolean allowLowResolution) {
        if (allowLowResolution)
            return cachedBoxesLowRes;
        if (cachedBoxes == null)
            cachedBoxes = shape.getBoxes(selection, false);
        return cachedBoxes;
    }
}
