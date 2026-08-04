package com.integral.littlevecx.action;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.Map.Entry;

import com.creativemd.creativecore.common.utils.type.HashMapList;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxes;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxesNoOverlap;
import com.integral.littlevecx.LittleVecXConfig;
import com.integral.littlevecx.network.PacketLittleVecXScrewdriverProgress;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;

public class LittleVecXQueuedActionHandler {

    public static final LittleVecXQueuedActionHandler INSTANCE = new LittleVecXQueuedActionHandler();

    private static final Queue<QueuedScrewdriverReplacement> REPLACEMENTS = new ArrayDeque<>();
    private static final Queue<QueuedScrewdriverSelectionReplacement> SELECTION_REPLACEMENTS = new ArrayDeque<>();
    private static final int MAX_SNAPSHOTS = 8;
    private static final LinkedHashMap<UUID, ScrewdriverSnapshots> SCREWDRIVER_SNAPSHOTS = new LinkedHashMap<UUID, ScrewdriverSnapshots>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, ScrewdriverSnapshots> eldest) {
            return size() > MAX_SNAPSHOTS;
        }
    };

    private LittleVecXQueuedActionHandler() {
    }

    public static void enqueueScrewdriverReplacement(LittleVecXActionReplaceBlockOnly template, EntityPlayerMP player,
            HashMapList<BlockPos, LittleBox> boxesMap) {
        captureScrewdriverSnapshot(template.getQueuedJobId(), player.world, boxesMap, false);
        QueuedScrewdriverReplacement replacement = new QueuedScrewdriverReplacement(template, player, boxesMap);
        if (!replacement.isEmpty()) {
            REPLACEMENTS.add(replacement);
            replacement.sendProgress(player, false);
            player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.screwdriver.queue", replacement.batchCount()), true);
        }
    }

    public static void enqueueScrewdriverSelectionReplacement(LittleVecXActionQueuedScrewdriverReplace template, EntityPlayerMP player) {
        QueuedScrewdriverSelectionReplacement replacement = new QueuedScrewdriverSelectionReplacement(template, player);
        if (!replacement.isEmpty()) {
            SELECTION_REPLACEMENTS.add(replacement);
            replacement.sendProgress(player, false);
            player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.screwdriver.selection_queue", replacement.batchCount()), true);
        }
    }

    public static boolean restoreScrewdriverSnapshot(UUID jobId, boolean restoreAfter, net.minecraft.entity.player.EntityPlayer player) {
        if (jobId == null)
            return false;

        REPLACEMENTS.removeIf(replacement -> jobId.equals(replacement.jobId));
        SELECTION_REPLACEMENTS.removeIf(replacement -> jobId.equals(replacement.jobId));
        ScrewdriverSnapshots snapshotSet = SCREWDRIVER_SNAPSHOTS.get(jobId);
        List<TileSnapshot> snapshots = snapshotSet == null ? null : (restoreAfter ? snapshotSet.after : snapshotSet.before);
        if (snapshots == null || snapshots.isEmpty()) {
            player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.screwdriver.no_snapshot"), true);
            return false;
        }

        World world = player.world;
        for (TileSnapshot snapshot : snapshots)
            snapshot.restore(world);
        refreshSnapshots(world, player, snapshots);

        if (player instanceof EntityPlayerMP)
            PacketHandler.sendPacketToPlayer(new PacketLittleVecXScrewdriverProgress(jobId, 1, 1, true), (EntityPlayerMP) player);
        player.sendStatusMessage(new TextComponentTranslation(restoreAfter ? "message.littlevecx.screwdriver.redone"
                : "message.littlevecx.screwdriver.undone"), true);
        return true;
    }

    private static void refreshSnapshots(World world, net.minecraft.entity.player.EntityPlayer player, List<TileSnapshot> snapshots) {
        for (TileSnapshot snapshot : snapshots) {
            IBlockState state = world.getBlockState(snapshot.pos);
            world.notifyBlockUpdate(snapshot.pos, state, state, 3);
            world.markBlockRangeForRenderUpdate(snapshot.pos, snapshot.pos);
            LittleAction.sendBlockResetToClient(world, player, snapshot.pos);
        }
    }

    private static void captureScrewdriverSnapshot(UUID jobId, World world, HashMapList<BlockPos, LittleBox> boxesMap, boolean after) {
        if (jobId == null || world == null)
            return;

        List<TileSnapshot> snapshots = new ArrayList<>();
        for (BlockPos pos : boxesMap.keySet())
            snapshots.add(TileSnapshot.capture(world, pos));

        ScrewdriverSnapshots snapshotSet = SCREWDRIVER_SNAPSHOTS.get(jobId);
        if (snapshotSet == null) {
            snapshotSet = new ScrewdriverSnapshots();
            SCREWDRIVER_SNAPSHOTS.put(jobId, snapshotSet);
        }
        if (after)
            snapshotSet.after = snapshots;
        else
            snapshotSet.before = snapshots;
    }

    @SubscribeEvent
    public void onWorldTick(WorldTickEvent event) {
        if (event.phase != Phase.END || event.world == null || event.world.isRemote)
            return;

        int batchBudget = Math.max(1, LittleVecXConfig.screwdriverBatchesPerTick);
        int processedBatches = 0;

        while (processedBatches < batchBudget && !SELECTION_REPLACEMENTS.isEmpty()) {
            QueuedScrewdriverSelectionReplacement replacement = SELECTION_REPLACEMENTS.peek();
            if (replacement != null && replacement.dimension == event.world.provider.getDimension() && replacement.tick(event.world)) {
                SELECTION_REPLACEMENTS.poll();
                EntityPlayerMP player = replacement.getPlayer(event.world);
                if (player != null) {
                    event.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM, SoundCategory.BLOCKS, 1, 1);
                    player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.screwdriver.finished"), true);
                }
            }
            processedBatches++;
            if (replacement != null && replacement.dimension != event.world.provider.getDimension())
                return;
        }

        if (processedBatches >= batchBudget || !SELECTION_REPLACEMENTS.isEmpty())
            return;

        while (processedBatches < batchBudget && !REPLACEMENTS.isEmpty()) {
            QueuedScrewdriverReplacement replacement = REPLACEMENTS.peek();
            if (replacement == null)
                return;

            if (replacement.dimension != event.world.provider.getDimension())
                return;

            if (replacement.tick(event.world)) {
                REPLACEMENTS.poll();
                EntityPlayerMP player = replacement.getPlayer(event.world);
                if (player != null) {
                    captureScrewdriverSnapshot(replacement.jobId, event.world, replacement.sourceBoxesMap, true);
                    event.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM, SoundCategory.BLOCKS, 1, 1);
                    player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.screwdriver.finished"), true);
                }
            }
            processedBatches++;
        }
    }

    private static void appendScrewdriverSnapshot(UUID jobId, World world, BlockPos pos, boolean after) {
        if (jobId == null || world == null || pos == null)
            return;

        ScrewdriverSnapshots snapshotSet = SCREWDRIVER_SNAPSHOTS.get(jobId);
        if (snapshotSet == null) {
            snapshotSet = new ScrewdriverSnapshots();
            SCREWDRIVER_SNAPSHOTS.put(jobId, snapshotSet);
        }

        List<TileSnapshot> snapshots = after ? snapshotSet.after : snapshotSet.before;
        if (snapshots == null) {
            snapshots = new ArrayList<>();
            if (after)
                snapshotSet.after = snapshots;
            else
                snapshotSet.before = snapshots;
        }

        TileSnapshot captured = TileSnapshot.capture(world, pos);
        for (int i = 0; i < snapshots.size(); i++) {
            if (!snapshots.get(i).pos.equals(pos))
                continue;
            if (after)
                snapshots.set(i, captured);
            return;
        }
        snapshots.add(captured);
    }

    private static class QueuedScrewdriverReplacement {

        private final UUID playerId;
        private final UUID jobId;
        private final int dimension;
        private final Queue<LittleBoxes> batches = new ArrayDeque<>();
        private final LittleVecXActionReplaceBlockOnly template;
        private final HashMapList<BlockPos, LittleBox> sourceBoxesMap;
        private int totalBatches;
        private int processedBatches;

        private QueuedScrewdriverReplacement(LittleVecXActionReplaceBlockOnly template, EntityPlayerMP player,
                HashMapList<BlockPos, LittleBox> boxesMap) {
            this.template = template;
            this.playerId = player.getUniqueID();
            this.jobId = template.getQueuedJobId();
            this.dimension = player.world.provider.getDimension();
            this.sourceBoxesMap = copyBoxesMap(boxesMap);
            splitIntoBatches(template.boxes, boxesMap);
            this.totalBatches = batches.size();
        }

        private void splitIntoBatches(LittleBoxes source, HashMapList<BlockPos, LittleBox> boxesMap) {
            int blocksPerBatch = Math.max(1, LittleVecXConfig.screwdriverBlocksPerTick);
            int boxesPerBatch = Math.max(1, LittleVecXConfig.screwdriverBoxesPerTick);
            HashMapList<BlockPos, LittleBox> current = new HashMapList<>();
            int blockCount = 0;
            int boxCount = 0;

            for (Entry<BlockPos, java.util.ArrayList<LittleBox>> entry : boxesMap.entrySet()) {
                if ((blockCount >= blocksPerBatch || boxCount >= boxesPerBatch) && !current.isEmpty()) {
                    batches.add(new LittleBoxesNoOverlap(source.pos, source.context, current));
                    current = new HashMapList<>();
                    blockCount = 0;
                    boxCount = 0;
                }

                blockCount++;
                for (LittleBox box : entry.getValue()) {
                    if (boxCount >= boxesPerBatch && !current.isEmpty()) {
                        batches.add(new LittleBoxesNoOverlap(source.pos, source.context, current));
                        current = new HashMapList<>();
                        blockCount = 1;
                        boxCount = 0;
                    }
                    current.add(entry.getKey(), box.copy());
                    boxCount++;
                }
            }

            if (!current.isEmpty())
                batches.add(new LittleBoxesNoOverlap(source.pos, source.context, current));
        }

        private boolean isEmpty() {
            return batches.isEmpty();
        }

        private int batchCount() {
            return batches.size();
        }

        private boolean tick(World world) {
            EntityPlayerMP player = getPlayer(world);
            if (player == null)
                return true;

            LittleBoxes batch = batches.poll();
            if (batch == null)
                return true;

            try {
                template.createQueuedBatch(batch).runQueuedBatch(player);
            } catch (LittleActionException e) {
                player.sendStatusMessage(new TextComponentString(e.getLocalizedMessage()), true);
                sendProgress(player, true);
                return true;
            }

            processedBatches++;
            boolean finished = batches.isEmpty();
            sendProgress(player, finished);
            return finished;
        }

        private EntityPlayerMP getPlayer(World world) {
            if (world.getMinecraftServer() == null)
                return null;
            return world.getMinecraftServer().getPlayerList().getPlayerByUUID(playerId);
        }

        private void sendProgress(EntityPlayerMP player, boolean done) {
            PacketHandler.sendPacketToPlayer(new PacketLittleVecXScrewdriverProgress(jobId, processedBatches, totalBatches, done), player);
        }
    }

    private static class QueuedScrewdriverSelectionReplacement {

        private final UUID playerId;
        private final UUID jobId;
        private final int dimension;
        private final LittleVecXActionQueuedScrewdriverReplace template;
        private final Queue<HashMapList<BlockPos, LittleBox>> batches = new ArrayDeque<>();
        private int totalBatches;
        private int processedBatches;

        private QueuedScrewdriverSelectionReplacement(LittleVecXActionQueuedScrewdriverReplace template, EntityPlayerMP player) {
            this.template = template;
            this.playerId = player.getUniqueID();
            this.jobId = template.getQueuedJobId();
            this.dimension = player.world.provider.getDimension();
            splitIntoBatches(template.boxes.generateBlockWise());
            this.totalBatches = batches.size();
        }

        private void splitIntoBatches(HashMapList<BlockPos, LittleBox> boxesMap) {
            int blocksPerBatch = Math.max(1, LittleVecXConfig.screwdriverBlocksPerTick);
            int boxesPerBatch = Math.max(1, LittleVecXConfig.screwdriverBoxesPerTick);
            HashMapList<BlockPos, LittleBox> current = new HashMapList<>();
            int blockCount = 0;
            int boxCount = 0;

            for (Entry<BlockPos, java.util.ArrayList<LittleBox>> entry : boxesMap.entrySet()) {
                int entryBoxes = entry.getValue().size();
                if (!current.isEmpty() && (blockCount >= blocksPerBatch || boxCount + entryBoxes > boxesPerBatch)) {
                    batches.add(current);
                    current = new HashMapList<>();
                    blockCount = 0;
                    boxCount = 0;
                }

                blockCount++;
                for (LittleBox box : entry.getValue()) {
                    current.add(entry.getKey(), box.copy());
                    boxCount++;
                }
            }

            if (!current.isEmpty())
                batches.add(current);
        }

        private boolean isEmpty() {
            return batches.isEmpty();
        }

        private int batchCount() {
            return batches.size();
        }

        private boolean tick(World world) {
            EntityPlayerMP player = getPlayer(world);
            if (player == null)
                return true;

            HashMapList<BlockPos, LittleBox> batch = batches.poll();
            if (batch == null)
                return true;

            LittleBoxes rawBatch = new LittleBoxesNoOverlap(template.boxes.pos, template.boxes.context, copyBoxesMap(batch));
            if (!rawBatch.isEmpty()) {
                HashMapList<BlockPos, LittleBox> rawMap = rawBatch.generateBlockWise();
                for (BlockPos pos : rawMap.keySet())
                    appendScrewdriverSnapshot(jobId, world, pos, false);

                LittleVecXActionReplaceBlockOnly replace = new LittleVecXActionReplaceBlockOnly(rawBatch, template.getBlock(), template.getMeta(), false,
                        template.shouldPreserveColor(), template.shouldApplyReplacementColor(), template.getReplacementColor(), template.getSelector());
                try {
                    replace.runQueuedBatch(player);
                } catch (LittleActionException e) {
                    player.sendStatusMessage(new TextComponentString(e.getLocalizedMessage()), true);
                    sendProgress(player, true);
                    return true;
                }

                for (BlockPos pos : rawMap.keySet())
                    appendScrewdriverSnapshot(jobId, world, pos, true);
            }

            processedBatches++;
            boolean finished = batches.isEmpty();
            sendProgress(player, finished);
            return finished;
        }

        private EntityPlayerMP getPlayer(World world) {
            if (world.getMinecraftServer() == null)
                return null;
            return world.getMinecraftServer().getPlayerList().getPlayerByUUID(playerId);
        }

        private void sendProgress(EntityPlayerMP player, boolean done) {
            PacketHandler.sendPacketToPlayer(new PacketLittleVecXScrewdriverProgress(jobId, processedBatches, totalBatches, done), player);
        }
    }

    private static HashMapList<BlockPos, LittleBox> copyBoxesMap(HashMapList<BlockPos, LittleBox> boxesMap) {
        HashMapList<BlockPos, LittleBox> copy = new HashMapList<>();
        for (Entry<BlockPos, java.util.ArrayList<LittleBox>> entry : boxesMap.entrySet())
            for (LittleBox box : entry.getValue())
                copy.add(entry.getKey(), box.copy());
        return copy;
    }

    private static class ScrewdriverSnapshots {

        private List<TileSnapshot> before;
        private List<TileSnapshot> after;
    }

    private static class TileSnapshot {

        private final BlockPos pos;
        private final IBlockState state;
        private final NBTTagCompound tileNbt;

        private TileSnapshot(BlockPos pos, IBlockState state, NBTTagCompound tileNbt) {
            this.pos = pos;
            this.state = state;
            this.tileNbt = tileNbt;
        }

        private static TileSnapshot capture(World world, BlockPos pos) {
            IBlockState state = world.getBlockState(pos);
            TileEntity tileEntity = world.getTileEntity(pos);
            NBTTagCompound tileNbt = null;
            if (tileEntity != null)
                tileNbt = tileEntity.writeToNBT(new NBTTagCompound());
            return new TileSnapshot(pos.toImmutable(), state, tileNbt);
        }

        private void restore(World world) {
            IBlockState oldState = world.getBlockState(pos);
            world.setBlockState(pos, state, 3);

            if (tileNbt != null) {
                TileEntity tileEntity = world.getTileEntity(pos);
                if (tileEntity != null) {
                    NBTTagCompound copy = tileNbt.copy();
                    copy.setInteger("x", pos.getX());
                    copy.setInteger("y", pos.getY());
                    copy.setInteger("z", pos.getZ());
                    tileEntity.readFromNBT(copy);
                    tileEntity.markDirty();
                    if (tileEntity instanceof com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles)
                        ((com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles) tileEntity).updateTiles(false);
                }
            }

            world.notifyBlockUpdate(pos, oldState, state, 3);
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }
}
