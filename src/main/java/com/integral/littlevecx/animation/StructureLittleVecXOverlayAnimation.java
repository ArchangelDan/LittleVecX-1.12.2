package com.integral.littlevecx.animation;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.creativecore.common.gui.controls.gui.GuiLabel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiStateButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiTextfield;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.GuiTimeline;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.GuiTimeline.KeyDeselectedEvent;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.GuiTimeline.KeySelectedEvent;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.KeyControl;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.TimelineChannel;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.TimelineChannel.TimelineChannelDouble;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.TimelineChannel.TimelineChannelInteger;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.gui.event.gui.GuiToolTipEvent;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.type.HashMapList;
import com.creativemd.creativecore.common.utils.type.PairList;
import com.creativemd.creativecore.common.utils.type.UUIDSupplier;
import com.creativemd.creativecore.common.utils.math.vec.IVecOrigin;
import com.creativemd.littletiles.client.gui.controls.GuiLTDistance;
import com.creativemd.littletiles.client.gui.controls.GuiTileViewer;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDialogAxis.GuiAxisButton;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDoorEvents.GuiDoorEventsButton;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDoorSettings.GuiDoorSettingsButton;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiSignalEvents.GuiSignalEventsButton;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.action.LittleActionException.LittleActionExceptionHidden;
import com.creativemd.littletiles.common.entity.DoorController;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.packet.LittleAnimationControllerPacket;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.animation.AnimationGuiHandler;
import com.creativemd.littletiles.common.structure.animation.AnimationKey;
import com.creativemd.littletiles.common.structure.animation.AnimationTimeline;
import com.creativemd.littletiles.common.structure.animation.ValueTimeline;
import com.creativemd.littletiles.common.structure.registry.LittleStructureGuiParser;
import com.creativemd.littletiles.common.structure.registry.LittleStructureRegistry;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType;
import com.creativemd.littletiles.common.structure.type.door.LittleDoor;
import com.creativemd.littletiles.common.structure.type.door.LittleAdvancedDoor;
import com.creativemd.littletiles.common.structure.type.door.LittleDoorBase;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.location.LocalStructureLocation;
import com.creativemd.littletiles.common.tile.parent.IStructureTileList;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.creativecore.common.world.SubWorld;
import com.creativemd.littletiles.client.render.world.LittleRenderChunkSuppilier;
import com.creativemd.littletiles.common.packet.LittleAnimationDataPacket;
import com.creativemd.littletiles.common.structure.relative.StructureAbsolute;
import com.creativemd.littletiles.common.tile.preview.LittleAbsolutePreviews;
import com.creativemd.littletiles.common.util.place.Placement;
import com.creativemd.littletiles.common.util.place.PlacementHelper;
import com.creativemd.littletiles.common.util.place.PlacementMode;
import com.creativemd.littletiles.common.util.place.PlacementResult;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;
import com.integral.littlevecx.LittleVecXConfig;
import com.integral.littlevecx.client.gui.GuiLittleVecXOverlaySettingsButton;
import com.integral.littlevecx.client.gui.GuiLittleVecXOverlayEventsButton;
import com.integral.littlevecx.client.gui.GuiLittleVecXOverlaySignalEventsButton;
import com.integral.littlevecx.client.gui.layout.LittleVecXGuiLayout;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class StructureLittleVecXOverlayAnimation extends StructureLittleVecXMultiAnimation {

    private static final Logger LOGGER = LogManager.getLogger("LittleVecX");

    private static final String OVERLAY_STAGE_TAG = "littlevecxOverlayStage";
    private static final String OVERLAY_RESETTING_TAG = "littlevecxOverlayResetting";
    private static final String CHECKPOINTS_TAG = "littlevecxCheckpointTicks";
    private static final String STEP_BACK_MODE_TAG = "littlevecxCheckpointStepBackMode";
    private static final String BLOCK_MOVEMENT_COLLISION_TAG = "littlevecxOverlayBlockMovementCollision";
    private static final double COLLISION_EPSILON = 1.0E-7D;

    public int overlayStage = 0;
    public boolean overlayResetting = false;
    public boolean shiftRightClickStepBackMode = false;
    /** When enabled, a transition is rejected before it can pass through world geometry. */
    public boolean blockMovementCollision = false;
    public List<Integer> checkpointTicks = new ArrayList<>();
    private transient boolean suppressStateOutputCallback = false;
    private transient DoorActivator pendingActivator = DoorActivator.RIGHTCLICK;
    private transient int pendingDirectStage = -1;

    public StructureLittleVecXOverlayAnimation(LittleStructureType type, IStructureTileList mainBlock) {
        super(type, mainBlock);
    }

    @Override
    public DoorController createController(UUIDSupplier supplier, com.creativemd.littletiles.common.util.place.Placement placement, int completeDuration) {
        LittleVecXAnimationLayer baseLayer = getBaseLayer();
        if (baseLayer == null)
            return super.createController(supplier, placement, completeDuration);

        List<Integer> boundaries = getCheckpointBoundaries(baseLayer);
        List<String> stateNames = new ArrayList<>();
        List<com.creativemd.littletiles.common.structure.animation.AnimationState> states = new ArrayList<>();
        List<Entry<String, AnimationTimeline>> transitions = new ArrayList<>();

        for (int i = 0; i < boundaries.size(); i++) {
            stateNames.add(getStageStateName(i));
            states.add(buildStateAtTick(baseLayer, boundaries.get(i)));
        }

        for (int from = 0; from < boundaries.size(); from++) {
            for (int to = 0; to < boundaries.size(); to++) {
                if (from == to)
                    continue;
                AnimationTimeline timeline = buildStageTimeline(baseLayer, boundaries.get(from), boundaries.get(to));
                if (timeline == null)
                    continue;
                transitions.add(new AbstractMap.SimpleEntry<>(getStageStateName(from) + ":" + getStageStateName(to), timeline));
            }
        }

        return new LittleVecXCheckpointController(supplier, stateNames, states, transitions, baseLayer.interpolation,
                Math.max(1, completeDuration));
    }

    @Override
    protected void loadFromNBTExtra(NBTTagCompound nbt) {
        super.loadFromNBTExtra(nbt);
        overlayStage = nbt.hasKey(OVERLAY_STAGE_TAG) ? nbt.getInteger(OVERLAY_STAGE_TAG) : 0;
        overlayResetting = nbt.getBoolean(OVERLAY_RESETTING_TAG);
        shiftRightClickStepBackMode = nbt.getBoolean(STEP_BACK_MODE_TAG);
        blockMovementCollision = nbt.getBoolean(BLOCK_MOVEMENT_COLLISION_TAG);
        checkpointTicks = readCheckpointTicks(nbt.getIntArray(CHECKPOINTS_TAG));
        stayAnimated = true;
        syncOverlayRuntimeMetadata();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX overlay settings debug load: disableRightClick={}, noClip={}, playPlaceSounds={}, shiftBack={}, events={}, checkpoints={}",
                disableRightClick, noClip, playPlaceSounds, shiftRightClickStepBackMode, events == null ? -1 : events.size(),
                checkpointTicks == null ? -1 : checkpointTicks.size());
        if (overlayStage < 0)
            overlayStage = 0;
        int maxStage = getMaxOverlayStage();
        if (overlayStage > maxStage)
            overlayStage = maxStage;
        if (overlayStage > 0 && !layers.isEmpty())
            currentLayerIndex = 0;
        opened = overlayStage > 0;
    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {
        stayAnimated = true;
        syncOverlayRuntimeMetadata();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX overlay settings debug save: disableRightClick={}, noClip={}, playPlaceSounds={}, shiftBack={}, events={}, checkpoints={}",
                disableRightClick, noClip, playPlaceSounds, shiftRightClickStepBackMode, events == null ? -1 : events.size(),
                checkpointTicks == null ? -1 : checkpointTicks.size());
        super.writeToNBTExtra(nbt);
        if (overlayStage > 0)
            nbt.setInteger(OVERLAY_STAGE_TAG, overlayStage);
        else
            nbt.removeTag(OVERLAY_STAGE_TAG);
        if (overlayResetting)
            nbt.setBoolean(OVERLAY_RESETTING_TAG, true);
        else
            nbt.removeTag(OVERLAY_RESETTING_TAG);
        if (shiftRightClickStepBackMode)
            nbt.setBoolean(STEP_BACK_MODE_TAG, true);
        else
            nbt.removeTag(STEP_BACK_MODE_TAG);
        if (blockMovementCollision)
            nbt.setBoolean(BLOCK_MOVEMENT_COLLISION_TAG, true);
        else
            nbt.removeTag(BLOCK_MOVEMENT_COLLISION_TAG);
        if (checkpointTicks != null && !checkpointTicks.isEmpty())
            nbt.setIntArray(CHECKPOINTS_TAG, checkpointTicks.stream().mapToInt(Integer::intValue).toArray());
        else
            nbt.removeTag(CHECKPOINTS_TAG);
    }

    @Override
    public EntityAnimation activate(DoorActivator activator, EntityPlayer player, UUID uuid) throws LittleActionException {
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX checkpoint debug overlay.activate: activator={}, overlayStage={}, opened={}, animated={}, inMotion={}, checkpoints={}",
                activator, overlayStage, opened, isAnimated(), isInMotion(), checkpointTicks);
        syncOverlayRuntimeMetadata();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX overlay settings debug activate: disableRightClick={}, noClip={}, playPlaceSounds={}, shiftBack={}, events={}",
                disableRightClick, noClip, playPlaceSounds, shiftRightClickStepBackMode, events == null ? -1 : events.size());
        if (mainBlock.isRemoved())
            throw new LittleActionException("Structure does not exist");

        if ((activator == DoorActivator.RIGHTCLICK || activator == DoorActivator.COMMAND) && disableRightClick)
            throw new LittleActionExceptionHidden("Door is locked!");

        load();

        if (activateParent && getParent() != null) {
            LittleStructure parentStructure = getParent().getStructure();
            if (parentStructure instanceof LittleDoor)
                return ((LittleDoor) parentStructure).activate(activator, player, uuid);
            throw new LittleActionException("Invalid parent");
        }

        if (isInMotion())
            throw new StillInMotionException();

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
            if (player != null)
                player.sendStatusMessage(new TextComponentTranslation("exception.door.notenoughspace"), true);
            throw new LittleActionException("Cannot open door");
        }

        if (layers.isEmpty())
            return null;

        stayAnimated = true;
        overlayResetting = false;
        pendingActivator = activator;
        pendingDirectStage = -1;
        return openDoor(player, new UUIDSupplier(uuid), false);
    }

    @Override
    public boolean onBlockActivated(World world, LittleTile tile, BlockPos pos, EntityPlayer player, EnumHand hand, ItemStack stack,
            EnumFacing facing, float hitX, float hitY, float hitZ, com.creativemd.littletiles.common.action.block.LittleActionActivated action)
            throws LittleActionException {
        DoorActivator activator = player != null && player.isSneaking() && shiftRightClickStepBackMode
                ? DoorActivator.COMMAND
                : DoorActivator.RIGHTCLICK;
        activate(activator, player, null);
        action.preventInteraction = true;
        return true;
    }

    @Override
    public EntityAnimation openDoor(@Nullable EntityPlayer player, UUIDSupplier uuid, boolean tickOnce) throws LittleActionException {
        syncOverlayRuntimeMetadata();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX checkpoint debug overlay.openDoor: animated={}, overlayStage={}, tickOnce={}, checkpoints={}",
                isAnimated(), overlayStage, tickOnce, checkpointTicks);
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX overlay settings debug openDoor: disableRightClick={}, noClip={}, playPlaceSounds={}, shiftBack={}, events={}",
                disableRightClick, noClip, playPlaceSounds, shiftRightClickStepBackMode, events == null ? -1 : events.size());
        int maxStage = getMaxOverlayStage();
        if (isAnimated()) {
            EntityAnimation liveAnimation = getAnimation();
            LittleVecXCheckpointController controller = getCheckpointController(liveAnimation);
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX checkpoint debug overlay.openDoor.live: controllerStage={}, changing={}",
                    controller == null ? -1 : controller.getCurrentStageIndex(),
                    controller != null && controller.isChanging());
            if (liveAnimation == null || controller == null)
                return super.openDoor(player, uuid, tickOnce);

            int currentStage = controller.getCurrentStageIndex();
            int targetStage = resolvePendingTargetStage(pendingActivator, currentStage, maxStage);
            int startStage = currentStage;
            if (targetStage == startStage)
                return liveAnimation;
            ensureBlockMovementPathClear(startStage, targetStage, player);
            if (!controller.transitionToStage(targetStage))
                return liveAnimation;

            if (tickOnce)
                liveAnimation.onUpdateForReal();

            if (!getWorld().isRemote)
                PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationControllerPacket(liveAnimation), liveAnimation, null);
            return liveAnimation;
        }

        if (mainBlock.isRemoved())
            return null;

        LittleAbsolutePreviews previews = getDoorPreviews();
        World world = getWorld();
        int currentStage = Math.max(0, overlayStage);
        int targetStage = resolvePendingTargetStage(pendingActivator, currentStage, maxStage);
        int startStage = currentStage;
        if (targetStage == startStage || targetStage <= 0)
            return null;
        ensureBlockMovementPathClear(startStage, targetStage, player);

        SubWorld fakeWorld = SubWorld.createFakeWorld(world);
        if (world.isRemote)
            fakeWorld.renderChunkSupplier = new LittleRenderChunkSuppilier();

        Placement placement = new Placement(player,
                PlacementHelper.getAbsolutePreviews(fakeWorld, previews, previews.pos, PlacementMode.all))
                        .setIgnoreWorldBoundaries(false);
        StructureAbsolute absolute = getAbsoluteAxis();

        HashMapList<BlockPos, IStructureTileList> blocks = collectAllBlocksListSameWorld();
        overlayStage = startStage;
        opened = startStage > 0;
        EntityAnimation animation = spawnCheckpointAnimation(world, fakeWorld, player, placement, uuid, absolute, tickOnce, startStage, targetStage);

        boolean sendUpdate = !world.isRemote;
        EntityAnimation topAnimation = world instanceof WorldServer ? null : (EntityAnimation) fakeWorld.getTopEntity();
        for (java.util.Map.Entry<BlockPos, ArrayList<IStructureTileList>> entry : blocks.entrySet()) {
            if (entry.getValue().isEmpty())
                continue;

            TileEntityLittleTiles te = entry.getValue().get(0).getTe();
            te.updateTiles((x) -> {
                for (IStructureTileList list : entry.getValue())
                    x.get(list).remove();
            });

            if (sendUpdate) {
                if (topAnimation == null)
                    ((WorldServer) world).getPlayerChunkMap().markBlockForUpdate(te.getPos());
                else
                    PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationDataPacket(topAnimation), topAnimation, null);
            }
        }

        return animation;
    }

    @Override
    public void sendActivationToServer(DoorActivator type, EntityPlayer activator, UUID uuid) {
        super.sendActivationToServer(type, activator, uuid);
    }

    @Override
    public void performInternalOutputChange(com.creativemd.littletiles.common.structure.signal.output.InternalSignalOutput output) {
        String identifier = output.component.identifier;
        if (suppressStateOutputCallback && "state".equals(identifier))
            return;
        boolean[] state = output.getState();
        if (state == null || state.length == 0)
            return;

        if (isInMotion())
            return;

        try {
            if ("state".equals(identifier)) {
                if (state[0]) {
                    pendingActivator = DoorActivator.RIGHTCLICK;
                    pendingDirectStage = -1;
                    openDoor(null, new UUIDSupplier(UUID.randomUUID()), false);
                } else if (overlayStage > 0) {
                    triggerCheckpointStage(0);
                }
                return;
            }

            int targetStage = getCheckpointSignalTargetStage(identifier);
            if (targetStage < 0)
                return;

            if (state[0])
                activateCheckpointSignal(targetStage);
        } catch (LittleActionException ignored) {
        }
    }

    @Override
    public void finishAnimation(EntityAnimation animation) {
        afterFinishAnimation(animation, getAnimatedStructureTarget());
    }

    @Override
    public void completeAnimation() {
        if (activateParent && getParent() != null) {
            try {
                LittleStructure parent = getParent().getStructure();
                if (parent instanceof LittleDoor)
                    ((LittleDoor) parent).onChildComplete(this, getParent().childId);
            } catch (com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException
                    | com.creativemd.littletiles.common.structure.exception.NotYetConnectedException e) {
                com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX overlay animation could not notify its parent because the connection is not ready", e);
            }
        }

        if (!mainBlock.isRemoved() && !isClient())
            notifyChange();
    }

    @Override
    protected void afterFinishAnimation(EntityAnimation animation, @Nullable StructureLittleVecXMultiAnimation targetBase) {
        StructureLittleVecXOverlayAnimation target = targetBase instanceof StructureLittleVecXOverlayAnimation
                ? (StructureLittleVecXOverlayAnimation) targetBase
                : null;

        LittleVecXCheckpointController controller = getCheckpointController(animation);
        overlayStage = controller != null ? controller.getCurrentStageIndex() : 0;
        overlayResetting = false;
        opened = overlayStage > 0;
        currentLayerIndex = overlayStage > 0 && !layers.isEmpty() ? 0 : -1;
        runtimeLayerOverride = null;
        refreshCurrentLayerFields();
        syncPrimaryStateOutput(overlayStage > 0, targetBase);

        if (target != null && target != this) {
            target.overlayStage = overlayStage;
            target.overlayResetting = overlayResetting;
            target.currentLayerIndex = currentLayerIndex;
            target.opened = opened;
            target.runtimeLayerOverride = null;
        }

        syncMirroredAnimatedState(targetBase);
    }

    public void onCheckpointTransitionStarted(int fromStage, int toStage) {
        LittleVecXAnimationLayer baseLayer = getBaseLayer();
        if (baseLayer == null)
            return;

        applyLayer(baseLayer);
        List<Integer> boundaries = getCheckpointBoundaries(baseLayer);
        int from = clampStage(fromStage, boundaries);
        int to = clampStage(toStage, boundaries);
        int startTick = boundaries.get(Math.min(from, to));
        int endTick = boundaries.get(Math.max(from, to));

        duration = Math.max(1, Math.abs(endTick - startTick));
        interpolation = baseLayer.interpolation;
        overlayResetting = to == 0 && from > 0;
        opened = to > 0;
        currentLayerIndex = opened && !layers.isEmpty() ? 0 : -1;
        events = to >= from ? sliceEvents(baseLayer.events, startTick, endTick) : new ArrayList<>();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX checkpoint debug overlay.segment: fromStage={}, toStage={}, startTick={}, endTick={}, duration={}",
                fromStage, toStage, startTick, endTick, duration);
    }

    @Override
    protected void prepareRebuiltController(com.creativemd.littletiles.common.entity.DoorController oldController,
            com.creativemd.littletiles.common.entity.DoorController newController, EntityAnimation liveAnimation) {
        // For overlay progression we always want a fresh closed controller, then open it as the next step.
    }

    @Override
    protected boolean shouldSyncRebuiltAnimationImmediately() {
        return false;
    }

    @Override
    protected boolean isControllerOpened(@Nullable DoorController controller) {
        if (controller instanceof LittleVecXCheckpointController)
            return ((LittleVecXCheckpointController) controller).getCurrentStageIndex() > 0;
        return super.isControllerOpened(controller);
    }

    @Override
    protected boolean getPrimaryStateOutputValue() {
        return overlayStage > 0;
    }

    private EntityAnimation spawnCheckpointAnimation(World world, SubWorld fakeWorld, @Nullable EntityPlayer player, Placement placement,
            UUIDSupplier supplier, StructureAbsolute absolute, boolean tickOnce, int startStage, int targetStage) throws LittleActionException {
        fakeWorld.preventNeighborUpdate = true;
        placement.setAfterNotifyPlace(false);
        PlacementResult result = placement.tryPlace();
        if (result == null)
            throw new RuntimeException("Something went wrong during placing the overlay animation!");

        DoorController rawController = createController(supplier, placement, getCompleteDuration());
        if (!(rawController instanceof LittleVecXCheckpointController))
            throw new RuntimeException("Overlay animation requires LittleVecXCheckpointController");

        LittleVecXCheckpointController controller = (LittleVecXCheckpointController) rawController;
        controller.noClip = noClip;
        controller.activator = player;
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX checkpoint debug overlay.spawn: maxStage={}, checkpoints={}, completeDuration={}",
                controller.getMaxStageIndex(), checkpointTicks, getCompleteDuration());

        fakeWorld.preventNeighborUpdate = false;

        LittleAdvancedDoor newDoor = (LittleAdvancedDoor) result.parentStructure;
        controller.setCurrentStageInstant(startStage);
        EntityAnimation animation = new EntityAnimation(world, fakeWorld, controller, placement.pos, supplier.next(), absolute,
                new LocalStructureLocation(newDoor));

        newDoor.transferChildrenToAnimation(animation);
        if (getParent() != null) {
            LittleStructure parentStructure = getParent().getStructure();
            boolean dynamic = getParent().dynamic;
            parentStructure.updateChildConnection(getParent().getChildId(), newDoor, dynamic);
            newDoor.updateParentConnection(getParent().getChildId(), parentStructure, dynamic);
            parentStructure.updateStructure();
        }

        newDoor.notifyAfterPlaced();
        controller.transitionToStage(targetStage);
        animation.updateTickState();
        animation.updateBoundingBox();
        world.spawnEntity(animation);

        if (tickOnce)
            animation.onUpdateForReal();
        return animation;
    }

    private EntityAnimation advanceOverlay(@Nullable EntityPlayer player, UUID uuid, int targetStage) throws LittleActionException {
        int maxStage = getMaxOverlayStage();
        if (targetStage <= overlayStage || targetStage <= 0 || targetStage > maxStage)
            return null;

        StructureLittleVecXOverlayAnimation target = getOverlayTarget();
        LittleVecXAnimationLayer transitionLayer = buildStageTransitionLayer(overlayStage, targetStage);
        if (transitionLayer == null)
            return null;
        if (getLiveAnimation(target) != null) {
            if (!rebuildPersistentAnimationLayer(-1, transitionLayer, player))
                return null;
            EntityAnimation liveAnimation = getLiveAnimation(target);
            if (!playAnimatedTransition(liveAnimation))
                return null;
        } else {
            runtimeLayerOverride = transitionLayer;
            applyLayer(transitionLayer);
            if (target != null && target != this) {
                target.runtimeLayerOverride = transitionLayer;
                target.applyLayer(transitionLayer);
            }
        }

        currentLayerIndex = 0;
        if (target != null && target != this)
            target.currentLayerIndex = 0;

        EntityAnimation result = getLiveAnimation(target);
        if (result == null)
            result = openDoor(player, new UUIDSupplier(uuid), false);
        runtimeLayerOverride = null;
        if (target != null && target != this)
            target.runtimeLayerOverride = null;
        overlayStage = targetStage;

        if (target != null && target != this)
            target.overlayStage = overlayStage;

        syncPrimaryStateOutput(true, target);
        return result;
    }

    private EntityAnimation resetOverlay(@Nullable EntityPlayer player, UUID uuid) throws LittleActionException {
        if (overlayStage <= 0)
            return null;

        StructureLittleVecXOverlayAnimation target = getOverlayTarget();
        LittleVecXAnimationLayer resetLayer = buildStageTransitionLayer(overlayStage, 0);
        if (resetLayer == null)
            return null;

        if (getLiveAnimation(target) != null) {
            if (!rebuildPersistentAnimationLayer(-1, resetLayer, player))
                return null;
            EntityAnimation liveAnimation = getLiveAnimation(target);
            if (!playAnimatedTransition(liveAnimation))
                return null;
        } else {
            runtimeLayerOverride = resetLayer;
            applyLayer(resetLayer);
            if (target != null && target != this) {
                target.runtimeLayerOverride = resetLayer;
                target.applyLayer(resetLayer);
            }
        }

        overlayResetting = true;
        if (target != null && target != this) {
            target.overlayResetting = true;
        }

        EntityAnimation result = getLiveAnimation(target);
        if (result == null)
            result = openDoor(player, new UUIDSupplier(uuid), false);
        runtimeLayerOverride = null;
        if (target != null && target != this)
            target.runtimeLayerOverride = null;
        return result;
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
        if (!isClient()) {
            PacketHandler.sendPacketToTrackingPlayers(new com.creativemd.littletiles.common.packet.LittleAnimationDataPacket(animation), animation, null);
            PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationControllerPacket(animation), animation, null);
        }
        return true;
    }

    private StructureLittleVecXOverlayAnimation getOverlayTarget() {
        StructureLittleVecXMultiAnimation target = getAnimatedStructureTarget();
        if (target instanceof StructureLittleVecXOverlayAnimation)
            return (StructureLittleVecXOverlayAnimation) target;
        return this;
    }

    @Nullable
    private LittleVecXAnimationLayer buildStageTransitionLayer(int fromStage, int toStage) {
        LittleVecXAnimationLayer baseLayer = getBaseLayer();
        if (baseLayer == null)
            return null;

        List<Integer> boundaries = getCheckpointBoundaries(baseLayer);
        if (boundaries.size() < 2)
            return null;

        int clampedFrom = clampStage(fromStage, boundaries);
        int clampedTo = clampStage(toStage, boundaries);
        if (clampedFrom == clampedTo)
            return null;

        int startTick = boundaries.get(Math.min(clampedFrom, clampedTo));
        int endTick = boundaries.get(Math.max(clampedFrom, clampedTo));
        LittleVecXAnimationLayer segment = buildSegmentLayer(baseLayer, startTick, endTick);
        if (segment == null)
            return null;
        if (clampedTo < clampedFrom)
            return invertSegmentLayer(segment);
        return segment;
    }

    @Nullable
    private LittleVecXAnimationLayer getBaseLayer() {
        return layers == null || layers.isEmpty() ? null : layers.get(0);
    }

    /**
     * Checks every rendered tick of a checkpoint transition before creating or retargeting an
     * animation entity. This keeps the animation in its current checkpoint when a solid block
     * occupies any part of the path, instead of allowing the moving tiles to overlay it.
     */
    private void ensureBlockMovementPathClear(int fromStage, int toStage, @Nullable EntityPlayer player) throws LittleActionException {
        if (!blockMovementCollision || isClient() || fromStage == toStage)
            return;

        LittleVecXAnimationLayer baseLayer = getBaseLayer();
        if (baseLayer == null)
            return;

        List<Integer> boundaries = getCheckpointBoundaries(baseLayer);
        if (boundaries.isEmpty())
            return;

        int startTick = boundaries.get(clampStage(fromStage, boundaries));
        int endTick = boundaries.get(clampStage(toStage, boundaries));
        LittleAbsolutePreviews source = getAbsolutePreviewsSameWorldOnly(getPos());
        if (source == null)
            return;

        World structureWorld = getWorld();
        IVecOrigin origin = structureWorld instanceof SubWorld ? ((SubWorld) structureWorld).getOrigin() : null;
        if (hasBlockOnMovementPath(getRealWorld(structureWorld), source, baseLayer, startTick, endTick, origin)) {
            if (player != null)
                player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.overlay.blocked_by_world"), true);
            throw new LittleActionException("Overlay animation path is blocked");
        }
    }

    @Nullable
    private static World getRealWorld(@Nullable World world) {
        return world instanceof SubWorld ? ((SubWorld) world).getRealWorld() : world;
    }

    private boolean hasBlockOnMovementPath(World world, LittleAbsolutePreviews source, LittleVecXAnimationLayer layer, int startTick, int endTick,
            @Nullable IVecOrigin origin) {
        if (world == null || source == null || layer == null)
            return false;

        // Before the animation entity exists, its source tiles are still placed in the
        // real world. They must not be mistaken for an obstacle to their own first move.
        Set<BlockPos> sourcePositions = origin == null ? collectSourcePositions(source) : Collections.emptySet();
        LittleGridContext offsetGrid = LittleGridContext.get(layer.getSafeOffGrid());
        double startOffsetX = offsetGrid.toVanillaGrid(valueAt(layer.offX, startTick));
        double startOffsetY = offsetGrid.toVanillaGrid(valueAt(layer.offY, startTick));
        double startOffsetZ = offsetGrid.toVanillaGrid(valueAt(layer.offZ, startTick));
        int direction = endTick >= startTick ? 1 : -1;
        for (int tick = startTick;; tick += direction) {
            double offsetX = offsetGrid.toVanillaGrid(valueAt(layer.offX, tick));
            double offsetY = offsetGrid.toVanillaGrid(valueAt(layer.offY, tick));
            double offsetZ = offsetGrid.toVanillaGrid(valueAt(layer.offZ, tick));
            if (hasStaticCollisionAt(world, source, sourcePositions, origin,
                    offsetX - startOffsetX, offsetY - startOffsetY, offsetZ - startOffsetZ))
                return true;
            if (tick == endTick)
                return false;
        }
    }

    private static Set<BlockPos> collectSourcePositions(LittleAbsolutePreviews source) {
        Set<BlockPos> positions = new HashSet<>();
        for (com.creativemd.littletiles.common.tile.preview.LittlePreview preview : source) {
            AxisAlignedBB box = preview.box.getBox(source.getContext(), source.pos);
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
        return positions;
    }

    private static boolean hasStaticCollisionAt(World world, LittleAbsolutePreviews source, Set<BlockPos> sourcePositions, @Nullable IVecOrigin origin,
            double offsetX, double offsetY, double offsetZ) {
        List<AxisAlignedBB> collisions = new ArrayList<>(1);
        for (com.creativemd.littletiles.common.tile.preview.LittlePreview preview : source) {
            AxisAlignedBB movingBox = preview.box.getBox(source.getContext(), source.pos);
            if (origin == null)
                movingBox = movingBox.offset(offsetX, offsetY, offsetZ);
            else {
                // In an active animation the structure lives in a SubWorld. Its preview boxes
                // are local, while blocks to test are in the real world. Project the current
                // local box first, then add only the timeline delta from the current checkpoint.
                // A parent animation may itself be rotated, so rotate that delta with the parent.
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
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
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
            }
        }
        return false;
    }

    private int getMaxOverlayStage() {
        LittleVecXAnimationLayer baseLayer = getBaseLayer();
        if (baseLayer == null)
            return 0;
        return Math.max(0, getCheckpointBoundaries(baseLayer).size() - 1);
    }

    private int clampStage(int stage, List<Integer> boundaries) {
        if (boundaries.isEmpty())
            return 0;
        if (stage < 0)
            return 0;
        int max = boundaries.size() - 1;
        return Math.min(stage, max);
    }

    private List<Integer> getCheckpointBoundaries(LittleVecXAnimationLayer baseLayer) {
        int duration = Math.max(1, baseLayer.getSafeDuration());
        List<Integer> boundaries = new ArrayList<>();
        boundaries.add(0);
        if (checkpointTicks != null) {
            for (Integer tick : checkpointTicks) {
                if (tick != null && tick > 0 && tick < duration && !boundaries.contains(tick))
                    boundaries.add(tick);
            }
        }
        Collections.sort(boundaries);
        if (boundaries.get(boundaries.size() - 1) != duration)
            boundaries.add(duration);
        return boundaries;
    }

    @Nullable
    private LittleVecXAnimationLayer buildSegmentLayer(LittleVecXAnimationLayer baseLayer, int startTick, int endTick) {
        if (endTick <= startTick)
            return null;

        LittleVecXAnimationLayer segment = new LittleVecXAnimationLayer();
        segment.name = baseLayer.name;
        segment.trigger = LittleVecXAnimationTriggerMode.NONE;
        segment.doorType = baseLayer.doorType;
        segment.duration = Math.max(1, endTick - startTick);
        segment.interpolation = baseLayer.interpolation;
        segment.offGrid = baseLayer.getSafeOffGrid();
        segment.axisData = baseLayer.axisData == null ? null : baseLayer.axisData.clone();
        segment.axisLocalData = baseLayer.axisLocalData == null ? null : baseLayer.axisLocalData.clone();
        segment.doorData = baseLayer.doorData == null ? new NBTTagCompound() : baseLayer.doorData.copy();
        segment.events = sliceEvents(baseLayer.events, startTick, endTick);
        segment.rotX = sliceTimeline(baseLayer.rotX, startTick, endTick);
        segment.rotY = sliceTimeline(baseLayer.rotY, startTick, endTick);
        segment.rotZ = sliceTimeline(baseLayer.rotZ, startTick, endTick);
        segment.offX = sliceTimeline(baseLayer.offX, startTick, endTick);
        segment.offY = sliceTimeline(baseLayer.offY, startTick, endTick);
        segment.offZ = sliceTimeline(baseLayer.offZ, startTick, endTick);
        return segment;
    }

    private LittleVecXAnimationLayer invertSegmentLayer(LittleVecXAnimationLayer forwardLayer) {
        LittleVecXAnimationLayer reverse = new LittleVecXAnimationLayer();
        reverse.name = "Reset";
        reverse.trigger = LittleVecXAnimationTriggerMode.NONE;
        reverse.doorType = forwardLayer.doorType;
        reverse.duration = forwardLayer.getSafeDuration();
        reverse.interpolation = forwardLayer.interpolation;
        reverse.offGrid = forwardLayer.getSafeOffGrid();
        reverse.axisData = forwardLayer.axisData == null ? null : forwardLayer.axisData.clone();
        reverse.axisLocalData = forwardLayer.axisLocalData == null ? null : forwardLayer.axisLocalData.clone();
        reverse.doorData = forwardLayer.doorData == null ? new NBTTagCompound() : forwardLayer.doorData.copy();
        reverse.events = new ArrayList<>();
        reverse.rotX = invertCompiledTimeline(forwardLayer.rotX, reverse.duration);
        reverse.rotY = invertCompiledTimeline(forwardLayer.rotY, reverse.duration);
        reverse.rotZ = invertCompiledTimeline(forwardLayer.rotZ, reverse.duration);
        reverse.offX = invertCompiledTimeline(forwardLayer.offX, reverse.duration);
        reverse.offY = invertCompiledTimeline(forwardLayer.offY, reverse.duration);
        reverse.offZ = invertCompiledTimeline(forwardLayer.offZ, reverse.duration);
        return reverse;
    }

    private boolean triggerCheckpointStage(int targetStage) throws LittleActionException {
        LittleVecXCheckpointController controller = getCheckpointController(getAnimation());
        EntityAnimation animation = getAnimation();
        if (controller == null || animation == null || controller.isChanging())
            return false;
        ensureBlockMovementPathClear(controller.getCurrentStageIndex(), targetStage, null);
        if (!controller.transitionToStage(targetStage))
            return false;
        animation.updateTickState();
        animation.updateBoundingBox();
        if (!isClient())
            PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationControllerPacket(animation), animation, null);
        return true;
    }

    protected static int getSignalLayerIndex(@Nullable String identifier) {
        if (identifier == null)
            return -1;

        if (identifier.startsWith("checkpoint_")) {
            try {
                int index = Integer.parseInt(identifier.substring("checkpoint_".length()));
                return index >= 0 && index < LittleVecXConfig.multiAnimationSignalCount ? index : -1;
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }

        return StructureLittleVecXMultiAnimation.getSignalLayerIndex(identifier);
    }

    private boolean activateCheckpointSignal(int targetStage) throws LittleActionException {
        if (targetStage < 0 || targetStage > getMaxOverlayStage())
            return false;
        if (isInMotion())
            return false;
        pendingActivator = DoorActivator.SIGNAL;
        pendingDirectStage = targetStage;
        return openDoor(null, new UUIDSupplier(UUID.randomUUID()), false) != null;
    }

    /**
     * Each checkpoint signal addresses the matching user-saved point, rather than
     * an implicit stage after it. This keeps {@code checkpoint_0} usable when the
     * first point is intentionally placed at tick zero.
     */
    private int getCheckpointSignalTargetStage(@Nullable String identifier) {
        int signalIndex = getSignalLayerIndex(identifier);
        if (signalIndex < 0 || checkpointTicks == null || signalIndex >= checkpointTicks.size())
            return -1;

        Integer checkpointTick = checkpointTicks.get(signalIndex);
        LittleVecXAnimationLayer baseLayer = getBaseLayer();
        if (checkpointTick == null || baseLayer == null)
            return -1;

        int duration = Math.max(1, baseLayer.getSafeDuration());
        int clampedTick = Math.max(0, Math.min(checkpointTick, duration));
        return getCheckpointBoundaries(baseLayer).indexOf(clampedTick);
    }

    private int resolveClickTargetStage(DoorActivator activator, int currentStage, int maxStage) {
        if (activator == DoorActivator.COMMAND && shiftRightClickStepBackMode)
            return currentStage > 0 ? currentStage - 1 : 0;

        if (currentStage < maxStage)
            return currentStage + 1;

        if (shiftRightClickStepBackMode)
            return currentStage;

        return currentStage > 0 ? 0 : currentStage;
    }

    private int resolvePendingTargetStage(DoorActivator activator, int currentStage, int maxStage) {
        if (pendingDirectStage >= 0) {
            int targetStage = Math.max(0, Math.min(pendingDirectStage, maxStage));
            pendingDirectStage = -1;
            return targetStage;
        }
        return resolveClickTargetStage(activator, currentStage, maxStage);
    }

    private void syncOverlayRuntimeMetadata() {
        LittleVecXAnimationLayer baseLayer = getBaseLayer();
        if (baseLayer == null)
            return;

        events = copyEvents(baseLayer.events);
        duration = baseLayer.getSafeDuration();
        interpolation = baseLayer.interpolation;
        try {
            offGrid = LittleGridContext.get(baseLayer.getSafeOffGrid());
        } catch (RuntimeException e) {
            offGrid = LittleGridContext.get(LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID);
        }
        stayAnimated = true;
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX overlay settings debug syncRuntime: baseLayerEvents={}, topDisableRightClick={}, topNoClip={}, topPlayPlaceSounds={}, duration={}, interpolation={}",
                baseLayer.events == null ? -1 : baseLayer.events.size(), disableRightClick, noClip, playPlaceSounds, duration, interpolation);
    }

    @Override
    public void startAnimation(EntityAnimation animation) {
        super.startAnimation(animation);
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX overlay settings debug startAnimation: events={}, overlayStage={}, resetting={}",
                events == null ? -1 : events.size(), overlayStage, overlayResetting);
    }

    @Override
    public void beforeTick(EntityAnimation animation, int tick) {
        if (events != null) {
            for (int i = 0; i < events.size(); i++) {
                if (events.get(i).shouldBeProcessed(tick))
                    com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX overlay settings debug eventTick: tick={}, eventIndex={}, eventClass={}", tick, i,
                            events.get(i).getClass().getSimpleName());
            }
        }
        super.beforeTick(animation, tick);
    }

    @Nullable
    private LittleVecXCheckpointController getCheckpointController(@Nullable EntityAnimation animation) {
        if (animation == null || !(animation.controller instanceof LittleVecXCheckpointController))
            return null;
        return (LittleVecXCheckpointController) animation.controller;
    }

    private static String getStageStateName(int stageIndex) {
        if (stageIndex <= 0)
            return DoorController.closedState;
        if (stageIndex == 1)
            return DoorController.openedState;
        return "checkpoint_" + stageIndex;
    }

    private com.creativemd.littletiles.common.structure.animation.AnimationState buildStateAtTick(LittleVecXAnimationLayer baseLayer, int tick) {
        com.creativemd.littletiles.common.structure.animation.AnimationState state =
                new com.creativemd.littletiles.common.structure.animation.AnimationState();
        LittleGridContext grid = LittleGridContext.get(baseLayer.getSafeOffGrid());
        state.set(AnimationKey.rotX, valueAt(baseLayer.rotX, tick));
        state.set(AnimationKey.rotY, valueAt(baseLayer.rotY, tick));
        state.set(AnimationKey.rotZ, valueAt(baseLayer.rotZ, tick));
        state.set(AnimationKey.offX, grid.toVanillaGrid(valueAt(baseLayer.offX, tick)));
        state.set(AnimationKey.offY, grid.toVanillaGrid(valueAt(baseLayer.offY, tick)));
        state.set(AnimationKey.offZ, grid.toVanillaGrid(valueAt(baseLayer.offZ, tick)));
        return state;
    }

    @Nullable
    private AnimationTimeline buildStageTimeline(LittleVecXAnimationLayer baseLayer, int fromTick, int toTick) {
        if (fromTick == toTick)
            return null;

        int startTick = Math.min(fromTick, toTick);
        int endTick = Math.max(fromTick, toTick);
        boolean reverse = toTick < fromTick;

        PairList<AnimationKey, ValueTimeline> values = new PairList<>();
        LittleGridContext grid = LittleGridContext.get(baseLayer.getSafeOffGrid());
        addStageTimeline(values, AnimationKey.rotX, baseLayer.rotX, startTick, endTick, reverse, false, grid);
        addStageTimeline(values, AnimationKey.rotY, baseLayer.rotY, startTick, endTick, reverse, false, grid);
        addStageTimeline(values, AnimationKey.rotZ, baseLayer.rotZ, startTick, endTick, reverse, false, grid);
        addStageTimeline(values, AnimationKey.offX, baseLayer.offX, startTick, endTick, reverse, true, grid);
        addStageTimeline(values, AnimationKey.offY, baseLayer.offY, startTick, endTick, reverse, true, grid);
        addStageTimeline(values, AnimationKey.offZ, baseLayer.offZ, startTick, endTick, reverse, true, grid);
        return new AnimationTimeline(Math.max(1, endTick - startTick), values);
    }

    private void addStageTimeline(PairList<AnimationKey, ValueTimeline> values, AnimationKey key, @Nullable ValueTimeline source,
            int startTick, int endTick, boolean reverse, boolean vanillaGrid, LittleGridContext grid) {
        ValueTimeline timeline = sliceTimeline(source, startTick, endTick);
        if (timeline == null)
            return;
        if (reverse)
            timeline = timeline.invert(Math.max(1, endTick - startTick));
        if (vanillaGrid)
            timeline.factor(grid.pixelSize);
        values.add(key, timeline);
    }

    @Nullable
    private LittleVecXAnimationLayer buildPoseLayerAtStage(int stage) {
        LittleVecXAnimationLayer baseLayer = getBaseLayer();
        if (baseLayer == null)
            return null;

        List<Integer> boundaries = getCheckpointBoundaries(baseLayer);
        if (boundaries.isEmpty())
            return null;

        int clampedStage = clampStage(stage, boundaries);
        int tick = boundaries.get(clampedStage);

        LittleVecXAnimationLayer pose = new LittleVecXAnimationLayer();
        pose.name = baseLayer.name;
        pose.trigger = LittleVecXAnimationTriggerMode.NONE;
        pose.doorType = baseLayer.doorType;
        pose.duration = 1;
        pose.interpolation = baseLayer.interpolation;
        pose.offGrid = baseLayer.getSafeOffGrid();
        pose.axisData = baseLayer.axisData == null ? null : baseLayer.axisData.clone();
        pose.axisLocalData = baseLayer.axisLocalData == null ? null : baseLayer.axisLocalData.clone();
        pose.doorData = baseLayer.doorData == null ? new NBTTagCompound() : baseLayer.doorData.copy();
        pose.events = new ArrayList<>();
        pose.rotX = constantTimeline(1, valueAt(baseLayer.rotX, tick));
        pose.rotY = constantTimeline(1, valueAt(baseLayer.rotY, tick));
        pose.rotZ = constantTimeline(1, valueAt(baseLayer.rotZ, tick));
        pose.offX = constantTimeline(1, valueAt(baseLayer.offX, tick));
        pose.offY = constantTimeline(1, valueAt(baseLayer.offY, tick));
        pose.offZ = constantTimeline(1, valueAt(baseLayer.offZ, tick));
        return pose;
    }

    private static double valueAt(@Nullable ValueTimeline timeline, int tick) {
        return timeline == null ? 0.0D : timeline.value(tick);
    }

    @Nullable
    private static LittleVecXAnimationLayer copyLayer(@Nullable LittleVecXAnimationLayer source) {
        if (source == null)
            return null;
        return LittleVecXAnimationLayer.readFromNBT(source.writeToNBT());
    }

    @Nullable
    private static ValueTimeline sliceTimeline(@Nullable ValueTimeline timeline, int startTick, int endTick) {
        if (timeline == null || endTick <= startTick)
            return null;

        int duration = endTick - startTick;
        PairList<Integer, Double> source = timeline.getPointsCopy();
        PairList<Integer, Double> segmentPoints = new PairList<>();

        addTimelinePoint(segmentPoints, 0, timeline.value(startTick));
        for (int i = 0; i < source.size(); i++) {
            int tick = source.get(i).key;
            if (tick > startTick && tick < endTick)
                addTimelinePoint(segmentPoints, tick - startTick, source.get(i).value);
        }
        addTimelinePoint(segmentPoints, duration, timeline.value(endTick));
        return ValueTimeline.create(ValueTimeline.getId(timeline.getClass()), segmentPoints);
    }

    private static void addTimelinePoint(PairList<Integer, Double> points, int tick, double value) {
        if (!points.isEmpty() && points.getLast().key == tick) {
            points.getLast().value = value;
            return;
        }
        points.add(tick, value);
    }

    private static List<com.creativemd.littletiles.common.structure.animation.event.AnimationEvent> sliceEvents(
            List<com.creativemd.littletiles.common.structure.animation.event.AnimationEvent> source, int startTick, int endTick) {
        List<com.creativemd.littletiles.common.structure.animation.event.AnimationEvent> copy = new ArrayList<>();
        if (source == null || source.isEmpty() || endTick <= startTick)
            return copy;

        for (com.creativemd.littletiles.common.structure.animation.event.AnimationEvent event : source) {
            int tick = event.getTick();
            if (tick < startTick || tick > endTick)
                continue;
            if (tick == startTick && startTick > 0)
                continue;

            NBTTagCompound tag = event.writeToNBT(new NBTTagCompound());
            tag.setInteger("tick", tick - startTick);
            com.creativemd.littletiles.common.structure.animation.event.AnimationEvent cloned =
                    com.creativemd.littletiles.common.structure.animation.event.AnimationEvent.loadFromNBT(tag);
            if (cloned != null)
                copy.add(cloned);
        }
        return copy;
    }

    private LittleVecXAnimationLayerCompiler.CompiledAnimation compilePrefix(int count) {
        if (count <= 0)
            return LittleVecXAnimationLayerCompiler.compile(new ArrayList<>());
        return LittleVecXAnimationLayerCompiler.compile(new ArrayList<>(layers.subList(0, Math.min(count, layers.size()))));
    }

    @Nullable
    private static ValueTimeline invertCompiledTimeline(@Nullable ValueTimeline timeline, int duration) {
        return timeline == null ? null : timeline.invert(duration);
    }

    @Nullable
    private static ValueTimeline copyTimeline(@Nullable ValueTimeline timeline) {
        return timeline == null ? null : timeline.copy();
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
            points.add(0, new com.creativemd.creativecore.common.utils.type.Pair<>(0, baseValue));
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

    private static List<Integer> readCheckpointTicks(int[] values) {
        List<Integer> ticks = new ArrayList<>();
        if (values == null)
            return ticks;
        for (int value : values) {
            if (value >= 0 && !ticks.contains(value))
                ticks.add(value);
        }
        Collections.sort(ticks);
        return ticks;
    }

    public static class StructureLittleVecXOverlayAnimationParser extends LittleStructureGuiParser {

        private static final int CHECKPOINT_CHANNEL_INDEX = 6;

        @SideOnly(Side.CLIENT)
        private LittleGridContext context = LittleGridContext.get();
        @SideOnly(Side.CLIENT)
        @Nullable
        private KeyControl selected;
        @SideOnly(Side.CLIENT)
        @Nullable
        private Boolean cachedDisableRightClick;
        @SideOnly(Side.CLIENT)
        @Nullable
        private Boolean cachedNoClip;
        @SideOnly(Side.CLIENT)
        @Nullable
        private Boolean cachedPlayPlaceSounds;
        @SideOnly(Side.CLIENT)
        @Nullable
        private Boolean cachedShiftRightClickStepBackMode;
        @SideOnly(Side.CLIENT)
        @Nullable
        private Boolean cachedBlockMovementCollision;
        @SideOnly(Side.CLIENT)
        @Nullable
        private List<com.creativemd.littletiles.common.structure.animation.event.AnimationEvent> cachedEvents;

        public StructureLittleVecXOverlayAnimationParser(GuiParent parent, AnimationGuiHandler handler) {
            super(parent, handler);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void create(LittlePreviews previews, @Nullable LittleStructure structure) {
            createControls(previews, structure);
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected void createControls(LittlePreviews previews, @Nullable LittleStructure structure) {
            StructureLittleVecXOverlayAnimation animation = structure instanceof StructureLittleVecXOverlayAnimation ? (StructureLittleVecXOverlayAnimation) structure : null;
            LittleAdvancedDoor door = buildEditingDoor(animation, previews);
            applyCachedOverlayState(animation, door);
            LittleVecXGuiLayout layout = new LittleVecXGuiLayout(190, 130, 0);
            int timelineHeight = 77;
            int valueRowY = layout.nextRow(0, timelineHeight, 4);
            int positionRowY = layout.nextRow(valueRowY, 10, 4);
            int actionsRowY = layout.nextRow(positionRowY, 10, 6);
            int footerRowY = layout.nextRow(actionsRowY, 7, 5);
            int compactButtonHeight = 7;
            int actionControlsY = actionsRowY - 2;
            int smallFieldWidth = 40;
            int positionLabelX = 0;
            int positionFieldX = 54;
            int axisButtonX = 104;
            int axisButtonWidth = 40;
            int actionButtonWidth = 40;
            int actionGap = 10;
            int settingsButtonX = 0;
            int eventsButtonX = settingsButtonX + actionButtonWidth + actionGap;
            int signalButtonX = eventsButtonX + actionButtonWidth + actionGap;
            int interpolationButtonX = signalButtonX + actionButtonWidth + actionGap;
            List<TimelineChannel> channels = new ArrayList<>();
            channels.add(new TimelineChannelDouble("rot X").addKeys(door.rotX != null ? door.rotX.getPointsCopy() : null));
            channels.add(new TimelineChannelDouble("rot Y").addKeys(door.rotY != null ? door.rotY.getPointsCopy() : null));
            channels.add(new TimelineChannelDouble("rot Z").addKeys(door.rotZ != null ? door.rotZ.getPointsCopy() : null));
            channels.add(new TimelineChannelInteger("off X").addKeys(door.offX != null ? door.offX.getRoundedPointsCopy() : null));
            channels.add(new TimelineChannelInteger("off Y").addKeys(door.offY != null ? door.offY.getRoundedPointsCopy() : null));
            channels.add(new TimelineChannelInteger("off Z").addKeys(door.offZ != null ? door.offZ.getRoundedPointsCopy() : null));
            channels.add(new TimelineChannelInteger("save *").addKeys(toCheckpointPairs(animation != null ? animation.checkpointTicks : null)));
            parent.controls.add(new GuiTimeline("timeline", 0, 0, 190, timelineHeight, door.duration, channels, handler).setSidebarWidth(30));
            parent.controls.add(new GuiLabel("tick", "0", layout.right(40), valueRowY + 1));

            context = door.offGrid != null ? door.offGrid : LittleGridContext.get();
            parent.controls.add(new GuiTextfield("keyValue", "", 0, valueRowY, smallFieldWidth, 10).setFloatOnly().setEnabled(false));
            GuiLTDistance keyDistance = new GuiLTDistance("keyDistance", 0, valueRowY, context, 0);
            compactCheckpointDistanceField(keyDistance);
            keyDistance.setVisible(false);
            parent.controls.add(keyDistance);
            parent.controls.add(new GuiLabel("Position:", positionLabelX, positionRowY + 1));
            parent.controls.add(new GuiTextfield("keyPosition", "", positionFieldX, positionRowY, actionButtonWidth, compactButtonHeight).setNumbersOnly().setEnabled(false));
            parent.controls.add(new GuiAxisButton("axis", "open axis", axisButtonX, positionRowY, axisButtonWidth, compactButtonHeight, previews.getContext(), door, handler));
            parent.controls.add(new GuiLittleVecXOverlaySettingsButton("settings", settingsButtonX, actionControlsY, true, !door.disableRightClick, door.noClip,
                    door.playPlaceSounds, resolveShiftBackMode(animation), resolveBlockMovementCollision(animation), (button) -> cacheSettings(button)));
            parent.controls.add(new GuiLittleVecXOverlayEventsButton("children_activate", eventsButtonX, actionControlsY, previews,
                    cachedEvents != null ? cachedEvents : door.events, (events) -> cacheEvents(events)));
            parent.controls.add(new GuiLittleVecXOverlaySignalEventsButton("signal", signalButtonX, actionControlsY, previews, structure, getStructureType()));
            parent.controls.add(new GuiStateButton("interpolation", door.interpolation, interpolationButtonX, actionControlsY, actionButtonWidth, compactButtonHeight,
                    ValueTimeline.interpolationTypes));

            GuiLabel durationLabel = new GuiLabel(CoreControl.translate("gui.door.duration") + ":", 0, footerRowY);
            int durationGroupWidth = durationLabel.width + 6 + smallFieldWidth;
            durationLabel.posX = layout.center(durationGroupWidth);
            int durationFieldX = durationLabel.posX + durationLabel.width + 6;
            parent.controls.add(durationLabel);
            parent.controls.add(new GuiTextfield("duration_s", Integer.toString(door.duration), durationFieldX, footerRowY, smallFieldWidth, compactButtonHeight - 1).setNumbersOnly());
            updateTimeline();
        }

        @SideOnly(Side.CLIENT)
        private void compactCheckpointDistanceField(GuiLTDistance distance) {
            int compactHeight = 13;
            int fieldWidth = 19;
            int gridWidth = fieldWidth;
            int gridX = fieldWidth + 9;
            int distanceX = gridX + gridWidth + 7;

            distance.width = distanceX + fieldWidth;
            distance.height = compactHeight;

            GuiTextfield blocks = (GuiTextfield) distance.get("blocks");
            blocks.posX = 0;
            blocks.posY = 0;
            blocks.width = fieldWidth;
            blocks.height = compactHeight;

            GuiStateButton grid = (GuiStateButton) distance.get("grid");
            grid.posX = gridX;
            grid.posY = 0;
            grid.width = gridWidth;
            grid.height = compactHeight;

            GuiTextfield gridDistance = (GuiTextfield) distance.get("ltdistance");
            gridDistance.posX = distanceX;
            gridDistance.posY = 0;
            gridDistance.width = fieldWidth;
            gridDistance.height = compactHeight;
        }

        @CustomEventSubscribe
        @SideOnly(Side.CLIENT)
        public void onChanged(GuiControlChangedEvent event) {
            if (event.source.is("keyDistance")) {
                if (selected == null || !selected.modifiable || isCheckpointSelected())
                    return;

                GuiLTDistance distance = (GuiLTDistance) event.source;
                LittleGridContext newContext = distance.getDistanceContext();
                if (newContext.size > context.size) {
                    int scale = newContext.size / context.size;
                    GuiTimeline timeline = (GuiTimeline) parent.get("timeline");
                    for (int i = 0; i < timeline.channels.size(); i++) {
                        TimelineChannel channel = timeline.channels.get(i);
                        if (i == CHECKPOINT_CHANNEL_INDEX)
                            continue;
                        if (channel instanceof TimelineChannelInteger) {
                            for (Object control : channel.controls)
                                ((KeyControl<Integer>) control).value *= scale;
                        }
                    }
                }

                context = newContext;
                selected.value = distance.getDistance();
            } else if (event.source.is("keyValue")) {
                if (selected == null || !selected.modifiable || isCheckpointSelected())
                    return;
                try {
                    selected.value = Double.parseDouble(((GuiTextfield) event.source).text);
                } catch (NumberFormatException ignored) {
                }
            } else if (event.source.is("keyPosition")) {
                if (selected == null || !selected.modifiable)
                    return;
                try {
                    GuiTimeline timeline = (GuiTimeline) parent.get("timeline");
                    int tick = selected.tick;
                    int newTick = Integer.parseInt(((GuiTextfield) event.source).text);
                    if (selected.channel.isSpaceFor(selected, newTick)) {
                        selected.tick = newTick;
                        selected.channel.movedKey(selected);
                        if (tick != selected.tick)
                            timeline.adjustKeysPositionX();
                    }
                } catch (NumberFormatException ignored) {
                }
            } else if (event.source.is("duration_s")) {
                try {
                    GuiTimeline timeline = (GuiTimeline) parent.get("timeline");
                    timeline.setDuration(Integer.parseInt(((GuiTextfield) event.source).text));
                } catch (NumberFormatException ignored) {
                }
            } else if (event.source.is("settings")) {
                cacheSettings((GuiLittleVecXOverlaySettingsButton) event.source);
            } else if (event.source.is("children_activate")) {
                cacheEvents(((GuiDoorEventsButton) event.source).events);
                updateTimeline();
            } else if (event.source.is("timeline") || event.source.is("interpolation"))
                updateTimeline();
        }

        @CustomEventSubscribe
        @SideOnly(Side.CLIENT)
        public void onKeySelected(KeySelectedEvent event) {
            GuiTextfield textfield = (GuiTextfield) parent.get("keyValue");
            GuiLTDistance distance = (GuiLTDistance) parent.get("keyDistance");
            selected = (KeyControl) event.source;

            if (isCheckpointSelected()) {
                distance.setEnabled(false);
                distance.setVisible(false);
                textfield.setEnabled(false);
                textfield.setVisible(false);
                textfield.text = "";
            } else if (((KeyControl) event.source).value instanceof Double) {
                distance.setVisible(false);
                textfield.setEnabled(true);
                textfield.setVisible(true);
                textfield.text = "" + selected.value;
            } else {
                distance.setEnabled(true);
                distance.setVisible(true);
                textfield.setVisible(false);
                distance.setDistance(context, (int) selected.value);
            }

            GuiTextfield position = (GuiTextfield) parent.get("keyPosition");
            position.setEnabled(true);
            position.text = "" + selected.tick;
        }

        @CustomEventSubscribe
        @SideOnly(Side.CLIENT)
        public void onKeyDeselected(KeyDeselectedEvent event) {
            selected = null;
            GuiTextfield textfield = (GuiTextfield) parent.get("keyValue");
            textfield.setEnabled(false);
            textfield.setVisible(true);
            textfield.text = "";
            textfield.setCursorPositionZero();

            textfield = (GuiTextfield) parent.get("keyPosition");
            textfield.setEnabled(false);
            textfield.text = "";
            textfield.setCursorPositionZero();

            GuiLTDistance distance = (GuiLTDistance) parent.get("keyDistance");
            distance.setEnabled(false);
            distance.resetTextfield();
            distance.setVisible(false);

            updateTimeline();
        }

        @CustomEventSubscribe
        @SideOnly(Side.CLIENT)
        public void toolTip(GuiToolTipEvent event) {
            if (event.source.is("timeline")) {
                ((GuiLabel) parent.get("tick")).setCaption(event.tooltip.get(0));
                event.CancelEvent();
            }
        }

        @SideOnly(Side.CLIENT)
        private void updateTimeline() {
            GuiTimeline timeline = (GuiTimeline) parent.get("timeline");
            GuiDoorEventsButton children = (GuiDoorEventsButton) parent.get("children_activate");
            AnimationTimeline animation = new AnimationTimeline(timeline.getDuration(), new PairList<>());
            GuiStateButton interpolationButton = (GuiStateButton) parent.get("interpolation");
            int interpolation = interpolationButton.getState();

            ValueTimeline rotX = ValueTimeline.create(interpolation, timeline.channels.get(0).getPairs());
            if (rotX != null)
                animation.values.add(AnimationKey.rotX, rotX);

            ValueTimeline rotY = ValueTimeline.create(interpolation, timeline.channels.get(1).getPairs());
            if (rotY != null)
                animation.values.add(AnimationKey.rotY, rotY);

            ValueTimeline rotZ = ValueTimeline.create(interpolation, timeline.channels.get(2).getPairs());
            if (rotZ != null)
                animation.values.add(AnimationKey.rotZ, rotZ);

            ValueTimeline offX = ValueTimeline.create(interpolation, timeline.channels.get(3).getPairs());
            if (offX != null)
                animation.values.add(AnimationKey.offX, offX.factor(context.pixelSize));

            ValueTimeline offY = ValueTimeline.create(interpolation, timeline.channels.get(4).getPairs());
            if (offY != null)
                animation.values.add(AnimationKey.offY, offY.factor(context.pixelSize));

            ValueTimeline offZ = ValueTimeline.create(interpolation, timeline.channels.get(5).getPairs());
            if (offZ != null)
                animation.values.add(AnimationKey.offZ, offZ.factor(context.pixelSize));

            handler.setTimeline(animation, children.events);

            GuiLittleVecXOverlaySettingsButton settings = (GuiLittleVecXOverlaySettingsButton) parent.get("settings");
            settings.stayAnimatedPossible = LittleAdvancedDoor.isAligned(AnimationKey.offX, offX)
                    && LittleAdvancedDoor.isAligned(AnimationKey.offY, offY)
                    && LittleAdvancedDoor.isAligned(AnimationKey.offZ, offZ)
                    && LittleAdvancedDoor.isAligned(AnimationKey.rotX, rotX)
                    && LittleAdvancedDoor.isAligned(AnimationKey.rotY, rotY)
                    && LittleAdvancedDoor.isAligned(AnimationKey.rotZ, rotZ);
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructure parseStructure(LittlePreviews previews) {
            StructureLittleVecXOverlayAnimation animation = createStructure(StructureLittleVecXOverlayAnimation.class, null);
            LittleAdvancedDoor door = parseAdvancedDoorFromControls(previews);

            GuiLittleVecXOverlaySettingsButton settings = (GuiLittleVecXOverlaySettingsButton) parent.get("settings");
            GuiDoorEventsButton eventsButton = (GuiDoorEventsButton) parent.get("children_activate");
            cacheSettings(settings);
            cacheEvents(eventsButton.events);
            animation.stayAnimated = true;
            animation.disableRightClick = cachedDisableRightClick != null ? cachedDisableRightClick : !settings.disableRightClick;
            animation.noClip = cachedNoClip != null ? cachedNoClip : settings.noClip;
            animation.playPlaceSounds = cachedPlayPlaceSounds != null ? cachedPlayPlaceSounds : settings.playPlaceSounds;
            animation.shiftRightClickStepBackMode = cachedShiftRightClickStepBackMode != null ? cachedShiftRightClickStepBackMode
                    : settings.shiftRightClickStepBackMode;
            animation.blockMovementCollision = cachedBlockMovementCollision != null ? cachedBlockMovementCollision : settings.blockMovementCollision;
            animation.layers = new ArrayList<>();
            if (door != null) {
                if (cachedEvents != null)
                    door.events = copyEvents(cachedEvents);
                LittleVecXAnimationLayer layer = new LittleVecXAnimationLayer();
                layer.name = "Overlay";
                layer.trigger = LittleVecXAnimationTriggerMode.NONE;
                LittleVecXAnimationLayerDoorHelper.applyParsedDoor(layer, door, previews);
                animation.layers.add(layer);
                animation.events = copyEvents(door.events);
                animation.interpolation = door.interpolation;
                animation.duration = door.duration;
                animation.offGrid = door.offGrid;
            } else {
                animation.events = new ArrayList<>();
                animation.interpolation = 0;
            }
            animation.currentLayerIndex = -1;
            animation.overlayStage = 0;
            animation.overlayResetting = false;
            animation.checkpointTicks = readCheckpointTicks(getCheckpointTicksFromTimeline());
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX overlay settings debug parse: disableRightClick={}, noClip={}, playPlaceSounds={}, shiftBack={}, doorEvents={}, checkpoints={}",
                    animation.disableRightClick, animation.noClip, animation.playPlaceSounds, animation.shiftRightClickStepBackMode,
                    door == null || door.events == null ? -1 : door.events.size(),
                    animation.checkpointTicks == null ? -1 : animation.checkpointTicks.size());
            animation.refreshCurrentLayerFields();
            if (animation.axisCenter == null)
                animation.axisCenter = new com.creativemd.littletiles.common.structure.relative.StructureRelative(defaultAxis(previews));
            return animation;
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructureType getStructureType() {
            return LittleStructureRegistry.getStructureType(StructureLittleVecXOverlayAnimation.class);
        }

        @SideOnly(Side.CLIENT)
        private boolean isCheckpointSelected() {
            return selected != null && selected.channel != null && selected.channel.index == CHECKPOINT_CHANNEL_INDEX;
        }

        @SideOnly(Side.CLIENT)
        private static PairList<Integer, Integer> toCheckpointPairs(@Nullable List<Integer> ticks) {
            PairList<Integer, Integer> pairs = new PairList<>();
            if (ticks == null)
                return pairs;
            for (Integer tick : ticks) {
                if (tick != null && tick >= 0)
                    pairs.add(tick, 1);
            }
            return pairs;
        }

        @SideOnly(Side.CLIENT)
        private int[] getCheckpointTicksFromTimeline() {
            GuiTimeline timeline = (GuiTimeline) parent.get("timeline");
            if (timeline == null || timeline.channels.size() <= CHECKPOINT_CHANNEL_INDEX)
                return new int[0];
            PairList<Integer, Integer> pairs = timeline.channels.get(CHECKPOINT_CHANNEL_INDEX).getPairs();
            int[] values = new int[pairs.size()];
            for (int i = 0; i < pairs.size(); i++)
                values[i] = pairs.get(i).key;
            return values;
        }

        @SideOnly(Side.CLIENT)
        @Nullable
        private LittleAdvancedDoor parseAdvancedDoorFromControls(LittlePreviews previews) {
            LittleAdvancedDoor door = createStructure(LittleAdvancedDoor.class, null);
            if (door == null)
                return null;

            GuiTileViewer viewer = ((GuiAxisButton) parent.get("axis")).viewer;
            GuiDoorEventsButton button = (GuiDoorEventsButton) parent.get("children_activate");
            GuiTimeline timeline = (GuiTimeline) parent.get("timeline");
            GuiLittleVecXOverlaySettingsButton settings = (GuiLittleVecXOverlaySettingsButton) parent.get("settings");
            GuiStateButton interpolationButton = (GuiStateButton) parent.get("interpolation");

            door.axisCenter = new com.creativemd.littletiles.common.structure.relative.StructureRelative(viewer.getBox(), viewer.getAxisContext());
            door.duration = timeline.getDuration();
            door.events = copyEvents(button.events);
            door.disableRightClick = !settings.disableRightClick;
            door.interpolation = interpolationButton.getState();
            door.rotX = ValueTimeline.create(door.interpolation, timeline.channels.get(0).getPairs());
            door.rotY = ValueTimeline.create(door.interpolation, timeline.channels.get(1).getPairs());
            door.rotZ = ValueTimeline.create(door.interpolation, timeline.channels.get(2).getPairs());
            door.offX = ValueTimeline.create(door.interpolation, timeline.channels.get(3).getPairs());
            door.offY = ValueTimeline.create(door.interpolation, timeline.channels.get(4).getPairs());
            door.offZ = ValueTimeline.create(door.interpolation, timeline.channels.get(5).getPairs());
            door.noClip = settings.noClip;
            if (!LittleAdvancedDoor.isAligned(AnimationKey.offX, door.offX)
                    || !LittleAdvancedDoor.isAligned(AnimationKey.offY, door.offY)
                    || !LittleAdvancedDoor.isAligned(AnimationKey.offZ, door.offZ)
                    || !LittleAdvancedDoor.isAligned(AnimationKey.rotX, door.rotX)
                    || !LittleAdvancedDoor.isAligned(AnimationKey.rotY, door.rotY)
                    || !LittleAdvancedDoor.isAligned(AnimationKey.rotZ, door.rotZ))
                door.stayAnimated = true;
            else
                door.stayAnimated = settings.stayAnimated;
            door.playPlaceSounds = settings.playPlaceSounds;
            door.offGrid = context;
            return door;
        }

        @SideOnly(Side.CLIENT)
        private static LittleAdvancedDoor buildEditingDoor(@Nullable StructureLittleVecXOverlayAnimation animation, LittlePreviews previews) {
            LittleAdvancedDoor door = null;
            if (animation != null && animation.layers != null && !animation.layers.isEmpty()) {
                LittleDoorBase base = LittleVecXAnimationLayerDoorHelper.createDoorForEdit(animation.layers.get(0), previews);
                if (base instanceof LittleAdvancedDoor)
                    door = (LittleAdvancedDoor) base;
            }
            if (door == null)
                door = (LittleAdvancedDoor) LittleStructureRegistry.getStructureType(LittleAdvancedDoor.class).createStructure(null);
            if (animation != null) {
                door.disableRightClick = animation.disableRightClick;
                door.noClip = animation.noClip;
                door.playPlaceSounds = animation.playPlaceSounds;
                door.stayAnimated = animation.stayAnimated;
                if (animation.layers != null && !animation.layers.isEmpty())
                    door.events = copyEvents(animation.layers.get(0).events);
            }
            if (door.axisCenter == null)
                door.axisCenter = new com.creativemd.littletiles.common.structure.relative.StructureRelative(defaultAxis(previews));
            if (door.offGrid == null)
                door.offGrid = LittleGridContext.get();
            if (door.duration <= 0)
                door.duration = 10;
            door.stayAnimated = true;
            return door;
        }

        @SideOnly(Side.CLIENT)
        private void applyCachedOverlayState(@Nullable StructureLittleVecXOverlayAnimation animation, LittleAdvancedDoor door) {
            if (cachedDisableRightClick != null)
                door.disableRightClick = cachedDisableRightClick;
            if (cachedNoClip != null)
                door.noClip = cachedNoClip;
            if (cachedPlayPlaceSounds != null)
                door.playPlaceSounds = cachedPlayPlaceSounds;
            if (cachedEvents != null)
                door.events = copyEvents(cachedEvents);
            if (animation != null && cachedShiftRightClickStepBackMode == null)
                cachedShiftRightClickStepBackMode = animation.shiftRightClickStepBackMode;
            if (animation != null && cachedBlockMovementCollision == null)
                cachedBlockMovementCollision = animation.blockMovementCollision;
        }

        @SideOnly(Side.CLIENT)
        private boolean resolveShiftBackMode(@Nullable StructureLittleVecXOverlayAnimation animation) {
            if (cachedShiftRightClickStepBackMode != null)
                return cachedShiftRightClickStepBackMode;
            return animation != null && animation.shiftRightClickStepBackMode;
        }

        @SideOnly(Side.CLIENT)
        private boolean resolveBlockMovementCollision(@Nullable StructureLittleVecXOverlayAnimation animation) {
            if (cachedBlockMovementCollision != null)
                return cachedBlockMovementCollision;
            return animation != null && animation.blockMovementCollision;
        }

        @SideOnly(Side.CLIENT)
        private void cacheSettings(GuiLittleVecXOverlaySettingsButton settings) {
            cachedDisableRightClick = !settings.disableRightClick;
            cachedNoClip = settings.noClip;
            cachedPlayPlaceSounds = settings.playPlaceSounds;
            cachedShiftRightClickStepBackMode = settings.shiftRightClickStepBackMode;
            cachedBlockMovementCollision = settings.blockMovementCollision;
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                    "LittleVecX overlay settings debug cacheSettings: disableRightClick={}, noClip={}, playPlaceSounds={}, shiftBack={}, blockCollision={}",
                    cachedDisableRightClick, cachedNoClip, cachedPlayPlaceSounds, cachedShiftRightClickStepBackMode, cachedBlockMovementCollision);
        }

        @SideOnly(Side.CLIENT)
        private void cacheEvents(@Nullable List<com.creativemd.littletiles.common.structure.animation.event.AnimationEvent> events) {
            cachedEvents = copyEvents(events);
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX overlay settings debug cacheEvents: events={}", cachedEvents == null ? -1 : cachedEvents.size());
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

