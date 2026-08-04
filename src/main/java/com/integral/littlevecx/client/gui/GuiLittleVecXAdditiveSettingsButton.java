package com.integral.littlevecx.client.gui;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiCheckBox;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDoorSettings.GuiDoorSettingsButton;

import net.minecraft.nbt.NBTTagCompound;

/** Settings dialog used by the structure registered as {@code overlay_animation}. */
public class GuiLittleVecXAdditiveSettingsButton extends GuiDoorSettingsButton {

    public boolean blockMovementCollision;

    public GuiLittleVecXAdditiveSettingsButton(String name, int x, int y, boolean stayAnimated, boolean rightClickEnabled,
            boolean noClip, boolean playPlaceSounds, boolean blockMovementCollision) {
        super(name, x, y, stayAnimated, rightClickEnabled, noClip, playPlaceSounds);
        this.blockMovementCollision = blockMovementCollision;
    }

    @Override
    public void onClicked(int x, int y, int button) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("dialog", true);
        SubGui dialog = new SubGuiLittleVecXAdditiveSettings(this);
        dialog.gui = getParent().getOrigin().gui;
        PacketHandler.sendPacketToServer(new GuiLayerPacket(nbt, dialog.gui.getLayers().size() - 1, false));
        dialog.container = new SubContainerEmpty(getPlayer());
        dialog.gui.addLayer(dialog);
        dialog.onOpened();
    }

    private static class SubGuiLittleVecXAdditiveSettings extends SubGui {

        private final GuiLittleVecXAdditiveSettingsButton button;

        private SubGuiLittleVecXAdditiveSettings(GuiLittleVecXAdditiveSettingsButton button) {
            super(176, 166);
            this.button = button;
        }

        @Override
        public void createControls() {
            addControl(new GuiCheckBox("stayAnimated", CoreControl.translate("gui.door.stayAnimated"), 0, 0, button.stayAnimated)
                    .setCustomTooltip(CoreControl.translate("gui.door.stayAnimatedTooltip")).setEnabled(button.stayAnimatedPossible));
            addControl(new GuiCheckBox("rightclick", CoreControl.translate("gui.door.rightclick"), 0, 15, button.disableRightClick));
            addControl(new GuiCheckBox("noClip", CoreControl.translate("gui.door.noClip"), 0, 45, button.noClip));
            addControl(new GuiCheckBox("blockMovementCollision", CoreControl.translate("gui.littlevecx.overlay.block_movement_collision"), 0, 65,
                    button.blockMovementCollision));
            addControl(new GuiCheckBox("playPlaceSounds", CoreControl.translate("gui.door.playPlaceSounds"), 0, 85, button.playPlaceSounds));
            addControl(new GuiButton("Close", 0, 143) {
                @Override
                public void onClicked(int x, int y, int buttonId) {
                    closeGui();
                }
            });
        }

        @Override
        public void onClosed() {
            super.onClosed();
            button.stayAnimated = ((GuiCheckBox) get("stayAnimated")).value;
            button.disableRightClick = ((GuiCheckBox) get("rightclick")).value;
            button.noClip = ((GuiCheckBox) get("noClip")).value;
            button.blockMovementCollision = ((GuiCheckBox) get("blockMovementCollision")).value;
            button.playPlaceSounds = ((GuiCheckBox) get("playPlaceSounds")).value;
        }
    }
}
