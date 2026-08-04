package com.integral.littlevecx.animation;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.creativemd.creativecore.common.utils.type.Pair;
import com.creativemd.littletiles.common.entity.DoorController;
import com.creativemd.littletiles.common.structure.animation.AnimationKey;
import com.creativemd.littletiles.common.structure.animation.AnimationState;
import com.creativemd.littletiles.common.structure.animation.AnimationTimeline;
import com.creativemd.littletiles.common.structure.animation.ValueTimeline;
import com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException;
import com.creativemd.littletiles.common.structure.exception.NotYetConnectedException;
import com.creativemd.littletiles.common.structure.type.door.LittleDoor;

import net.minecraft.nbt.NBTTagCompound;

public class LittleVecXLoopDoorController extends DoorController {

    private static final Logger LOGGER = LogManager.getLogger("LittleVecX");

    public static final String CONTROLLER_ID = "littlevecx_loop_door";
    private static final String LOOP_RUNNING_TAG = "littlevecxLoopRunning";
    private static final String LOOP_TICK_TAG = "littlevecxLoopTick";
    private static final String LOOP_POSITION_TAG = "littlevecxLoopPosition";
    private static final String RAMP_UP_TICKS_TAG = "littlevecxLoopRampUpTicks";
    private static final String RAMP_DOWN_TICKS_TAG = "littlevecxLoopRampDownTicks";
    private static final String RAMP_UP_PROGRESS_TAG = "littlevecxLoopRampUpProgress";
    private static final String RAMP_DOWN_PROGRESS_TAG = "littlevecxLoopRampDownProgress";
    private static final String STOPPING_TAG = "littlevecxLoopStopping";
    private static final String PAUSED_TAG = "littlevecxLoopPaused";
    private static final String PAUSED_STATE_TAG = "littlevecxLoopPausedState";
    private static final String PAUSED_STATE = "littlevecx_paused";

    private boolean loopRunning;
    private int loopTick;
    private double loopPosition;
    private int rampUpTicks;
    private int rampDownTicks;
    private int rampUpProgress;
    private int rampDownProgress;
    private boolean stopping;
    /** The pose actually returned to EntityAnimation; loopPosition points at the next simulation frame after tick(). */
    private AnimationState lastRenderedState;
    private double lastRenderedPosition;
    private final Map<AnimationKey, Double> loopDeltas = new HashMap<>();

    public LittleVecXLoopDoorController() {
        super();
    }

    public LittleVecXLoopDoorController(DoorController source) {
        copyFrom(source);
    }

    public void copyFrom(DoorController source) {
        if (source == null)
            return;

        NBTTagCompound nbt = new NBTTagCompound();
        source.writeToNBT(nbt);
        states.clear();
        stateTransition.clear();
        currentState = null;
        aimedState = null;
        tick = 0;
        tickingState = null;
        animation = null;
        lastRenderedState = null;
        lastRenderedPosition = 0D;
        loopDeltas.clear();
        readFromNBT(nbt);
        activator = source.activator;
        supplier = source.supplier;
        noClip = source.noClip;
        turnBack = source.turnBack;
        duration = source.duration;
        completeDuration = source.completeDuration;
        interpolation = source.interpolation;
    }

    @Override
    public boolean activate() {
        if (loopRunning && !stopping)
            stopLoop();
        else
            startLoop();
        return true;
    }

    public void setRampTicks(int rampUpTicks, int rampDownTicks) {
        this.rampUpTicks = Math.max(0, rampUpTicks);
        this.rampDownTicks = Math.max(0, rampDownTicks);
    }

    @Override
    public void startTransition(String name) {
        if (DoorController.openedState.equals(name)) {
            startLoop();
            return;
        }
        super.startTransition(name);
    }

    @Override
    public boolean isChanging() {
        return loopRunning || super.isChanging();
    }

    @Override
    public AnimationState getTickingState() {
        if (!loopRunning && currentState != null && PAUSED_STATE.equals(currentState.name) && tickingState != null)
            return tickingState;
        if (!loopRunning)
            return super.getTickingState();
        AnimationState state = tickLoopTimeline(false);
        rememberRenderedState(state, loopPosition);
        return state;
    }

    @Override
    public AnimationState tick() {
        if (!loopRunning && currentState != null && PAUSED_STATE.equals(currentState.name) && tickingState != null)
            return tickingState;
        if (!loopRunning)
            return super.tick();

        try {
            if (parent != null && parent.structure instanceof LittleDoor)
                parent.structure.load();
        } catch (CorruptedConnectionException | NotYetConnectedException ignored) {
            return currentState != null ? currentState.state : new AnimationState();
        }

        if (parent != null && parent.structure instanceof LittleDoor)
            ((LittleDoor) parent.structure).beforeTick(parent, loopTick);
        double renderedPosition = loopPosition;
        AnimationState state = tickLoopTimeline(true);
        if (parent != null && parent.structure instanceof LittleDoor)
            ((LittleDoor) parent.structure).afterTick(parent, loopTick);
        rememberRenderedState(state, renderedPosition);
        return state;
    }

