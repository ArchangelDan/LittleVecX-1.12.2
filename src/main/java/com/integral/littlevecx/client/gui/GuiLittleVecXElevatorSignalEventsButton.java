package com.integral.littlevecx.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiLabel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiListBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiPanel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiScrollBox;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiSignalEvents;
import com.creativemd.littletiles.client.gui.signal.SubGuiDialogSignal;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiSignalEvents.GuiSignalEvent;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiSignalEvents.GuiSignalEventsButton;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;

public class GuiLittleVecXElevatorSignalEventsButton extends GuiSignalEventsButton {

    private static final int WINDOW_WIDTH = 390;
    private static final int WINDOW_HEIGHT = 220;
    private static final int CONTENT_WIDTH = 170;
    private static final int CONTENT_HEIGHT = 182;
    private static final int COMPONENTS_X = 180;
    private static final int COMPONENTS_WIDTH = 210;
    private static final int COMPONENTS_HEIGHT = 182;
    private static final int PANEL_WIDTH = 158;
    private static final int EDIT_BUTTON_X = 84;
    private static final int RESET_BUTTON_X = 122;
    private static final int FOOTER_Y = 192;
    private static final int SAVE_BUTTON_X = 146;

    private static final Pattern CABIN_PATTERN = Pattern.compile("^button_cabin_(\\d+)$");
    private static final Pattern FLOOR_PATTERN = Pattern.compile("^button_floor_(\\d+)$");
    private static final Pattern CURRENT_FLOOR_PATTERN = Pattern.compile("^current_floor_(\\d+)$");

    public interface SignalEventsConsumer {
        void accept(List<GuiSignalEvent> events);
    }

    private final SignalEventsConsumer consumer;
    private final List<com.creativemd.littletiles.client.gui.signal.SubGuiDialogSignal.GuiSignalComponent> allInputs = new ArrayList<>();
    private final List<com.creativemd.littletiles.client.gui.signal.SubGuiDialogSignal.GuiSignalComponent> allOutputs = new ArrayList<>();
    private final List<GuiSignalEvent> allEvents = new ArrayList<>();
    private int maxFloors;

    public GuiLittleVecXElevatorSignalEventsButton(String name, int x, int y, LittlePreviews previews, LittleStructure structure,
            LittleStructureType type, int maxFloors, @Nullable List<GuiSignalEvent> cachedEvents,
            @Nullable SignalEventsConsumer consumer) {
        super(name, x, y, previews, structure, type);
        this.consumer = consumer;
        this.maxFloors = Math.max(2, maxFloors);
        allInputs.addAll(inputs);
        allOutputs.addAll(outputs);
        allEvents.addAll(copyEvents(events));
        applyCachedEvents(cachedEvents);
        rebuildVisibleSignals();
    }

    @Override
    public void onClicked(int x, int y, int button) {
        openClientLayer(new SubGuiLittleVecXElevatorSignalEvents(this));
    }

    public void setMaxFloors(int maxFloors) {
        this.maxFloors = Math.max(2, maxFloors);
        rebuildVisibleSignals();
    }

    private void rebuildVisibleSignals() {
        inputs.clear();
        outputs.clear();

        for (com.creativemd.littletiles.client.gui.signal.SubGuiDialogSignal.GuiSignalComponent component : allInputs)
            if (isVisibleSignal(component.totalName))
                inputs.add(component);

        for (com.creativemd.littletiles.client.gui.signal.SubGuiDialogSignal.GuiSignalComponent component : allOutputs)
            if (isVisibleSignal(component.totalName))
                outputs.add(component);

        events = new ArrayList<>();
        for (GuiSignalEvent event : allEvents)
            if (event != null && event.component != null && isVisibleSignal(event.component.totalName))
                events.add(event.copy());
    }

    private void applyCachedEvents(@Nullable List<GuiSignalEvent> cachedEvents) {
        if (cachedEvents == null || cachedEvents.isEmpty())
            return;

        HashMap<String, GuiSignalEvent> cachedByName = new HashMap<>();
        for (GuiSignalEvent cachedEvent : cachedEvents) {
            if (cachedEvent == null || cachedEvent.component == null || cachedEvent.component.totalName == null)
                continue;
            cachedByName.put(cachedEvent.component.totalName, cachedEvent);
        }

        for (GuiSignalEvent event : allEvents) {
            if (event.component == null || event.component.totalName == null)
                continue;

            GuiSignalEvent cachedEvent = cachedByName.get(event.component.totalName);
            if (cachedEvent == null)
                continue;

            event.condition = cachedEvent.condition;
            event.modeConfig = cachedEvent.modeConfig != null ? cachedEvent.modeConfig.copy() : null;
        }
    }

    public List<GuiSignalEvent> getAllEventsCopy() {
        return copyEvents(allEvents);
    }

    public List<com.creativemd.littletiles.client.gui.signal.SubGuiDialogSignal.GuiSignalComponent> getAllInputsCopy() {
        return new ArrayList<>(allInputs);
    }

    @Override
    public void setEventsInStructure(LittleStructure structure) {
        HashMap<Integer, com.creativemd.littletiles.common.structure.signal.output.SignalExternalOutputHandler> map = new HashMap<>();
        for (GuiSignalEvent event : allEvents) {
            if (event == null || event.component == null)
                continue;

            if (event.component.external) {
                if (event.condition != null)
                    map.put(event.component.index,
                            new com.creativemd.littletiles.common.structure.signal.output.SignalExternalOutputHandler(null,
                                    event.component.index, event.condition, (x) -> event.getHandler(x, structure)));
            } else {
                com.creativemd.littletiles.common.structure.signal.output.InternalSignalOutput output = structure
                        .getOutput(event.component.index);
                output.condition = event.condition;
                output.handler = event.getHandler(output, structure);
            }
        }
        structure.setExternalHandler(map);
    }

