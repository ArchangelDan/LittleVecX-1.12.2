package com.integral.littlevecx.client.gui;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiLabel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiStateButton;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.integral.littlevecx.animation.LittleVecXAnimationTriggerMode;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDoorSettings.GuiDoorSettingsButton;

import net.minecraft.nbt.NBTTagCompound;

public class GuiLittleVecXAnimationLayerSettingsButton extends GuiDoorSettingsButton {

    public LittleVecXAnimationTriggerMode trigger;

    public GuiLittleVecXAnimationLayerSettingsButton(String name, int x, int y, LittleVecXAnimationTriggerMode trigger) {
        super(name, x, y, false, true, false, true);
        this.trigger = trigger == null ? LittleVecXAnimationTriggerMode.NONE : trigger;
    }

    @Override
    public void onClicked(int x, int y, int button) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("dialog", true);

        SubGui dialog = new SubGuiLittleVecXAnimationLayerSettings(this);
        SubGui owner = getGui();
        if (owner == null || owner.gui == null)
            return;
        dialog.gui = owner.gui;

        PacketHandler.sendPacketToServer(new GuiLayerPacket(nbt, dialog.gui.getLayers().size() - 1, false));
        dialog.container = new SubContainerEmpty(getPlayer());
        dialog.gui.addLayer(dialog);
        dialog.onOpened();
    }

    public static class SubGuiLittleVecXAnimationLayerSettings extends SubGui {

        private final GuiLittleVecXAnimationLayerSettingsButton button;

        public SubGuiLittleVecXAnimationLayerSettings(GuiLittleVecXAnimationLayerSettingsButton button) {
            super(140, 60);
            this.button = button;
        }

        @Override
        public void createControls() {
            addControl(new GuiLabel("triggerLabel", CoreControl.translate("gui.littlevecx.animation_trigger"), 0, 0));
            addControl(new GuiStateButton("trigger", button.trigger.ordinal(), 0, 14, 120, 10, LittleVecXAnimationTriggerMode.captions()));
            addControl(new GuiButton("close", 90, 41) {
                @Override
                public void onClicked(int x, int y, int buttonId) {
                    GuiStateButton trigger = (GuiStateButton) get("trigger");
                    button.trigger = LittleVecXAnimationTriggerMode.fromIndex(trigger.getState());
                    onClosed();
                    gui.removeLayer(SubGuiLittleVecXAnimationLayerSettings.this);
                }
            });
        }
    }
}
