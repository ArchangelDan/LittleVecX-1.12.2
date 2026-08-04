package com.integral.littlevecx.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.lang3.mutable.MutableInt;

import com.creativemd.creativecore.common.utils.type.HashMapList;
import com.creativemd.creativecore.common.utils.mc.ColorUtils;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.LittleTilesConfig.NotAllowedToEditException;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionCombined;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.action.block.LittleActionBoxes;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.LittleTileColored;
import com.creativemd.littletiles.common.tile.math.box.LittleAbsoluteBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxReturnedVolume;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxes;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxesSimple;
import com.creativemd.littletiles.common.tile.preview.LittlePreview;
import com.creativemd.littletiles.common.tile.parent.IParentTileList;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.ingredient.LittleIngredients;
import com.creativemd.littletiles.common.util.ingredient.LittleInventory;
import com.creativemd.littletiles.common.util.selection.selector.TileSelector;
import com.integral.littlevecx.LittleVecXConfig;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class LittleVecXActionReplaceBlockOnly extends LittleActionBoxes {

    private Block block = Blocks.AIR;
    private int meta;
    private boolean toVanilla;
    private boolean preserveColor = true;
    private boolean applyReplacementColor;
    private int replacementColor = ColorUtils.WHITE;
    private TileSelector selector;

    private HashMapList<BlockMetaKey, LittleBoxes> revertList;
    private boolean changedAny;
    private boolean doneSomething;
    private boolean queuedClientAction;
    private boolean queuedBatchExecution;
    private int changedTilePieces;
    private UUID queuedJobId;

    public LittleVecXActionReplaceBlockOnly(LittleBoxes boxes, Block block, int meta, boolean toVanilla) {
        this(boxes, block, meta, toVanilla, true);
    }

    public LittleVecXActionReplaceBlockOnly(LittleBoxes boxes, Block block, int meta, boolean toVanilla, boolean preserveColor) {
        super(boxes);
        this.block = block;
        this.meta = meta;
        this.toVanilla = toVanilla;
        this.preserveColor = preserveColor;
    }

    public LittleVecXActionReplaceBlockOnly(LittleBoxes boxes, Block block, int meta, boolean toVanilla, boolean preserveColor, boolean applyReplacementColor, int replacementColor) {
        this(boxes, block, meta, toVanilla, preserveColor);
        this.applyReplacementColor = applyReplacementColor;
        this.replacementColor = replacementColor;
    }

    public LittleVecXActionReplaceBlockOnly(LittleBoxes boxes, Block block, int meta, boolean toVanilla, boolean preserveColor, boolean applyReplacementColor, int replacementColor,
            TileSelector selector) {
        this(boxes, block, meta, toVanilla, preserveColor, applyReplacementColor, replacementColor);
        this.selector = selector;
    }

    public LittleVecXActionReplaceBlockOnly() {}

    @Override
    public void writeBytes(ByteBuf buf) {
        super.writeBytes(buf);
        ResourceLocation blockName = Block.REGISTRY.getNameForObject(block);
        writeString(buf, blockName == null ? "" : blockName.toString());
        buf.writeInt(meta);
        buf.writeBoolean(toVanilla);
        buf.writeBoolean(preserveColor);
        buf.writeBoolean(applyReplacementColor);
        buf.writeInt(replacementColor);
        buf.writeBoolean(selector != null);
        if (selector != null)
            writeSelector(selector, buf);
        buf.writeBoolean(queuedJobId != null);
        if (queuedJobId != null) {
            buf.writeLong(queuedJobId.getMostSignificantBits());
            buf.writeLong(queuedJobId.getLeastSignificantBits());
        }
    }

    @Override
    public void readBytes(ByteBuf buf) {
        super.readBytes(buf);
        Block readBlock = Block.getBlockFromName(readString(buf));
        block = readBlock == null ? Blocks.AIR : readBlock;
        meta = buf.readInt();
        toVanilla = buf.readBoolean();
        preserveColor = buf.readBoolean();
        applyReplacementColor = buf.readBoolean();
        replacementColor = buf.readInt();
        selector = buf.readBoolean() ? readSelector(buf) : null;
        if (buf.readBoolean())
            queuedJobId = new UUID(buf.readLong(), buf.readLong());
        else
            queuedJobId = null;
    }

    private void addRevert(Block oldBlock, int oldMeta, BlockPos pos, LittleGridContext context, List<LittleBox> boxes) {
        if (revertList == null || boxes.isEmpty())
            return;

        LittleBoxes newBoxes = new LittleBoxesSimple(pos, context);
        for (LittleBox box : boxes)
            newBoxes.add(box.copy());
        revertList.add(new BlockMetaKey(oldBlock, oldMeta), newBoxes);
    }

    private boolean isSameBlock(LittleTile tile) {
        if (tile.getBlock() != block || tile.getMeta() != meta)
            return false;
        if (applyReplacementColor)
            return LittleTileColored.getColor(tile) == replacementColor;
        if (!preserveColor)
            return LittleTileColored.getColor(tile) == ColorUtils.WHITE;
        return true;
    }

    private boolean shouldSkipTile(IParentTileList parent, LittleTile tile) {
        return selector != null && !selector.is(parent, tile);
    }

    private LittleTile createReplacementTile(LittleTile source, LittleBox box) {
        LittleTile copy = createReplacementTile(source);
        if (copy != null)
            copy.setBox(box.copy());
        return copy;
    }

    private LittleTile createReplacementTile(LittleTile source) {
        LittleTile copy = source.copy();
        if (copy == null)
            return null;

        copy.setBlock(block, meta);
        if (applyReplacementColor)
            copy = LittleTileColored.setColor(copy, replacementColor);
        else if (!preserveColor)
            copy = removeColorKeepingSpecialTile(copy);
        return copy;
    }

    private LittleTile removeColorKeepingSpecialTile(LittleTile tile) {
        if (!(tile instanceof LittleTileColored))
            return tile;

        if (tile.getClass() != LittleTileColored.class) {
            LittleTileColored coloredTile = (LittleTileColored) tile;
            coloredTile.color = ColorUtils.WHITE;
            coloredTile.updateTranslucent();
            return coloredTile;
        }

        return LittleTileColored.removeColor(tile);
    }

    private void addPieceIngredients(LittleIngredients ingredients, LittleTile tile, LittleBox box, LittleGridContext context) {
        LittlePreview preview = tile.getPreviewTile();
        preview.box = box.copy();
        ingredients.add(getIngredients(preview, box.getPercentVolume(context)));
    }

    private ReplaceIngredients collectIngredients(TileEntityLittleTiles te, List<LittleBox> boxes, LittleGridContext context) {
        ReplaceIngredients ingredients = new ReplaceIngredients();

        for (IParentTileList parent : te.groups()) {
            if (parent.isStructure())
                continue;

            for (LittleTile tile : parent) {
                if (shouldSkipTile(parent, tile))
                    continue;
                if (isSameBlock(tile))
                    continue;

                LittleBox intersecting = getIntersectingBox(tile, boxes);
                if (intersecting == null)
                    continue;

                if (!shouldReplaceWholeTile(tile, intersecting, boxes)) {
                    List<LittleBox> cutout = new ArrayList<>();
                    LittleBoxReturnedVolume returnedVolume = new LittleBoxReturnedVolume();
                    List<LittleBox> newBoxes = tile.cutOut(boxes, cutout, returnedVolume);

                    if (newBoxes == null)
                        continue;

                    List<LittleTile> replacements = new ArrayList<>();
                    for (LittleBox cutBox : cutout) {
                        LittleTile replacement = createReplacementTile(tile, cutBox);
                        if (replacement == null) {
                            replacements.clear();
                            break;
                        }
                        replacements.add(replacement);
                    }

                    if (replacements.size() != cutout.size())
                        continue;

                    for (int i = 0; i < cutout.size(); i++) {
                        LittleBox cutBox = cutout.get(i);
                        addPieceIngredients(ingredients.gained, tile, cutBox, context);
                        addPieceIngredients(ingredients.drained, replacements.get(i), cutBox, context);
                    }

                    if (returnedVolume.has()) {
                        double volume = returnedVolume.getPercentVolume(context);
                        LittleTile replacement = createReplacementTile(tile);
                        if (replacement != null) {
                            ingredients.gained.add(getIngredients(tile.getPreviewTile(), volume));
                            ingredients.drained.add(getIngredients(replacement.getPreviewTile(), volume));
                        }
                    }
                } else {
                    addPieceIngredients(ingredients.gained, tile, tile.getBox(), context);

                    LittleTile replacement = createReplacementTile(tile, tile.getBox());
                    if (replacement != null)
                        addPieceIngredients(ingredients.drained, replacement, tile.getBox(), context);
                }
            }
        }

        return ingredients;
    }

    private LittleBox getIntersectingBox(LittleTile tile, List<LittleBox> boxes) {
        for (LittleBox box : boxes)
            if (tile.intersectsWith(box))
                return box;
        return null;
    }

    private boolean shouldReplaceWholeTile(LittleTile tile, LittleBox intersecting, List<LittleBox> boxes) {
        return tile.equalsBox(intersecting) || isTileBoxCovered(tile.getBox(), boxes);
    }

    private boolean isTileBoxCovered(LittleBox tileBox, List<LittleBox> boxes) {
        for (LittleBox box : boxes)
            if (contains(box, tileBox))
                return true;
        return false;
    }

    private boolean contains(LittleBox outer, LittleBox inner) {
        return outer.minX <= inner.minX && outer.minY <= inner.minY && outer.minZ <= inner.minZ
                && outer.maxX >= inner.maxX && outer.maxY >= inner.maxY && outer.maxZ >= inner.maxZ;
    }

    private void replaceTiles(TileEntityLittleTiles te, List<LittleBox> boxes, LittleGridContext context) {
        doneSomething = false;

        te.updateTiles(x -> {
            for (IParentTileList parent : te.groups()) {
                if (parent.isStructure())
                    continue;

                for (LittleTile tile : parent) {
                    if (shouldSkipTile(parent, tile))
                        continue;
                    if (isSameBlock(tile))
                        continue;

                    LittleBox intersecting = getIntersectingBox(tile, boxes);
                    if (intersecting == null)
                        continue;

                    if (!shouldReplaceWholeTile(tile, intersecting, boxes)) {
                        List<LittleBox> cutout = new ArrayList<>();
                        List<LittleBox> newBoxes = tile.cutOut(boxes, cutout, null);

                        if (newBoxes == null || cutout.isEmpty())
                            continue;

                        List<LittleTile> changedTiles = new ArrayList<>();
                        for (LittleBox cutBox : cutout) {
                            LittleTile changedTile = createReplacementTile(tile, cutBox);
                            if (changedTile == null) {
                                changedTiles.clear();
                                break;
                            }
                            changedTiles.add(changedTile);
                        }

                        if (changedTiles.isEmpty())
                            continue;

                        addRevert(tile.getBlock(), tile.getMeta(), te.getPos(), context, cutout);

                        for (LittleBox box : newBoxes) {
                            LittleTile unchangedTile = tile.copy();
                            if (unchangedTile != null) {
                                unchangedTile.setBox(box);
                                x.get(parent).add(unchangedTile);
                            }
                        }

                        for (LittleTile changedTile : changedTiles)
                            x.get(parent).add(changedTile);

                        x.get(parent).remove(tile);
                        changedTilePieces += changedTiles.size();
                        doneSomething = true;
                        changedAny = true;
                    } else {
                        LittleTile changedTile = createReplacementTile(tile, tile.getBox());
                        if (changedTile == null)
                            continue;

                        List<LittleBox> oldBoxes = new ArrayList<>();
                        oldBoxes.add(tile.getBox());
                        addRevert(tile.getBlock(), tile.getMeta(), te.getPos(), context, oldBoxes);

                        x.get(parent).add(changedTile);
                        x.get(parent).remove(tile);
                        changedTilePieces++;
                        doneSomething = true;
                        changedAny = true;
                    }
                }
            }
        });
    }

    @Override
    public void action(World world, EntityPlayer player, BlockPos pos, IBlockState state, List<LittleBox> boxes, LittleGridContext context) throws LittleActionException {
        fireBlockBreakEvent(world, pos, player);

        TileEntity tileEntity = loadTe(player, world, pos, null, true, 0);
        if (!(tileEntity instanceof TileEntityLittleTiles))
            return;

        TileEntityLittleTiles te = (TileEntityLittleTiles) tileEntity;
        if (context != te.getContext()) {
            if (context.size < te.getContext().size) {
                for (LittleBox box : boxes)
                    box.convertTo(context, te.getContext());
                context = te.getContext();
            } else
                te.convertTo(context);
        }

        if (needIngredients(player)) {
            ReplaceIngredients ingredients = collectIngredients(te, boxes, context);
            LittleInventory inventory = new LittleInventory(player);
            try {
                inventory.startSimulation();
                take(player, inventory, ingredients.drained);
                give(player, inventory, ingredients.gained);
            } finally {
                inventory.stopSimulation();
            }

            take(player, inventory, ingredients.drained);
            give(player, inventory, ingredients.gained);
        }

        int changedBefore = changedTilePieces;
        replaceTiles(te, boxes, context);
        if (shouldCombineAfterReplacement(changedTilePieces - changedBefore))
            te.combineTiles();

        if (toVanilla || !doneSomething)
            te.convertBlockToVanilla();
    }

    private boolean shouldCombineAfterReplacement(int changedInBlock) {
        if (changedInBlock <= 0)
            return false;
        int limit = LittleVecXConfig.screwdriverCombineChangedTileLimit;
        return limit > 0 && changedTilePieces <= limit;
    }

    @Override
    protected boolean action(EntityPlayer player) throws LittleActionException {
        if (boxes.isEmpty())
            return true;

        HashMapList<BlockPos, LittleBox> boxesMap = boxes.generateBlockWise();
        if (shouldQueue(player, boxesMap)) {
            ensureQueuedJobId();
            if (player.world.isRemote) {
                queuedClientAction = true;
                player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.screwdriver.gradual"), true);
            } else if (player instanceof EntityPlayerMP)
                LittleVecXQueuedActionHandler.enqueueScrewdriverReplacement(this, (EntityPlayerMP) player, boxesMap);
            return true;
        }

        revertList = queuedBatchExecution ? null : new HashMapList<>();
        changedAny = false;
        changedTilePieces = 0;
        runBoxes(player, boxesMap, true);
        return changedAny;
    }

    private boolean shouldQueue(EntityPlayer player, HashMapList<BlockPos, LittleBox> boxesMap) {
        return !queuedBatchExecution && (boxesMap.size() > LittleVecXConfig.screwdriverQueuedBlockThreshold
                || countBoxes(boxesMap) > LittleVecXConfig.screwdriverQueuedBoxThreshold);
    }

    private int countBoxes(HashMapList<BlockPos, LittleBox> boxesMap) {
        int count = 0;
        for (Entry<BlockPos, ArrayList<LittleBox>> entry : boxesMap.entrySet())
            count += entry.getValue().size();
        return count;
    }

    UUID getQueuedJobId() {
        ensureQueuedJobId();
        return queuedJobId;
    }

    private void ensureQueuedJobId() {
        if (queuedJobId == null)
            queuedJobId = UUID.randomUUID();
    }

    void runQueuedBatch(EntityPlayerMP player) throws LittleActionException {
        revertList = null;
        changedAny = false;
        changedTilePieces = 0;
        queuedBatchExecution = true;
        runBoxes(player, boxes.generateBlockWise(), false);
    }

    LittleVecXActionReplaceBlockOnly createQueuedBatch(LittleBoxes batch) {
        LittleVecXActionReplaceBlockOnly action = new LittleVecXActionReplaceBlockOnly(batch, block, meta, toVanilla, preserveColor, applyReplacementColor, replacementColor, selector);
        action.queuedBatchExecution = true;
        return action;
    }

    private boolean runBoxes(EntityPlayer player, HashMapList<BlockPos, LittleBox> boxesMap, boolean playSound) throws LittleActionException {
        World world = player.world;

        if (LittleTiles.CONFIG.isEditLimited(player)) {
            if (boxes.getSurroundingBox().getPercentVolume(boxes.context) > LittleTiles.CONFIG.build.get(player).maxEditBlocks)
                throw new NotAllowedToEditException(player);
        }

        MutableInt affectedBlocks = new MutableInt();

        try {
            for (BlockPos pos : boxesMap.keySet()) {
                TileEntityLittleTiles te = LittleAction.loadTe(player, world, pos, null, false, 0);
                if (te != null)
                    continue;
                IBlockState state = world.getBlockState(pos);
                if (state.getMaterial().isReplaceable())
                    continue;
                else if (LittleAction.isBlockValid(state) && LittleAction.canConvertBlock(player, world, pos, state, affectedBlocks.incrementAndGet()))
                    continue;
            }
        } catch (LittleActionException e) {
            for (BlockPos pos : boxesMap.keySet())
                sendBlockResetToClient(world, player, pos);
            throw e;
        }

        for (Iterator<Entry<BlockPos, ArrayList<LittleBox>>> iterator = boxesMap.entrySet().iterator(); iterator.hasNext();) {
            Entry<BlockPos, ArrayList<LittleBox>> entry = iterator.next();
            BlockPos pos = entry.getKey();
            IBlockState state = world.getBlockState(pos);
            if (!isAllowedToInteract(world, player, pos, false, EnumFacing.EAST)) {
                if (!world.isRemote)
                    sendBlockResetToClient(world, player, pos);
                continue;
            }

            action(world, player, pos, state, entry.getValue(), boxes.context);
        }

        actionDone(player, world);

        if (playSound)
            world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM, SoundCategory.BLOCKS, 1, 1);
        return true;
    }

    @Override
    public boolean canBeReverted() {
        return true;
    }

    @Override
    public LittleAction revert(EntityPlayer player) {
        if (queuedClientAction || (queuedJobId != null && revertList == null))
            return new LittleVecXActionRestoreScrewdriverSnapshot(queuedJobId, false);

        List<LittleAction> actions = new ArrayList<>();
        if (revertList != null) {
            for (Entry<BlockMetaKey, ArrayList<LittleBoxes>> entry : revertList.entrySet()) {
                for (LittleBoxes boxes : entry.getValue()) {
                    boxes.convertToSmallest();
                    actions.add(new LittleVecXActionReplaceBlockOnly(boxes, entry.getKey().block, entry.getKey().meta, true));
                }
            }
        }
        return new LittleActionCombined(actions.toArray(new LittleAction[0]));
    }

    @Override
    public LittleAction flip(Axis axis, LittleAbsoluteBox box) {
        LittleVecXActionReplaceBlockOnly action = new LittleVecXActionReplaceBlockOnly();
        action.block = block;
        action.meta = meta;
        action.toVanilla = toVanilla;
        action.preserveColor = preserveColor;
        action.applyReplacementColor = applyReplacementColor;
        action.replacementColor = replacementColor;
        action.selector = selector;
        action.queuedBatchExecution = queuedBatchExecution;
        action.queuedJobId = queuedJobId;
        return assignFlip(action, axis, box);
    }

    private static class ReplaceIngredients {

        private final LittleIngredients gained = new LittleIngredients();
        private final LittleIngredients drained = new LittleIngredients();
    }

    private static class BlockMetaKey {

        private final Block block;
        private final int meta;

        private BlockMetaKey(Block block, int meta) {
            this.block = block;
            this.meta = meta;
        }

        @Override
        public int hashCode() {
            return Block.getIdFromBlock(block) * 31 + meta;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof BlockMetaKey))
                return false;
            BlockMetaKey other = (BlockMetaKey) obj;
            return block == other.block && meta == other.meta;
        }
    }
}
