package com.integral.littlevecx.client.gui;

import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDoorEvents;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDoorEvents.GuiDoorEventsButton;
import com.creativemd.littletiles.common.structure.type.door.LittleDoorBase;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;

import net.minecraft.nbt.NBTTagCompound;

public class GuiLittleVecXAnimationLayerEventsButton extends GuiDoorEventsButton {

    public GuiLittleVecXAnimationLayerEventsButton(String name, int x, int y, LittlePreviews previews, LittleDoorBase door) {
        super(name, x, y, previews, door);
    }

    @Override
    public void onClicked(int x, int y, int button) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("dialog", true);

        SubGuiDoorEvents dialog = new SubGuiDoorEvents(this);
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
