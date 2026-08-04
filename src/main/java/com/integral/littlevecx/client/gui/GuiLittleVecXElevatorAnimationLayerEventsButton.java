package com.integral.littlevecx.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.GuiControl;
import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiComboBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiPanel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiScrollBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiStateButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiTextfield;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.creativemd.littletiles.client.gui.controls.SubGuiSoundSelector.GuiPickSoundButton;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDoorEvents.GuiDoorEventsButton;
import com.creativemd.littletiles.common.structure.animation.event.AnimationEvent;
import com.creativemd.littletiles.common.structure.animation.event.AnimationEventGuiParser;
import com.creativemd.littletiles.common.structure.animation.event.PlaySoundEvent;
import com.creativemd.littletiles.common.structure.type.door.LittleDoorBase;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiLittleVecXElevatorAnimationLayerEventsButton extends GuiDoorEventsButton {

    public GuiLittleVecXElevatorAnimationLayerEventsButton(String name, int x, int y, LittlePreviews previews, LittleDoorBase door) {
        super(name, x, y, previews, door);
    }

    @Override
    public void onClicked(int x, int y, int button) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("dialog", true);

        SubGuiLittleVecXElevatorEvents dialog = new SubGuiLittleVecXElevatorEvents(this);
        SubGui owner = getGui();
        if (owner == null || owner.gui == null)
            return;
        dialog.gui = owner.gui;

        PacketHandler.sendPacketToServer(new GuiLayerPacket(nbt, dialog.gui.getLayers().size() - 1, false));
        dialog.container = new SubContainerEmpty(getPlayer());
        dialog.gui.addLayer(dialog);
        dialog.onOpened();
    }

    @SideOnly(Side.CLIENT)
    public static class SubGuiLittleVecXElevatorEvents extends SubGui {

        private static final ElevatorSoundEventParser ELEVATOR_SOUND_PARSER = new ElevatorSoundEventParser();

        public final GuiDoorEventsButton button;

        public SubGuiLittleVecXElevatorEvents(GuiDoorEventsButton button) {
            this.button = button;
        }

        public void addEntry(@Nullable AnimationEvent event, String type) {
            GuiScrollBox box = (GuiScrollBox) get("content");
            AnimationEventGuiParser parser = getParser(type);

            GuiPanel panel = new GuiPanel(type, 2, 2, 158, parser.getHeight());
            panel.addControl(new GuiTextfield("tick", "" + (event != null ? event.getTick() : 0), 0, 0, 30, 10).setNumbersOnly()
                    .setCustomTooltip("tick"));
            panel.addControl(new GuiButton("x", 145, 0, 6, 6) {

                @Override
                public void onClicked(int x, int y, int button) {
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

        @Override
        public void createControls() {
            GuiScrollBox box = new GuiScrollBox("content", 0, 0, 170, 110);
            controls.add(box);
            GuiComboBox type = new GuiComboBox("type", 0, 120, 100, AnimationEvent.typeNamestranslated());
            controls.add(type);
            controls.add(new GuiButton("+", 110, 123, 10, 8) {

                @Override
                public void onClicked(int x, int y, int button) {
                    addEntry(null, AnimationEvent.typeNames().get(type.index));
                }
            });
            for (AnimationEvent event : button.events)
                addEntry(event, AnimationEvent.getId(event.getClass()));

            controls.add(new GuiButton("save", 140, 143) {

                @Override
                public void onClicked(int x, int y, int button) {
                    SubGuiLittleVecXElevatorEvents.this.button.events.clear();
                    for (GuiControl control : box.controls) {
                        if (!(control instanceof GuiPanel))
                            continue;

                        AnimationEventGuiParser parser = getParser(control.name);
                        GuiTextfield textfield = (GuiTextfield) ((GuiPanel) control).get("tick");
                        AnimationEvent event = AnimationEvent.create(textfield.parseInteger(), control.name);
                        event = parser.parse((GuiParent) control, event);
                        if (event != null)
                            SubGuiLittleVecXElevatorEvents.this.button.events.add(event);
                    }

                    SubGuiLittleVecXElevatorEvents.this.button.events.sort(null);
                    closeGui();
                    SubGuiLittleVecXElevatorEvents.this.button.raiseEvent(new GuiControlChangedEvent(SubGuiLittleVecXElevatorEvents.this.button));
                }
            });
            controls.add(new GuiButton("cancel", 0, 143) {

                @Override
                public void onClicked(int x, int y, int button) {
                    closeGui();
                }
            });
        }

        private AnimationEventGuiParser getParser(String type) {
            if ("sound-event".equals(type))
                return ELEVATOR_SOUND_PARSER;
            return AnimationEvent.getParser(type);
        }
    }

    @SideOnly(Side.CLIENT)
    private static class ElevatorSoundEventParser extends AnimationEventGuiParser<PlaySoundEvent> {

        @Override
        public void createControls(GuiParent parent, PlaySoundEvent event, LittlePreviews previews) {
            parent.addControl(new GuiStateButton("mode", event != null && !event.opening ? 1 : 0, 37, 0, 52, 7,
                    CoreControl.translate("gui.littlevecx.elevator_sound_movement"),
                    CoreControl.translate("gui.littlevecx.elevator_sound_end")));
            parent.addControl(new GuiPickSoundButton("sound", 94, 0, event));
        }

        @Override
        public PlaySoundEvent parse(GuiParent parent, PlaySoundEvent event) {
            GuiPickSoundButton picker = (GuiPickSoundButton) parent.get("sound");
            GuiStateButton mode = (GuiStateButton) parent.get("mode");
            if (picker.selected == null)
                return null;

            event.pitch = picker.pitch;
            event.volume = picker.volume;
            event.sound = picker.selected;
            event.opening = mode.getState() == 0;
            return event;
        }

        @Override
        public int getHeight() {
            return 20;
        }
    }
}
