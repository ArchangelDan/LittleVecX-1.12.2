package com.integral.littlevecx.screwdriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.creativemd.creativecore.common.utils.type.Pair;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.math.box.LittleAbsoluteBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxes;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxesSimple;
import com.creativemd.littletiles.common.tile.parent.IParentTileList;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.selection.selector.TileSelector;
import com.integral.littlevecx.selection.IndustrialSelectionMode;
import com.integral.littlevecx.selection.IndustrialSelectionRegion;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class LittleVecXIndustrialScrewdriverLogic {

    private LittleVecXIndustrialScrewdriverLogic() {}

    public static LittleBoxes buildFilteredBoxes(World world, ItemStack stack, TileSelector selector) {
        List<IndustrialSelectionRegion> regions = IndustrialSelectionMode.getSavedOrCurrentRegions(stack);
        if (regions.isEmpty())
            return new LittleBoxesSimple(BlockPos.ORIGIN, LittleGridContext.getMin());

        LittleBoxes baseBoxes = IndustrialSelectionMode.buildBoxes(regions);
        return buildFilteredBoxes(world, baseBoxes, baseBoxes.generateBlockWise(), selector);
    }

    public static LittleBoxes buildFilteredBoxes(World world, LittleBoxes sourceBoxes, com.creativemd.creativecore.common.utils.type.HashMapList<BlockPos, LittleBox> selectionMap,
            TileSelector selector) {
        if (selectionMap == null || selectionMap.isEmpty())
            return new LittleBoxesSimple(sourceBoxes.pos, sourceBoxes.context);

        LittleBoxesSimple filteredBoxes = new LittleBoxesSimple(sourceBoxes.pos, LittleGridContext.getMin());

        for (Entry<BlockPos, ArrayList<LittleBox>> entry : selectionMap.entrySet()) {
            BlockPos sourcePos = entry.getKey();
            net.minecraft.tileentity.TileEntity tileEntity = world.getTileEntity(sourcePos);
            if (!(tileEntity instanceof com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles))
                continue;
            com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles te = (com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles) tileEntity;

            LittleGridContext workingContext = LittleGridContext.max(sourceBoxes.context, te.getContext());
            List<LittleBox> selectedBoxes = convertBoxes(entry.getValue(), sourceBoxes.context, workingContext);

            for (Pair<IParentTileList, LittleTile> pair : te.allTiles()) {
                if (!selector.is(pair.key, pair.value))
                    continue;

                LittleTile workingTile = pair.value;
                if (workingContext != te.getContext()) {
                    workingTile = pair.value.copy();
                    if (workingTile == null)
                        continue;
                    workingTile.convertTo(te.getContext(), workingContext);
                }

                List<LittleBox> selectedParts = new ArrayList<>();
                workingTile.cutOut(selectedBoxes, selectedParts, null);
                for (LittleBox cutBox : selectedParts)
                    filteredBoxes.addBox(workingContext, sourcePos, cutBox.copy());
            }
        }

        filteredBoxes.combineBoxesBlocks();
        return filteredBoxes;
    }

    private static List<LittleBox> convertBoxes(List<LittleBox> boxes, LittleGridContext from, LittleGridContext to) {
        if (from == to)
            return boxes;

        List<LittleBox> converted = new ArrayList<>(boxes.size());
        for (LittleBox box : boxes) {
            LittleBox copy = box.copy();
            copy.convertTo(from, to);
            converted.add(copy);
        }
        return converted;
    }

}
