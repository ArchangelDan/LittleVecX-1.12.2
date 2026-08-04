package com.integral.littlevecx.client.gui;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiLabel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiStateButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiTextfield;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.integral.littlevecx.client.gui.layout.LittleVecXGuiLayout;
import com.creativemd.littletiles.client.gui.controls.GuiLTDistance;
import com.creativemd.littletiles.common.structure.animation.ValueTimeline;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;

import net.minecraft.nbt.NBTTagCompound;

public class GuiLittleVecXElevatorBasicSettingsButton extends GuiButton {

    public interface BasicSettingsConsumer {
        void accept(GuiLittleVecXElevatorBasicSettingsButton button);
    }

    public int floorDistance;
    public int offGrid;
    public int duration;
    public int interpolation;

    private final BasicSettingsConsumer consumer;

    public GuiLittleVecXElevatorBasicSettingsButton(String name, int x, int y, int floorDistance, int offGrid, int duration,
            int interpolation, BasicSettingsConsumer consumer) {
        super(name, CoreControl.translate("gui.littlevecx.elevator_basic_animation"), x, y, 78, 10);
        this.floorDistance = Math.max(1, floorDistance);
        this.offGrid = offGrid > 0 ? offGrid : 16;
        this.duration = Math.max(1, duration);
        this.interpolation = clampInterpolation(interpolation);
        this.consumer = consumer;
    }

    @Override
    public void onClicked(int x, int y, int button) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("dialog", true);

        SubGui dialog = new SubGuiLittleVecXElevatorBasicSettings(this);
        SubGui owner = getGui();
        if (owner == null || owner.gui == null)
            return;
        dialog.gui = owner.gui;

        PacketHandler.sendPacketToServer(new GuiLayerPacket(nbt, dialog.gui.getLayers().size() - 1, false));
        dialog.container = new SubContainerEmpty(getPlayer());
        dialog.gui.addLayer(dialog);
        dialog.onOpened();
    }

    private static int clampInterpolation(int interpolation) {
        if (interpolation < 0 || interpolation >= ValueTimeline.interpolationTypes.length)
            return 0;
        return interpolation;
    }

    public static class SubGuiLittleVecXElevatorBasicSettings extends SubGui {

        private final GuiLittleVecXElevatorBasicSettingsButton button;
        private boolean applied = false;

        public SubGuiLittleVecXElevatorBasicSettings(GuiLittleVecXElevatorBasicSettingsButton button) {
            super(246, 120);
            this.button = button;
        }

        @Override
        public void createControls() {
            LittleVecXGuiLayout layout = new LittleVecXGuiLayout(width, height);
            int distanceLabelY = 8;
            int distanceFieldY = 20;
            int durationLabelY = 40;
            int durationFieldY = 52;
            int interpolationLabelY = 72;
            int interpolationFieldY = 84;
            int distanceFieldX = layout.center(72);
            int durationFieldWidth = 52;
            int durationFieldX = layout.center(durationFieldWidth);
            int interpolationFieldWidth = 112;
            int interpolationFieldX = layout.center(interpolationFieldWidth);
            int closeY = 104;

            LittleGridContext context;
            try {
                context = LittleGridContext.get(button.offGrid);
            } catch (RuntimeException e) {
                context = LittleGridContext.get();
            }

            GuiLabel distanceLabel = new GuiLabel("distanceLabel", CoreControl.translate("gui.littlevecx.elevator_floor_distance"), 0, distanceLabelY);
            distanceLabel.posX = layout.center(distanceLabel.width);
            addControl(distanceLabel);
            addControl(new GuiLTDistance("distance", distanceFieldX, distanceFieldY, context, button.floorDistance));

            GuiLabel durationLabel = new GuiLabel("durationLabel", CoreControl.translate("gui.littlevecx.elevator_duration"), 0, durationLabelY);
            durationLabel.posX = layout.center(durationLabel.width);
            addControl(durationLabel);
            addControl(new GuiTextfield("duration", Integer.toString(Math.max(1, button.duration)), durationFieldX, durationFieldY, durationFieldWidth, 10)
                    .setNumbersOnly());

            GuiLabel interpolationLabel = new GuiLabel("interpolationLabel", CoreControl.translate("gui.littlevecx.elevator_interpolation"), 0,
                    interpolationLabelY);
            interpolationLabel.posX = layout.center(interpolationLabel.width);
            addControl(interpolationLabel);
            addControl(new GuiStateButton("interpolation", button.interpolation, interpolationFieldX, interpolationFieldY, interpolationFieldWidth, 10,
                    ValueTimeline.interpolationTypes));

            addControl(new GuiButton("close", layout.center(42), closeY, 42, 10) {
                @Override
                public void onClicked(int x, int y, int buttonId) {
                    applyValues();
                    SubGuiLittleVecXElevatorBasicSettings.this.closeGui();
                }
            });
        }

        private void applyValues() {
            if (applied)
                return;
            applied = true;

            GuiLTDistance distance = (GuiLTDistance) get("distance");
            button.floorDistance = Math.max(1, Math.abs(distance.getDistance()));
            button.offGrid = Math.max(1, distance.getDistanceContext().size);
            button.duration = parsePositiveInt((GuiTextfield) get("duration"), button.duration);
            button.interpolation = clampInterpolation(((GuiStateButton) get("interpolation")).getState());

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

    private static int parsePositiveInt(GuiTextfield field, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(field.text));
        } catch (NumberFormatException ignored) {
            return Math.max(1, fallback);
        }
    }
}
