package com.integral.littlevecx.animation;

import com.creativemd.littletiles.common.entity.DoorController;
import com.creativemd.littletiles.common.structure.animation.AnimationState;

import net.minecraft.nbt.NBTTagCompound;

public class LittleVecXRetargetableDoorController extends DoorController {

    public static final String CONTROLLER_ID = "retargetable_door";
    private static final String PAUSED_TAG = "littlevecxPaused";
    private static final String PAUSED_STATE_TAG = "littlevecxPausedState";
    private boolean paused;
    /** Tick at which the controller was frozen. Needed when a signal pauses it from inside DoorController.tick(). */
    private int pausedTick;
    /** Last state returned to EntityAnimation. The controller's tick counter otherwise points at the next frame. */
    private AnimationState lastRenderedState;

    public LittleVecXRetargetableDoorController() {
        super();
    }

    public void retargetFrom(DoorController source) {
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

        readFromNBT(nbt);
        activator = source.activator;
        supplier = source.supplier;
        noClip = source.noClip;
        turnBack = source.turnBack;
        duration = source.duration;
        completeDuration = source.completeDuration;
        interpolation = source.interpolation;
    }

    public static LittleVecXRetargetableDoorController from(DoorController source) {
        LittleVecXRetargetableDoorController controller = new LittleVecXRetargetableDoorController();
        controller.retargetFrom(source);
        return controller;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        if (this.paused == paused)
            return;
        if (paused) {
            AnimationState state = lastRenderedState != null ? lastRenderedState : super.getTickingState();
            tickingState = new AnimationState(state.writeToNBT(new NBTTagCompound()));
            pausedTick = tick;
        }
        this.paused = paused;
    }

    @Override
    public AnimationState getTickingState() {
        if (paused && tickingState != null)
            return tickingState;
        AnimationState state = super.getTickingState();
        lastRenderedState = new AnimationState(state.writeToNBT(new NBTTagCompound()));
        return state;
    }

    @Override
    public AnimationState tick() {
        if (paused && tickingState != null)
            return tickingState;

        AnimationState state = super.tick();
        // A stop signal can be processed by structure.load() inside super.tick(). In that case
        // super.tick() has already advanced one frame, but the current EntityAnimation tick must
        // still render the snapshot captured by setPaused(), not that next frame.
        if (paused && tickingState != null) {
            tick = pausedTick;
            return tickingState;
        }
        lastRenderedState = new AnimationState(state.writeToNBT(new NBTTagCompound()));
        return state;
    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {
        super.writeToNBTExtra(nbt);
        nbt.setBoolean(PAUSED_TAG, paused);
        if (paused && tickingState != null)
            nbt.setTag(PAUSED_STATE_TAG, tickingState.writeToNBT(new NBTTagCompound()));
        else
            nbt.removeTag(PAUSED_STATE_TAG);
    }

    @Override
    protected void readFromNBT(NBTTagCompound nbt) {
        boolean shouldPause = nbt.getBoolean(PAUSED_TAG);
        AnimationState pausedState = nbt.hasKey(PAUSED_STATE_TAG, 10)
                ? new AnimationState(nbt.getCompoundTag(PAUSED_STATE_TAG)) : null;
        super.readFromNBT(nbt);
        paused = false;
        if (shouldPause && (pausedState != null || isChanging())) {
            tickingState = pausedState != null ? pausedState
                    : new AnimationState(super.getTickingState().writeToNBT(new NBTTagCompound()));
            lastRenderedState = new AnimationState(tickingState.writeToNBT(new NBTTagCompound()));
            pausedTick = tick;
            paused = true;
        }
    }
}