    public boolean isLoopRunning() {
        return loopRunning;
    }

    public void startLoop() {
        AnimationTimeline open = stateTransition.get(DoorController.closedState + ":" + DoorController.openedState);
        if (open == null) {
            super.startTransition(DoorController.openedState);
            loopRunning = false;
            return;
        }

        animation = open;
        cacheLoopDeltas(open);
        if (currentState == null)
            currentState = getState(DoorController.closedState);
        aimedState = getState(DoorController.openedState);
        if (tickingState == null)
            tickingState = new AnimationState();
        stopping = false;
        rampDownProgress = 0;
        rampUpProgress = 0;
        lastRenderedState = null;
        loopRunning = true;
        logSyncState("startLoop");
    }

    public void stopLoop() {
        if (!loopRunning)
            return;
        logSyncState("stopRequested");
        if (rampDownTicks > 0) {
            stopping = true;
            rampDownProgress = 0;
            return;
        }
        if (lastRenderedState != null) {
            loopPosition = lastRenderedPosition;
            loopTick = (int) Math.floor(loopPosition);
            pauseAt(lastRenderedState);
        } else
            pauseAt(tickLoopTimeline(false));
    }

    private void pauseAt(AnimationState state) {
        AnimationState pausedState = copyState(state);
        addStateAndSelect(PAUSED_STATE, pausedState);
        aimedState = null;
        animation = null;
        // DoorController otherwise falls back to its generic state path on the next frame.
        // Keep the exact sampled pose that was returned by the loop, not an approximated state.
        tickingState = copyState(pausedState);
        loopRunning = false;
        stopping = false;
        logSyncState("paused");
    }

    private AnimationState tickLoopTimeline(boolean advance) {
        if (animation == null)
            animation = stateTransition.get(DoorController.closedState + ":" + DoorController.openedState);
        if (animation == null) {
            loopDeltas.clear();
            return currentState != null ? currentState.state : new AnimationState();
        }
        if (tickingState == null)
            tickingState = new AnimationState();
        if (loopDeltas.isEmpty())
            cacheLoopDeltas(animation);

        // There must be no extra pose on the tick that completes braking. EntityAnimation has
        // already rendered lastRenderedState, so sampling the next position here makes the stop
        // visibly jump even though the server and client are otherwise synchronized.
        if (advance && stopping && rampDownTicks > 0 && rampDownProgress + 1 >= rampDownTicks
                && lastRenderedState != null) {
            loopPosition = lastRenderedPosition;
            loopTick = (int) Math.floor(loopPosition);
            pauseAt(lastRenderedState);
            return lastRenderedState;
        }

        int duration = Math.max(1, animation.duration);
        double sampledPosition = loopPosition;
        AnimationState state = sampleLoopState(duration, sampledPosition);
        tickingState = state;
        if (advance) {
            double speed = getCurrentSpeed();
            if (stopping)
                logSyncState("brake.before speed=" + speed);
            loopPosition += speed;
            loopTick = (int) Math.floor(loopPosition);
            if (stopping) {
                rampDownProgress++;
                if (rampDownProgress >= rampDownTicks) {
                    // Fallback for a controller restored before it has rendered a frame.
                    loopPosition = sampledPosition;
                    loopTick = (int) Math.floor(loopPosition);
                    pauseAt(state);
                    return state;
                }
            } else if (rampUpProgress < rampUpTicks)
                rampUpProgress++;
        }
        return state;
    }

    private double getCurrentSpeed() {
        if (stopping)
            return rampDownTicks <= 0 ? 0D : Math.max(0D, 1D - rampDownProgress / (double) rampDownTicks);
        return rampUpTicks <= 0 ? 1D : Math.min(1D, (rampUpProgress + 1D) / rampUpTicks);
    }

    private AnimationState sampleLoopState(int duration, double position) {
        double safePosition = Math.max(0D, position);
        int cycle = (int) Math.floor(safePosition / duration);
        double framePosition = safePosition - cycle * (double) duration;
        int lowerFrame = (int) Math.floor(framePosition);
        int upperFrame = Math.min(duration, lowerFrame + 1);
        double fraction = framePosition - lowerFrame;

        AnimationState lower = new AnimationState();
        animation.tick(lowerFrame, lower);
        applyLoopDeltas(lower, cycle);
        if (fraction <= 1.0E-9)
            return lower;

        AnimationState upper = new AnimationState();
        animation.tick(upperFrame, upper);
        applyLoopDeltas(upper, cycle);
        AnimationState blended = new AnimationState();
        for (AnimationKey key : AnimationKey.getKeys())
            blended.set(key, lower.get(key) + (upper.get(key) - lower.get(key)) * fraction);
        return blended;
    }

