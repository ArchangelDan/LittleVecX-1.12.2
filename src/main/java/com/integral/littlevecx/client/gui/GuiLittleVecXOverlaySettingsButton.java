package com.integral.littlevecx.client.gui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiCheckBox;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDoorSettings.GuiDoorSettingsButton;

import net.minecraft.nbt.NBTTagCompound;

public class GuiLittleVecXOverlaySettingsButton extends GuiDoorSettingsButton {

    private static final Logger LOGGER = LogManager.getLogger("LittleVecX");

    public interface SettingsConsumer {
        void accept(GuiLittleVecXOverlaySettingsButton button);
    }

    public boolean shiftRightClickStepBackMode;
    /** Prevents an overlay animation from travelling through solid world blocks. */
    public boolean blockMovementCollision;
    private final SettingsConsumer consumer;

    public GuiLittleVecXOverlaySettingsButton(String name, int x, int y, boolean stayAnimated, boolean disableRightClick,
            boolean noClip, boolean playPlaceSounds, boolean shiftRightClickStepBackMode, boolean blockMovementCollision, SettingsConsumer consumer) {
        super(name, x, y, stayAnimated, disableRightClick, noClip, playPlaceSounds);
        this.shiftRightClickStepBackMode = shiftRightClickStepBackMode;
        this.blockMovementCollision = blockMovementCollision;
        this.consumer = consumer;
    }

    @Override
    public void onClicked(int x, int y, int button) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("dialog", true);

        SubGui dialog = new SubGuiLittleVecXOverlaySettings(this);
        SubGui owner = getGui();
        if (owner == null || owner.gui == null)
            return;
        dialog.gui = owner.gui;

        PacketHandler.sendPacketToServer(new GuiLayerPacket(nbt, dialog.gui.getLayers().size() - 1, false));
        dialog.container = new SubContainerEmpty(getPlayer());
        dialog.gui.addLayer(dialog);
        dialog.onOpened();
    }

    public static class SubGuiLittleVecXOverlaySettings extends SubGui {

        private final GuiLittleVecXOverlaySettingsButton button;
        private boolean applied = false;

        public SubGuiLittleVecXOverlaySettings(GuiLittleVecXOverlaySettingsButton button) {
            // Keep the native LittleTiles settings dialog dimensions. Smaller layers can be
            // clipped by CreativeCore when opened over the animation editor.
            super(176, 166);
            this.button = button;
        }

        @Override
        public void createControls() {
            addControl(new GuiCheckBox("rightclick", CoreControl.translate("gui.door.rightclick"), 0, 0, button.disableRightClick));
            addControl(new GuiCheckBox("noClip", CoreControl.translate("gui.door.noClip"), 0, 45, button.noClip));
            // Directly below No Clip: this is deliberately independent from it. No Clip
            // controls entity collision; this option stops a checkpoint transition before
            // its tiles would intersect the world.
            addControl(new GuiCheckBox("blockMovementCollision", CoreControl.translate("gui.littlevecx.overlay.block_movement_collision"), 0, 65,
                    button.blockMovementCollision));
            addControl(new GuiCheckBox("playPlaceSounds", CoreControl.translate("gui.door.playPlaceSounds"), 0, 85, button.playPlaceSounds));
            addControl(new GuiCheckBox("stepBackMode", CoreControl.translate("gui.littlevecx.overlay.step_back_mode"), 0, 105,
                    button.shiftRightClickStepBackMode));

            addControl(new GuiButton("close", 0, 143) {
                @Override
                public void onClicked(int x, int y, int buttonId) {
                    applyValues();
                    SubGuiLittleVecXOverlaySettings.this.closeGui();
                }
            });
        }

        private void applyValues() {
            if (applied)
                return;
            applied = true;

            GuiCheckBox rightclick = (GuiCheckBox) get("rightclick");
            GuiCheckBox noClip = (GuiCheckBox) get("noClip");
            GuiCheckBox playPlaceSounds = (GuiCheckBox) get("playPlaceSounds");
            GuiCheckBox stepBackMode = (GuiCheckBox) get("stepBackMode");
            GuiCheckBox blockMovementCollision = (GuiCheckBox) get("blockMovementCollision");

            button.stayAnimated = true;
            button.disableRightClick = rightclick.value;
            button.noClip = noClip.value;
            button.playPlaceSounds = playPlaceSounds.value;
            button.shiftRightClickStepBackMode = stepBackMode.value;
            button.blockMovementCollision = blockMovementCollision.value;
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                    "LittleVecX overlay settings debug dialogApply: stayAnimated={}, disableRightClick={}, noClip={}, playPlaceSounds={}, shiftBack={}, blockCollision={}",
                    button.stayAnimated, button.disableRightClick, button.noClip, button.playPlaceSounds,
                    button.shiftRightClickStepBackMode, button.blockMovementCollision);
            if (button.consumer != null)
                button.consumer.accept(button);
            button.raiseEvent(new GuiControlChangedEvent(button));
        }

        @Override
        public void onClosed() {
            super.onClosed();
            applyValues();
        }
    }
}

