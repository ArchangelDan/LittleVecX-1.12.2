package com.integral.littlevecx.client.gui;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiLabel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiTextfield;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;

import net.minecraft.nbt.NBTTagCompound;

public class GuiLittleVecXElevatorFloorCountButton extends GuiButton {

    public interface FloorCountConsumer {
        void accept(GuiLittleVecXElevatorFloorCountButton button);
    }

    public int floorCount;
    public final int maxFloors;

    private final FloorCountConsumer consumer;

    public GuiLittleVecXElevatorFloorCountButton(String name, int x, int y, int floorCount, int maxFloors, FloorCountConsumer consumer) {
        super(name, CoreControl.translate("gui.littlevecx.elevator_floor_count"), x, y, 70, 10);
        this.maxFloors = Math.max(2, maxFloors);
        this.floorCount = clampFloorCount(floorCount, this.maxFloors);
        this.consumer = consumer;
    }

    @Override
    public void onClicked(int x, int y, int button) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("dialog", true);

        SubGui dialog = new SubGuiLittleVecXElevatorFloorCount(this);
        SubGui owner = getGui();
        if (owner == null || owner.gui == null)
            return;
        dialog.gui = owner.gui;

        PacketHandler.sendPacketToServer(new GuiLayerPacket(nbt, dialog.gui.getLayers().size() - 1, false));
        dialog.container = new SubContainerEmpty(getPlayer());
        dialog.gui.addLayer(dialog);
        dialog.onOpened();
    }

    public static int clampFloorCount(int floorCount, int maxFloors) {
        return Math.max(2, Math.min(Math.max(2, maxFloors), floorCount));
    }

    public static class SubGuiLittleVecXElevatorFloorCount extends SubGui {

        private final GuiLittleVecXElevatorFloorCountButton button;
        private boolean applied = false;

        public SubGuiLittleVecXElevatorFloorCount(GuiLittleVecXElevatorFloorCountButton button) {
            super(180, 72);
            this.button = button;
        }

        @Override
        public void createControls() {
            addControl(new GuiLabel("floorsLabel", CoreControl.translate("gui.littlevecx.elevator_floor_count_input"), 0, 0));
            addControl(new GuiTextfield("floors", Integer.toString(button.floorCount), 0, 14, 42, 10).setNumbersOnly());
            addControl(new GuiLabel("range", "2-" + button.maxFloors, 52, 16));

            addControl(new GuiButton("close", 140, 43) {
                @Override
                public void onClicked(int x, int y, int buttonId) {
                    applyValues();
                    SubGuiLittleVecXElevatorFloorCount.this.closeGui();
                }
            });
        }

        private void applyValues() {
            if (applied)
                return;
            applied = true;

            try {
                button.floorCount = clampFloorCount(Integer.parseInt(((GuiTextfield) get("floors")).text), button.maxFloors);
            } catch (NumberFormatException ignored) {
                button.floorCount = clampFloorCount(button.floorCount, button.maxFloors);
            }

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