    private void cacheLoopDeltas(AnimationTimeline timeline) {
        loopDeltas.clear();
        if (timeline == null || timeline.values == null)
            return;

        for (Pair<AnimationKey, ValueTimeline> pair : timeline.values) {
            if (pair == null || pair.key == null || pair.value == null)
                continue;

            double delta = pair.value.last(pair.key) - pair.value.first(pair.key);
            if (Math.abs(delta) > 1.0E-9)
                loopDeltas.put(pair.key, delta);
        }
    }

    private void applyLoopDeltas(AnimationState state, int cycle) {
        if (state == null || cycle <= 0 || loopDeltas.isEmpty())
            return;

        for (Map.Entry<AnimationKey, Double> entry : loopDeltas.entrySet())
            state.set(entry.getKey(), state.get(entry.getKey()) + entry.getValue() * cycle);
    }

    private static AnimationState copyState(AnimationState state) {
        if (state == null)
            return new AnimationState();
        return new AnimationState(state.writeToNBT(new NBTTagCompound()));
    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {
        logSyncState("writeNbt");
        super.writeToNBTExtra(nbt);
        nbt.setBoolean(LOOP_RUNNING_TAG, loopRunning);
        nbt.setInteger(LOOP_TICK_TAG, loopTick);
        nbt.setDouble(LOOP_POSITION_TAG, loopPosition);
        nbt.setInteger(RAMP_UP_TICKS_TAG, rampUpTicks);
        nbt.setInteger(RAMP_DOWN_TICKS_TAG, rampDownTicks);
        nbt.setInteger(RAMP_UP_PROGRESS_TAG, rampUpProgress);
        nbt.setInteger(RAMP_DOWN_PROGRESS_TAG, rampDownProgress);
        nbt.setBoolean(STOPPING_TAG, stopping);
        boolean paused = !loopRunning && currentState != null && PAUSED_STATE.equals(currentState.name);
        nbt.setBoolean(PAUSED_TAG, paused);
        if (paused)
            nbt.setTag(PAUSED_STATE_TAG, currentState.state.writeToNBT(new NBTTagCompound()));
        else
            nbt.removeTag(PAUSED_STATE_TAG);
    }

    @Override
    protected void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        loopRunning = nbt.getBoolean(LOOP_RUNNING_TAG);
        loopTick = nbt.getInteger(LOOP_TICK_TAG);
        loopPosition = nbt.hasKey(LOOP_POSITION_TAG) ? nbt.getDouble(LOOP_POSITION_TAG) : loopTick;
        rampUpTicks = Math.max(0, nbt.getInteger(RAMP_UP_TICKS_TAG));
        rampDownTicks = Math.max(0, nbt.getInteger(RAMP_DOWN_TICKS_TAG));
        rampUpProgress = Math.max(0, nbt.getInteger(RAMP_UP_PROGRESS_TAG));
        rampDownProgress = Math.max(0, nbt.getInteger(RAMP_DOWN_PROGRESS_TAG));
        stopping = nbt.getBoolean(STOPPING_TAG);
        if (loopRunning) {
            int savedRampUpProgress = rampUpProgress;
            int savedRampDownProgress = rampDownProgress;
            boolean savedStopping = stopping;
            startLoop();
            rampUpProgress = savedRampUpProgress;
            rampDownProgress = savedRampDownProgress;
            stopping = savedStopping;
        }
        else if (nbt.getBoolean(PAUSED_TAG) && nbt.hasKey(PAUSED_STATE_TAG, 10)) {
            AnimationState pausedState = new AnimationState(nbt.getCompoundTag(PAUSED_STATE_TAG));
            addStateAndSelect(PAUSED_STATE, pausedState);
            tickingState = copyState(pausedState);
            lastRenderedState = copyState(pausedState);
            lastRenderedPosition = loopPosition;
        }
        logSyncState("readNbt");
    }

    private void logSyncState(String phase) {
        if (!com.integral.littlevecx.LittleVecXConfig.enableVerboseLogging)
            return;
        boolean remote = parent != null && parent.world != null && parent.world.isRemote;
        double originY = parent == null || parent.origin == null ? Double.NaN : parent.origin.offY();
        double offsetY = tickingState == null ? Double.NaN : tickingState.getOffset().y;
        String renderedState = lastRenderedState == null ? "null"
                : lastRenderedState.writeToNBT(new NBTTagCompound()).toString();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX loop sync {}: remote={}, entity={}, running={}, stopping={}, loopPosition={}, loopTick={}, renderedPosition={}, rampUp={}/{}, rampDown={}/{}, originY={}, stateOffY={}, renderedState={}",
                phase, remote, parent == null ? "null" : parent.getUniqueID(), loopRunning, stopping, loopPosition, loopTick,
                lastRenderedPosition, rampUpProgress, rampUpTicks, rampDownProgress, rampDownTicks, originY, offsetY,
                renderedState);
    }

    private void rememberRenderedState(AnimationState state, double position) {
        lastRenderedState = copyState(state);
        lastRenderedPosition = position;
    }
}
