package com.creativemd.littletiles.common.tile.preview;

import com.creativemd.littletiles.common.tile.math.box.LittleVecXSliceFixBox;
import com.integral.littlevecx.preview.LittleVecXSliceFixPreview;

public final class LittleVecXPreviewFixHelper {

    private LittleVecXPreviewFixHelper() {}

    public static LittlePreview convertSlicePreview(LittlePreview preview) {
        if (preview == null || preview instanceof LittleVecXSliceFixPreview)
            return preview;

        LittleVecXSliceFixBox convertedBox = LittleVecXSliceFixBox.tryConvertVanillaSlice(preview.getBox());
        if (convertedBox == null)
            return preview;

        return new LittleVecXSliceFixPreview(convertedBox, preview.getTileData().copy());
    }

    public static LittlePreviews convertSlicesToSliceFix(LittlePreviews previews) {
        if (previews == null)
            return null;

        for (int i = 0; i < previews.previews.size(); i++)
            previews.previews.set(i, convertSlicePreview(previews.previews.get(i)));

        for (LittlePreviews child : previews.children)
            convertSlicesToSliceFix(child);

        return previews;
    }
}
