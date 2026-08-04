package com.integral.littlevecx.client.gui;

import com.creativemd.creativecore.common.gui.ContainerControl;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.container.SlotControl;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiScrollBox;
import com.creativemd.littletiles.common.container.SubContainerStorage.StorageSize;
import com.integral.littlevecx.storage.StructureLittleVecXStorage;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SubGuiLittleVecXStorage extends SubGui {

    public StructureLittleVecXStorage storage;
    public final StorageSize size;

    public SubGuiLittleVecXStorage(StructureLittleVecXStorage storage) {
        super(250, 250);
        this.size = StorageSize.getSizeFromInventory(storage.inventory);
        this.storage = storage;
        setDimension(size.width, size.height);
    }

    @Override
    public void addContainerControls() {
        // This GUI can be opened multiple times for the same underlying structure/container.
        // If we keep old controls around, CreativeCore may end up rendering a control whose parent was cleared on close,
        // causing an NPE in GuiControl.isMouseOver (parent == null). Rebuild controls on every open.
        controls.clear();

        if (!size.scrollbox) {
            super.addContainerControls();
            addSortButton();
            return;
        }

        GuiScrollBox box = new GuiScrollBox("box", 0, 0, 244, 150);
        addControl(box);
        for (int i = 0; i < container.controls.size(); i++) {
            ContainerControl control = container.controls.get(i);
            control.onOpened();

            if (control instanceof SlotControl && ((SlotControl) control).slot.inventory == storage.inventory) {
                box.addControl(control.getGuiControl());
            } else
                addControl(control.getGuiControl());
        }
        addSortButton();
    }

    private void addSortButton() {
        int x = size.playerOffsetX + 170;
        int y = size.playerOffsetY;
        if (size == StorageSize.SMALL) {
            x = size.playerOffsetX;
            y = size.playerOffsetY - 23;
        }

        controls.add(new GuiButton("sort", x, y) {

            @Override
            public void onClicked(int x, int y, int button) {
                NBTTagCompound nbt = new NBTTagCompound();
                nbt.setBoolean("sort", true);
                sendPacketToServer(nbt);
            }
        });
    }

    @Override
    public void createControls() {
    }
}
