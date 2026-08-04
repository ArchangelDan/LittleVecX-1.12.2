package com.integral.littlevecx.client.gui;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiCheckBox;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.integral.littlevecx.client.gui.layout.LittleVecXGuiLayout;

import net.minecraft.nbt.NBTTagCompound;

public class GuiLittleVecXElevatorSettingsButton extends GuiButton {

    public interface SettingsConsumer {
        void accept(GuiLittleVecXElevatorSettingsButton button);
    }

    public boolean useCustomAnimations;
    public boolean noClip;
    public boolean playPlaceSounds;
    public boolean ignoreCallsWhileMoving;

    private final SettingsConsumer consumer;

    public GuiLittleVecXElevatorSettingsButton(String name, int x, int y, boolean useCustomAnimations, boolean noClip,
            boolean playPlaceSounds, boolean ignoreCallsWhileMoving, SettingsConsumer consumer) {
        super(name, CoreControl.translate("gui.littlevecx.elevator_settings"), x, y, 40, 7);
        this.useCustomAnimations = useCustomAnimations;
        this.noClip = noClip;
        this.playPlaceSounds = playPlaceSounds;
        this.ignoreCallsWhileMoving = ignoreCallsWhileMoving;
        this.consumer = consumer;
    }

    @Override
    public void onClicked(int x, int y, int button) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("dialog", true);

        SubGui dialog = new SubGuiLittleVecXElevatorSettings(this);
        SubGui owner = getGui();
        if (owner == null || owner.gui == null)
            return;
        dialog.gui = owner.gui;

        PacketHandler.sendPacketToServer(new GuiLayerPacket(nbt, dialog.gui.getLayers().size() - 1, false));
        dialog.container = new SubContainerEmpty(getPlayer());
        dialog.gui.addLayer(dialog);
        dialog.onOpened();
    }

    public static class SubGuiLittleVecXElevatorSettings extends SubGui {

        private final GuiLittleVecXElevatorSettingsButton button;
        private boolean applied = false;

        public SubGuiLittleVecXElevatorSettings(GuiLittleVecXElevatorSettingsButton button) {
            super(266, 92);
            this.button = button;
        }

        @Override
        public void createControls() {
            LittleVecXGuiLayout layout = new LittleVecXGuiLayout(width, height);
            int rowOneY = 8;
            int rowTwoY = 24;
            int rowThreeY = 40;
            int rowFourY = 56;
            int closeY = 74;

            GuiCheckBox customAnimations = new GuiCheckBox("customAnimations",
                    CoreControl.translate("gui.littlevecx.elevator_use_custom_animations"), 0, rowOneY, button.useCustomAnimations);
            customAnimations.posX = layout.center(customAnimations.width);
            addControl(customAnimations);

            GuiCheckBox noClip = new GuiCheckBox("noClip", CoreControl.translate("gui.door.noClip"), 0, rowTwoY, button.noClip);
            noClip.posX = layout.center(noClip.width);
            addControl(noClip);

            GuiCheckBox playPlaceSounds = new GuiCheckBox("playPlaceSounds", CoreControl.translate("gui.door.playPlaceSounds"), 0, rowThreeY,
                    button.playPlaceSounds);
            playPlaceSounds.posX = layout.center(playPlaceSounds.width);
            addControl(playPlaceSounds);

            GuiCheckBox ignoreCallsWhileMoving = new GuiCheckBox("ignoreCallsWhileMoving",
                    CoreControl.translate("gui.littlevecx.elevator_ignore_calls_while_moving"), 0, rowFourY, button.ignoreCallsWhileMoving);
            ignoreCallsWhileMoving.posX = layout.center(ignoreCallsWhileMoving.width);
            addControl(ignoreCallsWhileMoving);

            addControl(new CloseSettingsButton(this, layout.center(42), closeY));
        }

        private void applyValues() {
            if (applied)
                return;
            applied = true;

            button.useCustomAnimations = ((GuiCheckBox) get("customAnimations")).value;
            button.noClip = ((GuiCheckBox) get("noClip")).value;
            button.playPlaceSounds = ((GuiCheckBox) get("playPlaceSounds")).value;
            button.ignoreCallsWhileMoving = ((GuiCheckBox) get("ignoreCallsWhileMoving")).value;

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

    public static class CloseSettingsButton extends GuiButton {

        private final SubGuiLittleVecXElevatorSettings owner;

        public CloseSettingsButton(SubGuiLittleVecXElevatorSettings owner, int x, int y) {
            super("close", x, y, 42, 10);
            this.owner = owner;
        }

        @Override
        public void onClicked(int x, int y, int buttonId) {
            owner.applyValues();
            owner.closeGui();
        }
    }
}
