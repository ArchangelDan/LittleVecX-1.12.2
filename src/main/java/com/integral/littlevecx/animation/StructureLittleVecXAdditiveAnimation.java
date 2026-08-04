package com.integral.littlevecx.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.math.vec.IVecOrigin;
import com.creativemd.creativecore.common.utils.type.PairList;
import com.creativemd.creativecore.common.utils.type.UUIDSupplier;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDoorSettings.GuiDoorSettingsButton;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.action.LittleActionException.LittleActionExceptionHidden;
import com.creativemd.littletiles.common.action.block.LittleActionActivated;
import com.creativemd.littletiles.common.entity.DoorController;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.packet.LittleAnimationControllerPacket;
import com.creativemd.littletiles.common.packet.LittleAnimationDataPacket;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.animation.AnimationGuiHandler;
import com.creativemd.littletiles.common.structure.animation.AnimationTimeline;
import com.creativemd.littletiles.common.structure.animation.ValueTimeline;
import com.creativemd.littletiles.common.structure.registry.LittleStructureGuiParser;
import com.creativemd.littletiles.common.structure.registry.LittleStructureRegistry;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType;
import com.creativemd.littletiles.common.structure.signal.output.InternalSignalOutput;
import com.creativemd.littletiles.common.structure.type.door.LittleDoor;
import com.creativemd.littletiles.common.structure.type.door.LittleDoor.DoorActivator;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.parent.IStructureTileList;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.tile.preview.LittleAbsolutePreviews;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.creativecore.common.world.SubWorld;
import com.integral.littlevecx.client.gui.GuiLittleVecXAnimationLayersButton;
import com.integral.littlevecx.client.gui.GuiLittleVecXAdditiveSettingsButton;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class StructureLittleVecXAdditiveAnimation extends StructureLittleVecXMultiAnimation {

    private static final Logger LOGGER = LogManager.getLogger("LittleVecX");
    private static final String ACCUMULATED_POSE_TAG = "littlevecxAccumulatedPose";
    private static final String PENDING_ACCUMULATED_POSE_TAG = "littlevecxPendingAccumulatedPose";
    private static final String BLOCK_MOVEMENT_COLLISION_TAG = "littlevecxAdditiveBlockMovementCollision";
    private static final double COLLISION_EPSILON = 1.0E-7D;

    @Nullable
    public LittleVecXAnimationLayer accumulatedPoseLayer;
    /** Stops a transition before its tiles would pass through solid world geometry. */
    public boolean blockMovementCollision = false;
    @Nullable
    private transient LittleVecXAnimationLayer pendingAccumulatedPoseLayer;
    private transient int pendingSignalLayerIndex = -1;
    private transient boolean suppressStateOutputCallback = false;
    private transient int queuedLayerIndex = -1;
    @Nullable
    private transient DoorActivator queuedActivator = null;
    @Nullable
    private transient EntityPlayer queuedPlayer = null;
    private transient boolean drainingQueuedActivation = false;

    public StructureLittleVecXAdditiveAnimation(LittleStructureType type, IStructureTileList mainBlock) {
        super(type, mainBlock);
    }

    @Override
    protected void loadFromNBTExtra(NBTTagCompound nbt) {
        super.loadFromNBTExtra(nbt);
        accumulatedPoseLayer = nbt.hasKey(ACCUMULATED_POSE_TAG, 10)
                ? LittleVecXAnimationLayer.readFromNBT(nbt.getCompoundTag(ACCUMULATED_POSE_TAG))
                : null;
        pendingAccumulatedPoseLayer = nbt.hasKey(PENDING_ACCUMULATED_POSE_TAG, 10)
                ? LittleVecXAnimationLayer.readFromNBT(nbt.getCompoundTag(PENDING_ACCUMULATED_POSE_TAG))
                : null;
        blockMovementCollision = nbt.getBoolean(BLOCK_MOVEMENT_COLLISION_TAG);
        pendingSignalLayerIndex = -1;
        queuedLayerIndex = -1;
        queuedActivator = null;
        queuedPlayer = null;
        drainingQueuedActivation = false;
        if (accumulatedPoseLayer != null) {
            opened = !isZeroPose(accumulatedPoseLayer);
            currentLayerIndex = -1;
            refreshCurrentLayerFields();
        }
    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {
        super.writeToNBTExtra(nbt);
        if (accumulatedPoseLayer != null && !isZeroPose(accumulatedPoseLayer))
            nbt.setTag(ACCUMULATED_POSE_TAG, accumulatedPoseLayer.writeToNBT());
        else
            nbt.removeTag(ACCUMULATED_POSE_TAG);

        if (pendingAccumulatedPoseLayer != null && !isZeroPose(pendingAccumulatedPoseLayer))
            nbt.setTag(PENDING_ACCUMULATED_POSE_TAG, pendingAccumulatedPoseLayer.writeToNBT());
        else
            nbt.removeTag(PENDING_ACCUMULATED_POSE_TAG);

        if (blockMovementCollision)
            nbt.setBoolean(BLOCK_MOVEMENT_COLLISION_TAG, true);
        else
            nbt.removeTag(BLOCK_MOVEMENT_COLLISION_TAG);
    }

    @Override
    public EntityAnimation activate(DoorActivator activator, EntityPlayer player, UUID uuid) throws LittleActionException {
        int requestedLayer = resolveRequestedLayerIndex(activator);
        if (!isValidLayerIndex(requestedLayer))
            return null;

        if ((activator == DoorActivator.RIGHTCLICK || activator == DoorActivator.COMMAND) && disableRightClick)
            throw new LittleActionExceptionHidden("Door is locked!");

        load();

        if (isInMotion()) {
            queueActivationRequest(requestedLayer, activator, player);
            return getAnimation();
        }

        if (uuid == null) {
            if (this.isAnimated() && this.getAnimation() != null)
                uuid = this.getAnimation().getUniqueID();
            else
                uuid = UUID.randomUUID();
        }

        if (getWorld().isRemote) {
            sendActivationToServer(activator, player, uuid);
            return null;
        }

        if (!canOpenDoor(player)) {
            throw new LittleActionException("Cannot open door");
        }

        stayAnimated = true;
        return activateAccumulatedLayer(requestedLayer, player, uuid);
    }

    @Override
    public boolean onBlockActivated(World world, LittleTile tile, BlockPos pos, EntityPlayer player, EnumHand hand, ItemStack stack,
            EnumFacing facing, float hitX, float hitY, float hitZ, LittleActionActivated action) throws LittleActionException {
        DoorActivator activator = player != null && player.isSneaking() && findFirstLayerIndex(LittleVecXAnimationTriggerMode.SHIFT_RIGHT_CLICK) >= 0
                ? DoorActivator.COMMAND
                : DoorActivator.RIGHTCLICK;
        activate(activator, player, null);
        action.preventInteraction = true;
        return true;
    }

    @Override
    public void performInternalOutputChange(InternalSignalOutput output) {
        String identifier = output.component.identifier;
        // The additive recipe is addressed exclusively by animation_N signals.
        // Its state port is only a mirrored status value, never an activator.
        if ("state".equals(identifier))
            return;
        boolean[] state = output.getState();
        if (state == null || state.length == 0 || !state[0] || isInMotion())
            return;

        if ("state".equals(identifier)) {
            int defaultLayer = getDefaultLayerIndex();
            if (!isValidLayerIndex(defaultLayer))
                return;
            pendingSignalLayerIndex = defaultLayer;
        } else {
            int layerIndex = getSignalLayerIndex(identifier);
            if (!isValidLayerIndex(layerIndex))
                return;
            pendingSignalLayerIndex = layerIndex;
        }

        try {
            activate(DoorActivator.SIGNAL, null, null);
        } catch (LittleActionException ignored) {
            pendingSignalLayerIndex = -1;
        }
    }

    @Override
    protected int resolveRequestedLayerIndex(DoorActivator activator) {
        if (activator == DoorActivator.SIGNAL) {
            int layerIndex = pendingSignalLayerIndex;
            pendingSignalLayerIndex = -1;
            return layerIndex;
        }
        return super.resolveRequestedLayerIndex(activator);
    }

    @Override
    public void completeAnimation() {
        if (activateParent && getParent() != null) {
            try {
                LittleStructure parent = getParent().getStructure();
                if (parent instanceof LittleDoor)
                    ((LittleDoor) parent).onChildComplete(this, getParent().getChildId());
            } catch (com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException
                    | com.creativemd.littletiles.common.structure.exception.NotYetConnectedException e) {
                com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX additive animation could not notify its parent because the connection is not ready", e);
            }
        }

        if (!mainBlock.isRemoved() && !isClient())
            notifyChange();
    }

    @Override
    public void refreshCurrentLayerFields() {
        if (runtimeLayerOverride != null) {
            applyLayer(runtimeLayerOverride);
            return;
        }
        if (accumulatedPoseLayer != null) {
            applyLayer(accumulatedPoseLayer);
            return;
        }
        super.refreshCurrentLayerFields();
    }

    @Override
    protected void afterFinishAnimation(EntityAnimation animation, @Nullable StructureLittleVecXMultiAnimation targetBase) {
        if (pendingAccumulatedPoseLayer != null)
            accumulatedPoseLayer = copyLayer(pendingAccumulatedPoseLayer);
        pendingAccumulatedPoseLayer = null;
        opened = accumulatedPoseLayer != null && !isZeroPose(accumulatedPoseLayer);
        currentLayerIndex = -1;
        refreshCurrentLayerFields();

        if (targetBase instanceof StructureLittleVecXAdditiveAnimation) {
            StructureLittleVecXAdditiveAnimation target = (StructureLittleVecXAdditiveAnimation) targetBase;
            if (target.pendingAccumulatedPoseLayer != null)
                target.accumulatedPoseLayer = copyLayer(target.pendingAccumulatedPoseLayer);
            else
                target.accumulatedPoseLayer = copyLayer(accumulatedPoseLayer);
            target.pendingAccumulatedPoseLayer = null;
            target.opened = target.accumulatedPoseLayer != null && !isZeroPose(target.accumulatedPoseLayer);
            target.currentLayerIndex = -1;
            target.refreshCurrentLayerFields();
        }

        syncPrimaryStateOutput(opened, targetBase);

        if (!isClient())
            drainQueuedActivation(targetBase);
    }

    protected void syncAccumulatedPoseFrom(@Nullable StructureLittleVecXAdditiveAnimation source) {
        if (source == null || source == this)
            return;

        accumulatedPoseLayer = copyLayer(source.accumulatedPoseLayer);
        pendingAccumulatedPoseLayer = copyLayer(source.pendingAccumulatedPoseLayer);
        opened = accumulatedPoseLayer != null && !isZeroPose(accumulatedPoseLayer);
        currentLayerIndex = -1;
        refreshCurrentLayerFields();
    }

    protected void resetAdditiveRuntimeState() {
        accumulatedPoseLayer = null;
        pendingAccumulatedPoseLayer = null;
        pendingSignalLayerIndex = -1;
        queuedLayerIndex = -1;
        queuedActivator = null;
        queuedPlayer = null;
        drainingQueuedActivation = false;
        runtimeLayerOverride = null;
        currentLayerIndex = -1;
        opened = false;
        refreshCurrentLayerFields();
    }

    @Override
    protected void syncPrimaryStateOutput(boolean openedState, @Nullable StructureLittleVecXMultiAnimation target) {
        suppressStateOutputCallback = true;
        if (target instanceof StructureLittleVecXAdditiveAnimation)
            ((StructureLittleVecXAdditiveAnimation) target).suppressStateOutputCallback = true;
        try {
            super.syncPrimaryStateOutput(openedState, target);
        } finally {
            suppressStateOutputCallback = false;
            if (target instanceof StructureLittleVecXAdditiveAnimation)
                ((StructureLittleVecXAdditiveAnimation) target).suppressStateOutputCallback = false;
        }
    }

    @Override
    protected void prepareRebuiltController(DoorController oldController, DoorController newController, EntityAnimation liveAnimation) {
        // Additive transitions always start from the current accumulated pose.
    }

    @Override
    protected boolean shouldSyncRebuiltAnimationImmediately() {
        return false;
    }

    protected EntityAnimation activateAccumulatedLayer(int requestedLayer, @Nullable EntityPlayer player, UUID uuid) throws LittleActionException {
        if (!isValidLayerIndex(requestedLayer))
            return null;
        return activateAccumulatedLayer(layers.get(requestedLayer), player, uuid);
    }

    protected EntityAnimation activateAccumulatedLayer(LittleVecXAnimationLayer sourceLayer, @Nullable EntityPlayer player, UUID uuid)
            throws LittleActionException {
        LittleVecXAnimationLayer transitionLayer = buildAccumulatedTransitionLayer(accumulatedPoseLayer, sourceLayer);
        ensureBlockMovementPathClear(transitionLayer, player);
        pendingAccumulatedPoseLayer = buildPoseLayerAtEnd(transitionLayer);

        StructureLittleVecXAdditiveAnimation target = getAdditiveTarget();
        if (target != null && target != this)
            target.pendingAccumulatedPoseLayer = copyLayer(pendingAccumulatedPoseLayer);
        EntityAnimation liveAnimation = getLiveAnimation(target);
        if (liveAnimation != null) {
            if (!rebuildPersistentAnimationLayer(-1, transitionLayer, player))
                return liveAnimation;
            liveAnimation = getLiveAnimation(target);
            if (liveAnimation == null || !playAnimatedTransition(liveAnimation))
                return liveAnimation;
            syncPrimaryStateOutput(true, target);
            return liveAnimation;
        }

        runtimeLayerOverride = transitionLayer;
        applyLayer(transitionLayer);
        if (target != null && target != this) {
            target.runtimeLayerOverride = transitionLayer;
            target.applyLayer(transitionLayer);
        }

        currentLayerIndex = -1;
        boolean previousOpened = opened;
        opened = false;
        if (target != null && target != this) {
            target.currentLayerIndex = -1;
            target.opened = false;
        }

        try {
            EntityAnimation result = super.activate(DoorActivator.SIGNAL, player, uuid);
            syncPrimaryStateOutput(true, target);
            return result;
        } finally {
            if (getLiveAnimation(target) == null) {
                opened = previousOpened;
                if (target != null && target != this)
                    target.opened = previousOpened;
                pendingAccumulatedPoseLayer = null;
                if (target != null && target != this)
                    target.pendingAccumulatedPoseLayer = null;
            }
            runtimeLayerOverride = null;
            if (target != null && target != this)
                target.runtimeLayerOverride = null;
        }
    }

    /**
     * The animation registered in LittleTiles as {@code overlay_animation} builds one
     * transition from the current accumulated pose to the next layer. Check that complete
     * transition before it is placed or retargeted, so it cannot enter a solid world block.
     */
    private void ensureBlockMovementPathClear(LittleVecXAnimationLayer transitionLayer, @Nullable EntityPlayer player) throws LittleActionException {
        if (!blockMovementCollision || isClient() || transitionLayer == null)
            return;

        LittleAbsolutePreviews source = getAbsolutePreviewsSameWorldOnly(getPos());
        if (source == null)
            return;

        World structureWorld = getWorld();
        IVecOrigin origin = structureWorld instanceof SubWorld ? ((SubWorld) structureWorld).getOrigin() : null;
        World realWorld = structureWorld instanceof SubWorld ? ((SubWorld) structureWorld).getRealWorld() : structureWorld;
        if (hasWorldCollisionOnTransition(realWorld, source, transitionLayer, origin)) {
            throw new LittleActionException("Overlay animation path is blocked");
        }
    }

    private static boolean hasWorldCollisionOnTransition(World world, LittleAbsolutePreviews source, LittleVecXAnimationLayer layer,
            @Nullable IVecOrigin origin) {
        if (world == null)
            return false;

        Set<BlockPos> sourcePositions = origin == null ? collectSourcePositions(source) : Collections.emptySet();
        LittleGridContext grid = LittleGridContext.get(layer.getSafeOffGrid());
        int duration = Math.max(1, layer.getSafeDuration());
        double startX = grid.toVanillaGrid(timelineValue(layer.offX, 0));
        double startY = grid.toVanillaGrid(timelineValue(layer.offY, 0));
        double startZ = grid.toVanillaGrid(timelineValue(layer.offZ, 0));
        for (int tick = 0; tick <= duration; tick++) {
            double offsetX = grid.toVanillaGrid(timelineValue(layer.offX, tick)) - startX;
            double offsetY = grid.toVanillaGrid(timelineValue(layer.offY, tick)) - startY;
            double offsetZ = grid.toVanillaGrid(timelineValue(layer.offZ, tick)) - startZ;
            if (hasWorldCollisionAt(world, source, sourcePositions, origin, offsetX, offsetY, offsetZ))
                return true;
        }
        return false;
    }

    private static double timelineValue(@Nullable ValueTimeline timeline, int tick) {
        return timeline == null ? 0 : timeline.value(tick);
    }

    private static Set<BlockPos> collectSourcePositions(LittleAbsolutePreviews source) {
        Set<BlockPos> positions = new HashSet<>();
        for (com.creativemd.littletiles.common.tile.preview.LittlePreview preview : source) {
            AxisAlignedBB box = preview.box.getBox(source.getContext(), source.pos);
            addCoveredPositions(positions, box);
        }
        return positions;
    }

    private static void addCoveredPositions(Set<BlockPos> positions, AxisAlignedBB box) {
        int minX = (int) Math.floor(box.minX + COLLISION_EPSILON);
        int minY = (int) Math.floor(box.minY + COLLISION_EPSILON);
        int minZ = (int) Math.floor(box.minZ + COLLISION_EPSILON);
        int maxX = (int) Math.floor(box.maxX - COLLISION_EPSILON);
        int maxY = (int) Math.floor(box.maxY - COLLISION_EPSILON);
        int maxZ = (int) Math.floor(box.maxZ - COLLISION_EPSILON);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    positions.add(new BlockPos(x, y, z));
    }

    private static boolean hasWorldCollisionAt(World world, LittleAbsolutePreviews source, Set<BlockPos> sourcePositions,
            @Nullable IVecOrigin origin, double offsetX, double offsetY, double offsetZ) {
        List<AxisAlignedBB> collisions = new ArrayList<>(1);
        for (com.creativemd.littletiles.common.tile.preview.LittlePreview preview : source) {
            AxisAlignedBB movingBox = preview.box.getBox(source.getContext(), source.pos);
            if (origin == null)
                movingBox = movingBox.offset(offsetX, offsetY, offsetZ);
            else {
                AxisAlignedBB projected = origin.getAxisAlignedBox(movingBox);
                javax.vecmath.Vector3d delta = new javax.vecmath.Vector3d(offsetX, offsetY, offsetZ);
                if (origin.getParent() != null)
                    origin.getParent().onlyRotateWithoutCenter(delta);
                movingBox = projected.offset(delta.x, delta.y, delta.z);
            }

            int minX = (int) Math.floor(movingBox.minX + COLLISION_EPSILON);
            int minY = (int) Math.floor(movingBox.minY + COLLISION_EPSILON);
            int minZ = (int) Math.floor(movingBox.minZ + COLLISION_EPSILON);
            int maxX = (int) Math.floor(movingBox.maxX - COLLISION_EPSILON);
            int maxY = (int) Math.floor(movingBox.maxY - COLLISION_EPSILON);
            int maxZ = (int) Math.floor(movingBox.maxZ - COLLISION_EPSILON);
            for (int x = minX; x <= maxX; x++)
                for (int y = minY; y <= maxY; y++)
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (sourcePositions.contains(pos))
                            continue;
                        IBlockState state = world.getBlockState(pos);
                        collisions.clear();
                        state.addCollisionBoxToList(world, pos, movingBox, collisions, null, false);
                        if (!collisions.isEmpty())
                            return true;
                    }
        }
        return false;
    }

    private boolean playAnimatedTransition(@Nullable EntityAnimation animation) {
        if (animation == null || !(animation.controller instanceof DoorController))
            return false;
        DoorController controller = (DoorController) animation.controller;
        if (controller.isChanging())
            return false;
        controller.startTransition(DoorController.openedState);
        animation.updateTickState();
        animation.updateBoundingBox();
        if (!isClient())
            syncStartedAnimatedTransition(animation);
        return true;
    }

    protected void syncStartedAnimatedTransition(EntityAnimation animation) {
        PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationDataPacket(animation), animation, null);
        PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationControllerPacket(animation), animation, null);
    }

    private StructureLittleVecXAdditiveAnimation getAdditiveTarget() {
        StructureLittleVecXMultiAnimation target = getAnimatedStructureTarget();
        if (target instanceof StructureLittleVecXAdditiveAnimation)
            return (StructureLittleVecXAdditiveAnimation) target;
        return this;
    }

    private void queueActivationRequest(int requestedLayer, DoorActivator activator, @Nullable EntityPlayer player) {
        if (!isValidLayerIndex(requestedLayer))
            return;

        queuedLayerIndex = requestedLayer;
        queuedActivator = activator;
        queuedPlayer = player;

        StructureLittleVecXAdditiveAnimation target = getAdditiveTarget();
        if (target != null && target != this) {
            target.queuedLayerIndex = requestedLayer;
            target.queuedActivator = activator;
            target.queuedPlayer = player;
        }
    }

    private void clearQueuedActivation(@Nullable StructureLittleVecXAdditiveAnimation target) {
        queuedLayerIndex = -1;
        queuedActivator = null;
        queuedPlayer = null;
        if (target != null && target != this) {
            target.queuedLayerIndex = -1;
            target.queuedActivator = null;
            target.queuedPlayer = null;
        }
    }

    private void drainQueuedActivation(@Nullable StructureLittleVecXMultiAnimation targetBase) {
        StructureLittleVecXAdditiveAnimation target = targetBase instanceof StructureLittleVecXAdditiveAnimation
                ? (StructureLittleVecXAdditiveAnimation) targetBase
                : getAdditiveTarget();
        if (drainingQueuedActivation || (target != null && target.drainingQueuedActivation))
            return;

        int nextLayer = queuedLayerIndex;
        DoorActivator nextActivator = queuedActivator;
        EntityPlayer nextPlayer = queuedPlayer;

        if (!isValidLayerIndex(nextLayer) && target != null && target != this) {
            nextLayer = target.queuedLayerIndex;
            nextActivator = target.queuedActivator;
            nextPlayer = target.queuedPlayer;
        }

        if (!isValidLayerIndex(nextLayer))
            return;

        clearQueuedActivation(target);
        drainingQueuedActivation = true;
        if (target != null && target != this)
            target.drainingQueuedActivation = true;
        try {
            if (nextActivator == DoorActivator.SIGNAL)
                pendingSignalLayerIndex = nextLayer;
            activateAccumulatedLayer(nextLayer, nextPlayer, UUID.randomUUID());
        } catch (LittleActionException ignored) {
        } finally {
            drainingQueuedActivation = false;
            if (target != null && target != this)
                target.drainingQueuedActivation = false;
        }
    }

    private static LittleVecXAnimationLayer buildAccumulatedTransitionLayer(@Nullable LittleVecXAnimationLayer basePose, LittleVecXAnimationLayer layer) {
        LittleVecXAnimationLayer transition = new LittleVecXAnimationLayer();
        transition.name = layer.name;
        transition.trigger = LittleVecXAnimationTriggerMode.NONE;
        transition.doorType = layer.doorType;
        transition.duration = layer.getSafeDuration();
        transition.interpolation = layer.interpolation;

        int baseGrid = basePose != null ? basePose.getSafeOffGrid() : layer.getSafeOffGrid();
        int layerGrid = layer.getSafeOffGrid();
        int targetGrid = lcmCapped(baseGrid, layerGrid);
        transition.offGrid = targetGrid;
        transition.axisData = layer.axisData == null ? null : layer.axisData.clone();
        transition.axisLocalData = layer.axisLocalData == null ? null : layer.axisLocalData.clone();
        transition.doorData = layer.doorData == null ? new NBTTagCompound() : layer.doorData.copy();
        transition.events = copyEvents(layer.events);

        transition.rotX = buildAccumulatedTimeline(basePose != null ? basePose.rotX : null, baseGrid, layer.rotX, layerGrid,
                transition.duration, false, targetGrid);
        transition.rotY = buildAccumulatedTimeline(basePose != null ? basePose.rotY : null, baseGrid, layer.rotY, layerGrid,
                transition.duration, false, targetGrid);
        transition.rotZ = buildAccumulatedTimeline(basePose != null ? basePose.rotZ : null, baseGrid, layer.rotZ, layerGrid,
                transition.duration, false, targetGrid);
        transition.offX = buildAccumulatedTimeline(basePose != null ? basePose.offX : null, baseGrid, layer.offX, layerGrid,
                transition.duration, true, targetGrid);
        transition.offY = buildAccumulatedTimeline(basePose != null ? basePose.offY : null, baseGrid, layer.offY, layerGrid,
                transition.duration, true, targetGrid);
        transition.offZ = buildAccumulatedTimeline(basePose != null ? basePose.offZ : null, baseGrid, layer.offZ, layerGrid,
                transition.duration, true, targetGrid);
        return transition;
    }

    private static LittleVecXAnimationLayer buildPoseLayerAtEnd(LittleVecXAnimationLayer layer) {
        LittleVecXAnimationLayer pose = new LittleVecXAnimationLayer();
        pose.name = layer.name;
        pose.trigger = LittleVecXAnimationTriggerMode.NONE;
        pose.doorType = layer.doorType;
        pose.duration = 1;
        pose.interpolation = layer.interpolation;
        pose.offGrid = layer.getSafeOffGrid();
        pose.axisData = layer.axisData == null ? null : layer.axisData.clone();
        pose.axisLocalData = layer.axisLocalData == null ? null : layer.axisLocalData.clone();
        pose.doorData = layer.doorData == null ? new NBTTagCompound() : layer.doorData.copy();
        pose.events = new ArrayList<>();
        pose.rotX = constantTimeline(1, finalValue(layer.rotX));
        pose.rotY = constantTimeline(1, finalValue(layer.rotY));
        pose.rotZ = constantTimeline(1, finalValue(layer.rotZ));
        pose.offX = constantTimeline(1, finalValue(layer.offX));
        pose.offY = constantTimeline(1, finalValue(layer.offY));
        pose.offZ = constantTimeline(1, finalValue(layer.offZ));
        return pose;
    }

    @Nullable
    protected static LittleVecXAnimationLayer copyLayer(@Nullable LittleVecXAnimationLayer source) {
        return source == null ? null : LittleVecXAnimationLayer.readFromNBT(source.writeToNBT());
    }

    private static boolean isZeroPose(@Nullable LittleVecXAnimationLayer layer) {
        if (layer == null)
            return true;
        return Math.abs(finalValue(layer.rotX)) < 1.0E-9
                && Math.abs(finalValue(layer.rotY)) < 1.0E-9
                && Math.abs(finalValue(layer.rotZ)) < 1.0E-9
                && Math.abs(finalValue(layer.offX)) < 1.0E-9
                && Math.abs(finalValue(layer.offY)) < 1.0E-9
                && Math.abs(finalValue(layer.offZ)) < 1.0E-9;
    }

    @Nullable
    private static ValueTimeline buildAccumulatedTimeline(@Nullable ValueTimeline baseTimeline, int baseGrid,
            @Nullable ValueTimeline layerTimeline, int layerGrid, int duration, boolean scaleToGrid, int targetGrid) {
        double baseValue = finalValue(baseTimeline);
        if (scaleToGrid && baseGrid > 0)
            baseValue = baseValue * targetGrid / (double) baseGrid;

        if (layerTimeline == null) {
            if (Math.abs(baseValue) < 1.0E-9)
                return null;
            return constantTimeline(duration, baseValue);
        }

        PairList<Integer, Double> sourcePoints = layerTimeline.getPointsCopy();
        PairList<Integer, Double> points = new PairList<>();
        for (int i = 0; i < sourcePoints.size(); i++) {
            double value = sourcePoints.get(i).value;
            if (scaleToGrid && layerGrid > 0)
                value = value * targetGrid / (double) layerGrid;
            points.add(sourcePoints.get(i).key, baseValue + value);
        }

        if (points.isEmpty())
            return Math.abs(baseValue) < 1.0E-9 ? null : constantTimeline(duration, baseValue);
        if (points.getFirst().key != 0 || Math.abs(points.getFirst().value - baseValue) > 1.0E-9)
            points.add(0, baseValue);
        if (points.getLast().key != duration)
            points.add(duration, points.getLast().value);
        return ValueTimeline.create(ValueTimeline.getId(layerTimeline.getClass()), points);
    }

    private static double finalValue(@Nullable ValueTimeline timeline) {
        if (timeline == null)
            return 0.0D;
        PairList<Integer, Double> points = timeline.getPointsCopy();
        if (points == null || points.isEmpty())
            return 0.0D;
        return points.getLast().value;
    }

    @Nullable
    private static ValueTimeline constantTimeline(int duration, double value) {
        if (Math.abs(value) < 1.0E-9)
            return null;
        PairList<Integer, Double> points = new PairList<>();
        points.add(0, value);
        points.add(duration, value);
        return ValueTimeline.create(0, points);
    }

    private static int lcmCapped(int a, int b) {
        if (a <= 0)
            return b <= 0 ? LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID : b;
        if (b <= 0)
            return a;
        int gcd = gcd(a, b);
        long value = (long) a / gcd * (long) b;
        if (value > 4096L)
            return Math.max(a, b);
        return (int) value;
    }

    private static int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            int tmp = a % b;
            a = b;
            b = tmp;
        }
        return a == 0 ? 1 : a;
    }

    private static List<com.creativemd.littletiles.common.structure.animation.event.AnimationEvent> copyEvents(
            List<com.creativemd.littletiles.common.structure.animation.event.AnimationEvent> source) {
        List<com.creativemd.littletiles.common.structure.animation.event.AnimationEvent> copy = new ArrayList<>();
        if (source == null)
            return copy;

        for (com.creativemd.littletiles.common.structure.animation.event.AnimationEvent event : source) {
            com.creativemd.littletiles.common.structure.animation.event.AnimationEvent cloned =
                    com.creativemd.littletiles.common.structure.animation.event.AnimationEvent.loadFromNBT(
                            event.writeToNBT(new NBTTagCompound()));
            if (cloned != null)
                copy.add(cloned);
        }
        return copy;
    }

    public static class StructureLittleVecXAdditiveAnimationParser extends LittleStructureGuiParser {

        public StructureLittleVecXAdditiveAnimationParser(GuiParent parent, AnimationGuiHandler handler) {
            super(parent, handler);
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected void createControls(LittlePreviews previews, @Nullable LittleStructure structure) {
            StructureLittleVecXAdditiveAnimation animation = structure instanceof StructureLittleVecXAdditiveAnimation
                    ? (StructureLittleVecXAdditiveAnimation) structure
                    : null;

            boolean stayAnimated = true;
            boolean enableRightClick = animation == null || !animation.disableRightClick;
            boolean noClip = animation != null && animation.noClip;
            boolean playPlaceSounds = animation == null || animation.playPlaceSounds;
            boolean blockMovementCollision = animation != null && animation.blockMovementCollision;

            parent.controls.add(new GuiLittleVecXAnimationLayersButton("animation_layers", 0, 0, animation != null ? animation.layers : null, previews));
            parent.controls.add(new GuiLittleVecXAdditiveSettingsButton("settings", 130, 93, stayAnimated, enableRightClick, noClip, playPlaceSounds,
                    blockMovementCollision));

            updateTimeline();
        }

        @CustomEventSubscribe
        @SideOnly(Side.CLIENT)
        public void onChanged(GuiControlChangedEvent event) {
            if (event.source.is("animation_layers"))
                updateTimeline();
        }

        @SideOnly(Side.CLIENT)
        private void updateTimeline() {
            GuiLittleVecXAnimationLayersButton layersButton = (GuiLittleVecXAnimationLayersButton) parent.get("animation_layers");
            List<LittleVecXAnimationLayer> layers = layersButton != null ? layersButton.getLayersCopy() : new ArrayList<>();

            LittleVecXAnimationLayerCompiler.CompiledAnimation compiled = LittleVecXAnimationLayerCompiler.compile(layers);
            AnimationTimeline previewTimeline = compiled.timeline;
            if (previewTimeline == null)
                previewTimeline = new AnimationTimeline(1, new PairList<>());

            handler.setTimeline(previewTimeline, null);
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructure parseStructure(LittlePreviews previews) {
            StructureLittleVecXAdditiveAnimation animation = createStructure(StructureLittleVecXAdditiveAnimation.class, null);

            GuiLittleVecXAdditiveSettingsButton settings = (GuiLittleVecXAdditiveSettingsButton) parent.get("settings");
            animation.stayAnimated = true;
            animation.disableRightClick = !settings.disableRightClick;
            animation.noClip = settings.noClip;
            animation.playPlaceSounds = settings.playPlaceSounds;
            animation.blockMovementCollision = settings.blockMovementCollision;

            GuiLittleVecXAnimationLayersButton layersButton = (GuiLittleVecXAnimationLayersButton) parent.get("animation_layers");
            animation.layers = layersButton != null ? layersButton.getLayersCopy() : new ArrayList<>();
            animation.events = new ArrayList<>();
            animation.interpolation = 0;
            animation.currentLayerIndex = -1;
            animation.accumulatedPoseLayer = null;
            animation.refreshCurrentLayerFields();
            if (animation.axisCenter == null)
                animation.axisCenter = new com.creativemd.littletiles.common.structure.relative.StructureRelative(defaultAxis(previews));
            return animation;
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructureType getStructureType() {
            return LittleStructureRegistry.getStructureType(StructureLittleVecXAdditiveAnimation.class);
        }

        @SideOnly(Side.CLIENT)
        private static int[] defaultAxis(LittlePreviews previews) {
            LittleBox box = previews.getSurroundingBox();
            int minX = axisMin(box.minX, box.maxX);
            int minY = axisMin(box.minY, box.maxY);
            int minZ = axisMin(box.minZ, box.maxZ);
            int maxX = axisMax(box.minX, box.maxX);
            int maxY = axisMax(box.minY, box.maxY);
            int maxZ = axisMax(box.minZ, box.maxZ);
            return new int[] { minX, minY, minZ, maxX, maxY, maxZ, previews.getContext().size };
        }

        private static int axisMin(int min, int max) {
            int center = (min + max) / 2;
            return center;
        }

        private static int axisMax(int min, int max) {
            int center = (min + max) / 2;
            return center + 1;
        }
    }
}