    private boolean isVisibleSignal(@Nullable String totalName) {
        Integer cabinFloor = parseFloor(totalName, CABIN_PATTERN);
        if (cabinFloor != null)
            return cabinFloor <= maxFloors;

        Integer floorCall = parseFloor(totalName, FLOOR_PATTERN);
        if (floorCall != null)
            return floorCall <= maxFloors;

        Integer currentFloor = parseFloor(totalName, CURRENT_FLOOR_PATTERN);
        if (currentFloor != null)
            return currentFloor <= maxFloors;

        return true;
    }

    @Nullable
    private static Integer parseFloor(@Nullable String totalName, Pattern pattern) {
        if (totalName == null)
            return null;

        Matcher matcher = pattern.matcher(totalName);
        if (!matcher.matches())
            return null;

        try {
            int floor = Integer.parseInt(matcher.group(1));
            return floor > 0 ? floor : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static List<GuiSignalEvent> copyEvents(@Nullable List<GuiSignalEvent> source) {
        List<GuiSignalEvent> copy = new ArrayList<>();
        if (source == null)
            return copy;

        for (GuiSignalEvent event : source)
            if (event != null)
                copy.add(event.copy());
        return copy;
    }

    private void mergeEvents(@Nullable List<GuiSignalEvent> updatedEvents) {
        HashMap<String, GuiSignalEvent> updatedByName = new HashMap<>();
        if (updatedEvents != null) {
            for (GuiSignalEvent event : updatedEvents)
                if (event != null && event.component != null && event.component.totalName != null)
                    updatedByName.put(event.component.totalName, event);
        }

        for (int i = 0; i < allEvents.size(); i++) {
            GuiSignalEvent current = allEvents.get(i);
            if (current == null || current.component == null || current.component.totalName == null)
                continue;

            GuiSignalEvent updated = updatedByName.get(current.component.totalName);
            if (updated != null)
                allEvents.set(i, updated.copy());
        }

        rebuildVisibleSignals();
    }

    public static class SubGuiLittleVecXElevatorSignalEvents extends SubGuiSignalEvents {

        private final GuiLittleVecXElevatorSignalEventsButton button;
        private boolean applied = false;
        private boolean discardChanges = false;

        public SubGuiLittleVecXElevatorSignalEvents(GuiLittleVecXElevatorSignalEventsButton button) {
            super(button);
            this.button = button;
            this.width = WINDOW_WIDTH;
            this.height = WINDOW_HEIGHT;
        }

        @Override
        public void createControls() {
            GuiScrollBox box = new GuiScrollBox("content", 0, 0, CONTENT_WIDTH, CONTENT_HEIGHT);
            List<String> values = new ArrayList<>();
            values.add("Components:");

            for (com.creativemd.littletiles.client.gui.signal.SubGuiDialogSignal.GuiSignalComponent component : button.inputs)
                values.add(component.display());

            GuiListBox components = new GuiListBox("components", COMPONENTS_X, 0, COMPONENTS_WIDTH, COMPONENTS_HEIGHT,
                    values);

            controls.add(components);
            controls.add(box);

            for (GuiSignalEvent event : events)
                addEntry(event);

            controls.add(new GuiButton("save", SAVE_BUTTON_X, FOOTER_Y) {
                @Override
                public void onClicked(int x, int y, int buttonId) {
                    applyEdits();
                    SubGuiLittleVecXElevatorSignalEvents.this.closeGui();
                }
            });
            controls.add(new GuiButton("cancel", 0, FOOTER_Y) {
                @Override
                public void onClicked(int x, int y, int buttonId) {
                    discardChanges = true;
                    SubGuiLittleVecXElevatorSignalEvents.this.closeGui();
                }
            });
        }

        @Override
        public void addEntry(GuiSignalEvent event) {
            GuiScrollBox box = (GuiScrollBox) get("content");
            GuiPanel panel = new GuiPanel("event", 2, 2, PANEL_WIDTH, 30);
            panel.addControl(new GuiLabel("label", 0, 0));
            panel.addControl(new GuiLabel("mode", 0, 16));
            panel.addControl(new GuiButton("edit", EDIT_BUTTON_X, 14, 30, 10) {

                @Override
                public void onClicked(int x, int y, int buttonId) {
                    openClientLayer(new SubGuiDialogSignal(button.getAllInputsCopy(), event));
                }
            });

            panel.addControl(new GuiButton("reset", RESET_BUTTON_X, 14, 30, 10) {

                @Override
                public void onClicked(int x, int y, int buttonId) {
                    event.reset();
                }
            });

            box.addControl(panel);
            event.panel = panel;
            event.update();
            reloadListBox();
        }

        private void applyEdits() {
            if (applied || discardChanges)
                return;
            applied = true;

            button.mergeEvents(copyEvents(events));
            if (button.consumer != null)
                button.consumer.accept(button.getAllEventsCopy());
            button.raiseEvent(new GuiControlChangedEvent(button));
        }

        @Override
        public void onClosed() {
            super.onClosed();
            applyEdits();
        }
    }
}
