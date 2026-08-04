package com.integral.littlevecx.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.creativemd.creativecore.common.utils.type.UUIDSupplier;
import com.creativemd.littletiles.common.entity.DoorController;
import com.creativemd.littletiles.common.structure.animation.AnimationState;
import com.creativemd.littletiles.common.structure.animation.AnimationTimeline;
import com.creativemd.littletiles.common.util.vec.LittleTransformation;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class LittleVecXCheckpointController extends DoorController {

    private static final Logger LOGGER = LogManager.getLogger("LittleVecX");

    public static final String CONTROLLER_ID = "checkpoint";

    private static final String STATES_TAG = "states";
    private static final String TRANSITIONS_TAG = "transitions";
    private static final String CURRENT_STATE_TAG = "current";
    private static final String AIMED_STATE_TAG = "aimed";
    private static final String TICK_TAG = "tick";
    private static final String ORIGINAL_UUID_TAG = "originaluuid";
    private static final String UUID_TAG = "uuid";
    private static final String DURATION_TAG = "duration";
    private static final String COMPLETE_DURATION_TAG = "completeDuration";
    private static final String INTERPOLATION_TAG = "interpolation";
    private static final String NO_CLIP_TAG = "noClip";

    private final List<String> orderedStates = new ArrayList<>();

    public LittleVecXCheckpointController() {}

    public LittleVecXCheckpointController(UUIDSupplier supplier, List<String> stateNames, List<AnimationState> states,
            List<Entry<String, AnimationTimeline>> transitions, int interpolation, int completeDuration) {
        this.supplier = supplier;
        this.turnBack = null;
        this.interpolation = interpolation;
        this.duration = completeDuration;
        this.completeDuration = completeDuration;

        for (int i = 0; i < stateNames.size(); i++)
            addOrderedState(stateNames.get(i), states.get(i), i == 0);

        for (Entry<String, AnimationTimeline> entry : transitions)
            addTransition(entry.getKey(), entry.getValue());
    }

    private void addOrderedState(String name, AnimationState state, boolean select) {
        orderedStates.add(name);
        if (select)
            addStateAndSelect(name, state);
        else
            addState(name, state);
    }

    public int getMaxStageIndex() {
        return Math.max(0, orderedStates.size() - 1);
    }

    public int getCurrentStageIndex() {
        return currentState == null ? 0 : getStageIndex(currentState.name);
    }

    public int getAimedStageIndex() {
        return aimedState == null ? getCurrentStageIndex() : getStageIndex(aimedState.name);
    }

    public int getStageIndex(@Nullable String name) {
        if (name == null)
            return 0;
        int index = orderedStates.indexOf(name);
        return index >= 0 ? index : 0;
    }

    @Nullable
    public String getStageName(int index) {
        if (index < 0 || index >= orderedStates.size())
            return null;
        return orderedStates.get(index);
    }

    public boolean transitionToStage(int targetStage) {
        if (isChanging())
            return false;
        String targetName = getStageName(targetStage);
        if (targetName == null || currentState == null || targetName.equals(currentState.name))
            return false;
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX checkpoint debug transitionToStage: currentStage={}, currentName={}, targetStage={}, targetName={}",
                getCurrentStageIndex(), currentState.name, targetStage, targetName);
        startTransition(targetName);
        return true;
    }

    public boolean setCurrentStageInstant(int targetStage) {
        String targetName = getStageName(targetStage);
        if (targetName == null)
            return false;
        AnimationControllerState targetState = getState(targetName);
        if (targetState == null)
            return false;
        currentState = targetState;
        aimedState = null;
        tickingState = null;
        animation = null;
        tick = 0;
        return true;
    }

    @Override
    public boolean activate() {
        if (isChanging())
            return false;
        int currentStage = getCurrentStageIndex();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX checkpoint debug activate: currentStage={}, aimedStage={}, maxStage={}, currentName={}",
                currentStage, getAimedStageIndex(), getMaxStageIndex(), currentState == null ? "null" : currentState.name);
        if (currentStage < getMaxStageIndex())
            return transitionToStage(currentStage + 1);
        if (currentStage > 0)
            return transitionToStage(0);
        return false;
    }

    @Override
    public void startTransition(String key) {
        int fromStage = getCurrentStageIndex();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX checkpoint debug startTransition: fromStage={}, fromName={}, targetName={}",
                fromStage, currentState == null ? "null" : currentState.name, key);
        super.startTransition(key);
        if (parent != null && parent.structure instanceof StructureLittleVecXOverlayAnimation)
            ((StructureLittleVecXOverlayAnimation) parent.structure).onCheckpointTransitionStarted(fromStage, getStageIndex(key));
    }

    @Override
    public void endTransition() {
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX checkpoint debug endTransition.before: currentStage={}, aimedStage={}, currentName={}, aimedName={}",
                getCurrentStageIndex(), getAimedStageIndex(),
                currentState == null ? "null" : currentState.name,
                aimedState == null ? "null" : aimedState.name);
        super.endTransition();
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX checkpoint debug endTransition.after: currentStage={}, currentName={}",
                getCurrentStageIndex(), currentState == null ? "null" : currentState.name);
    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {
        nbt.setString(ORIGINAL_UUID_TAG, supplier.original().toString());
        nbt.setString(UUID_TAG, supplier.uuid.toString());
        nbt.setInteger(DURATION_TAG, duration);
        nbt.setInteger(COMPLETE_DURATION_TAG, completeDuration);
        nbt.setInteger(INTERPOLATION_TAG, interpolation);
        nbt.setBoolean(NO_CLIP_TAG, noClip);

        if (currentState != null)
            nbt.setString(CURRENT_STATE_TAG, currentState.name);
        if (aimedState != null) {
            nbt.setString(AIMED_STATE_TAG, aimedState.name);
            nbt.setInteger(TICK_TAG, tick);
        }

        NBTTagList statesList = new NBTTagList();
        for (String stateName : orderedStates) {
            AnimationControllerState state = getState(stateName);
            if (state == null)
                continue;
            NBTTagCompound stateTag = new NBTTagCompound();
            stateTag.setString("name", stateName);
            stateTag.setTag("state", state.state.writeToNBT(new NBTTagCompound()));
            statesList.appendTag(stateTag);
        }
        nbt.setTag(STATES_TAG, statesList);

        NBTTagList transitionsList = new NBTTagList();
        for (Entry<String, AnimationTimeline> entry : stateTransition.entrySet()) {
            NBTTagCompound transitionTag = entry.getValue().writeToNBT(new NBTTagCompound());
            transitionTag.setString("key", entry.getKey());
            transitionsList.appendTag(transitionTag);
        }
        nbt.setTag(TRANSITIONS_TAG, transitionsList);
    }

    @Override
    protected void readFromNBT(NBTTagCompound nbt) {
        orderedStates.clear();
        states.clear();
        stateTransition.clear();
        currentState = null;
        aimedState = null;
        tickingState = null;
        animation = null;

        supplier = new UUIDSupplier(UUID.fromString(nbt.getString(ORIGINAL_UUID_TAG)), UUID.fromString(nbt.getString(UUID_TAG)));
        duration = nbt.getInteger(DURATION_TAG);
        completeDuration = nbt.getInteger(COMPLETE_DURATION_TAG);
        interpolation = nbt.getInteger(INTERPOLATION_TAG);
        noClip = nbt.getBoolean(NO_CLIP_TAG);
        turnBack = null;

        String currentName = nbt.getString(CURRENT_STATE_TAG);
        NBTTagList statesList = nbt.getTagList(STATES_TAG, 10);
        for (int i = 0; i < statesList.tagCount(); i++) {
            NBTTagCompound stateTag = statesList.getCompoundTagAt(i);
            String name = stateTag.getString("name");
            AnimationState state = new AnimationState(stateTag.getCompoundTag("state"));
            addOrderedState(name, state, currentState == null && name.equals(currentName));
        }

        if (currentState == null && !orderedStates.isEmpty())
            currentState = getState(orderedStates.get(0));

        NBTTagList transitionsList = nbt.getTagList(TRANSITIONS_TAG, 10);
        for (int i = 0; i < transitionsList.tagCount(); i++) {
            NBTTagCompound transitionTag = transitionsList.getCompoundTagAt(i);
            addTransition(transitionTag.getString("key"), new AnimationTimeline(transitionTag));
        }

        if (nbt.hasKey(AIMED_STATE_TAG)) {
            String aimedName = nbt.getString(AIMED_STATE_TAG);
            startTransition(aimedName);
            tick = nbt.getInteger(TICK_TAG);
        }
    }

    @Override
    public void transform(LittleTransformation transformation) {
        super.transform(transformation);
    }
}

