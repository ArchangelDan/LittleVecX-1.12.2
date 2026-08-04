package com.integral.littlevecx.animation;

import java.util.UUID;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDoorSettings.GuiDoorSettingsButton;
import com.integral.littlevecx.client.gui.GuiLittleVecXLoopSettingsButton;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.type.UUIDSupplier;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.action.LittleActionException.LittleActionExceptionHidden;
import com.creativemd.littletiles.common.entity.DoorController;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.packet.LittleAnimationControllerPacket;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.animation.AnimationGuiHandler;
import com.creativemd.littletiles.common.structure.registry.LittleStructureGuiParser;
import com.creativemd.littletiles.common.structure.registry.LittleStructureRegistry;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType;
import com.creativemd.littletiles.common.structure.signal.output.InternalSignalOutput;
import com.creativemd.littletiles.common.structure.type.door.LittleAdvancedDoor;
import com.creativemd.littletiles.common.structure.type.door.LittleDoor;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.parent.IStructureTileList;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.util.place.Placement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class StructureLittleVecXLoopAnimation extends LittleAdvancedDoor {

    private static final String RAMP_UP_TICKS_TAG = "littlevecxLoopRampUpTicks";
    private static final String RAMP_DOWN_TICKS_TAG = "littlevecxLoopRampDownTicks";

    /** Duration of the speed-up phase in ticks. Zero keeps the legacy immediate start. */
    public int rampUpTicks = 0;
    /** Duration of the braking phase in ticks. Zero keeps the legacy immediate stop. */
    public int rampDownTicks = 0;

    public StructureLittleVecXLoopAnimation(LittleStructureType type, IStructureTileList mainBlock) {
        super(type, mainBlock);
        stayAnimated = true;
    }

    @Override
    protected void loadFromNBTExtra(NBTTagCompound nbt) {
        super.loadFromNBTExtra(nbt);
        stayAnimated = true;
        rampUpTicks = Math.max(0, nbt.getInteger(RAMP_UP_TICKS_TAG));
        rampDownTicks = Math.max(0, nbt.getInteger(RAMP_DOWN_TICKS_TAG));
    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {
        stayAnimated = true;
        super.writeToNBTExtra(nbt);
        nbt.setInteger(RAMP_UP_TICKS_TAG, Math.max(0, rampUpTicks));
        nbt.setInteger(RAMP_DOWN_TICKS_TAG, Math.max(0, rampDownTicks));
    }

    @Override
    public DoorController createController(UUIDSupplier supplier, Placement placement, int completeDuration) {
        DoorController controller = super.createController(supplier, placement, completeDuration);
        LittleVecXLoopDoorController loopController = new LittleVecXLoopDoorController(controller);
        loopController.setRampTicks(rampUpTicks, rampDownTicks);
        return loopController;
    }

    @Override
    public EntityAnimation activate(DoorActivator activator, EntityPlayer player, UUID uuid) throws LittleActionException {
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

        if (uuid == null) {
            if (isAnimated() && getAnimation() != null)
                uuid = getAnimation().getUniqueID();
            else
                uuid = UUID.randomUUID();
        }

        if (getWorld().isRemote) {
            // The server owns the exact stop frame. Do not stop locally: network latency would
            // freeze the client early, then snap it forward to the server's later frame.
            sendActivationToServer(activator, player, uuid);
            return null;
        }

        LittleVecXLoopDoorController controller = getLoopController();
        if (controller != null) {
            // Persistent loop animations may outlive an edit of their recipe. Refresh the values on
            // every toggle so the Settings dialog affects both newly created and already live loops.
            controller.setRampTicks(rampUpTicks, rampDownTicks);
            controller.activate();
            opened = controller.isLoopRunning();
            syncStateOutput(opened);
            PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationControllerPacket(animation), animation, null);
            notifyChange();
            return animation;
        }

        if (!canOpenDoor(player)) {
            if (player != null)
                player.sendStatusMessage(new net.minecraft.util.text.TextComponentTranslation("exception.door.notenoughspace"), true);
            throw new LittleActionException("Cannot open door");
        }

        opened = true;
        if (activator != DoorActivator.SIGNAL)
            syncStateOutput(true);
        return openDoor(player, new UUIDSupplier(uuid), false);
    }

    @Override
    public boolean onBlockActivated(World world, LittleTile tile, BlockPos pos, EntityPlayer player, EnumHand hand, ItemStack stack,
            EnumFacing facing, float hitX, float hitY, float hitZ, com.creativemd.littletiles.common.action.block.LittleActionActivated action)
            throws LittleActionException {
        activate(DoorActivator.RIGHTCLICK, player, null);
        action.preventInteraction = true;
        return true;
    }

    @Override
    public void performInternalOutputChange(InternalSignalOutput output) {
        String identifier = output.component.identifier;
        if (!"state".equals(identifier))
            return;

        boolean[] state = output.getState();
        if (state == null || state.length == 0)
            return;

        boolean targetRunning = state[0];
        if (targetRunning == isLoopRunning())
            return;

        try {
            activate(DoorActivator.SIGNAL, null, null);
        } catch (LittleActionException ignored) {
        }
    }

    private boolean isLoopRunning() {
        LittleVecXLoopDoorController controller = getLoopController();
        return controller != null ? controller.isLoopRunning() : opened;
    }

    @Nullable
    private LittleVecXLoopDoorController getLoopController() {
        if (animation == null || !(animation.controller instanceof LittleVecXLoopDoorController))
            return null;
        return (LittleVecXLoopDoorController) animation.controller;
    }

    private void syncStateOutput(boolean running) {
        try {
            InternalSignalOutput stateOutput = getOutput(0);
            if (stateOutput == null)
                return;
            boolean[] state = stateOutput.getState();
            if (state == null || state.length == 0 || state[0] == running)
                return;
            state[0] = running;
            stateOutput.changed();
        } catch (Exception ignored) {
        }
    }

    public static class StructureLittleVecXLoopAnimationParser extends LittleAdvancedDoor.LittleAdvancedDoorParser {

        public StructureLittleVecXLoopAnimationParser(GuiParent parent, AnimationGuiHandler handler) {
            super(parent, handler);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void createControls(LittlePreviews previews, LittleStructure structure) {
            super.createControls(previews, structure);
            StructureLittleVecXLoopAnimation loop = structure instanceof StructureLittleVecXLoopAnimation
                    ? (StructureLittleVecXLoopAnimation) structure : null;
            GuiDoorSettingsButton settings = (GuiDoorSettingsButton) parent.get("settings");
            if (settings != null)
                parent.controls.remove(settings);
            parent.controls.add(new GuiLittleVecXLoopSettingsButton("settings", 0, 110,
                    settings != null && settings.stayAnimated, settings == null || settings.disableRightClick,
                    settings != null && settings.noClip, settings == null || settings.playPlaceSounds,
                    loop == null ? 0 : loop.rampUpTicks, loop == null ? 0 : loop.rampDownTicks));
        }

        @Override
        @SideOnly(Side.CLIENT)
        public LittleStructure parseStructure(LittlePreviews previews) {
            LittleStructure structure = super.parseStructure(previews);
            if (!(structure instanceof LittleAdvancedDoor))
                return structure;

            LittleAdvancedDoor door = (LittleAdvancedDoor) structure;
            StructureLittleVecXLoopAnimation loop = createStructure(StructureLittleVecXLoopAnimation.class, null);
            loop.axisCenter = door.axisCenter == null ? null
                    : new com.creativemd.littletiles.common.structure.relative.StructureRelative(door.axisCenter.write());
            loop.duration = door.duration;
            loop.events = door.events;
            loop.disableRightClick = door.disableRightClick;
            loop.interpolation = door.interpolation;
            loop.rotX = door.rotX == null ? null : door.rotX.copy();
            loop.rotY = door.rotY == null ? null : door.rotY.copy();
            loop.rotZ = door.rotZ == null ? null : door.rotZ.copy();
            loop.offGrid = door.offGrid;
            loop.offX = door.offX == null ? null : door.offX.copy();
            loop.offY = door.offY == null ? null : door.offY.copy();
            loop.offZ = door.offZ == null ? null : door.offZ.copy();
            loop.noClip = door.noClip;
            loop.playPlaceSounds = door.playPlaceSounds;
            GuiLittleVecXLoopSettingsButton settings = (GuiLittleVecXLoopSettingsButton) parent.get("settings");
            if (settings != null)
                settings.commitDialogValues();
            loop.rampUpTicks = settings == null ? 0 : settings.rampUpTicks;
            loop.rampDownTicks = settings == null ? 0 : settings.rampDownTicks;
            loop.stayAnimated = true;
            loop.opened = false;
            return loop;
        }


        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructureType getStructureType() {
            return LittleStructureRegistry.getStructureType(StructureLittleVecXLoopAnimation.class);
        }
    }
}
