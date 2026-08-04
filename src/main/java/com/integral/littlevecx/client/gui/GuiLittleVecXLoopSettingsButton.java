package com.integral.littlevecx.client.gui;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiCheckBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiLabel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiTextfield;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDoorSettings.GuiDoorSettingsButton;

import net.minecraft.nbt.NBTTagCompound;

/** Settings button for cyclic animations, including their acceleration and braking durations. */
public class GuiLittleVecXLoopSettingsButton extends GuiDoorSettingsButton {

    public int rampUpTicks;
    public int rampDownTicks;
    private SubGuiLittleVecXLoopSettings openDialog;

    public GuiLittleVecXLoopSettingsButton(String name, int x, int y, boolean stayAnimated, boolean disableRightClick, boolean noClip,
            boolean playPlaceSounds, int rampUpTicks, int rampDownTicks) {
        super(name, x, y, stayAnimated, disableRightClick, noClip, playPlaceSounds);
        this.rampUpTicks = Math.max(0, rampUpTicks);
        this.rampDownTicks = Math.max(0, rampDownTicks);
    }

    @Override
    public void onClicked(int x, int y, int button) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("dialog", true);
        SubGui dialog = new SubGuiLittleVecXLoopSettings(this);
        SubGui owner = getGui();
        if (owner == null || owner.gui == null)
            return;
        dialog.gui = owner.gui;
        PacketHandler.sendPacketToServer(new GuiLayerPacket(nbt, dialog.gui.getLayers().size() - 1, false));
        dialog.container = new SubContainerEmpty(getPlayer());
        dialog.gui.addLayer(dialog);
        dialog.onOpened();
        openDialog = (SubGuiLittleVecXLoopSettings) dialog;
    }

    /** Flushes text fields before the parent recipe is parsed and written to NBT. */
    public void commitDialogValues() {
        if (openDialog != null)
            openDialog.apply();
    }

    private static class SubGuiLittleVecXLoopSettings extends SubGui {

        private final GuiLittleVecXLoopSettingsButton button;
        private boolean applied;

        private SubGuiLittleVecXLoopSettings(GuiLittleVecXLoopSettingsButton button) {
            super(160, 165);
            this.button = button;
        }

        @Override
        public void createControls() {
            addControl(new GuiCheckBox("stayAnimated", CoreControl.translate("gui.door.stayAnimated"), 0, 0, button.stayAnimated)
                    .setCustomTooltip(CoreControl.translate("gui.door.stayAnimatedTooltip")).setEnabled(button.stayAnimatedPossible));
            addControl(new GuiCheckBox("rightclick", CoreControl.translate("gui.door.rightclick"), 0, 15, button.disableRightClick));
            addControl(new GuiCheckBox("noClip", CoreControl.translate("gui.door.noClip"), 0, 45, button.noClip));
            addControl(new GuiCheckBox("playPlaceSounds", CoreControl.translate("gui.door.playPlaceSounds"), 0, 65, button.playPlaceSounds));
            addControl(new GuiLabel("rampUpLabel", CoreControl.translate("gui.littlevecx.loop.ramp_up") + " (ticks):", 0, 91));
            addControl(new GuiTextfield("rampUp", Integer.toString(button.rampUpTicks), 110, 90, 40, 10).setNumbersOnly());
            addControl(new GuiLabel("rampDownLabel", CoreControl.translate("gui.littlevecx.loop.ramp_down") + " (ticks):", 0, 106));
            addControl(new GuiTextfield("rampDown", Integer.toString(button.rampDownTicks), 110, 105, 40, 10).setNumbersOnly());
            addControl(new GuiButton("close", 108, 144) {
                @Override
                public void onClicked(int x, int y, int buttonId) {
                    apply();
                    closeGui();
                }
            });
        }

        @Override
        public void onClosed() {
            super.onClosed();
            apply();
            button.openDialog = null;
        }

        private void apply() {
            if (applied)
                return;
            applied = true;
            button.stayAnimated = ((GuiCheckBox) get("stayAnimated")).value;
            button.disableRightClick = ((GuiCheckBox) get("rightclick")).value;
            button.noClip = ((GuiCheckBox) get("noClip")).value;
            button.playPlaceSounds = ((GuiCheckBox) get("playPlaceSounds")).value;
            button.rampUpTicks = readTicks((GuiTextfield) get("rampUp"));
            button.rampDownTicks = readTicks((GuiTextfield) get("rampDown"));
            button.raiseEvent(new GuiControlChangedEvent(button));
        }

        private static int readTicks(GuiTextfield field) {
            try {
                return Math.max(0, Integer.parseInt(field.text));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }
}
