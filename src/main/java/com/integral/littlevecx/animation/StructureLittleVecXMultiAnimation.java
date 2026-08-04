package com.integral.littlevecx.animation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.world.SubWorld;
import com.creativemd.creativecore.common.utils.type.PairList;
import com.creativemd.creativecore.common.utils.type.UUIDSupplier;
import com.creativemd.littletiles.client.render.world.LittleRenderChunkSuppilier;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDoorSettings.GuiDoorSettingsButton;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.action.block.LittleActionActivated;
import com.creativemd.littletiles.common.entity.DoorController;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.structure.animation.AnimationKey;
import com.creativemd.littletiles.common.structure.animation.AnimationState;
import com.creativemd.littletiles.common.packet.LittleAnimationDataPacket;
import com.creativemd.littletiles.common.packet.LittleAnimationControllerPacket;
import com.creativemd.littletiles.common.structure.animation.AnimationGuiHandler;
import com.creativemd.littletiles.common.structure.animation.AnimationTimeline;
import com.creativemd.littletiles.common.structure.animation.ValueTimeline;
import com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException;
import com.creativemd.littletiles.common.structure.exception.NotYetConnectedException;
import com.creativemd.littletiles.common.structure.registry.LittleStructureGuiParser;
import com.creativemd.littletiles.common.structure.registry.LittleStructureRegistry;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.signal.output.InternalSignalOutput;
import com.creativemd.littletiles.common.structure.relative.StructureAbsolute;
import com.creativemd.littletiles.common.structure.relative.StructureRelative;
import com.creativemd.littletiles.common.structure.type.door.LittleAdvancedDoor;
import com.creativemd.littletiles.common.structure.type.door.LittleDoor.DoorActivator;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.vec.LittleAbsoluteVec;
import com.creativemd.littletiles.common.tile.parent.IStructureTileList;
import com.creativemd.littletiles.common.tile.preview.LittleAbsolutePreviews;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.place.Placement;
import com.creativemd.littletiles.common.util.place.PlacementHelper;
import com.creativemd.littletiles.common.util.place.PlacementMode;
import com.creativemd.littletiles.common.util.place.PlacementResult;
import com.integral.littlevecx.LittleVecXConfig;
import com.integral.littlevecx.client.gui.GuiLittleVecXAnimationLayersButton;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class StructureLittleVecXMultiAnimation extends LittleAdvancedDoor {

    private static final Logger LOGGER = LogManager.getLogger("LittleVecX");
    private static final String LAYERS_TAG = "littlevecxLayers";
    private static final String ACTIVE_LAYER_TAG = "littlevecxActiveLayer";
    private static final String PENDING_SIGNAL_CLOSE_LAYER_TAG = "littlevecxPendingSignalCloseLayer";
    public List<LittleVecXAnimationLayer> layers = new ArrayList<>();
    public int currentLayerIndex = -1;
    /** A pulse ended before its opening animation could finish. */
    protected int pendingSignalCloseLayerIndex = -1;
    private transient boolean signalActivationInProgress;
    @Nullable
    protected LittleVecXAnimationLayer runtimeLayerOverride = null;

    public StructureLittleVecXMultiAnimation(LittleStructureType type, IStructureTileList mainBlock) {
        super(type, mainBlock);
    }

    @Override
    protected void loadFromNBTExtra(NBTTagCompound nbt) {
        super.loadFromNBTExtra(nbt);
        events = new ArrayList<>();
        layers = new ArrayList<>();

        NBTTagList list = nbt.getTagList(LAYERS_TAG, 10);
        for (int i = 0; i < list.tagCount(); i++)
            layers.add(LittleVecXAnimationLayer.readFromNBT(list.getCompoundTagAt(i)));

        currentLayerIndex = nbt.hasKey(ACTIVE_LAYER_TAG) ? nbt.getInteger(ACTIVE_LAYER_TAG) : -1;
        pendingSignalCloseLayerIndex = nbt.hasKey(PENDING_SIGNAL_CLOSE_LAYER_TAG)
                ? nbt.getInteger(PENDING_SIGNAL_CLOSE_LAYER_TAG) : -1;
        refreshCurrentLayerFields();
    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {
        refreshCurrentLayerFields();
        events = new ArrayList<>();
        super.writeToNBTExtra(nbt);

        NBTTagList list = new NBTTagList();
        for (LittleVecXAnimationLayer layer : layers)
            list.appendTag(layer.writeToNBT());
        nbt.setTag(LAYERS_TAG, list);

        if (currentLayerIndex >= 0)
            nbt.setInteger(ACTIVE_LAYER_TAG, currentLayerIndex);
        else
            nbt.removeTag(ACTIVE_LAYER_TAG);
        if (isValidLayerIndex(pendingSignalCloseLayerIndex))
            nbt.setInteger(PENDING_SIGNAL_CLOSE_LAYER_TAG, pendingSignalCloseLayerIndex);
        else
            nbt.removeTag(PENDING_SIGNAL_CLOSE_LAYER_TAG);
    }

    @Override
    public NBTTagCompound writeToNBTPreview(NBTTagCompound nbt, BlockPos pos) {
        NBTTagCompound preview = super.writeToNBTPreview(nbt, pos);
        if (preview.hasKey("state", 10))
            preview.getCompoundTag("state").setInteger("state", getPrimaryStateOutputValue() ? 1 : 0);
        return preview;
    }

    @Override
    public void transformDoorPreview(LittleAbsolutePreviews previews) {
        refreshCurrentLayerFields();
        super.transformDoorPreview(previews);
    }

    @Override
    public DoorController createController(UUIDSupplier supplier,
            com.creativemd.littletiles.common.util.place.Placement placement, int totalDuration) {
        if (runtimeLayerOverride != null)
            applyLayer(runtimeLayerOverride);
        else if (isValidLayerIndex(currentLayerIndex))
            applyLayer(layers.get(currentLayerIndex));
        else
            refreshCurrentLayerFields();
        return super.createController(supplier, placement, totalDuration);
    }

    @Override
    public StructureAbsolute getAbsoluteAxis() {
        try {
            LittleAbsolutePreviews previews = getDoorPreviews();
            StructureAbsolute runtimeAbsolute = getRuntimeAbsoluteAxis(previews);
            if (runtimeAbsolute != null)
                return runtimeAbsolute;
        } catch (CorruptedConnectionException | NotYetConnectedException | RuntimeException ignored) {
        }
        return super.getAbsoluteAxis();
    }

    @Override
    public EntityAnimation activate(DoorActivator activator, EntityPlayer player, UUID uuid) throws LittleActionException {
        int requestedLayer = resolveRequestedLayerIndex(activator);
        if (!opened && requestedLayer < 0
                && (activator == DoorActivator.RIGHTCLICK || activator == DoorActivator.COMMAND))
            return null;
        return activateLayerIndex(requestedLayer, activator, player, uuid);
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
    public void finishAnimation(EntityAnimation animation) {
        super.finishAnimation(animation);
        afterFinishAnimation(animation, getAnimatedStructureTarget());
    }

    @Override
    public void completeAnimation() {
        super.completeAnimation();
    }

    @Override
    public void performInternalOutputChange(InternalSignalOutput output) {
        String identifier = output.component.identifier;
        // Layers are controlled exclusively through animation_N. The base door's
        // state callback resolves DoorActivator.SIGNAL to the default (first)
        // layer, which must never happen for a layer-specific signal.
        if ("state".equals(identifier))
            return;

        int layerIndex = getSignalLayerIndex(identifier);
        if (layerIndex < 0 || !isValidLayerIndex(layerIndex))
            return;

        boolean[] state = output.getState();
        if (state == null || state.length == 0)
            return;

        StructureLittleVecXMultiAnimation target = getAnimatedStructureTarget();
        syncOpenedFromLiveController(target, getLiveAnimation(target));
        boolean liveAnimationChanging = isLiveAnimationChanging(target);
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX multi signal: identifier={}, state={}, layer={}, opened={}, moving={}, activeLayer={}",
                identifier, state[0], layerIndex, opened, liveAnimationChanging, getCurrentAnimatedLayer(target));

        try {
            if (state[0]) {
                clearPendingSignalClose(target, layerIndex);
                if (liveAnimationChanging)
                    return;
                if (!opened) {
                    signalActivationInProgress = true;
                    try {
                        activateLayerIndex(layerIndex, DoorActivator.SIGNAL, null, null);
                    } finally {
                        signalActivationInProgress = false;
                    }
                }
                return;
            }

            int activeLayer = getCurrentAnimatedLayer(target);
            if (liveAnimationChanging) {
                if (activeLayer == layerIndex || activeLayer < 0)
                    setPendingSignalClose(target, layerIndex);
                return;
            }
            if (opened && (activeLayer == layerIndex || activeLayer < 0))
                closeLiveSignalAnimation(target);
        } catch (LittleActionException ignored) {
        }
    }

    protected EntityAnimation activateLayerIndex(int requestedLayer, DoorActivator activator, EntityPlayer player, UUID uuid) throws LittleActionException {
        StructureLittleVecXMultiAnimation target = getAnimatedStructureTarget();
        EntityAnimation liveAnimation = getLiveAnimation(target);
        syncOpenedFromLiveController(target, liveAnimation);

        int activeLayer = getCurrentAnimatedLayer(target);
        if (opened && activeLayer >= 0)
            requestedLayer = activeLayer;
        else if (requestedLayer < 0)
            requestedLayer = getDefaultLayerIndex();

        if (opened) {
            if (requestedLayer < 0)
                requestedLayer = getDefaultLayerIndex();

            if (requestedLayer < 0) {
                refreshCurrentLayerFields();
                return super.activate(activator, player, uuid);
            }

            applyLayer(layers.get(requestedLayer));
            syncRequestedAnimatedState(target, requestedLayer, false);
            return super.activate(activator, player, uuid);
        }

        if (requestedLayer < 0) {
            refreshCurrentLayerFields();
            return super.activate(activator, player, uuid);
        }

        rebuildPersistentAnimationLayer(requestedLayer, player);
        syncOpenedFromLiveController(target, getLiveAnimation(target));

        if (!opened) {
            currentLayerIndex = requestedLayer;
            applyLayer(layers.get(requestedLayer));
            syncRequestedAnimatedState(target, requestedLayer, true);
            return super.activate(activator, player, uuid);
        }

        return super.activate(activator, player, uuid);
    }

    @Override
    public EntityAnimation openDoor(@Nullable EntityPlayer player, UUIDSupplier uuid, boolean tickOnce) throws LittleActionException {
        // LittleDoor.activate() reloads the recipe before reaching this method.
        // The flag therefore has to be set here, immediately before the
        // controller is built, rather than when the signal is first received.
        if (signalActivationInProgress)
            stayAnimated = true;
        return super.openDoor(player, uuid, tickOnce);
    }

    protected int getCurrentAnimatedLayer(@Nullable StructureLittleVecXMultiAnimation target) {
        if (target != null && isValidLayerIndex(target.currentLayerIndex))
            return target.currentLayerIndex;
        if (isValidLayerIndex(currentLayerIndex))
            return currentLayerIndex;
        return -1;
    }

    protected int resolveRequestedLayerIndex(DoorActivator activator) {
        if (activator == DoorActivator.RIGHTCLICK)
            return findFirstLayerIndex(LittleVecXAnimationTriggerMode.RIGHT_CLICK);
        if (activator == DoorActivator.COMMAND)
            return findFirstLayerIndex(LittleVecXAnimationTriggerMode.SHIFT_RIGHT_CLICK);
        if (activator == DoorActivator.SIGNAL)
            return getDefaultLayerIndex();
        return getDefaultLayerIndex();
    }

    protected int getDefaultLayerIndex() {
        int rightClick = findFirstLayerIndex(LittleVecXAnimationTriggerMode.RIGHT_CLICK);
        if (rightClick >= 0)
            return rightClick;
        return layers.isEmpty() ? -1 : 0;
    }

    protected int findFirstLayerIndex(LittleVecXAnimationTriggerMode mode) {
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i).trigger == mode)
                return i;
        }
        return -1;
    }

    protected boolean isValidLayerIndex(int index) {
        return index >= 0 && index < layers.size();
    }

    protected boolean rebuildPersistentAnimationLayer(int requestedLayer, @Nullable EntityPlayer player) {
        return rebuildPersistentAnimationLayer(requestedLayer, null, player);
    }

    protected boolean rebuildPersistentAnimationLayer(int requestedLayer, @Nullable LittleVecXAnimationLayer overrideLayer, @Nullable EntityPlayer player) {
        if (!stayAnimated || (overrideLayer == null && !isValidLayerIndex(requestedLayer)))
            return false;

        StructureLittleVecXMultiAnimation target = getAnimatedStructureTarget();
        EntityAnimation liveAnimation = getLiveAnimation(target);
        if (target == null || liveAnimation == null || liveAnimation.controller == null || liveAnimation.controller.isChanging()
                || !(liveAnimation.controller instanceof DoorController))
            return false;

        LittleVecXAnimationLayer layer = overrideLayer != null ? overrideLayer : layers.get(requestedLayer);
        StructureLittleVecXMultiAnimation previewSource = getRebuildPreviewSource(target, liveAnimation);
        currentLayerIndex = overrideLayer == null ? requestedLayer : -1;
        runtimeLayerOverride = overrideLayer;
        applyLayer(layer);
        if (target != this) {
            target.currentLayerIndex = overrideLayer == null ? requestedLayer : -1;
            target.runtimeLayerOverride = overrideLayer;
            target.applyLayer(layer);
        }

        try {
            LittleAbsolutePreviews previews = previewSource.getDoorPreviews();
            World world = getRebuildPlacementWorld(previewSource, target, liveAnimation);
            if (world == null) {
                runtimeLayerOverride = null;
                if (target != this)
                    target.runtimeLayerOverride = null;
                return false;
            }
            SubWorld tempWorld = SubWorld.createFakeWorld(world);
            if (world.isRemote)
                tempWorld.renderChunkSupplier = new LittleRenderChunkSuppilier();
            Placement placement = new Placement(player,
                    PlacementHelper.getAbsolutePreviews(tempWorld, previews, previews.pos, PlacementMode.all))
                            .setIgnoreWorldBoundaries(false);
            PlacementResult placementResult = placement.tryPlace();
            if (placementResult == null || placement.origin == null || placement.origin.getStructure() == null) {
                runtimeLayerOverride = null;
                if (target != this)
                    target.runtimeLayerOverride = null;
                refreshCurrentLayerFields();
                return false;
            }

            DoorController oldController = (DoorController) liveAnimation.controller;
            UUIDSupplier supplier = oldController.supplier != null ? oldController.supplier : new UUIDSupplier(UUID.randomUUID());
            DoorController newController = target.createController(supplier, placement, target.getCompleteDuration());
            newController.activator = player;
            newController.noClip = target.noClip;

            liveAnimation.controller = newController;
            newController.setParent(liveAnimation);
            StructureAbsolute runtimeAbsolute = target.getRuntimeAbsoluteAxis(previews);
            StructureAbsolute nextAbsolute = runtimeAbsolute != null ? runtimeAbsolute : target.getAbsoluteAxis();
            if (!sameStructureAbsolute(liveAnimation.center, nextAbsolute))
                liveAnimation.setCenter(nextAbsolute);
            runtimeLayerOverride = null;
            if (target != this)
                target.runtimeLayerOverride = null;
            prepareRebuiltController(oldController, newController, liveAnimation);
            syncNestedAnimatedChildrenToLiveAnimation(liveAnimation);

            liveAnimation.updateTickState();
            liveAnimation.updateBoundingBox();

            if (!isClient() && shouldSyncRebuiltAnimationImmediately()) {
                PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationDataPacket(liveAnimation), liveAnimation, null);
                PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationControllerPacket(liveAnimation), liveAnimation, null);
            }
            return true;
        } catch (CorruptedConnectionException | NotYetConnectedException | RuntimeException ex) {
            runtimeLayerOverride = null;
            if (target != this)
                target.runtimeLayerOverride = null;
            refreshCurrentLayerFields();
            return false;
        }
    }

    protected StructureLittleVecXMultiAnimation getRebuildPreviewSource(@Nullable StructureLittleVecXMultiAnimation target,
            @Nullable EntityAnimation liveAnimation) {
        return target != null ? target : this;
    }

    @Nullable
    protected World getRebuildPlacementWorld(StructureLittleVecXMultiAnimation previewSource,
            @Nullable StructureLittleVecXMultiAnimation target, @Nullable EntityAnimation liveAnimation) {
        return previewSource == null ? null : previewSource.getWorld();
    }

    @Nullable
    protected StructureLittleVecXMultiAnimation getAnimatedStructureTarget() {
        if (animation != null && animation.structure instanceof StructureLittleVecXMultiAnimation)
            return (StructureLittleVecXMultiAnimation) animation.structure;
        return this;
    }

    @Nullable
    protected EntityAnimation getLiveAnimation(@Nullable StructureLittleVecXMultiAnimation target) {
        if (target != null && target.animation != null)
            return target.animation;
        return animation;
    }

    protected void syncRequestedAnimatedState(@Nullable StructureLittleVecXMultiAnimation target, int requestedLayer, boolean nextOpened) {
        if (target == null || target == this)
            return;

        target.currentLayerIndex = requestedLayer;
        target.opened = nextOpened;
        if (isValidLayerIndex(requestedLayer))
            target.applyLayer(target.layers.get(requestedLayer));
        else
            target.refreshCurrentLayerFields();
    }

    protected void syncMirroredAnimatedState(@Nullable StructureLittleVecXMultiAnimation target) {
        if (target == null || target == this)
            return;

        target.opened = opened;
        target.currentLayerIndex = currentLayerIndex;
        if (isValidLayerIndex(currentLayerIndex))
            target.applyLayer(target.layers.get(currentLayerIndex));
        else
            target.refreshCurrentLayerFields();
    }

    protected void syncOpenedFromLiveController(@Nullable StructureLittleVecXMultiAnimation target, @Nullable EntityAnimation liveAnimation) {
        if (liveAnimation == null || !(liveAnimation.controller instanceof DoorController))
            return;

        DoorController controller = (DoorController) liveAnimation.controller;
        if (controller.isChanging())
            return;

        boolean controllerOpened = isControllerOpened(controller);
        opened = controllerOpened;

        if (target != null)
            target.opened = controllerOpened;
    }

    protected boolean isControllerOpened(@Nullable DoorController controller) {
        if (controller == null || controller.getCurrentState() == null)
            return false;
        return DoorController.openedState.equals(controller.getCurrentState().name);
    }

    protected void syncPrimaryStateOutput(boolean openedState, @Nullable StructureLittleVecXMultiAnimation target) {
        syncPrimaryStateOutputOn(this, openedState);
        if (target != null && target != this)
            syncPrimaryStateOutputOn(target, openedState);
    }

    protected void syncPrimaryStateOutputOn(StructureLittleVecXMultiAnimation structure, boolean openedState) {
        try {
            InternalSignalOutput stateOutput = structure.getOutput(0);
            if (stateOutput == null)
                return;
            boolean[] outputState = stateOutput.getState();
            if (outputState != null && outputState.length > 0)
                outputState[0] = openedState;
        } catch (Exception ignored) {
        }
    }

    protected boolean getPrimaryStateOutputValue() {
        try {
            InternalSignalOutput stateOutput = getOutput(0);
            if (stateOutput != null) {
                boolean[] outputState = stateOutput.getState();
                if (outputState != null && outputState.length > 0)
                    return outputState[0];
            }
        } catch (Exception ignored) {
        }
        return opened;
    }

    protected static int getSignalLayerIndex(@Nullable String identifier) {
        if (identifier == null || !identifier.startsWith("animation_"))
            return -1;

        try {
            int index = Integer.parseInt(identifier.substring("animation_".length()));
            return index >= 0 && index < LittleVecXConfig.multiAnimationSignalCount ? index : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public void refreshCurrentLayerFields() {
        if (runtimeLayerOverride != null) {
            applyLayer(runtimeLayerOverride);
            return;
        }
        int index = isValidLayerIndex(currentLayerIndex) ? currentLayerIndex : getDefaultLayerIndex();
        applyLayer(isValidLayerIndex(index) ? layers.get(index) : null);
    }

    protected void applyLayer(@Nullable LittleVecXAnimationLayer layer) {
        if (layer == null) {
            duration = 1;
            interpolation = 0;
            events = new ArrayList<>();
            axisCenter = axisCenter != null ? new StructureRelative(axisCenter.write()) : createFallbackAxisCenter();
            rotX = null;
            rotY = null;
            rotZ = null;
            offX = null;
            offY = null;
            offZ = null;
            offGrid = LittleGridContext.get();
            return;
        }

        duration = layer.getSafeDuration();
        interpolation = layer.interpolation;
        events = copyEvents(layer.events);
        try {
            offGrid = LittleGridContext.get(layer.getSafeOffGrid());
        } catch (RuntimeException e) {
            offGrid = LittleGridContext.get(LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID);
        }

        StructureRelative mirrorAxis = LittleVecXAnimationLayerDoorHelper.extractAxisCenter(layer);
        axisCenter = mirrorAxis != null ? mirrorAxis : createFallbackAxisCenter();

        rotX = copyTimeline(layer.rotX);
        rotY = copyTimeline(layer.rotY);
        rotZ = copyTimeline(layer.rotZ);
        offX = copyTimeline(layer.offX);
        offY = copyTimeline(layer.offY);
        offZ = copyTimeline(layer.offZ);
    }

    protected void afterFinishAnimation(EntityAnimation animation, @Nullable StructureLittleVecXMultiAnimation target) {
        syncOpenedFromLiveController(target, animation);
        if (!opened)
            currentLayerIndex = -1;
        syncMirroredAnimatedState(target);
        finishPendingSignalClose(target);
    }

    private void finishPendingSignalClose(@Nullable StructureLittleVecXMultiAnimation target) {
        int pendingLayer = getPendingSignalClose(target);

        if (!isValidLayerIndex(pendingLayer))
            return;

        clearPendingSignalClose(target, pendingLayer);

        int activeLayer = getCurrentAnimatedLayer(target);
        if (!opened || (activeLayer >= 0 && activeLayer != pendingLayer))
            return;

        closeLiveSignalAnimation(target);
    }

    private boolean isLiveAnimationChanging(@Nullable StructureLittleVecXMultiAnimation target) {
        EntityAnimation liveAnimation = getLiveAnimation(target);
        return liveAnimation != null && liveAnimation.controller != null && liveAnimation.controller.isChanging();
    }

    private void setPendingSignalClose(@Nullable StructureLittleVecXMultiAnimation target, int layerIndex) {
        pendingSignalCloseLayerIndex = layerIndex;
        if (target != null && target != this)
            target.pendingSignalCloseLayerIndex = layerIndex;
    }

    private int getPendingSignalClose(@Nullable StructureLittleVecXMultiAnimation target) {
        if (target != null && target != this && isValidLayerIndex(target.pendingSignalCloseLayerIndex))
            return target.pendingSignalCloseLayerIndex;
        return pendingSignalCloseLayerIndex;
    }

    private void clearPendingSignalClose(@Nullable StructureLittleVecXMultiAnimation target, int layerIndex) {
        if (pendingSignalCloseLayerIndex == layerIndex)
            pendingSignalCloseLayerIndex = -1;
        if (target != null && target != this && target.pendingSignalCloseLayerIndex == layerIndex)
            target.pendingSignalCloseLayerIndex = -1;
    }

    /**
     * Closing an already animated layer must use the controller that owns its
     * reverse timeline. Re-entering LittleDoor.activate() may rebuild a door
     * and resolve SIGNAL to the default layer instead.
     */
    private void closeLiveSignalAnimation(@Nullable StructureLittleVecXMultiAnimation target) {
        EntityAnimation liveAnimation = getLiveAnimation(target);
        if (liveAnimation == null || !(liveAnimation.controller instanceof DoorController))
            return;

        DoorController controller = (DoorController) liveAnimation.controller;
        if (controller.isChanging() || !isControllerOpened(controller))
            return;

        opened = false;
        if (target != null && target != this)
            target.opened = false;

        boolean transitionStarted = controller.activate();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX multi pulse close: started={}, changing={}, currentState={}, opened={}",
                transitionStarted, controller.isChanging(),
                controller.getCurrentState() == null ? null : controller.getCurrentState().name, opened);
        if (transitionStarted && !isClient())
            PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationControllerPacket(liveAnimation), liveAnimation, null);
    }

    protected void prepareRebuiltController(DoorController oldController, DoorController newController, EntityAnimation liveAnimation) {
        if (isControllerOpened(oldController)) {
            newController.startTransition(DoorController.openedState);
            newController.endTransition();
        }
    }

    protected void syncNestedAnimatedChildrenToLiveAnimation(@Nullable EntityAnimation liveAnimation) {
        if (liveAnimation == null || !(liveAnimation.structure instanceof LittleStructure))
            return;

        try {
            ((LittleStructure) liveAnimation.structure).transferChildrenToAnimation(liveAnimation);
        } catch (CorruptedConnectionException | NotYetConnectedException | RuntimeException ignored) {
        }
    }

    protected boolean shouldSyncRebuiltAnimationImmediately() {
        return true;
    }

    @Nullable
    protected StructureAbsolute getRuntimeAbsoluteAxis(@Nullable LittleAbsolutePreviews previews) {
        StructureRelative runtimeAxis = getCurrentLayerRuntimeAxis(previews);
        if (runtimeAxis == null || previews == null)
            return null;
        return new StructureAbsolute(previews.pos, runtimeAxis.getBox().copy(), runtimeAxis.getContext());
    }

    @Nullable
    protected StructureAbsolute getRuntimeAbsoluteAxisDirect() {
        StructureRelative runtimeAxis = getCurrentLayerRuntimeAxis(null);
        if (runtimeAxis == null)
            return null;
        return new StructureAbsolute(new LittleAbsoluteVec(getPos(), mainBlock.getContext()), runtimeAxis);
    }

    protected DoorController createRuntimeControllerWithoutPlacement(UUIDSupplier supplier, int completeDuration) {
        int runtimeDuration = duration;

        PairList<com.creativemd.littletiles.common.structure.animation.AnimationKey, ValueTimeline> open = new PairList<>();
        PairList<com.creativemd.littletiles.common.structure.animation.AnimationKey, ValueTimeline> close = new PairList<>();

        AnimationState openedState = new AnimationState();
        AnimationState closedState = new AnimationState();

        if (offX != null) {
            openedState.set(AnimationKey.offX, offGrid.toVanillaGrid(offX.last(AnimationKey.offX)));
            closedState.set(AnimationKey.offX, offGrid.toVanillaGrid(offX.first(AnimationKey.offX)));
            open.add(AnimationKey.offX, offX.copy().factor(offGrid.pixelSize));
            close.add(AnimationKey.offX, offX.invert(runtimeDuration).factor(offGrid.pixelSize));
        }
        if (offY != null) {
            openedState.set(AnimationKey.offY, offGrid.toVanillaGrid(offY.last(AnimationKey.offY)));
            closedState.set(AnimationKey.offY, offGrid.toVanillaGrid(offY.first(AnimationKey.offY)));
            open.add(AnimationKey.offY, offY.copy().factor(offGrid.pixelSize));
            close.add(AnimationKey.offY, offY.invert(runtimeDuration).factor(offGrid.pixelSize));
        }
        if (offZ != null) {
            openedState.set(AnimationKey.offZ, offGrid.toVanillaGrid(offZ.last(AnimationKey.offZ)));
            closedState.set(AnimationKey.offZ, offGrid.toVanillaGrid(offZ.first(AnimationKey.offZ)));
            open.add(AnimationKey.offZ, offZ.copy().factor(offGrid.pixelSize));
            close.add(AnimationKey.offZ, offZ.invert(runtimeDuration).factor(offGrid.pixelSize));
        }
        if (rotX != null) {
            openedState.set(AnimationKey.rotX, rotX.last(AnimationKey.rotX));
            closedState.set(AnimationKey.rotX, rotX.first(AnimationKey.rotX));
            open.add(AnimationKey.rotX, rotX);
            close.add(AnimationKey.rotX, rotX.invert(runtimeDuration));
        }
        if (rotY != null) {
            openedState.set(AnimationKey.rotY, rotY.last(AnimationKey.rotY));
            closedState.set(AnimationKey.rotY, rotY.first(AnimationKey.rotY));
            open.add(AnimationKey.rotY, rotY);
            close.add(AnimationKey.rotY, rotY.invert(runtimeDuration));
        }
        if (rotZ != null) {
            openedState.set(AnimationKey.rotZ, rotZ.last(AnimationKey.rotZ));
            closedState.set(AnimationKey.rotZ, rotZ.first(AnimationKey.rotZ));
            open.add(AnimationKey.rotZ, rotZ);
            close.add(AnimationKey.rotZ, rotZ.invert(runtimeDuration));
        }

        return new DoorController(supplier, closedState, openedState, stayAnimated ? null : false, runtimeDuration,
                completeDuration, new AnimationTimeline(runtimeDuration, open), new AnimationTimeline(runtimeDuration, close),
                interpolation);
    }

    @Nullable
    protected StructureRelative getCurrentLayerRuntimeAxis(@Nullable LittlePreviews previews) {
        if (runtimeLayerOverride != null)
            return LittleVecXAnimationLayerDoorHelper.resolveAxisCenter(runtimeLayerOverride, previews);
        int index = isValidLayerIndex(currentLayerIndex) ? currentLayerIndex : getDefaultLayerIndex();
        if (!isValidLayerIndex(index))
            return null;
        return LittleVecXAnimationLayerDoorHelper.resolveAxisCenter(layers.get(index), previews);
    }

    private StructureRelative createFallbackAxisCenter() {
        return new StructureRelative(new LittleBox(0, 0, 0, 1, 1, 1), mainBlock != null ? mainBlock.getContext() : LittleGridContext.get());
    }

    @Nullable
    private static ValueTimeline copyTimeline(@Nullable ValueTimeline timeline) {
        return timeline == null ? null : timeline.copy();
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

    protected static boolean sameStructureAbsolute(@Nullable StructureAbsolute first, @Nullable StructureAbsolute second) {
        if (first == second)
            return true;
        if (first == null || second == null)
            return false;
        if (!first.baseOffset.equals(second.baseOffset))
            return false;
        if (first.getContext().size != second.getContext().size)
            return false;
        return Arrays.equals(first.getBox().getArray(), second.getBox().getArray());
    }

    public static class StructureLittleVecXMultiAnimationParser extends LittleStructureGuiParser {

        public StructureLittleVecXMultiAnimationParser(GuiParent parent, AnimationGuiHandler handler) {
            super(parent, handler);
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected void createControls(LittlePreviews previews, @Nullable LittleStructure structure) {
            StructureLittleVecXMultiAnimation animation = structure instanceof StructureLittleVecXMultiAnimation ? (StructureLittleVecXMultiAnimation) structure : null;

            boolean stayAnimated = animation != null && animation.stayAnimated;
            boolean enableRightClick = animation == null || !animation.disableRightClick;
            boolean noClip = animation != null && animation.noClip;
            boolean playPlaceSounds = animation == null || animation.playPlaceSounds;

            parent.controls.add(new GuiLittleVecXAnimationLayersButton("animation_layers", 0, 0, animation != null ? animation.layers : null, previews));
            parent.controls.add(new GuiDoorSettingsButton("settings", 130, 93, stayAnimated, enableRightClick, noClip, playPlaceSounds));

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
            StructureLittleVecXMultiAnimation animation = createStructure(StructureLittleVecXMultiAnimation.class, null);

            GuiDoorSettingsButton settings = (GuiDoorSettingsButton) parent.get("settings");
            animation.stayAnimated = settings.stayAnimated;
            animation.disableRightClick = !settings.disableRightClick;
            animation.noClip = settings.noClip;
            animation.playPlaceSounds = settings.playPlaceSounds;

            GuiLittleVecXAnimationLayersButton layersButton = (GuiLittleVecXAnimationLayersButton) parent.get("animation_layers");
            animation.layers = layersButton != null ? layersButton.getLayersCopy() : new ArrayList<>();
            animation.events = new ArrayList<>();
            animation.interpolation = 0;
            animation.currentLayerIndex = -1;
            animation.refreshCurrentLayerFields();
            if (animation.axisCenter == null)
                animation.axisCenter = new StructureRelative(defaultAxis(previews));
            return animation;
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructureType getStructureType() {
            return LittleStructureRegistry.getStructureType(StructureLittleVecXMultiAnimation.class);
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
