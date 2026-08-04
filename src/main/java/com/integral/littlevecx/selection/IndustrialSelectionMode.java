package com.integral.littlevecx.selection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashSet;
import java.lang.reflect.Field;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.utils.type.HashMapList;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.LittleTilesConfig.AreaTooLarge;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.action.block.LittleActionDestroyBoxes;
import com.creativemd.littletiles.common.mod.chiselsandbits.ChiselsAndBitsManager;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException;
import com.creativemd.littletiles.common.structure.exception.NotYetConnectedException;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.math.box.LittleAbsoluteBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxReturnedVolume;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxes;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxesSimple;
import com.creativemd.littletiles.common.tile.math.vec.LittleAbsoluteVec;
import com.creativemd.littletiles.common.tile.math.vec.LittleVec;
import com.creativemd.littletiles.common.tile.preview.LittlePreview;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.tile.preview.LittleVecXPreviewFixHelper;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.selection.mode.SelectionMode;
import com.creativemd.littletiles.common.util.shape.ShapeSelection;
import com.creativemd.littletiles.common.util.shape.ShapeSelection.ShapeSelectPos;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class IndustrialSelectionMode extends SelectionMode {

    public static final String KEY_PENDING = "littlevecx_industrial_pending";
    public static final String KEY_PENDING_FACE = "littlevecx_industrial_pending_face";
    public static final String KEY_REGIONS = "littlevecx_industrial_regions";
    public static final String KEY_SAVED_REGIONS = "littlevecx_industrial_saved_regions";
    private static Field shapeSelectionMarkedField;

    public IndustrialSelectionMode() {
        super("industrial");
    }

    @Override
    public SelectionResult generateResult(World world, ItemStack stack) {
        List<IndustrialSelectionRegion> regions = getCurrentRegions(stack);
        if (regions.isEmpty())
            return null;

        SelectionResult result = new SelectionResult(world);
        Set<BlockPos> positions = new HashSet<>();
        for (IndustrialSelectionRegion region : regions) {
            LittleAbsoluteBox box = region.toAbsoluteBox();
            if (box == null)
                continue;

            for (BlockPos pos : box.splitted().keySet())
                if (positions.add(pos))
                    result.addBlock(pos);
        }
        return result;
    }

    @Override
    public void onLeftClick(EntityPlayer player, ItemStack stack, BlockPos pos) {
        onSelectionClick(player, stack, new LittleAbsoluteVec(pos, LittleGridContext.getMin()), EnumFacing.UP, false);
    }

    @Override
    public void onRightClick(EntityPlayer player, ItemStack stack, BlockPos pos) {
        onSelectionClick(player, stack, new LittleAbsoluteVec(pos, LittleGridContext.getMin()), EnumFacing.UP, true);
    }

    public void onSelectionClick(EntityPlayer player, ItemStack stack, LittleAbsoluteVec position, @Nullable EnumFacing facing, boolean rightClick) {
        ensureTag(stack);

        if (!rightClick) {
            setPendingSelection(stack, position, facing == null ? EnumFacing.UP : facing);
            if (!player.world.isRemote)
                player.sendMessage(new TextComponentTranslation("selection.mode.industrial.pos.first", position.toString()));
            return;
        }

        PendingSelection pending = getPendingSelection(stack);
        if (pending == null) {
            setPendingSelection(stack, position, facing == null ? EnumFacing.UP : facing);
            if (!player.world.isRemote)
                player.sendMessage(new TextComponentTranslation("selection.mode.industrial.pos.first", position.toString()));
            return;
        }

        IndustrialSelectionRegion region = new IndustrialSelectionRegion(pending.position, position, facing == null ? pending.facing : facing);
        if (!region.isValid()) {
            setPendingSelection(stack, position, facing == null ? EnumFacing.UP : facing);
            if (!player.world.isRemote)
                player.sendMessage(new TextComponentTranslation("selection.mode.industrial.invalid"));
            return;
        }

        addRegion(stack, region);
        clearPendingSelection(stack);
        if (!player.world.isRemote)
            player.sendMessage(new TextComponentTranslation("selection.mode.industrial.region.added", getCurrentRegions(stack).size()));
    }

    @Override
    public void clearSelection(ItemStack stack) {
        ensureTag(stack);
        stack.getTagCompound().removeTag(KEY_PENDING);
        stack.getTagCompound().removeTag(KEY_PENDING_FACE);
        stack.getTagCompound().removeTag(KEY_REGIONS);
    }

    @Override
    public void saveSelection(ItemStack stack) {
        ensureTag(stack);
        NBTTagList saved = new NBTTagList();
        for (IndustrialSelectionRegion region : getCurrentRegions(stack))
            saved.appendTag(region.writeToNBT(new NBTTagCompound()));
        stack.getTagCompound().setTag(KEY_SAVED_REGIONS, saved);
    }

    @Override
    public LittlePreviews getPreviews(World world, EntityPlayer player, ItemStack stack, boolean includeVanilla, boolean includeCB, boolean includeLT, boolean rememberStructure)
            throws LittleActionException {
        return getPreviews(world, player, getCurrentRegions(stack), includeVanilla, includeCB, includeLT, rememberStructure);
    }

    public LittlePreviews getPreviews(World world, EntityPlayer player, List<IndustrialSelectionRegion> regions, boolean includeVanilla, boolean includeCB, boolean includeLT,
            boolean rememberStructure) throws LittleActionException {
        if (regions.isEmpty())
            return new LittlePreviews(LittleGridContext.getMin());

        int touchedBlocks = 0;
        for (IndustrialSelectionRegion region : regions) {
            LittleAbsoluteBox box = region.toAbsoluteBox();
            if (box != null)
                touchedBlocks += box.splitted().size();
        }

        if (LittleTiles.CONFIG.build.get(player).limitRecipeSize && touchedBlocks > LittleTiles.CONFIG.build.get(player).recipeBlocksLimit)
            throw new AreaTooLarge(player);

        BlockPos globalMin = getGlobalMin(regions);
        LittlePreviews previews = new LittlePreviews(LittleGridContext.getMin());
        List<LittleStructure> rememberedStructures = rememberStructure ? new ArrayList<>() : null;

        for (IndustrialSelectionRegion region : regions)
            appendRegionPreviews(previews, globalMin, world, region, includeVanilla, includeCB, includeLT, rememberStructure, rememberedStructures);

        return previews;
    }

    public LittlePreviews getCrashBackupPreviews(World world, List<IndustrialSelectionRegion> regions, boolean includeVanilla, boolean includeCB, boolean includeLT,
            boolean rememberStructure) {
        if (regions == null || regions.isEmpty())
            return new LittlePreviews(LittleGridContext.getMin());

        BlockPos globalMin = getGlobalMin(regions);
        LittlePreviews previews = new LittlePreviews(LittleGridContext.getMin());
        List<LittleStructure> rememberedStructures = rememberStructure ? new ArrayList<>() : null;

        for (IndustrialSelectionRegion region : regions)
            appendRegionPreviews(previews, globalMin, world, region, includeVanilla, includeCB, includeLT, rememberStructure, rememberedStructures);

        return previews;
    }

    public static List<IndustrialSelectionRegion> getCurrentRegions(ItemStack stack) {
        ensureTag(stack);
        List<IndustrialSelectionRegion> regions = new ArrayList<>();
        NBTTagList list = stack.getTagCompound().getTagList(KEY_REGIONS, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            IndustrialSelectionRegion region = IndustrialSelectionRegion.readFromNBT(list.getCompoundTagAt(i));
            if (region != null && region.isValid())
                regions.add(region);
        }
        return regions;
    }

    public static void setCurrentRegions(ItemStack stack, List<IndustrialSelectionRegion> regions) {
        ensureTag(stack);
        NBTTagList list = new NBTTagList();
        for (IndustrialSelectionRegion region : regions)
            if (region != null && region.isValid())
                list.appendTag(region.writeToNBT(new NBTTagCompound()));
        stack.getTagCompound().setTag(KEY_REGIONS, list);
        stack.getTagCompound().removeTag(KEY_PENDING);
        stack.getTagCompound().removeTag(KEY_PENDING_FACE);
    }

    public static void syncSelectionToStack(ItemStack stack, @Nullable ShapeSelection selection) {
        setCurrentRegions(stack, extractCommittedRegions(selection));
    }

    public static List<IndustrialSelectionRegion> extractCommittedRegions(@Nullable ShapeSelection selection) {
        List<IndustrialSelectionRegion> regions = new ArrayList<>();
        if (selection == null)
            return regions;

        int effectiveCount = selection.countPositions();
        if (effectiveCount <= 1)
            return regions;
        if (!isMarked(selection) && effectiveCount % 2 == 0)
            effectiveCount--;

        List<ShapeSelectPos> points = new ArrayList<>(effectiveCount);
        Iterator<ShapeSelectPos> iterator = selection.iterator();
        while (iterator.hasNext() && points.size() < effectiveCount)
            points.add(iterator.next());

        for (int i = 0; i + 1 < points.size(); i += 2) {
            ShapeSelectPos first = points.get(i);
            ShapeSelectPos second = points.get(i + 1);
            IndustrialSelectionRegion region = new IndustrialSelectionRegion(first.pos.copy(), second.pos.copy(), second.pos.facing);
            if (region.isValid())
                regions.add(region);
        }
        return regions;
    }

    public static List<IndustrialSelectionRegion> getSavedOrCurrentRegions(ItemStack stack) {
        ensureTag(stack);
        // The selection currently drawn and edited by the tool is the source for destructive
        // operations and previews. A recipe snapshot is only a fallback while restoring a
        // recipe that has no live selection yet; otherwise an old saved_regions tag makes
        // copy/move jump back to a seemingly unrelated place in the world.
        List<IndustrialSelectionRegion> current = getCurrentRegions(stack);
        if (!current.isEmpty())
            return current;

        NBTTagList list = stack.getTagCompound().getTagList(KEY_SAVED_REGIONS, 10);

        List<IndustrialSelectionRegion> regions = new ArrayList<>();
        for (int i = 0; i < list.tagCount(); i++) {
            IndustrialSelectionRegion region = IndustrialSelectionRegion.readFromNBT(list.getCompoundTagAt(i));
            if (region != null && region.isValid())
                regions.add(region);
        }
        return regions;
    }

    public static LittleBoxes buildSavedOrCurrentBoxes(ItemStack stack) {
        List<IndustrialSelectionRegion> regions = getSavedOrCurrentRegions(stack);
        return buildBoxes(regions);
    }

    public static LittleBoxes buildCurrentBoxes(ItemStack stack) {
        return buildBoxes(getCurrentRegions(stack));
    }

    public static LittleBoxes buildBoxes(List<IndustrialSelectionRegion> regions) {
        if (regions.isEmpty())
            return new LittleBoxesSimple(BlockPos.ORIGIN, LittleGridContext.getMin());

        BlockPos globalMin = getGlobalMin(regions);
        LittleBoxesSimple boxes = new LittleBoxesSimple(globalMin, LittleGridContext.getMin());
        for (IndustrialSelectionRegion region : regions) {
            LittleAbsoluteBox absoluteBox = region.toAbsoluteBox();
            if (absoluteBox == null)
                continue;

            HashMapList<BlockPos, LittleBox> split = absoluteBox.splitted();
            for (Entry<BlockPos, ArrayList<LittleBox>> entry : split.entrySet())
                for (LittleBox box : entry.getValue())
                    boxes.addBox(absoluteBox.context, entry.getKey(), box.copy());
        }
        return boxes;
    }

    private static void appendRegionPreviews(LittlePreviews combined, BlockPos globalMin, World world, IndustrialSelectionRegion region, boolean includeVanilla, boolean includeCB,
            boolean includeLT, boolean rememberStructure, @Nullable List<LittleStructure> rememberedStructures) {
        LittleAbsoluteBox absoluteBox = region.toAbsoluteBox();
        if (absoluteBox == null)
            return;

        HashMapList<BlockPos, LittleBox> split = absoluteBox.splitted();
        for (Entry<BlockPos, ArrayList<LittleBox>> entry : split.entrySet()) {
            BlockPos sourcePos = entry.getKey();
            List<LittleBox> selectedBoxes = entry.getValue();
            TileEntity tileEntity = world.getTileEntity(sourcePos);

            if (includeLT && tileEntity instanceof TileEntityLittleTiles)
                appendLittleTilePreviews(combined, globalMin, sourcePos, selectedBoxes, absoluteBox.context, (TileEntityLittleTiles) tileEntity, rememberStructure,
                    rememberedStructures);

            boolean handledCB = false;
            if (includeCB) {
                LittlePreviews cbPreviews = ChiselsAndBitsManager.getPreviews(tileEntity);
                if (cbPreviews != null) {
                    appendSpecialPreviews(combined, globalMin, sourcePos, selectedBoxes, absoluteBox.context, cbPreviews);
                    handledCB = true;
                }
            }

            if (includeVanilla && !handledCB) {
                IBlockState state = world.getBlockState(sourcePos);
                if (LittleAction.isBlockValid(state))
                    appendVanillaPreviews(combined, globalMin, sourcePos, selectedBoxes, state, absoluteBox.context);
            }
        }
    }

    private static void appendLittleTilePreviews(LittlePreviews combined, BlockPos globalMin, BlockPos sourcePos, List<LittleBox> selectedBoxes, LittleGridContext selectionContext,
            TileEntityLittleTiles te, boolean rememberStructure, @Nullable List<LittleStructure> rememberedStructures) {
        boolean exactLooseTilesHandled = appendLooseTilePreviewsFromClone(combined, globalMin, sourcePos, selectedBoxes, selectionContext, te);
        LittleGridContext workingContext = LittleGridContext.max(selectionContext, te.getContext());
        List<LittleBox> convertedBoxes = convertBoxes(selectedBoxes, selectionContext, workingContext);
        for (com.creativemd.littletiles.common.tile.parent.IParentTileList parent : te.groups()) {
            if (rememberStructure && parent.isStructure()) {
                if (!intersects(parent, te.getContext(), workingContext, convertedBoxes))
                    continue;

                try {
                    LittleStructure structure = parent.getStructure();
                    while (structure.getParent() != null)
                        structure = structure.getParent().getStructure();
                    structure.load();
                    if (rememberedStructures != null && !rememberedStructures.contains(structure)) {
                        combined.addChild(structure.getPreviews(globalMin), false);
                        rememberedStructures.add(structure);
                    }
                } catch (CorruptedConnectionException | NotYetConnectedException e) {}
                continue;
            }
            if (!parent.isStructure() && exactLooseTilesHandled)
                continue;

            for (LittleTile tile : parent) {
                LittleTile workingTile = tile;
                if (workingContext != te.getContext()) {
                    workingTile = tile.copy();
                    if (workingTile == null)
                        continue;
                    workingTile.convertTo(te.getContext(), workingContext);
                }

                LittleBox intersecting = null;
                boolean intersects = false;
                for (int i = 0; i < convertedBoxes.size(); i++) {
                    if (workingTile.intersectsWith(convertedBoxes.get(i))) {
                        intersects = true;
                        intersecting = convertedBoxes.get(i);
                        break;
                    }
                }

                if (!intersects)
                    continue;

                if (intersecting != null && workingTile.equalsBox(intersecting)) {
                    addTilePreviewPiece(combined, globalMin, sourcePos, workingTile, workingContext);
                    continue;
                }

                List<LittleBox> cutout = new ArrayList<>();
                workingTile.cutOut(convertedBoxes, cutout, new com.creativemd.littletiles.common.tile.math.box.LittleBoxReturnedVolume());
                if (cutout.isEmpty())
                    continue;

                for (LittleBox cutBox : cutout) {
                    LittleTile cutTile = workingTile.copy();
                    if (cutTile == null)
                        continue;
                    cutTile.setBox(cutBox.copy());
                    addTilePreviewPiece(combined, globalMin, sourcePos, cutTile, workingContext);
                }
            }
        }
    }

    private static boolean appendLooseTilePreviewsFromClone(LittlePreviews combined, BlockPos globalMin, BlockPos sourcePos, List<LittleBox> selectedBoxes,
            LittleGridContext selectionContext, TileEntityLittleTiles sourceTe) {
        TileEntityLittleTiles copyTe = cloneTileEntity(sourceTe);
        if (copyTe == null)
            return false;

        for (LittleBox selectedBox : selectedBoxes) {
            LittleBox workingBox = selectedBox.copy();
            List<LittleTile> removedTiles = LittleActionDestroyBoxes.removeBox(copyTe, selectionContext, workingBox, false, new LittleBoxReturnedVolume());
            LittleGridContext copyContext = copyTe.getContext();
            for (LittleTile removedTile : removedTiles)
                addTilePreviewPiece(combined, globalMin, sourcePos, removedTile, copyContext);
        }

        return true;
    }

    @Nullable
    private static TileEntityLittleTiles cloneTileEntity(TileEntityLittleTiles sourceTe) {
        try {
            NBTTagCompound copyNbt = sourceTe.writeToNBT(new NBTTagCompound());
            TileEntityLittleTiles copyTe = new TileEntityLittleTiles();
            copyTe.readFromNBT(copyNbt);
            return copyTe;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean intersects(com.creativemd.littletiles.common.tile.parent.IParentTileList parent, LittleGridContext sourceContext, LittleGridContext workingContext,
            List<LittleBox> workingBoxes) {
        for (LittleTile tile : parent) {
            LittleTile workingTile = tile;
            if (workingContext != sourceContext) {
                workingTile = tile.copy();
                if (workingTile == null)
                    continue;
                workingTile.convertTo(sourceContext, workingContext);
            }

            for (int i = 0; i < workingBoxes.size(); i++)
                if (workingTile.intersectsWith(workingBoxes.get(i)))
                    return true;
        }
        return false;
    }

    private static void appendSpecialPreviews(LittlePreviews combined, BlockPos globalMin, BlockPos sourcePos, List<LittleBox> selectedBoxes, LittleGridContext selectionContext,
            LittlePreviews specialPreviews) {
        List<LittleBox> convertedBoxes = convertBoxes(selectedBoxes, selectionContext, specialPreviews.getContext());
        for (LittlePreview preview : specialPreviews.allPreviews()) {
            List<LittleBox> cutout = new ArrayList<>();
            preview.box.cutOut(convertedBoxes, cutout, null);
            if (cutout.isEmpty())
                continue;

            for (LittleBox cutBox : cutout)
                addPreviewPiece(combined, globalMin, sourcePos, preview, cutBox, specialPreviews.getContext());
        }
    }

    private static void appendVanillaPreviews(LittlePreviews combined, BlockPos globalMin, BlockPos sourcePos, List<LittleBox> selectedBoxes, IBlockState state,
            LittleGridContext context) {
        LittleTile tile = new LittleTile(state.getBlock(), state.getBlock().getMetaFromState(state));
        tile.setBox(new LittleBox(0, 0, 0, context.size, context.size, context.size));
        LittlePreview preview = tile.getPreviewTile();
        for (LittleBox selectedBox : selectedBoxes)
            addPreviewPiece(combined, globalMin, sourcePos, preview, selectedBox, context);
    }

    private static void addPreviewPiece(LittlePreviews combined, BlockPos globalMin, BlockPos sourcePos, LittlePreview sourcePreview, LittleBox selectedBox, LittleGridContext context) {
        LittlePreview preview = sourcePreview.copy();
        preview.setBox(selectedBox.copy());
        preview.box.add(new LittleVec(context, sourcePos.subtract(globalMin)));
        preview = LittleVecXPreviewFixHelper.convertSlicePreview(preview);
        combined.addPreview(null, preview, context);
    }

    private static void addTilePreviewPiece(LittlePreviews combined, BlockPos globalMin, BlockPos sourcePos, LittleTile tile, LittleGridContext context) {
        LittlePreview preview = tile.getPreviewTile();
        preview.box.add(new LittleVec(context, sourcePos.subtract(globalMin)));
        preview = LittleVecXPreviewFixHelper.convertSlicePreview(preview);
        combined.addPreview(null, preview, context);
    }

    private static void addRegion(ItemStack stack, IndustrialSelectionRegion region) {
        ensureTag(stack);
        NBTTagList list = stack.getTagCompound().getTagList(KEY_REGIONS, 10);
        list.appendTag(region.writeToNBT(new NBTTagCompound()));
        stack.getTagCompound().setTag(KEY_REGIONS, list);
    }

    @Nullable
    private static PendingSelection getPendingSelection(ItemStack stack) {
        ensureTag(stack);
        if (!stack.getTagCompound().hasKey(KEY_PENDING))
            return null;

        LittleAbsoluteVec position = new LittleAbsoluteVec(KEY_PENDING, stack.getTagCompound());
        EnumFacing facing = EnumFacing.byIndex(stack.getTagCompound().getInteger(KEY_PENDING_FACE));
        return new PendingSelection(position, facing == null ? EnumFacing.UP : facing);
    }

    private static void setPendingSelection(ItemStack stack, LittleAbsoluteVec position, EnumFacing facing) {
        ensureTag(stack);
        position.writeToNBT(KEY_PENDING, stack.getTagCompound());
        stack.getTagCompound().setInteger(KEY_PENDING_FACE, facing.getIndex());
    }

    private static void clearPendingSelection(ItemStack stack) {
        ensureTag(stack);
        stack.getTagCompound().removeTag(KEY_PENDING);
        stack.getTagCompound().removeTag(KEY_PENDING_FACE);
    }

    public static BlockPos getGlobalMinPosition(List<IndustrialSelectionRegion> regions) {
        return getGlobalMin(regions);
    }

    private static BlockPos getGlobalMin(List<IndustrialSelectionRegion> regions) {
        BlockPos globalMin = null;
        for (IndustrialSelectionRegion region : regions) {
            LittleAbsoluteBox box = region.toAbsoluteBox();
            if (box == null)
                continue;

            BlockPos minPos = box.getMinPos();
            if (globalMin == null)
                globalMin = minPos;
            else
                globalMin = new BlockPos(Math.min(globalMin.getX(), minPos.getX()), Math.min(globalMin.getY(), minPos.getY()), Math.min(globalMin.getZ(), minPos.getZ()));
        }
        return globalMin == null ? BlockPos.ORIGIN : globalMin;
    }

    private static void ensureTag(ItemStack stack) {
        if (!stack.hasTagCompound())
            stack.setTagCompound(new NBTTagCompound());
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

    private static boolean isMarked(ShapeSelection selection) {
        try {
            if (shapeSelectionMarkedField == null) {
                shapeSelectionMarkedField = ShapeSelection.class.getDeclaredField("marked");
                shapeSelectionMarkedField.setAccessible(true);
            }
            return shapeSelectionMarkedField.getBoolean(selection);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static final class PendingSelection {
        private final LittleAbsoluteVec position;
        private final EnumFacing facing;

        private PendingSelection(LittleAbsoluteVec position, EnumFacing facing) {
            this.position = position;
            this.facing = facing;
        }
    }

}
