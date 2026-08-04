package com.integral.littlevecx.action;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import com.creativemd.creativecore.common.utils.mc.ColorUtils;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.action.block.LittleActionBoxes;
import com.creativemd.littletiles.common.tile.math.box.LittleAbsoluteBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxes;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.selection.selector.TileSelector;
import com.integral.littlevecx.selection.IndustrialSelectionMode;
import com.integral.littlevecx.selection.IndustrialSelectionRegion;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class LittleVecXActionQueuedScrewdriverReplace extends LittleActionBoxes {

    private TileSelector selector;
    private Block block = Blocks.AIR;
    private int meta;
    private boolean preserveColor = true;
    private boolean applyReplacementColor;
    private int replacementColor = ColorUtils.WHITE;
    private boolean queuedClientAction;
    private UUID queuedJobId;
    private boolean regionSelection;
    private List<IndustrialSelectionRegion> selectionRegions;

    public LittleVecXActionQueuedScrewdriverReplace(LittleBoxes boxes, TileSelector selector, Block block, int meta, boolean preserveColor,
            boolean applyReplacementColor, int replacementColor) {
        super(boxes);
        this.selector = selector;
        this.block = block;
        this.meta = meta;
        this.preserveColor = preserveColor;
        this.applyReplacementColor = applyReplacementColor;
        this.replacementColor = replacementColor;
    }

    public LittleVecXActionQueuedScrewdriverReplace(List<IndustrialSelectionRegion> regions, TileSelector selector, Block block, int meta, boolean preserveColor,
            boolean applyReplacementColor, int replacementColor) {
        this(IndustrialSelectionMode.buildBoxes(copyValidRegions(regions)), selector, block, meta, preserveColor, applyReplacementColor, replacementColor);
        this.regionSelection = true;
        this.selectionRegions = copyValidRegions(regions);
    }

    public LittleVecXActionQueuedScrewdriverReplace() {
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        buf.writeBoolean(regionSelection);
        if (regionSelection)
            writeRegions(selectionRegions, buf);
        else
            super.writeBytes(buf);
        writeSelector(selector, buf);
        ResourceLocation blockName = Block.REGISTRY.getNameForObject(block);
        writeString(buf, blockName == null ? "" : blockName.toString());
        buf.writeInt(meta);
        buf.writeBoolean(preserveColor);
        buf.writeBoolean(applyReplacementColor);
        buf.writeInt(replacementColor);
        buf.writeBoolean(queuedJobId != null);
        if (queuedJobId != null) {
            buf.writeLong(queuedJobId.getMostSignificantBits());
            buf.writeLong(queuedJobId.getLeastSignificantBits());
        }
    }

    @Override
    public void readBytes(ByteBuf buf) {
        regionSelection = buf.readBoolean();
        if (regionSelection) {
            selectionRegions = readRegions(buf);
            boxes = IndustrialSelectionMode.buildBoxes(selectionRegions);
        } else {
            super.readBytes(buf);
        }
        selector = readSelector(buf);
        Block readBlock = Block.getBlockFromName(readString(buf));
        block = readBlock == null ? Blocks.AIR : readBlock;
        meta = buf.readInt();
        preserveColor = buf.readBoolean();
        applyReplacementColor = buf.readBoolean();
        replacementColor = buf.readInt();
        if (buf.readBoolean())
            queuedJobId = new UUID(buf.readLong(), buf.readLong());
        else
            queuedJobId = null;
    }

    @Override
    public void action(World world, EntityPlayer player, BlockPos pos, IBlockState state, List<LittleBox> boxes, LittleGridContext context) throws LittleActionException {
    }

    @Override
    protected boolean action(EntityPlayer player) throws LittleActionException {
        if ((boxes == null || boxes.isEmpty()) && regionSelection)
            boxes = IndustrialSelectionMode.buildBoxes(selectionRegions);
        if (boxes == null || boxes.isEmpty())
            return false;

        ensureQueuedJobId();
        if (player.world.isRemote) {
            queuedClientAction = true;
            player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.screwdriver.gradual"), true);
            return true;
        }

        if (player instanceof EntityPlayerMP) {
            LittleVecXQueuedActionHandler.enqueueScrewdriverSelectionReplacement(this, (EntityPlayerMP) player);
            return true;
        }
        return false;
    }

    private void ensureQueuedJobId() {
        if (queuedJobId == null)
            queuedJobId = UUID.randomUUID();
    }

    UUID getQueuedJobId() {
        ensureQueuedJobId();
        return queuedJobId;
    }

    TileSelector getSelector() {
        return selector;
    }

    Block getBlock() {
        return block;
    }

    int getMeta() {
        return meta;
    }

    boolean shouldPreserveColor() {
        return preserveColor;
    }

    boolean shouldApplyReplacementColor() {
        return applyReplacementColor;
    }

    int getReplacementColor() {
        return replacementColor;
    }

    @Override
    public boolean canBeReverted() {
        return true;
    }

    @Override
    public LittleAction revert(EntityPlayer player) {
        if (queuedClientAction || queuedJobId != null)
            return new LittleVecXActionRestoreScrewdriverSnapshot(queuedJobId, false);
        return null;
    }

    @Override
    public LittleAction flip(Axis axis, LittleAbsoluteBox box) {
        LittleVecXActionQueuedScrewdriverReplace action = new LittleVecXActionQueuedScrewdriverReplace();
        action.selector = selector;
        action.block = block;
        action.meta = meta;
        action.preserveColor = preserveColor;
        action.applyReplacementColor = applyReplacementColor;
        action.replacementColor = replacementColor;
        action.queuedJobId = queuedJobId;
        action.regionSelection = regionSelection;
        action.selectionRegions = copyValidRegions(selectionRegions);
        if (action.regionSelection)
            action.boxes = IndustrialSelectionMode.buildBoxes(action.selectionRegions);
        return assignFlip(action, axis, box);
    }

    private static void writeRegions(List<IndustrialSelectionRegion> regions, ByteBuf buf) {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (IndustrialSelectionRegion region : copyValidRegions(regions))
            list.appendTag(region.writeToNBT(new NBTTagCompound()));
        root.setTag("regions", list);
        writeNBT(buf, root);
    }

    private static List<IndustrialSelectionRegion> readRegions(ByteBuf buf) {
        NBTTagCompound root = readNBT(buf);
        List<IndustrialSelectionRegion> regions = new ArrayList<>();
        NBTTagList list = root.getTagList("regions", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            IndustrialSelectionRegion region = IndustrialSelectionRegion.readFromNBT(list.getCompoundTagAt(i));
            if (region != null && region.isValid())
                regions.add(region);
        }
        return regions;
    }

    private static List<IndustrialSelectionRegion> copyValidRegions(List<IndustrialSelectionRegion> regions) {
        List<IndustrialSelectionRegion> copy = new ArrayList<>();
        if (regions == null)
            return copy;
        for (IndustrialSelectionRegion region : regions)
            if (region != null && region.isValid())
                copy.add(new IndustrialSelectionRegion(region.first, region.second, region.facing));
        return copy;
    }
}
