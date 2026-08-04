package com.integral.littlevecx.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.creativemd.creativecore.common.gui.GuiControl;
import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiComboBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiPanel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiScrollBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiTextfield;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDoorEvents;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDoorEvents.GuiDoorEventsButton;
import com.creativemd.littletiles.common.structure.animation.event.AnimationEvent;
import com.creativemd.littletiles.common.structure.animation.event.AnimationEventGuiParser;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;

import net.minecraft.nbt.NBTTagCompound;

public class GuiLittleVecXOverlayEventsButton extends GuiDoorEventsButton {

    private static final Logger LOGGER = LogManager.getLogger("LittleVecX");

    public interface EventsConsumer {
        void accept(List<AnimationEvent> events);
    }

    private final EventsConsumer consumer;

    public GuiLittleVecXOverlayEventsButton(String name, int x, int y, LittlePreviews previews, @Nullable List<AnimationEvent> events,
            EventsConsumer consumer) {
        super(name, x, y, previews, null);
        this.events = copyEvents(events);
        this.consumer = consumer;
    }

    @Override
    public void onClicked(int x, int y, int button) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("dialog", true);

        SubGui dialog = new SubGuiLittleVecXOverlayEvents(this);
        SubGui owner = getGui();
        if (owner == null || owner.gui == null)
            return;
        dialog.gui = owner.gui;

        PacketHandler.sendPacketToServer(new GuiLayerPacket(nbt, dialog.gui.getLayers().size() - 1, false));
        dialog.container = new SubContainerEmpty(getPlayer());
        dialog.gui.addLayer(dialog);
        dialog.onOpened();
    }

    private static List<AnimationEvent> copyEvents(@Nullable List<AnimationEvent> events) {
        List<AnimationEvent> copy = new ArrayList<>();
        if (events == null)
            return copy;
        for (AnimationEvent event : events) {
            AnimationEvent cloned = AnimationEvent.loadFromNBT(event.writeToNBT(new NBTTagCompound()));
            if (cloned != null)
                copy.add(cloned);
        }
        return copy;
    }

    public static class SubGuiLittleVecXOverlayEvents extends SubGui {

        private final GuiLittleVecXOverlayEventsButton button;
        private boolean applied = false;
        private boolean discardChanges = false;

        public SubGuiLittleVecXOverlayEvents(GuiLittleVecXOverlayEventsButton button) {
            this.button = button;
        }

        public void addEntry(@Nullable AnimationEvent event, String type) {
            GuiScrollBox box = (GuiScrollBox) get("content");

            AnimationEventGuiParser parser = AnimationEvent.getParser(type);

            GuiPanel panel = new GuiPanel(type, 2, 2, 158, parser.getHeight());
            panel.addControl(new GuiTextfield("tick", "" + (event != null ? event.getTick() : 0), 0, 0, 30, 10).setNumbersOnly().setCustomTooltip("tick"));
            panel.addControl(new GuiButton("x", 145, 0, 6, 6) {
                @Override
                public void onClicked(int x, int y, int buttonId) {
                    box.removeControl(panel);
                    reloadListBox();
                }
            });

            parser.createControls(panel, event, button.previews);
            box.addControl(panel);
            reloadListBox();
        }

        public void reloadListBox() {
            GuiScrollBox box = (GuiScrollBox) get("content");
            int height = 2;
            for (GuiControl control : box.controls) {
                control.posY = height;
                height += control.height + 2;
            }
        }

        private void applyEdits() {
            if (applied || discardChanges)
                return;
            applied = true;

            GuiScrollBox box = (GuiScrollBox) get("content");
            button.events.clear();
            for (GuiControl control : box.controls) {
                if (!(control instanceof GuiPanel))
                    continue;
                AnimationEventGuiParser parser = AnimationEvent.getParser(control.name);
                GuiTextfield textfield = (GuiTextfield) ((GuiPanel) control).get("tick");
                AnimationEvent event = AnimationEvent.create(textfield.parseInteger(), control.name);
                event = parser.parse((GuiParent) control, event);
                if (event != null)
                    button.events.add(event);
            }
            button.events.sort(null);
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX overlay settings debug eventsApply: events={}", button.events.size());
            if (button.consumer != null)
                button.consumer.accept(copyEvents(button.events));
            button.raiseEvent(new GuiControlChangedEvent(button));
        }

        @Override
        public void createControls() {
            GuiScrollBox box = new GuiScrollBox("content", 0, 0, 170, 110);
            controls.add(box);
            GuiComboBox type = new GuiComboBox("type", 0, 120, 100, AnimationEvent.typeNamestranslated());
            controls.add(type);
            controls.add(new GuiButton("+", 110, 123, 10, 8) {
                @Override
                public void onClicked(int x, int y, int buttonId) {
                    addEntry(null, AnimationEvent.typeNames().get(type.index));
                }
            });
            for (AnimationEvent event : button.events)
                addEntry(event, AnimationEvent.getId(event.getClass()));

            controls.add(new GuiButton("save", 140, 143) {
                @Override
                public void onClicked(int x, int y, int buttonId) {
                    applyEdits();
                    SubGuiLittleVecXOverlayEvents.this.closeGui();
                }
            });
            controls.add(new GuiButton("cancel", 0, 143) {
                @Override
                public void onClicked(int x, int y, int buttonId) {
                    discardChanges = true;
                    SubGuiLittleVecXOverlayEvents.this.closeGui();
                }
            });
        }

        @Override
        public void onClosed() {
            super.onClosed();
            applyEdits();
        }
    }
}

