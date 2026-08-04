package com.integral.littlevecx.animation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.utils.type.Pair;
import com.creativemd.creativecore.common.utils.type.PairList;
import com.creativemd.creativecore.common.utils.type.UUIDSupplier;
import com.creativemd.creativecore.common.world.SubWorld;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiSignalEvents.GuiSignalEvent;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.action.block.LittleActionActivated;
import com.creativemd.littletiles.common.entity.DoorController;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.packet.LittleAnimationControllerPacket;
import com.creativemd.littletiles.common.packet.LittleAnimationDataPacket;
import com.creativemd.littletiles.common.structure.IAnimatedStructure;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.connection.StructureChildConnection;
import com.creativemd.littletiles.common.structure.animation.AnimationGuiHandler;
import com.creativemd.littletiles.common.structure.animation.AnimationTimeline;
import com.creativemd.littletiles.common.structure.animation.ValueTimeline;
import com.creativemd.littletiles.common.structure.animation.event.AnimationEvent;
import com.creativemd.littletiles.common.structure.animation.event.PlaySoundEvent;
import com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException;
import com.creativemd.littletiles.common.structure.exception.NotYetConnectedException;
import com.creativemd.littletiles.common.structure.registry.LittleStructureGuiParser;
import com.creativemd.littletiles.common.structure.registry.LittleStructureRegistry;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType.InternalComponentOutput;
import com.creativemd.littletiles.common.structure.relative.StructureAbsolute;
import com.creativemd.littletiles.common.structure.signal.component.ISignalComponent;
import com.creativemd.littletiles.common.structure.signal.component.SignalComponentType;
import com.creativemd.littletiles.common.structure.signal.input.InternalSignalInput;
import com.creativemd.littletiles.common.structure.signal.output.InternalSignalOutput;
import com.creativemd.littletiles.common.structure.type.door.LittleDoorBase;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.location.LocalStructureLocation;
import com.creativemd.littletiles.common.tile.parent.IStructureTileList;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.place.Placement;
import com.integral.littlevecx.LittleVecXConfig;
import com.integral.littlevecx.client.gui.GuiLittleVecXElevatorBasicSettingsButton;
import com.integral.littlevecx.client.gui.GuiLittleVecXElevatorFloorCountButton;
import com.integral.littlevecx.client.gui.GuiLittleVecXElevatorSignalEventsButton;
import com.integral.littlevecx.client.gui.GuiLittleVecXElevatorSettingsButton;
import com.integral.littlevecx.client.gui.GuiLittleVecXAnimationLayersButton;
import com.integral.littlevecx.network.PacketLittleVecXElevatorTravelSound;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class StructureLittleVecXElevator extends StructureLittleVecXAdditiveAnimation {

    private static final Logger LOGGER = LogManager.getLogger("LittleVecX");

    private static final String FLOOR_COUNT_TAG = "littlevecxElevatorFloorCount";
    private static final String CURRENT_FLOOR_TAG = "littlevecxElevatorCurrentFloor";
    private static final String TARGET_FLOOR_TAG = "littlevecxElevatorTargetFloor";
    private static final String PENDING_ARRIVAL_FLOOR_TAG = "littlevecxElevatorPendingArrivalFloor";
    private static final String QUEUE_TAG = "littlevecxElevatorQueuedFloors";
    private static final String CUSTOM_ANIMATIONS_TAG = "littlevecxElevatorCustomAnimations";
    private static final String START_DELAY_TAG = "littlevecxElevatorStartDelay";
    private static final String IGNORE_CALLS_WHILE_MOVING_TAG = "littlevecxElevatorIgnoreCallsWhileMoving";
    private static final String BASIC_DISTANCE_TAG = "littlevecxElevatorBasicDistance";
    private static final String BASIC_OFFGRID_TAG = "littlevecxElevatorBasicOffGrid";
    private static final String BASIC_DURATION_TAG = "littlevecxElevatorBasicDuration";
    private static final String BASIC_INTERPOLATION_TAG = "littlevecxElevatorBasicInterpolation";
    private static final String DELAYED_FLOOR_TAG = "littlevecxElevatorDelayedFloor";
    private static final String DELAY_REMAINING_TAG = "littlevecxElevatorDelayRemaining";
    private static final String MIRROR_LOOKUP_POS_TAG = "littlevecxElevatorMirrorLookupPos";
    private static final String HARD_STOP_TAG = "littlevecxElevatorHardStop";
    private static final String LIGHT_STOP_TAG = "littlevecxElevatorLightStop";

    private static final Pattern CABIN_BUTTON_PATTERN = Pattern.compile("^button_cabin_(\\d+)$");
    private static final Pattern FLOOR_BUTTON_PATTERN = Pattern.compile("^button_floor_(\\d+)$");
    private static final String UP_SIGNAL = "up";
    private static final String DOWN_SIGNAL = "down";
    private static final String STOP_SIGNAL = "stop";
    private static final String STOP_LIGHT_SIGNAL = "stop_light";
    private static final String CURRENT_FLOOR_SIGNAL_PREFIX = "current_floor_";
    private static final String ARRIVAL_SIGNAL = "arrival";
    private static final int DEFAULT_FLOOR_COUNT = 2;

    public int configuredFloorCount = DEFAULT_FLOOR_COUNT;
    public int currentFloor = 1;
    public int targetFloor = 1;
    public boolean useCustomAnimations = false;
    public boolean ignoreCallsWhileMoving = false;
    public boolean hardStopActive = false;
    public boolean lightStopActive = false;
    public int startDelayTicks = 0;
    public int basicFloorDistance = defaultBasicFloorDistance();
    public int basicOffGrid = LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID;
    public int basicDuration = LittleVecXAnimationLayerCompiler.DEFAULT_DURATION;
    public int basicInterpolation = 0;
    public final List<Integer> queuedFloors = new ArrayList<>();

    private transient int pendingArrivalFloor = -1;
    private transient int delayedStartFloor = -1;
    private transient int delayedStartRemainingTicks = 0;
    @Nullable
    private BlockPos mirrorLookupPos;

    public StructureLittleVecXElevator(LittleStructureType type, IStructureTileList mainBlock) {
        super(type, mainBlock);
    }

    private static int defaultBasicFloorDistance() {
        try {
            return LittleGridContext.get(LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID).size * 4;
        } catch (RuntimeException ignored) {
            return 4 * LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID;
        }
    }

    private static int sanitizeBasicOffGrid(int offGrid) {
        if (offGrid <= 0)
            return LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID;
        try {
            return LittleGridContext.get(offGrid).size;
        } catch (RuntimeException ignored) {
            return LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID;
        }
    }

    private static int sanitizeBasicFloorDistance(int distance) {
        return Math.max(1, distance);
    }

    private static int sanitizeBasicInterpolation(int interpolation) {
        if (interpolation < 0 || interpolation >= ValueTimeline.interpolationTypes.length)
            return 0;
        return interpolation;
    }

    @Override
    protected void syncStartedAnimatedTransition(EntityAnimation animation) {
        PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationControllerPacket(animation), animation, null);
    }

    @Override
    protected void loadFromNBTExtra(NBTTagCompound nbt) {
        super.loadFromNBTExtra(nbt);
        configuredFloorCount = nbt.hasKey(FLOOR_COUNT_TAG) ? Math.max(DEFAULT_FLOOR_COUNT, nbt.getInteger(FLOOR_COUNT_TAG)) : DEFAULT_FLOOR_COUNT;
        currentFloor = Math.max(1, nbt.getInteger(CURRENT_FLOOR_TAG));
        targetFloor = Math.max(1, nbt.hasKey(TARGET_FLOOR_TAG) ? nbt.getInteger(TARGET_FLOOR_TAG) : currentFloor);
        pendingArrivalFloor = nbt.hasKey(PENDING_ARRIVAL_FLOOR_TAG) ? Math.max(-1, nbt.getInteger(PENDING_ARRIVAL_FLOOR_TAG)) : -1;
        useCustomAnimations = nbt.hasKey(CUSTOM_ANIMATIONS_TAG) ? nbt.getBoolean(CUSTOM_ANIMATIONS_TAG) : !layers.isEmpty();
        ignoreCallsWhileMoving = nbt.getBoolean(IGNORE_CALLS_WHILE_MOVING_TAG);
        startDelayTicks = Math.max(0, nbt.getInteger(START_DELAY_TAG));
        basicOffGrid = sanitizeBasicOffGrid(nbt.hasKey(BASIC_OFFGRID_TAG) ? nbt.getInteger(BASIC_OFFGRID_TAG) : LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID);
        basicFloorDistance = sanitizeBasicFloorDistance(
                nbt.hasKey(BASIC_DISTANCE_TAG) ? nbt.getInteger(BASIC_DISTANCE_TAG) : defaultBasicFloorDistance());
        basicDuration = Math.max(1,
                nbt.hasKey(BASIC_DURATION_TAG) ? nbt.getInteger(BASIC_DURATION_TAG) : LittleVecXAnimationLayerCompiler.DEFAULT_DURATION);
        basicInterpolation = sanitizeBasicInterpolation(nbt.hasKey(BASIC_INTERPOLATION_TAG) ? nbt.getInteger(BASIC_INTERPOLATION_TAG) : 0);
        hardStopActive = nbt.getBoolean(HARD_STOP_TAG);
        lightStopActive = !hardStopActive && nbt.getBoolean(LIGHT_STOP_TAG);
        delayedStartFloor = nbt.hasKey(DELAYED_FLOOR_TAG) ? Math.max(-1, nbt.getInteger(DELAYED_FLOOR_TAG)) : -1;
        delayedStartRemainingTicks = Math.max(0, nbt.getInteger(DELAY_REMAINING_TAG));
        mirrorLookupPos = nbt.hasKey(MIRROR_LOOKUP_POS_TAG) ? BlockPos.fromLong(nbt.getLong(MIRROR_LOOKUP_POS_TAG)) : null;
        queuedFloors.clear();
        NBTTagList queueList = nbt.getTagList(QUEUE_TAG, 3);
        for (int i = 0; i < queueList.tagCount(); i++) {
            int floor = queueList.getIntAt(i);
            if (floor > 0)
                queuedFloors.add(floor);
        }
        normalizeFloorState();
        syncCurrentFloorSignals(this, pendingArrivalFloor > 0 ? -1 : currentFloor, false);
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug load: worldRemote={}, configuredFloorCount={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}, name={}",
                isWorldRemoteSafe(), configuredFloorCount, currentFloor, targetFloor, pendingArrivalFloor,
                queuedFloors, name);
    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {
        super.writeToNBTExtra(nbt);
        nbt.setInteger(FLOOR_COUNT_TAG, Math.max(DEFAULT_FLOOR_COUNT, configuredFloorCount));
        nbt.setInteger(CURRENT_FLOOR_TAG, Math.max(1, currentFloor));
        nbt.setInteger(TARGET_FLOOR_TAG, Math.max(1, targetFloor));
        nbt.setBoolean(CUSTOM_ANIMATIONS_TAG, useCustomAnimations);
        nbt.setBoolean(IGNORE_CALLS_WHILE_MOVING_TAG, ignoreCallsWhileMoving);
        nbt.setInteger(START_DELAY_TAG, Math.max(0, startDelayTicks));
        nbt.setInteger(BASIC_DISTANCE_TAG, sanitizeBasicFloorDistance(basicFloorDistance));
        nbt.setInteger(BASIC_OFFGRID_TAG, sanitizeBasicOffGrid(basicOffGrid));
        nbt.setInteger(BASIC_DURATION_TAG, Math.max(1, basicDuration));
        nbt.setInteger(BASIC_INTERPOLATION_TAG, sanitizeBasicInterpolation(basicInterpolation));
        nbt.setBoolean(HARD_STOP_TAG, hardStopActive);
        nbt.setBoolean(LIGHT_STOP_TAG, lightStopActive);
        if (mirrorLookupPos != null)
            nbt.setLong(MIRROR_LOOKUP_POS_TAG, mirrorLookupPos.toLong());
        else
            nbt.removeTag(MIRROR_LOOKUP_POS_TAG);
        if (pendingArrivalFloor > 0)
            nbt.setInteger(PENDING_ARRIVAL_FLOOR_TAG, pendingArrivalFloor);
        else
            nbt.removeTag(PENDING_ARRIVAL_FLOOR_TAG);
        if (delayedStartFloor > 0)
            nbt.setInteger(DELAYED_FLOOR_TAG, delayedStartFloor);
        else
            nbt.removeTag(DELAYED_FLOOR_TAG);
        if (delayedStartRemainingTicks > 0)
            nbt.setInteger(DELAY_REMAINING_TAG, delayedStartRemainingTicks);
        else
            nbt.removeTag(DELAY_REMAINING_TAG);

        NBTTagList queueList = new NBTTagList();
        for (Integer floor : queuedFloors)
            if (floor != null && floor > 0)
                queueList.appendTag(new NBTTagInt(floor));
        nbt.setTag(QUEUE_TAG, queueList);
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug save: worldRemote={}, configuredFloorCount={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}, name={}",
                isWorldRemoteSafe(), configuredFloorCount, currentFloor, targetFloor, pendingArrivalFloor,
                queuedFloors, name);
    }

    protected boolean isWorldRemoteSafe() {
        try {
            World world = getWorld();
            return world != null && world.isRemote;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    protected void captureMirrorLookupPosIfNeeded() {
        if (mirrorLookupPos != null)
            return;

        World world = null;
        try {
            world = getWorld();
        } catch (RuntimeException ignored) {
        }

        if (world == null || world instanceof SubWorld)
            return;

        mirrorLookupPos = getPos();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug mirrorLookupPos.capture: pos={}, index={}, worldType={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                mirrorLookupPos, getIndex(), world.getClass().getSimpleName(), currentFloor, targetFloor,
                pendingArrivalFloor, queuedFloors);
    }

    protected static String formatSignalState(@Nullable boolean[] state) {
        if (state == null)
            return "null";

        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < state.length; i++) {
            if (i > 0)
                builder.append(',');
            builder.append(state[i]);
        }
        builder.append(']');
        return builder.toString();
    }

    protected String describeChildTiles() {
        if (children == null || children.isEmpty())
            return "[]";

        List<String> entries = new ArrayList<>();
        World world = null;
        try {
            world = getWorld();
        } catch (RuntimeException ignored) {
        }

        for (StructureChildConnection child : children) {
            if (child == null) {
                entries.add("null-child");
                continue;
            }

            try {
                BlockPos childPos = child.getStructurePosition();
                TileEntity tile = world == null ? null : world.getTileEntity(childPos);
                String tileName = tile == null ? "null" : tile.getClass().getSimpleName();
                entries.add("childId=" + child.getChildId() + ",index=" + child.getIndex() + ",pos=" + childPos + ",tile=" + tileName);
            } catch (RuntimeException ex) {
                entries.add("childId=" + child.getChildId() + ",index=" + child.getIndex() + ",error=" + ex.getClass().getSimpleName());
            }
        }

        return entries.toString();
    }

    @Override
    public void load() throws CorruptedConnectionException, NotYetConnectedException {
        captureMirrorLookupPosIfNeeded();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug load.enter: worldRemote={}, pos={}, index={}, removed={}, mainBlockSize={}, childCount={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}, layers={}",
                isWorldRemoteSafe(), getPos(), getIndex(), mainBlock.isRemoved(), mainBlock.size(), children == null ? -1 : children.size(),
                currentFloor, targetFloor, pendingArrivalFloor, queuedFloors, layers == null ? -1 : layers.size());
        try {
            super.load();
            forceStayAnimatedOnNestedDoors();
            if (isStopActive())
                applyStopState();
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                    "LittleVecX elevator debug load.success: worldRemote={}, pos={}, index={}, removed={}, mainBlockSize={}, childCount={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                    isWorldRemoteSafe(), getPos(), getIndex(), mainBlock.isRemoved(), mainBlock.size(),
                    children == null ? -1 : children.size(), currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
        } catch (CorruptedConnectionException | NotYetConnectedException ex) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                    "LittleVecX elevator debug load.fail: worldRemote={}, pos={}, index={}, removed={}, mainBlockSize={}, childCount={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}, exception={}, message={}, childTiles={}",
                    isWorldRemoteSafe(), getPos(), getIndex(), mainBlock.isRemoved(), mainBlock.size(),
                    children == null ? -1 : children.size(), currentFloor, targetFloor, pendingArrivalFloor, queuedFloors,
                    ex.getClass().getSimpleName(), ex.getMessage(), describeChildTiles());
            throw ex;
        }
    }

    @Override
    public void afterPlaced() {
        captureMirrorLookupPosIfNeeded();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug afterPlaced.before: worldRemote={}, pos={}, index={}, removed={}, mainBlockSize={}, childCount={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}, layers={}",
                isWorldRemoteSafe(), getPos(), getIndex(), mainBlock.isRemoved(), mainBlock.size(), children == null ? -1 : children.size(),
                currentFloor, targetFloor, pendingArrivalFloor, queuedFloors, layers == null ? -1 : layers.size());
        super.afterPlaced();
        forceStayAnimatedOnNestedDoors();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug afterPlaced.after: worldRemote={}, pos={}, index={}, removed={}, mainBlockSize={}, childCount={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                isWorldRemoteSafe(), getPos(), getIndex(), mainBlock.isRemoved(), mainBlock.size(), children == null ? -1 : children.size(),
                currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
    }

    @Override
    public void finishedPlacement(Placement placement) {
        captureMirrorLookupPosIfNeeded();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug finishedPlacement: worldRemote={}, pos={}, index={}, removed={}, mainBlockSize={}, childCount={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}, placementMode={}",
                isWorldRemoteSafe(), getPos(), getIndex(), mainBlock.isRemoved(), mainBlock.size(), children == null ? -1 : children.size(),
                currentFloor, targetFloor, pendingArrivalFloor, queuedFloors,
                placement == null || placement.mode == null ? "null" : placement.mode.getClass().getSimpleName());
        super.finishedPlacement(placement);
    }

    @Override
    public void onLittleTileDestroy() throws CorruptedConnectionException, NotYetConnectedException {
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug onLittleTileDestroy: worldRemote={}, pos={}, index={}, removed={}, mainBlockSize={}, childCount={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                isWorldRemoteSafe(), getPos(), getIndex(), mainBlock.isRemoved(), mainBlock.size(), children == null ? -1 : children.size(),
                currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
        super.onLittleTileDestroy();
    }

    @Override
    public boolean onBlockActivated(World world, LittleTile tile, BlockPos pos, EntityPlayer player, EnumHand hand, ItemStack stack,
            EnumFacing facing, float hitX, float hitY, float hitZ, LittleActionActivated action) throws LittleActionException {
        return false;
    }

    @Override
    public void changed(ISignalComponent changed) {
        super.changed(changed);

        if (isClient() || changed == null)
            return;

        try {
            boolean[] state = changed.getState();
            String identifier = resolveSignalIdentifier(changed);
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug changed.raw: identifier={}, type={}, state={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                    identifier, changed.getType(), formatSignalState(state), currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
            if (identifier == null)
                return;

            if (STOP_SIGNAL.equals(identifier)) {
                if (isActiveSignal(state))
                    toggleHardStop();
                return;
            }

            if (STOP_LIGHT_SIGNAL.equals(identifier)) {
                if (isActiveSignal(state))
                    toggleLightStop();
                return;
            }

            if (UP_SIGNAL.equals(identifier)) {
                if (isActiveSignal(state)) {
                    if (releaseLightStopForCall())
                        return;
                    requestAdjacentFloor(1);
                }
                return;
            }

            if (DOWN_SIGNAL.equals(identifier)) {
                if (isActiveSignal(state)) {
                    if (releaseLightStopForCall())
                        return;
                    requestAdjacentFloor(-1);
                }
                return;
            }

            Integer cabinFloor = tryParseFloor(identifier, CABIN_BUTTON_PATTERN);
            if (cabinFloor != null) {
                if (state == null || state.length == 0)
                    return;
                if (releaseLightStopForCall())
                    return;
                if (isStopActive())
                    return;
                com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug changed.edgeRequest: identifier={}, floor={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                        identifier, cabinFloor, currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
                logAnimatedDescendants("changed");
                requestFloor(cabinFloor, true, null);
                return;
            }

            Integer floorCall = tryParseFloor(identifier, FLOOR_BUTTON_PATTERN);
            if (floorCall != null) {
                if (state == null || state.length == 0)
                    return;
                if (releaseLightStopForCall())
                    return;
                if (isStopActive())
                    return;
                com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug changed.edgeRequest: identifier={}, floor={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                        identifier, floorCall, currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
                logAnimatedDescendants("changed");
                requestFloor(floorCall, false, null);
                return;
            }

            if (state == null || state.length == 0 || !state[0])
                return;

            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug changed: identifier={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                    identifier, currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
            logAnimatedDescendants("changed");
        } catch (com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException
                | com.creativemd.littletiles.common.structure.exception.NotYetConnectedException ignored) {
        }
    }

    @Override
    public void performInternalOutputChange(InternalSignalOutput output) {
        String identifier = output.component.identifier;
        if (identifier == null || "state".equals(identifier))
            return;

        boolean[] state = output.getState();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug internalOutput.raw: identifier={}, state={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                identifier, formatSignalState(state), currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);

        if (STOP_SIGNAL.equals(identifier)) {
            if (isActiveSignal(state))
                toggleHardStop();
            return;
        }

        if (STOP_LIGHT_SIGNAL.equals(identifier)) {
            if (isActiveSignal(state))
                toggleLightStop();
            return;
        }

        if (UP_SIGNAL.equals(identifier)) {
            if (isActiveSignal(state)) {
                if (releaseLightStopForCall())
                    return;
                requestAdjacentFloor(1);
            }
            return;
        }

        if (DOWN_SIGNAL.equals(identifier)) {
            if (isActiveSignal(state)) {
                if (releaseLightStopForCall())
                    return;
                requestAdjacentFloor(-1);
            }
            return;
        }

        Integer cabinFloor = tryParseFloor(identifier, CABIN_BUTTON_PATTERN);
        if (cabinFloor != null) {
            if (state == null || state.length == 0)
                return;
            if (releaseLightStopForCall())
                return;
            if (isStopActive())
                return;
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug internalOutput.edgeRequest: identifier={}, floor={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                    identifier, cabinFloor, currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
            logAnimatedDescendants("internalOutput");
            requestFloor(cabinFloor, true, null);
            return;
        }

        Integer floorCall = tryParseFloor(identifier, FLOOR_BUTTON_PATTERN);
        if (floorCall != null) {
            if (state == null || state.length == 0)
                return;
            if (releaseLightStopForCall())
                return;
            if (isStopActive())
                return;
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug internalOutput.edgeRequest: identifier={}, floor={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                    identifier, floorCall, currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
            logAnimatedDescendants("internalOutput");
            requestFloor(floorCall, false, null);
            return;
        }

        if (state == null || state.length == 0 || !state[0])
            return;

        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug internalOutput: identifier={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                identifier, currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
        logAnimatedDescendants("internalOutput");
    }

    @Nullable
    protected String resolveSignalIdentifier(ISignalComponent changed)
            throws com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException,
            com.creativemd.littletiles.common.structure.exception.NotYetConnectedException {
        if (changed instanceof InternalSignalInput)
            return ((InternalSignalInput) changed).component.identifier;

        if (changed.getStructure() != null && changed.getStructure() != this) {
            String childName = changed.getStructure().name;
            if (childName != null && !childName.trim().isEmpty()
                    && (changed.getType() == SignalComponentType.INPUT || changed.getType() == SignalComponentType.OUTPUT))
                return childName.trim();
        }

        return null;
    }

    @Override
    protected void afterFinishAnimation(EntityAnimation animation, @Nullable StructureLittleVecXMultiAnimation targetBase) {
        super.afterFinishAnimation(animation, targetBase);

        StructureLittleVecXElevator target = targetBase instanceof StructureLittleVecXElevator ? (StructureLittleVecXElevator) targetBase : getElevatorTarget();
        int arrivedFloor = resolvePendingArrivalFloor(target);
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug afterFinish.before: currentFloor={}, targetFloor={}, pendingArrivalFloor={}, targetCurrentFloor={}, targetTargetFloor={}, targetPendingArrivalFloor={}, queuedFloors={}",
                currentFloor, targetFloor, pendingArrivalFloor, target == null ? null : target.currentFloor,
                target == null ? null : target.targetFloor, target == null ? null : target.pendingArrivalFloor, queuedFloors);

        if (arrivedFloor > 0) {
            currentFloor = arrivedFloor;
            targetFloor = currentFloor;
            pendingArrivalFloor = -1;
            if (target != null && target != this) {
                target.currentFloor = arrivedFloor;
                target.targetFloor = arrivedFloor;
                target.pendingArrivalFloor = -1;
            }
        }

        normalizeFloorState();
        if (target != null && target != this)
            target.normalizeFloorState();
        mirrorFloorState(target);
        syncCurrentFloorSignals(this, currentFloor, true);
        if (target != null && target != this)
            syncCurrentFloorSignals(target, target.currentFloor, true);
        // The client mirrors this callback too, but SignalTicker is server-only. The server
        // owns the arrival edge and syncs the resulting output state to clients.
        if (!isClient())
            setArrivalSignalState(target, true);
        onArrivedAtFloor(currentFloor);
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug afterFinish.after: currentFloor={}, targetFloor={}, pendingArrivalFloor={}, targetCurrentFloor={}, targetTargetFloor={}, targetPendingArrivalFloor={}, queuedFloors={}",
                currentFloor, targetFloor, pendingArrivalFloor, target == null ? null : target.currentFloor,
                target == null ? null : target.targetFloor, target == null ? null : target.pendingArrivalFloor, queuedFloors);
        logAnimatedDescendants("afterFinish");

        // Internal outputs are propagated by LittleTiles at the end of the tick.
        // Starting a queued journey here would raise and clear arrival in the same
        // tick, making the arrival edge invisible to external signal consumers.
        // Let it remain high for one complete tick before reading the queue again.
        if (!isClient() && !queuedFloors.isEmpty())
            queueForNextTick();
    }

    @Override
    public DoorController createController(UUIDSupplier supplier, Placement placement, int totalDuration) {
        return LittleVecXRetargetableDoorController.from(super.createController(supplier, placement, totalDuration));
    }

    @Override
    protected void prepareRebuiltController(DoorController oldController, DoorController newController, EntityAnimation liveAnimation) {
        LittleVecXRetargetableDoorController controller;
        if (oldController instanceof LittleVecXRetargetableDoorController)
            controller = (LittleVecXRetargetableDoorController) oldController;
        else
            controller = LittleVecXRetargetableDoorController.from(oldController);

        controller.retargetFrom(newController);
        liveAnimation.controller = controller;
        controller.setParent(liveAnimation);

        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug controllerRetargeted: oldType={}, newType={}, liveType={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                oldController == null ? "null" : oldController.getClass().getSimpleName(),
                newController == null ? "null" : newController.getClass().getSimpleName(),
                liveAnimation.controller == null ? "null" : liveAnimation.controller.getClass().getSimpleName(),
                currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
    }

    @Override
    protected boolean rebuildPersistentAnimationLayer(int requestedLayer, @Nullable LittleVecXAnimationLayer overrideLayer,
            @Nullable EntityPlayer player) {
        if (!stayAnimated || (overrideLayer == null && !isValidLayerIndex(requestedLayer)))
            return false;

        StructureLittleVecXMultiAnimation target = getAnimatedStructureTarget();
        EntityAnimation liveAnimation = getLiveAnimation(target);
        if (target == null || liveAnimation == null || liveAnimation.controller == null || liveAnimation.controller.isChanging()
                || !(liveAnimation.controller instanceof DoorController))
            return false;

        LittleVecXAnimationLayer layer = overrideLayer != null ? overrideLayer : layers.get(requestedLayer);
        currentLayerIndex = overrideLayer == null ? requestedLayer : -1;
        runtimeLayerOverride = overrideLayer;
        applyLayer(layer);
        if (target != this) {
            target.currentLayerIndex = overrideLayer == null ? requestedLayer : -1;
            target.runtimeLayerOverride = overrideLayer;
            target.applyLayer(layer);
        }

        try {
            DoorController oldController = (DoorController) liveAnimation.controller;
            UUIDSupplier supplier = oldController.supplier != null ? oldController.supplier : new UUIDSupplier(UUID.randomUUID());
            DoorController newController = target.createRuntimeControllerWithoutPlacement(supplier, target.getCompleteDuration());
            newController.activator = player;
            newController.noClip = target.noClip;

            liveAnimation.controller = newController;
            newController.setParent(liveAnimation);
            runtimeLayerOverride = null;
            if (target != this)
                target.runtimeLayerOverride = null;

            StructureAbsolute nextAbsolute = target.getRuntimeAbsoluteAxisDirect();
            if (nextAbsolute == null)
                nextAbsolute = target.getAbsoluteAxis();
            if (!sameStructureAbsolute(liveAnimation.center, nextAbsolute))
                liveAnimation.setCenter(nextAbsolute);

            prepareRebuiltController(oldController, newController, liveAnimation);
            liveAnimation.updateTickState();
            liveAnimation.updateBoundingBox();

            if (!isClient() && shouldSyncRebuiltAnimationImmediately()) {
                PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationDataPacket(liveAnimation), liveAnimation, null);
                PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationControllerPacket(liveAnimation), liveAnimation, null);
            }

            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                    "LittleVecX elevator debug controllerOnlyRebuild: livePos={}, liveWorldType={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                    liveAnimation.getPosition(),
                    liveAnimation.world == null ? "null" : liveAnimation.world.getClass().getSimpleName(),
                    currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
            return true;
        } catch (RuntimeException ex) {
            runtimeLayerOverride = null;
            if (target != this)
                target.runtimeLayerOverride = null;
            refreshCurrentLayerFields();
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                    "LittleVecX elevator debug controllerOnlyRebuild.fail: currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}, exception={}, message={}",
                    currentFloor, targetFloor, pendingArrivalFloor, queuedFloors,
                    ex.getClass().getSimpleName(), ex.getMessage());
            return false;
        }
    }

    protected void syncNestedAnimatedChildrenToLiveAnimation(EntityAnimation liveAnimation) {
        if (liveAnimation == null) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug transferChildrenToAnimation.skip: reason=noLiveAnimation, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                    currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
            return;
        }

        if (!(liveAnimation.structure instanceof LittleStructure)) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug transferChildrenToAnimation.skip: reason=noLiveStructure, thisWorldType={}, liveWorldType={}, liveStructureType={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                    getWorld() == null ? "null" : getWorld().getClass().getSimpleName(),
                    liveAnimation.getRealWorld() == null ? "null" : liveAnimation.getRealWorld().getClass().getSimpleName(),
                    liveAnimation.structure == null ? "null" : liveAnimation.structure.getClass().getSimpleName(),
                    currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
            return;
        }

        LittleStructure liveStructure = (LittleStructure) liveAnimation.structure;

        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug transferChildrenToAnimation.source: sourcePos={}, sourceWorldType={}, liveWorldType={}, liveStructureType={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                liveStructure.getPos(),
                liveStructure.getWorld() == null ? "null" : liveStructure.getWorld().getClass().getSimpleName(),
                liveAnimation.getRealWorld() == null ? "null" : liveAnimation.getRealWorld().getClass().getSimpleName(),
                liveAnimation.structure == null ? "null" : liveAnimation.structure.getClass().getSimpleName(),
                currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);

        try {
            liveStructure.transferChildrenToAnimation(liveAnimation);
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                    "LittleVecX elevator debug transferChildrenToAnimation: targetPos={}, liveStructureType={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                    liveStructure.getPos(),
                    liveAnimation.structure == null ? "null" : liveAnimation.structure.getClass().getSimpleName(),
                    currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
        } catch (CorruptedConnectionException | NotYetConnectedException | RuntimeException ex) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                    "LittleVecX elevator debug transferChildrenToAnimation.fail: targetPos={}, liveStructureType={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}, exception={}, message={}",
                    liveStructure.getPos(),
                    liveAnimation.structure == null ? "null" : liveAnimation.structure.getClass().getSimpleName(),
                    currentFloor, targetFloor, pendingArrivalFloor, queuedFloors,
                    ex.getClass().getSimpleName(), ex.getMessage());
        }
    }

    @Nullable
    protected StructureLittleVecXElevator resolveRealWorldMirror(@Nullable World realWorld, @Nullable EntityAnimation liveAnimation) {
        if (realWorld == null)
            return null;

        if (getWorld() == realWorld)
            return this;

        try {
            if (mirrorLookupPos != null) {
                StructureLittleVecXElevator anchoredMirror = resolveRealWorldMirrorAt(realWorld, mirrorLookupPos, "anchor");
                if (anchoredMirror != null)
                    return anchoredMirror;
            }

            if (liveAnimation != null && liveAnimation.structureLocation != null) {
                LocalStructureLocation location = liveAnimation.structureLocation;
                try {
                    LittleStructure located = location.find(realWorld);
                    if (located instanceof StructureLittleVecXElevator) {
                        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                                "LittleVecX elevator debug resolveRealWorldMirror.location: pos={}, index={}, structureType={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                                location.pos, location.index, located.getClass().getSimpleName(),
                                currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
                        return (StructureLittleVecXElevator) located;
                    }

                    com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                            "LittleVecX elevator debug resolveRealWorldMirror.skip: reason=wrongLocationType, pos={}, index={}, structureType={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                            location.pos, location.index,
                            located == null ? "null" : located.getClass().getSimpleName(),
                            currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
                } catch (LittleActionException ex) {
                    com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                            "LittleVecX elevator debug resolveRealWorldMirror.skip: reason=locationFindFailed, pos={}, index={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}, exception={}, message={}",
                            location.pos, location.index,
                            currentFloor, targetFloor, pendingArrivalFloor, queuedFloors,
                            ex.getClass().getSimpleName(), ex.getMessage());
                }
            }

            return resolveRealWorldMirrorAt(realWorld, getPos(), "fallbackPos");
        } catch (CorruptedConnectionException | NotYetConnectedException | RuntimeException ex) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                    "LittleVecX elevator debug resolveRealWorldMirror.fail: pos={}, index={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}, exception={}, message={}",
                    getPos(), getIndex(), currentFloor, targetFloor, pendingArrivalFloor, queuedFloors,
                    ex.getClass().getSimpleName(), ex.getMessage());
            return null;
        }
    }

    @Nullable
    protected StructureLittleVecXElevator resolveRealWorldMirrorAt(World realWorld, BlockPos lookupPos, String sourceTag)
            throws CorruptedConnectionException, NotYetConnectedException {
        TileEntity tileEntity = realWorld.getTileEntity(lookupPos);
        if (!(tileEntity instanceof TileEntityLittleTiles)) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                    "LittleVecX elevator debug resolveRealWorldMirror.skip: reason=noTileEntity, source={}, pos={}, tileType={}, index={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                    sourceTag, lookupPos,
                    tileEntity == null ? "null" : tileEntity.getClass().getSimpleName(),
                    getIndex(), currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
            return null;
        }

        IStructureTileList structureList = ((TileEntityLittleTiles) tileEntity).getStructure(getIndex());
        if (structureList == null) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                    "LittleVecX elevator debug resolveRealWorldMirror.skip: reason=noStructureList, source={}, pos={}, index={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                    sourceTag, lookupPos, getIndex(), currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
            return null;
        }

        LittleStructure structure = structureList.getStructure();
        if (!(structure instanceof StructureLittleVecXElevator)) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                    "LittleVecX elevator debug resolveRealWorldMirror.skip: reason=wrongStructureType, source={}, pos={}, index={}, structureType={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                    sourceTag, lookupPos, getIndex(),
                    structure == null ? "null" : structure.getClass().getSimpleName(),
                    currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
            return null;
        }

        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug resolveRealWorldMirror.{}: pos={}, index={}, structureType={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                sourceTag, lookupPos, getIndex(), structure.getClass().getSimpleName(),
                currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
        return (StructureLittleVecXElevator) structure;
    }

    @Override
    protected StructureLittleVecXMultiAnimation getRebuildPreviewSource(@Nullable StructureLittleVecXMultiAnimation target,
            @Nullable EntityAnimation liveAnimation) {
        StructureLittleVecXMultiAnimation previewSource = target != null ? target : this;
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug rebuildPreviewSource: sourceType={}, targetType={}, liveWorldType={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                previewSource == null ? "null" : previewSource.getClass().getSimpleName(),
                target == null ? "null" : target.getClass().getSimpleName(),
                liveAnimation == null || liveAnimation.world == null ? "null" : liveAnimation.world.getClass().getSimpleName(),
                currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
        return previewSource;
    }

    @Override
    protected World getRebuildPlacementWorld(StructureLittleVecXMultiAnimation previewSource,
            @Nullable StructureLittleVecXMultiAnimation target, @Nullable EntityAnimation liveAnimation) {
        World world = liveAnimation != null ? liveAnimation.getRealWorld() : super.getRebuildPlacementWorld(previewSource, target, liveAnimation);
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug rebuildPlacementWorld: previewSourceType={}, worldType={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                previewSource == null ? "null" : previewSource.getClass().getSimpleName(),
                world == null ? "null" : world.getClass().getSimpleName(),
                currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
        return world;
    }

    protected boolean requestFloor(int floor, boolean cabinRequest, @Nullable EntityPlayer player) {
        if (isStopActive())
            return false;
        int effectiveFloorCount = getEffectiveFloorCount();
        if (floor < 1 || floor > effectiveFloorCount)
            return false;

        StructureLittleVecXElevator target = getElevatorTarget();
        StructureLittleVecXElevator stateSource = getStateSource(target);
        syncFloorStateFrom(stateSource);
        syncAccumulatedPoseFrom(stateSource);
        if (target != null && target != this)
            target.syncFloorStateFrom(stateSource);

        if (!isMotionActive(target) && stateSource.delayedStartFloor < 1 && stateSource.pendingArrivalFloor < 1 && stateSource.targetFloor > 0
                && stateSource.currentFloor != stateSource.targetFloor) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                    "LittleVecX elevator debug requestFloor.syncIdleFloor: oldCurrentFloor={}, settledTargetFloor={}, requestedFloor={}, queuedFloors={}",
                    stateSource.currentFloor, stateSource.targetFloor, floor, queuedFloors);
            stateSource.currentFloor = stateSource.targetFloor;
            syncFloorStateFrom(stateSource);
            if (target != null && target != this)
                target.syncFloorStateFrom(stateSource);
        }

        int currentFloor = stateSource.currentFloor;
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug requestFloor: requestedFloor={}, cabinRequest={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, stateSourceCurrentFloor={}, stateSourceTargetFloor={}, effectiveFloorCount={}, queuedFloors={}",
                floor, cabinRequest, this.currentFloor, this.targetFloor, this.pendingArrivalFloor, stateSource.currentFloor,
                stateSource.targetFloor, effectiveFloorCount, queuedFloors);
        logAnimatedDescendants("requestFloor");

        if (floor == currentFloor && !isMotionActive(target)) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug requestFloor.currentFloorHit: floor={}, cabinRequest={}, queuedFloors={}", floor,
                    cabinRequest, queuedFloors);
            return handleCurrentFloorRequest(floor, cabinRequest);
        }

        if (isQueuedOrActive(floor, target)) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug requestFloor.queuedOrActive: floor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                    floor, targetFloor, pendingArrivalFloor, queuedFloors);
            return false;
        }

        if (isMotionActive(target)) {
            if (stateSource.ignoreCallsWhileMoving) {
                com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                        "LittleVecX elevator debug requestFloor.ignoredWhileMoving: floor={}, currentFloor={}, targetFloor={}, queuedFloors={}",
                        floor, currentFloor, targetFloor, queuedFloors);
                return false;
            }
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug requestFloor.queuedWhileMoving: floor={}, currentFloor={}, targetFloor={}, queuedFloors={}",
                    floor, currentFloor, targetFloor, queuedFloors);
            enqueueFloor(floor, target);
            return true;
        }

        if (delayedStartFloor > 0) {
            enqueueFloor(floor, target);
            return true;
        }

        if (startDelayTicks > 0) {
            scheduleDelayedTravel(floor, target);
            return true;
        }

        canStartMoving();
        return startTravelToFloor(floor, stateSource, target, player);
    }

    /**
     * Pulses the elevator's {@code up} input for the Activator. The input remains
     * an ordinary configurable signal, so buttons and automation use the same path.
     */
    public boolean activateUpFromActivator() {
        return pulseDirectionInputFromActivator(0);
    }

    /** Pulses the elevator's {@code down} input for the Activator. */
    public boolean activateDownFromActivator() {
        return pulseDirectionInputFromActivator(1);
    }

    private boolean pulseDirectionInputFromActivator(int directionInputOffset) {
        if (isClient())
            return false;

        InternalSignalInput input = getInput(LittleVecXConfig.elevatorSignalCount * 2 + directionInputOffset);
        if (input == null || input.getState() == null || input.getState().length == 0)
            return false;

        boolean[] active = new boolean[input.getState().length];
        active[0] = true;
        input.updateState(active);
        input.updateState(new boolean[input.getState().length]);
        notifyChange();
        return true;
    }

    protected boolean requestAdjacentFloor(int direction) {
        if (isStopActive())
            return false;
        if (direction == 0)
            return false;

        StructureLittleVecXElevator target = getElevatorTarget();
        StructureLittleVecXElevator stateSource = getStateSource(target);
        int referenceFloor = isMotionActive(target) || stateSource.delayedStartFloor > 0 ? stateSource.targetFloor : stateSource.currentFloor;
        int requestedFloor = referenceFloor + (direction > 0 ? 1 : -1);
        if (requestedFloor < 1 || requestedFloor > getEffectiveFloorCount())
            return false;

        return requestFloor(requestedFloor, true, null);
    }

    private static boolean isActiveSignal(@Nullable boolean[] state) {
        return state != null && state.length > 0 && state[0];
    }

    protected boolean isStopActive() {
        return hardStopActive || lightStopActive;
    }

    protected void toggleHardStop() {
        hardStopActive = !hardStopActive;
        if (hardStopActive)
            lightStopActive = false;
        applyStopState();
    }

    protected void toggleLightStop() {
        if (hardStopActive)
            return;
        lightStopActive = !lightStopActive;
        applyStopState();
    }

    protected boolean releaseLightStopForCall() {
        if (!lightStopActive || hardStopActive)
            return false;
        lightStopActive = false;
        applyStopState();
        return true;
    }

    protected void applyStopState() {
        StructureLittleVecXElevator target = getElevatorTarget();
        if (target != null && target != this) {
            target.hardStopActive = hardStopActive;
            target.lightStopActive = lightStopActive;
        }
        setLiveAnimationPaused(target, isStopActive());
        syncCurrentFloorSignals(this, isStopActive() ? -1 : currentFloor, true);
        if (target != null && target != this)
            syncCurrentFloorSignals(target, isStopActive() ? -1 : target.currentFloor, true);
        notifyChange();
    }

    protected void setLiveAnimationPaused(@Nullable StructureLittleVecXElevator target, boolean paused) {
        EntityAnimation animation = getLiveAnimation(target);
        if (animation == null || !(animation.controller instanceof DoorController))
            return;
        LittleVecXRetargetableDoorController controller = animation.controller instanceof LittleVecXRetargetableDoorController
                ? (LittleVecXRetargetableDoorController) animation.controller
                : LittleVecXRetargetableDoorController.from((DoorController) animation.controller);
        animation.controller = controller;
        controller.setParent(animation);
        controller.setPaused(paused);
        animation.updateTickState();
        animation.updateBoundingBox();
        if (!isClient())
            PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationControllerPacket(animation), animation, null);
    }

    protected boolean canStartMoving() {
        logAnimatedDescendants("canStartMoving");
        return true;
    }

    /** Keeps all nested doors as EntityAnimations while the elevator moves. */
    protected void forceStayAnimatedOnNestedDoors() {
        if (isClient())
            return;

        if (forceStayAnimatedOnDoors(this))
            notifyChange();
    }

    private static boolean forceStayAnimatedOnDoors(@Nullable LittleStructure root) {
        if (root == null)
            return false;

        boolean changed = false;
        if (root instanceof LittleDoorBase) {
            LittleDoorBase door = (LittleDoorBase) root;
            if (!door.stayAnimated) {
                door.stayAnimated = true;
                changed = true;
            }
        }

        for (StructureChildConnection child : root.getChildren()) {
            try {
                changed |= forceStayAnimatedOnDoors(child.getStructure());
            } catch (CorruptedConnectionException | NotYetConnectedException ignored) {
                // A partially connected child will be checked again before travel starts.
            }
        }
        return changed;
    }

    protected boolean handleCurrentFloorRequest(int floor, boolean cabinRequest) {
        return false;
    }

    protected void onArrivedAtFloor(int floor) {
    }

    protected void syncCurrentFloorSignals(@Nullable StructureLittleVecXElevator signalSource, int activeFloor, boolean notify) {
        StructureLittleVecXElevator source = signalSource != null ? signalSource : this;
        int effectiveFloorCount = Math.max(DEFAULT_FLOOR_COUNT, source.getEffectiveFloorCount());
        for (int floor = 1; floor <= effectiveFloorCount; floor++)
            setOutputState(source, CURRENT_FLOOR_SIGNAL_PREFIX + floor, floor == activeFloor, notify);
    }

    /** arrival is high while the cabin is at its destination and falls only when it departs again. */
    protected void setArrivalSignalState(@Nullable StructureLittleVecXElevator target, boolean active) {
        logArrivalSignalState("before", this, active);
        setArrivalInputState(this, active);
        scheduleArrivalOutputState(this, active);
        logArrivalSignalState("after", this, active);
        if (target != null && target != this)
        {
            logArrivalSignalState("before", target, active);
            setArrivalInputState(target, active);
            scheduleArrivalOutputState(target, active);
            logArrivalSignalState("after", target, active);
        }
    }

    /**
     * Signal-event conditions inside an elevator are evaluated from internal inputs.
     * The public arrival output is kept in sync below, but the paired input must be
     * updated too so nested activators can use "arrival" as their condition source.
     */
    protected void setArrivalInputState(StructureLittleVecXElevator structure, boolean active) {
        int inputIndex = resolveInputIndex(ARRIVAL_SIGNAL);
        if (inputIndex < 0)
            return;

        InternalSignalInput input = structure.getInput(inputIndex);
        if (input == null)
            return;

        boolean[] state = new boolean[input.getState().length];
        Arrays.fill(state, active);
        if (structure.hasWorld() && !structure.getWorld().isRemote)
            input.updateState(state);
        else
            System.arraycopy(state, 0, input.getState(), 0, Math.min(state.length, input.getState().length));
    }

    /**
     * Uses LittleTiles' regular output handler instead of writing the output state
     * directly. The old door_cabin event used this route; it is what propagates a
     * generated signal through configured structure links and activators.
     */
    protected void scheduleArrivalOutputState(StructureLittleVecXElevator structure, boolean active) {
        int outputIndex = resolveOutputIndex(ARRIVAL_SIGNAL);
        if (outputIndex < 0)
            return;

        InternalSignalOutput output = structure.getOutput(outputIndex);
        if (output == null)
            return;

        boolean[] state = new boolean[output.getState().length];
        Arrays.fill(state, active);
        if (structure.hasWorld() && !structure.getWorld().isRemote && output.handler != null) {
            output.handler.schedule(state);
            return;
        }

        // Server packets mirror handler-driven state to clients. This fallback only
        // keeps transient/unplaced instances coherent before they own a world.
        System.arraycopy(state, 0, output.getState(), 0, Math.min(state.length, output.getState().length));
    }

    /**
     * Arrival is a generated output, so its route bypasses the usual input-condition
     * evaluator. Keep an opt-in trace here to distinguish a missing output from a
     * downstream connection or activator configuration problem.
     */
    private void logArrivalSignalState(String phase, StructureLittleVecXElevator structure, boolean requestedState) {
        int inputIndex = resolveInputIndex(ARRIVAL_SIGNAL);
        InternalSignalInput input = inputIndex >= 0 ? structure.getInput(inputIndex) : null;
        int outputIndex = resolveOutputIndex(ARRIVAL_SIGNAL);
        InternalSignalOutput output = outputIndex >= 0 ? structure.getOutput(outputIndex) : null;
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator arrival {}: requested={}, pos={}, inputIndex={}, inputPresent={}, inputState={}, outputIndex={}, outputPresent={}, outputState={}, mode={}, condition={}",
                phase, requestedState, structure.getPos(), inputIndex, input != null,
                input == null ? null : Arrays.toString(input.getState()), outputIndex, output != null,
                output == null ? null : Arrays.toString(output.getState()),
                output == null || output.handler == null ? null : output.handler.getMode(),
                output == null ? null : output.condition);
    }

    protected int resolveInputIndex(String identifier) {
        if (identifier == null || type == null || type.inputs == null)
            return -1;

        for (com.creativemd.littletiles.common.structure.registry.LittleStructureType.InternalComponent component : type.inputs)
            if (component != null && identifier.equals(component.identifier))
                return component.index;

        return -1;
    }

    protected void setOutputState(StructureLittleVecXElevator structure, String identifier, boolean active, boolean notify) {
        int outputIndex = resolveOutputIndex(identifier);
        if (outputIndex < 0)
            return;

        InternalSignalOutput output = structure.getOutput(outputIndex);
        if (output == null)
            return;

        boolean[] state = new boolean[output.getState().length];
        Arrays.fill(state, active);
        World world = null;
        if (structure.hasWorld())
            world = structure.getWorld();
        boolean canNotify = notify && world != null && !world.isRemote;
        if (canNotify)
            output.updateState(state);
        else
            System.arraycopy(state, 0, output.getState(), 0, Math.min(state.length, output.getState().length));
    }

    protected int resolveOutputIndex(String identifier) {
        if (identifier == null || type == null || type.outputs == null)
            return -1;

        for (InternalComponentOutput component : type.outputs)
            if (component != null && identifier.equals(component.identifier))
                return component.index;

        return -1;
    }

    @Override
    public boolean queueTick() {
        if (isClient())
            return false;

        StructureLittleVecXElevator target = getElevatorTarget();
        if (delayedStartFloor > 0 && !isMotionActive(target)) {
            if (delayedStartRemainingTicks > 0) {
                delayedStartRemainingTicks--;
                mirrorFloorState(target);
                return true;
            }

            int floor = delayedStartFloor;
            delayedStartFloor = -1;
            delayedStartRemainingTicks = 0;
            startTravelToFloor(floor, this, target, null);
            return true;
        }

        if (isMotionActive(target))
            return true;

        if (queuedFloors.isEmpty())
            return false;

        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug queueTick: currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
        canStartMoving();
        tryStartNextQueuedTravel(target, null);
        return false;
    }

    protected boolean startTravelToFloor(int floor, @Nullable StructureLittleVecXElevator stateSource, @Nullable StructureLittleVecXElevator target,
            @Nullable EntityPlayer player) {
        forceStayAnimatedOnNestedDoors();
        if (target != null && target != this)
            target.forceStayAnimatedOnNestedDoors();

        int startFloor = stateSource != null ? stateSource.currentFloor : currentFloor;
        if (floor == startFloor) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug startTravel.sameFloor: floor={}, startFloor={}, currentFloor={}, targetFloor={}", floor,
                    startFloor, currentFloor, targetFloor);
            return false;
        }

        int deltaFloors = floor - startFloor;
        LittleVecXAnimationLayer travelLayer = buildTravelLayer(deltaFloors);
        if (travelLayer == null) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug startTravel.noLayer: floor={}, startFloor={}, deltaFloors={}, layerCount={}", floor,
                    startFloor, deltaFloors, layers.size());
            return false;
        }

        ElevatorTravelSoundCue soundCue = extractTravelSoundCue(travelLayer);
        LittleVecXAnimationLayer activationLayer = stripPlaySoundEvents(travelLayer);

        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX elevator debug startTravel: startFloor={}, floor={}, deltaFloors={}, duration={}, offGrid={}, currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                startFloor, floor, deltaFloors, activationLayer.duration, activationLayer.getSafeOffGrid(), currentFloor, targetFloor,
                pendingArrivalFloor, queuedFloors);
        logAnimatedDescendants("startTravel");

        targetFloor = floor;
        pendingArrivalFloor = floor;
        setArrivalSignalState(target, false);
        syncCurrentFloorSignals(this, -1, true);
        if (target != null && target != this) {
            target.targetFloor = floor;
            target.pendingArrivalFloor = floor;
            syncCurrentFloorSignals(target, -1, true);
        }

        try {
            EntityAnimation animation = activateAccumulatedLayer(activationLayer, player, UUID.randomUUID());
            syncTravelSound(animation, soundCue, startFloor, floor, activationLayer.getSafeDuration());
            queueForNextTick();
            return true;
        } catch (LittleActionException ex) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug startTravel.exception: startFloor={}, floor={}, deltaFloors={}, message={}", startFloor,
                    floor, deltaFloors, ex.getMessage());
            targetFloor = startFloor;
            pendingArrivalFloor = -1;
            if (target != null && target != this) {
                target.targetFloor = startFloor;
                target.pendingArrivalFloor = -1;
            }
            return false;
        }
    }

    protected void tryStartNextQueuedTravel(@Nullable StructureLittleVecXElevator target, @Nullable EntityPlayer player) {
        if (isMotionActive(target) || delayedStartFloor > 0)
            return;

        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug tryStartNextQueuedTravel: currentFloor={}, targetFloor={}, pendingArrivalFloor={}, queuedFloors={}",
                currentFloor, targetFloor, pendingArrivalFloor, queuedFloors);
        canStartMoving();
        while (!queuedFloors.isEmpty()) {
            int nextFloor = queuedFloors.remove(0);
            if (nextFloor < 1 || nextFloor > getEffectiveFloorCount())
                continue;
            if (nextFloor == currentFloor) {
                handleCurrentFloorRequest(nextFloor, true);
                continue;
            }
            if (startDelayTicks > 0)
                scheduleDelayedTravel(nextFloor, target);
            else
                startTravelToFloor(nextFloor, this, target, player);
            break;
        }

        mirrorQueue(target);
    }

    protected boolean isQueuedOrActive(int floor, @Nullable StructureLittleVecXElevator target) {
        if (floor == targetFloor || floor == pendingArrivalFloor || floor == delayedStartFloor)
            return true;
        if (queuedFloors.contains(floor))
            return true;
        if (target != null && target != this) {
            if (floor == target.targetFloor || floor == target.pendingArrivalFloor || floor == target.delayedStartFloor)
                return true;
            if (target.queuedFloors.contains(floor))
                return true;
        }
        return false;
    }

    protected void enqueueFloor(int floor, @Nullable StructureLittleVecXElevator target) {
        if (!queuedFloors.contains(floor))
            queuedFloors.add(floor);
        mirrorQueue(target);
        queueForNextTick();
    }

    protected void mirrorQueue(@Nullable StructureLittleVecXElevator target) {
        if (target == null || target == this)
            return;
        target.queuedFloors.clear();
        target.queuedFloors.addAll(queuedFloors);
    }

    protected void mirrorFloorState(@Nullable StructureLittleVecXElevator target) {
        if (target == null || target == this)
            return;

        target.useCustomAnimations = useCustomAnimations;
        target.ignoreCallsWhileMoving = ignoreCallsWhileMoving;
        target.startDelayTicks = startDelayTicks;
        target.basicFloorDistance = basicFloorDistance;
        target.basicOffGrid = basicOffGrid;
        target.basicDuration = basicDuration;
        target.basicInterpolation = basicInterpolation;
        target.configuredFloorCount = configuredFloorCount;
        target.currentFloor = currentFloor;
        target.targetFloor = targetFloor;
        target.pendingArrivalFloor = pendingArrivalFloor;
        target.delayedStartFloor = delayedStartFloor;
        target.delayedStartRemainingTicks = delayedStartRemainingTicks;
        target.mirrorLookupPos = mirrorLookupPos;
        target.queuedFloors.clear();
        target.queuedFloors.addAll(queuedFloors);
    }

    protected void syncFloorStateFrom(@Nullable StructureLittleVecXElevator source) {
        if (source == null || source == this)
            return;

        useCustomAnimations = source.useCustomAnimations;
        ignoreCallsWhileMoving = source.ignoreCallsWhileMoving;
        startDelayTicks = source.startDelayTicks;
        basicFloorDistance = source.basicFloorDistance;
        basicOffGrid = source.basicOffGrid;
        basicDuration = source.basicDuration;
        basicInterpolation = source.basicInterpolation;
        configuredFloorCount = source.configuredFloorCount;
        currentFloor = source.currentFloor;
        targetFloor = source.targetFloor;
        pendingArrivalFloor = source.pendingArrivalFloor;
        delayedStartFloor = source.delayedStartFloor;
        delayedStartRemainingTicks = source.delayedStartRemainingTicks;
        mirrorLookupPos = source.mirrorLookupPos;
        queuedFloors.clear();
        queuedFloors.addAll(source.queuedFloors);
    }

    protected void normalizeFloorState() {
        int effectiveFloorCount = getEffectiveFloorCount();
        configuredFloorCount = Math.max(DEFAULT_FLOOR_COUNT, configuredFloorCount);
        basicOffGrid = sanitizeBasicOffGrid(basicOffGrid);
        basicFloorDistance = sanitizeBasicFloorDistance(basicFloorDistance);
        basicDuration = Math.max(1, basicDuration);
        basicInterpolation = sanitizeBasicInterpolation(basicInterpolation);
        currentFloor = clampFloor(currentFloor, effectiveFloorCount);
        targetFloor = clampFloor(targetFloor, effectiveFloorCount);
        if (pendingArrivalFloor > 0)
            pendingArrivalFloor = clampFloor(pendingArrivalFloor, effectiveFloorCount);
        if (delayedStartFloor > 0)
            delayedStartFloor = clampFloor(delayedStartFloor, effectiveFloorCount);
        queuedFloors.removeIf(floor -> floor == null || floor < 1 || floor > effectiveFloorCount);
    }

    protected int resolvePendingArrivalFloor(@Nullable StructureLittleVecXElevator target) {
        if (pendingArrivalFloor > 0)
            return pendingArrivalFloor;
        if (target != null && target != this && target.pendingArrivalFloor > 0)
            return target.pendingArrivalFloor;
        return -1;
    }

    protected int getEffectiveFloorCount() {
        if (configuredFloorCount < DEFAULT_FLOOR_COUNT) {
            int detected = detectCabinFloorCount(this);
            if (detected >= DEFAULT_FLOOR_COUNT)
                configuredFloorCount = detected;
        }
        return Math.max(DEFAULT_FLOOR_COUNT, configuredFloorCount);
    }

    @Nullable
    protected LittleVecXAnimationLayer buildTravelLayer(int deltaFloors) {
        if (deltaFloors == 0)
            return null;

        int steps = Math.abs(deltaFloors);
        LittleVecXAnimationLayer template;
        if (useCustomAnimations) {
            if (layers.size() < 2)
                return null;
            template = deltaFloors > 0 ? layers.get(0) : layers.get(1);
        } else {
            template = createBasicTravelTemplate(deltaFloors > 0, basicFloorDistance, basicOffGrid, basicDuration, basicInterpolation);
        }
        if (template == null)
            return null;

        return scaleTravelLayer(template, steps);
    }

    protected void scheduleDelayedTravel(int floor, @Nullable StructureLittleVecXElevator target) {
        delayedStartFloor = floor;
        delayedStartRemainingTicks = Math.max(0, startDelayTicks);
        targetFloor = floor;
        pendingArrivalFloor = -1;
        if (target != null && target != this) {
            target.delayedStartFloor = floor;
            target.delayedStartRemainingTicks = delayedStartRemainingTicks;
            target.targetFloor = floor;
            target.pendingArrivalFloor = -1;
        }
        queueForNextTick();
    }

    protected static LittleVecXAnimationLayer createBasicTravelTemplate(boolean upwards, int distance, int offGrid, int duration,
            int interpolation) {
        LittleVecXAnimationLayer layer = new LittleVecXAnimationLayer();
        layer.trigger = LittleVecXAnimationTriggerMode.NONE;
        layer.doorType = LittleVecXAnimationLayerDoorType.ADVANCED;
        layer.duration = Math.max(1, duration);
        layer.offGrid = sanitizeBasicOffGrid(offGrid);
        layer.interpolation = sanitizeBasicInterpolation(interpolation);
        layer.offY = ValueTimeline.create(layer.interpolation)
                .addPoint(0, 0D)
                .addPoint(layer.duration, upwards ? (double) sanitizeBasicFloorDistance(distance)
                        : (double) -sanitizeBasicFloorDistance(distance));
        return layer;
    }

    protected static LittleVecXAnimationLayer scaleTravelLayer(LittleVecXAnimationLayer template, int steps) {
        LittleVecXAnimationLayer scaled = template.copy();
        scaled.trigger = LittleVecXAnimationTriggerMode.NONE;
        scaled.duration = Math.max(1, template.getSafeDuration() * Math.max(1, steps));
        scaled.rotX = stretchTimeline(template.rotX, steps, false);
        scaled.rotY = stretchTimeline(template.rotY, steps, false);
        scaled.rotZ = stretchTimeline(template.rotZ, steps, false);
        scaled.offX = stretchTimeline(template.offX, steps, true);
        scaled.offY = stretchTimeline(template.offY, steps, true);
        scaled.offZ = stretchTimeline(template.offZ, steps, true);
        scaled.events = stretchEvents(template.events, steps);
        return scaled;
    }

    @Nullable
    protected static ValueTimeline stretchTimeline(@Nullable ValueTimeline timeline, int steps, boolean scaleValues) {
        if (timeline == null)
            return null;
        if (steps <= 1)
            return timeline.copy();

        PairList<Integer, Double> source = timeline.getPointsCopy();
        PairList<Integer, Double> scaled = new PairList<>();
        for (Pair<Integer, Double> point : source) {
            double value = scaleValues ? point.value * steps : point.value;
            scaled.add(point.key * steps, value);
        }
        return ValueTimeline.create(ValueTimeline.getId(timeline.getClass()), scaled);
    }

    protected static List<AnimationEvent> stretchEvents(List<AnimationEvent> source, int steps) {
        List<AnimationEvent> scaled = new ArrayList<>();
        if (source == null)
            return scaled;

        for (AnimationEvent event : source) {
            NBTTagCompound data = event.writeToNBT(new NBTTagCompound());
            if (steps > 1)
                data.setInteger("tick", event.getTick() * steps);
            AnimationEvent cloned = AnimationEvent.loadFromNBT(data);
            if (cloned != null)
                scaled.add(cloned);
        }
        return scaled;
    }

    @Nullable
    protected ElevatorTravelSoundCue extractTravelSoundCue(@Nullable LittleVecXAnimationLayer layer) {
        if (layer == null || layer.events == null || layer.events.isEmpty())
            return null;

        for (AnimationEvent event : layer.events) {
            if (!(event instanceof PlaySoundEvent))
                continue;

            PlaySoundEvent soundEvent = (PlaySoundEvent) event;
            if (soundEvent.sound == null || soundEvent.sound instanceof PlaySoundEvent.SoundEventMissing
                    || soundEvent.sound.getRegistryName() == null)
                continue;

            return new ElevatorTravelSoundCue(soundEvent.sound.getRegistryName().toString(), soundEvent.volume, soundEvent.pitch);
        }
        return null;
    }

    protected static LittleVecXAnimationLayer stripPlaySoundEvents(LittleVecXAnimationLayer layer) {
        if (layer == null || layer.events == null || layer.events.isEmpty())
            return layer;

        boolean hasPlaySound = false;
        for (AnimationEvent event : layer.events) {
            if (event instanceof PlaySoundEvent) {
                hasPlaySound = true;
                break;
            }
        }

        if (!hasPlaySound)
            return layer;

        LittleVecXAnimationLayer copy = layer.copy();
        copy.events.removeIf(event -> event instanceof PlaySoundEvent);
        return copy;
    }

    protected void syncTravelSound(@Nullable EntityAnimation animation, @Nullable ElevatorTravelSoundCue cue, int startFloor, int targetFloor,
            int durationTicks) {
        // A signal can start an elevator from a transient structure instance which has
        // no main block yet. LittleStructure#isClient dereferences that block, so only
        // send the optional sound packet once a real server world is available.
        if (animation == null || cue == null || cue.soundId == null || cue.soundId.isEmpty() || !hasWorld() || getWorld().isRemote)
            return;

        PacketHandler.sendPacketToTrackingPlayers(
                new PacketLittleVecXElevatorTravelSound(animation.getUniqueID(), cue.soundId, cue.volume, cue.pitch, startFloor, targetFloor,
                        getEffectiveFloorCount(), Math.max(1, durationTicks), targetFloor > startFloor),
                animation, null);
    }

    protected static final class ElevatorTravelSoundCue {
        public final String soundId;
        public final float volume;
        public final float pitch;

        private ElevatorTravelSoundCue(String soundId, float volume, float pitch) {
            this.soundId = soundId;
            this.volume = volume;
            this.pitch = pitch;
        }
    }

    protected static int detectCabinFloorCount(LittlePreviews previews) {
        TreeSet<Integer> floors = new TreeSet<>();
        collectCabinButtonFloors(previews, floors);
        return continuousFloorCount(floors);
    }

    protected static int detectCabinFloorCount(LittleStructure structure) {
        TreeSet<Integer> floors = new TreeSet<>();
        collectCabinButtonFloors(structure, floors);
        return continuousFloorCount(floors);
    }

    protected static void collectCabinButtonFloors(LittlePreviews previews, TreeSet<Integer> floors) {
        if (previews == null)
            return;

        Integer floor = tryParseFloor(previews.getStructureName(), CABIN_BUTTON_PATTERN);
        if (floor != null)
            floors.add(floor);

        for (LittlePreviews child : previews.getChildren())
            collectCabinButtonFloors(child, floors);
    }

    protected static void collectCabinButtonFloors(LittleStructure structure, TreeSet<Integer> floors) {
        if (structure == null)
            return;

        Integer floor = tryParseFloor(structure.name, CABIN_BUTTON_PATTERN);
        if (floor != null)
            floors.add(floor);

        for (com.creativemd.littletiles.common.structure.connection.StructureChildConnection child : structure.getChildren()) {
            try {
                collectCabinButtonFloors(child.getStructure(), floors);
            } catch (com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException
                    | com.creativemd.littletiles.common.structure.exception.NotYetConnectedException ignored) {
            }
        }
    }

    protected static int continuousFloorCount(TreeSet<Integer> floors) {
        int count = 0;
        while (floors.contains(count + 1))
            count++;
        return count;
    }

    @Nullable
    protected static Integer tryParseFloor(@Nullable String name, Pattern pattern) {
        if (name == null)
            return null;

        Matcher matcher = pattern.matcher(name.trim());
        if (!matcher.matches())
            return null;

        try {
            int floor = Integer.parseInt(matcher.group(1));
            return floor > 0 ? floor : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    protected int clampFloor(int floor, int effectiveFloorCount) {
        if (effectiveFloorCount <= 0)
            return 1;
        return Math.max(1, Math.min(effectiveFloorCount, floor));
    }

    protected boolean isMotionActive(@Nullable StructureLittleVecXElevator target) {
        EntityAnimation animation = getLiveAnimation(target);
        return animation != null && animation.controller != null && animation.controller.isChanging();
    }

    protected boolean hasAnimatedDescendant(@Nullable LittleStructure structure) {
        if (structure == null)
            return false;

        for (StructureChildConnection child : structure.getChildren()) {
            try {
                LittleStructure childStructure = child.getStructure();
                if (childStructure instanceof IAnimatedStructure) {
                    IAnimatedStructure animated = (IAnimatedStructure) childStructure;
                    if (animated.isAnimated() || animated.isInMotion())
                        return true;
                }

                if (hasAnimatedDescendant(childStructure))
                    return true;
            } catch (com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException
                    | com.creativemd.littletiles.common.structure.exception.NotYetConnectedException ignored) {
            }
        }

        return false;
    }

    protected boolean hasAnimatedDescendantAnywhere(@Nullable StructureLittleVecXElevator target) {
        if (hasAnimatedDescendant(this))
            return true;
        if (target != null && target != this && hasAnimatedDescendant(target))
            return true;
        return false;
    }

    protected void logAnimatedDescendants(String phase) {
        List<String> details = new ArrayList<>();
        collectAnimatedDescendantDetails(this, "base", details);

        StructureLittleVecXElevator target = getElevatorTarget();
        if (target != null && target != this)
            collectAnimatedDescendantDetails(target, "live", details);

        if (details.isEmpty())
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug animatedChildren[{}]: none", phase);
        else
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX elevator debug animatedChildren[{}]: {}", phase, details);
    }

    protected void collectAnimatedDescendantDetails(@Nullable LittleStructure structure, String path, List<String> details) {
        if (structure == null)
            return;

        for (StructureChildConnection child : structure.getChildren()) {
            try {
                LittleStructure childStructure = child.getStructure();
                String childName = childStructure.name != null ? childStructure.name : childStructure.getClass().getSimpleName();
                String childPath = path + "/" + child.childId + ":" + childName;
                if (childStructure instanceof IAnimatedStructure) {
                    IAnimatedStructure animated = (IAnimatedStructure) childStructure;
                    details.add(childPath + "{animated=" + animated.isAnimated() + ",moving=" + animated.isInMotion() + "}");
                }
                collectAnimatedDescendantDetails(childStructure, childPath, details);
            } catch (com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException
                    | com.creativemd.littletiles.common.structure.exception.NotYetConnectedException ex) {
                details.add(path + "/error{" + ex.getClass().getSimpleName() + "}");
            }
        }
    }

    @Nullable
    protected StructureLittleVecXElevator getStateSource(@Nullable StructureLittleVecXElevator target) {
        if (target != null && target != this)
            return target;
        return this;
    }

    @Nullable
    protected StructureLittleVecXElevator getElevatorTarget() {
        StructureLittleVecXMultiAnimation target = getAnimatedStructureTarget();
        if (target instanceof StructureLittleVecXElevator)
            return (StructureLittleVecXElevator) target;
        return this;
    }

    public static class StructureLittleVecXElevatorParser extends LittleStructureGuiParser {

        @SideOnly(Side.CLIENT)
        @Nullable
        private List<GuiSignalEvent> cachedSignalEvents;

        public StructureLittleVecXElevatorParser(GuiParent parent, AnimationGuiHandler handler) {
            super(parent, handler);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void create(LittlePreviews previews, @Nullable LittleStructure structure) {
            createControls(previews, structure);
            int floorCount = getSelectedFloorCount();
            parent.controls.add(new GuiLittleVecXElevatorSignalEventsButton("signal", 0, 122, previews, structure, getStructureType(),
                    floorCount, cachedSignalEvents, (events) -> cacheSignalEvents(events)));
            updateControlStates();
            updateSignalButton();
            updateTimeline();
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected void createControls(LittlePreviews previews, @Nullable LittleStructure structure) {
            StructureLittleVecXElevator elevator = structure instanceof StructureLittleVecXElevator ? (StructureLittleVecXElevator) structure : null;
            boolean useCustomAnimations = elevator != null && elevator.useCustomAnimations;
            boolean ignoreCallsWhileMoving = elevator != null && elevator.ignoreCallsWhileMoving;
            boolean noClip = elevator != null && elevator.noClip;
            boolean playPlaceSounds = elevator == null || elevator.playPlaceSounds;
            int basicDistance = elevator != null ? sanitizeBasicFloorDistance(elevator.basicFloorDistance) : defaultBasicFloorDistance();
            int basicOffGrid = elevator != null ? sanitizeBasicOffGrid(elevator.basicOffGrid) : LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID;
            int basicDuration = elevator != null ? Math.max(1, elevator.basicDuration) : LittleVecXAnimationLayerCompiler.DEFAULT_DURATION;
            int basicInterpolation = elevator != null ? sanitizeBasicInterpolation(elevator.basicInterpolation) : 0;
            int floorCount = elevator != null ? Math.max(DEFAULT_FLOOR_COUNT, elevator.configuredFloorCount) : DEFAULT_FLOOR_COUNT;

            parent.controls.add(new GuiLittleVecXAnimationLayersButton("animation_layers", 0, 0, elevator != null ? elevator.layers : null, previews,
                    2,
                    new String[] {
                            com.creativemd.creativecore.common.gui.CoreControl.translate("gui.littlevecx.elevator_layer_up"),
                            com.creativemd.creativecore.common.gui.CoreControl.translate("gui.littlevecx.elevator_layer_down")
                    }, true));
            parent.controls.add(new GuiLittleVecXElevatorSettingsButton("settings", 150, 122, useCustomAnimations, noClip, playPlaceSounds,
                    ignoreCallsWhileMoving,
                    (button) -> {
                    }));
            parent.controls.add(new GuiLittleVecXElevatorBasicSettingsButton("basic_settings", 0, 20, basicDistance, basicOffGrid,
                    basicDuration, basicInterpolation, (button) -> {
                    }));
            parent.controls.add(new GuiLittleVecXElevatorFloorCountButton("floors", 0, 40, floorCount, LittleVecXConfig.elevatorSignalCount,
                    (button) -> {
                    }));
        }

        @CustomEventSubscribe
        @SideOnly(Side.CLIENT)
        public void onChanged(GuiControlChangedEvent event) {
            if (event.source.is("animation_layers"))
                updateTimeline();
            else if (event.source.is("settings")) {
                updateControlStates();
                updateTimeline();
            } else if (event.source.is("basic_settings"))
                updateTimeline();
            else if (event.source.is("floors"))
                updateSignalButton();
            else if (event.source.is("signal"))
                cacheSignalEvents(((GuiLittleVecXElevatorSignalEventsButton) event.source).events);
        }

        @SideOnly(Side.CLIENT)
        private void updateTimeline() {
            if (handler == null)
                return;

            List<LittleVecXAnimationLayer> layers = getPreviewLayers();

            LittleVecXAnimationLayerCompiler.CompiledAnimation compiled = LittleVecXAnimationLayerCompiler.compile(layers);
            AnimationTimeline previewTimeline = compiled.timeline;
            if (previewTimeline == null)
                previewTimeline = new AnimationTimeline(1, new PairList<>());

            handler.setTimeline(previewTimeline, null);
        }

        @SideOnly(Side.CLIENT)
        private List<LittleVecXAnimationLayer> getPreviewLayers() {
            GuiLittleVecXElevatorSettingsButton settings = (GuiLittleVecXElevatorSettingsButton) parent.get("settings");
            if (settings != null && settings.useCustomAnimations) {
                GuiLittleVecXAnimationLayersButton layersButton = (GuiLittleVecXAnimationLayersButton) parent.get("animation_layers");
                List<LittleVecXAnimationLayer> layers = layersButton != null ? layersButton.getLayersCopy() : new ArrayList<>();
                if (layers.size() > 2)
                    layers = new ArrayList<>(layers.subList(0, 2));
                return layers;
            }

            GuiLittleVecXElevatorBasicSettingsButton basicSettings = (GuiLittleVecXElevatorBasicSettingsButton) parent.get("basic_settings");
            int distance = basicSettings != null ? sanitizeBasicFloorDistance(basicSettings.floorDistance) : defaultBasicFloorDistance();
            int offGrid = basicSettings != null ? sanitizeBasicOffGrid(basicSettings.offGrid) : LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID;
            int duration = basicSettings != null ? Math.max(1, basicSettings.duration) : LittleVecXAnimationLayerCompiler.DEFAULT_DURATION;
            int interpolation = basicSettings != null ? sanitizeBasicInterpolation(basicSettings.interpolation) : 0;

            List<LittleVecXAnimationLayer> previewLayers = new ArrayList<>();
            previewLayers.add(createBasicTravelTemplate(true, distance, offGrid, duration, interpolation));
            previewLayers.add(createBasicTravelTemplate(false, distance, offGrid, duration, interpolation));
            return previewLayers;
        }

        @SideOnly(Side.CLIENT)
        private void updateControlStates() {
            GuiLittleVecXElevatorSettingsButton settings = (GuiLittleVecXElevatorSettingsButton) parent.get("settings");
            boolean useCustomAnimations = settings != null && settings.useCustomAnimations;

            if (parent.get("animation_layers") != null)
                parent.get("animation_layers").setEnabled(useCustomAnimations);
            if (parent.get("basic_settings") != null)
                parent.get("basic_settings").setEnabled(!useCustomAnimations);
        }

        @SideOnly(Side.CLIENT)
        private int getSelectedFloorCount() {
            GuiLittleVecXElevatorFloorCountButton floorsButton = (GuiLittleVecXElevatorFloorCountButton) parent.get("floors");
            return floorsButton != null
                    ? GuiLittleVecXElevatorFloorCountButton.clampFloorCount(floorsButton.floorCount, floorsButton.maxFloors)
                    : DEFAULT_FLOOR_COUNT;
        }

        @SideOnly(Side.CLIENT)
        private void updateSignalButton() {
            if (parent.get("signal") instanceof GuiLittleVecXElevatorSignalEventsButton)
                ((GuiLittleVecXElevatorSignalEventsButton) parent.get("signal")).setMaxFloors(getSelectedFloorCount());
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructure parseStructure(LittlePreviews previews) {
            StructureLittleVecXElevator elevator = createStructure(StructureLittleVecXElevator.class, null);

            GuiLittleVecXElevatorSettingsButton settings = (GuiLittleVecXElevatorSettingsButton) parent.get("settings");
            GuiLittleVecXElevatorBasicSettingsButton basicSettings = (GuiLittleVecXElevatorBasicSettingsButton) parent.get("basic_settings");
            GuiLittleVecXElevatorFloorCountButton floorsButton = (GuiLittleVecXElevatorFloorCountButton) parent.get("floors");
            elevator.stayAnimated = true;
            elevator.disableRightClick = true;
            elevator.noClip = settings != null && settings.noClip;
            elevator.playPlaceSounds = settings == null || settings.playPlaceSounds;
            elevator.useCustomAnimations = settings != null && settings.useCustomAnimations;
            elevator.ignoreCallsWhileMoving = settings != null && settings.ignoreCallsWhileMoving;
            elevator.startDelayTicks = 0;
            elevator.basicFloorDistance = basicSettings != null ? sanitizeBasicFloorDistance(basicSettings.floorDistance) : defaultBasicFloorDistance();
            elevator.basicOffGrid = basicSettings != null ? sanitizeBasicOffGrid(basicSettings.offGrid) : LittleVecXAnimationLayerCompiler.DEFAULT_OFFGRID;
            elevator.basicDuration = basicSettings != null ? Math.max(1, basicSettings.duration) : LittleVecXAnimationLayerCompiler.DEFAULT_DURATION;
            elevator.basicInterpolation = basicSettings != null ? sanitizeBasicInterpolation(basicSettings.interpolation) : 0;

            GuiLittleVecXAnimationLayersButton layersButton = (GuiLittleVecXAnimationLayersButton) parent.get("animation_layers");
            elevator.layers = layersButton != null ? layersButton.getLayersCopy() : new ArrayList<>();
            if (elevator.layers.size() > 2)
                elevator.layers = new ArrayList<>(elevator.layers.subList(0, 2));
            elevator.events = new ArrayList<>();
            elevator.interpolation = 0;
            elevator.resetAdditiveRuntimeState();
            elevator.currentFloor = 1;
            elevator.targetFloor = 1;
            elevator.pendingArrivalFloor = -1;
            elevator.delayedStartFloor = -1;
            elevator.delayedStartRemainingTicks = 0;
            elevator.configuredFloorCount = floorsButton != null
                    ? GuiLittleVecXElevatorFloorCountButton.clampFloorCount(floorsButton.floorCount, floorsButton.maxFloors)
                    : DEFAULT_FLOOR_COUNT;
            elevator.queuedFloors.clear();
            elevator.refreshCurrentLayerFields();
            if (elevator.axisCenter == null)
                elevator.axisCenter = new com.creativemd.littletiles.common.structure.relative.StructureRelative(defaultAxis(previews));
            elevator.normalizeFloorState();
            elevator.syncCurrentFloorSignals(elevator, elevator.currentFloor, false);
            return elevator;
        }

        @SideOnly(Side.CLIENT)
        private void cacheSignalEvents(@Nullable List<GuiSignalEvent> events) {
            cachedSignalEvents = new ArrayList<>();
            if (events == null)
                return;

            for (GuiSignalEvent event : events)
                if (event != null)
                    cachedSignalEvents.add(event.copy());
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructureType getStructureType() {
            return LittleStructureRegistry.getStructureType(StructureLittleVecXElevator.class);
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
            return (min + max) / 2;
        }

        private static int axisMax(int min, int max) {
            return (min + max) / 2 + 1;
        }
    }
}

