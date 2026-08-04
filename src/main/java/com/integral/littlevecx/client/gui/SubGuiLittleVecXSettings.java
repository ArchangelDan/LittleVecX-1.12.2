package com.integral.littlevecx.client.gui;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiCheckBox;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.integral.littlevecx.client.gui.layout.LittleVecXGuiLayout;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SubGuiLittleVecXSettings extends SubGui {

    public GuiLittleVecXSettingsButton button;

    public SubGuiLittleVecXSettings(GuiLittleVecXSettingsButton button) {
        super(160, 82);
        this.button = button;
    }

    @Override
    public void createControls() {
        LittleVecXGuiLayout layout = new LittleVecXGuiLayout(width, height);
        int noClipY = layout.top();
        int stayAnimatedY = layout.nextRow(noClipY, 10, 4);

        addControl(new GuiCheckBox("noClip", CoreControl.translate("gui.littlevecx.noclip"), layout.left(), noClipY, button.noClip));
        addControl(new GuiCheckBox("forceStayAnimatedDoors", CoreControl.translate("gui.littlevecx.force_stay_animated_doors"), layout.left(), stayAnimatedY,
                button.forceStayAnimatedDoors));

        addControl(new GuiButton("close", "Close", layout.center(40), layout.footerY(10) - 10, 40, 10) {

            @Override
            public void onClicked(int x, int y, int button) {
                onClosed();
                gui.removeLayer(SubGuiLittleVecXSettings.this);
            }
        });
    }

    @Override
    public void onClosed() {
        super.onClosed();
        GuiCheckBox noClip = (GuiCheckBox) get("noClip");
        GuiCheckBox stayAnimatedDoors = (GuiCheckBox) get("forceStayAnimatedDoors");
        button.noClip = noClip.value;
        button.forceStayAnimatedDoors = stayAnimatedDoors.value;
    }

    public static class GuiLittleVecXSettingsButton extends GuiButton {

        public boolean noClip;
        public boolean forceStayAnimatedDoors;

        public GuiLittleVecXSettingsButton(String name, int x, int y, boolean noClip, boolean forceStayAnimatedDoors) {
            super(name, "Settings", x, y, 40, 7);
            this.noClip = noClip;
            this.forceStayAnimatedDoors = forceStayAnimatedDoors;
        }

        @Override
        public void onClicked(int x, int y, int button) {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setBoolean("dialog", true);
            SubGuiLittleVecXSettings dialog = new SubGuiLittleVecXSettings(this);
            SubGui owner = getGui();
            if (owner == null || owner.gui == null)
                return;
            dialog.gui = owner.gui;
            PacketHandler.sendPacketToServer(new GuiLayerPacket(nbt, dialog.gui.getLayers().size() - 1, false));
            dialog.container = new SubContainerEmpty(getPlayer());
            dialog.gui.addLayer(dialog);
            dialog.onOpened();
        }
    }
}
