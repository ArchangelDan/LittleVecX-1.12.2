package com.integral.littlevecx.client.gui;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.GuiControl;
import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiComboBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiIconButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiPanel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiStateButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiTextfield;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.GuiTimeline.KeyDeselectedEvent;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.GuiTimeline.KeySelectedEvent;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlClickEvent;
import com.creativemd.creativecore.common.gui.event.gui.GuiToolTipEvent;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.creativemd.littletiles.client.gui.controls.GuiAnimationViewer;
import com.creativemd.littletiles.client.gui.controls.GuiDirectionIndicator;
import com.creativemd.littletiles.client.gui.controls.GuiTileViewer;
import com.creativemd.littletiles.client.gui.controls.IAnimationControl;
import com.creativemd.littletiles.common.entity.AnimationPreview;
import com.creativemd.littletiles.client.gui.controls.GuiTileViewer.GuiTileViewerAxisChangedEvent;
import com.creativemd.littletiles.common.structure.animation.AnimationGuiHandler;
import com.creativemd.littletiles.common.structure.registry.LittleStructureGuiParser;
import com.creativemd.littletiles.common.structure.relative.StructureRelative;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.structure.type.door.LittleAdvancedDoor;
import com.creativemd.littletiles.common.structure.type.door.LittleAxisDoor;
import com.creativemd.littletiles.common.structure.type.door.LittleDoorBase;
import com.creativemd.littletiles.common.structure.type.door.LittleSlidingDoor;
import com.integral.littlevecx.animation.LittleVecXAnimationLayer;
import com.integral.littlevecx.animation.LittleVecXAnimationLayerDoorHelper;
import com.integral.littlevecx.animation.LittleVecXAnimationLayerDoorType;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SubGuiLittleVecXAnimationLayerEditor extends SubGui {

    private static final int WINDOW_WIDTH = 350;
    private static final int WINDOW_HEIGHT = 200;
    private static final int PANEL_Y = 30;
    private static final int FOOTER_Y = 176;
    private static final int PREVIEW_X = 208;
    private static final int PREVIEW_Y = 30;
    private static final int PREVIEW_WIDTH = 136;
    private static final int PREVIEW_HEIGHT = 135;

    private final SubGuiLittleVecXAnimationList owner;
    private final int layerIndex;
    private final AnimationGuiHandler previewHandler = new AnimationGuiHandler();
    private LittleVecXAnimationLayer editing;
    private boolean reopening;
    @Nullable
    private AnimationPreview animationPreview;
    @Nullable
    private LoadingThread loadingThread;
    @Nullable
    private GuiPanel parserPanel;
    @Nullable
    private LittleStructureGuiParser currentParser;

    public SubGuiLittleVecXAnimationLayerEditor(SubGuiLittleVecXAnimationList owner, int layerIndex) {
        super(WINDOW_WIDTH, WINDOW_HEIGHT);
        this.owner = owner;
        this.layerIndex = layerIndex;
        this.editing = owner.getLayerCopy(layerIndex);
    }

    @Override
    public void createControls() {
        rebuildLayout(false);
        if (loadingThread == null)
            loadingThread = new LoadingThread(owner.getPreviews());
    }

    private void saveLayer() {
        editing.name = ((GuiTextfield) get("name")).text;
        if (get("doorType") instanceof GuiComboBox)
            editing.doorType = LittleVecXAnimationLayerDoorType.fromIndex(((GuiComboBox) get("doorType")).index);

        GuiLittleVecXAnimationLayerSettingsButton settings = getLayerSettingsButton();
        if (settings != null)
            editing.trigger = settings.trigger;

        LittleDoorBase door = parseDoor();
        if (door != null)
            LittleVecXAnimationLayerDoorHelper.applyParsedDoor(editing, door, owner.getPreviews());

        owner.updateLayer(layerIndex, editing);
        onClosed();
        gui.removeLayer(this);
    }

    private void rebuildLayout(boolean preserveDoorState) {
        captureUiState(preserveDoorState);
        controls.clear();

        LittleVecXAnimationLayerDoorType type = editing.doorType;
        GuiComboBox doorType = new GuiComboBox("doorType", 0, 5, 90, doorTypeCaptions());
        doorType.select(type.ordinal());
        addControl(doorType);
        GuiTextfield nameField = new GuiTextfield("name", editing.name == null ? "" : editing.name, 102, 5, 88, 10);
        nameField.setCustomTooltip(CoreControl.translate("gui.littlevecx.animation_name"));
        String fixedLayerName = owner.getFixedLayerName(layerIndex);
        if (fixedLayerName != null && !fixedLayerName.trim().isEmpty()) {
            nameField.text = fixedLayerName;
            nameField.setEnabled(false);
        }
        addControl(nameField);

        addControl(new GuiAnimationViewer("renderer", PREVIEW_X, PREVIEW_Y, PREVIEW_WIDTH, PREVIEW_HEIGHT));
        addControl(new GuiIconButton("play", 248, 172, 10) {
            @Override
            public void onClicked(int x, int y, int button) {
                previewHandler.play();
            }
        });
        addControl(new GuiIconButton("pause", 268, 172, 9) {
            @Override
            public void onClicked(int x, int y, int button) {
                previewHandler.pause();
            }
        });
        addControl(new GuiIconButton("stop", 288, 172, 11) {
            @Override
            public void onClicked(int x, int y, int button) {
                previewHandler.stop();
            }
        });

        LittleDoorBase structure = LittleVecXAnimationLayerDoorHelper.createDoorForEdit(editing, owner.getPreviews());
        ensureDoorDefaults(structure);

        parserPanel = new GuiPanel("panel", 0, PANEL_Y, 200, 135);
        addControl(parserPanel);

        currentParser = createParser(type, parserPanel);
        createParserControls(type, structure);
        replaceAxisButton(structure);
        replaceSettingsButton();
        replaceEventsButton(structure);
        decorateTypeSpecificControls(type);

        addControl(new GuiButton("save", 150, FOOTER_Y, 40) {
            @Override
            public void onClicked(int x, int y, int button) {
                saveLayer();
            }
        });

        addControl(new GuiButton("cancel", 105, FOOTER_Y, 38) {
            @Override
            public void onClicked(int x, int y, int button) {
                onClosed();
                gui.removeLayer(SubGuiLittleVecXAnimationLayerEditor.this);
            }
        });

        rebindControlsAfterRebuild();
        if (animationPreview != null) {
            onLoaded(animationPreview);
            if (currentParser != null)
                currentParser.onLoaded(animationPreview);
        }
    }

    @Nullable
    private LittleStructureGuiParser createParser(LittleVecXAnimationLayerDoorType type, GuiParent parent) {
        switch (type) {
        case AXIS:
            return new LittleAxisDoor.LittleAxisDoorParser(parent, previewHandler);
        case SLIDING:
            return new LittleSlidingDoor.LittleSlidingDoorParser(parent, previewHandler);
        case ADVANCED:
        default:
            return new LittleAdvancedDoor.LittleAdvancedDoorParser(parent, previewHandler);
        }
    }

    private void createParserControls(LittleVecXAnimationLayerDoorType type, @Nullable LittleDoorBase structure) {
        if (currentParser == null)
            return;

        switch (type) {
        case AXIS:
            ((LittleAxisDoor.LittleAxisDoorParser) currentParser).createControls(owner.getPreviews(), structure);
            break;
        case SLIDING:
            ((LittleSlidingDoor.LittleSlidingDoorParser) currentParser).createControls(owner.getPreviews(), structure);
            break;
        case ADVANCED:
        default:
            ((LittleAdvancedDoor.LittleAdvancedDoorParser) currentParser).createControls(owner.getPreviews(), structure);
            break;
        }
    }

    private void replaceSettingsButton() {
        if (parserPanel == null || parserPanel.get("settings") == null)
            return;

        int x = parserPanel.get("settings").posX;
        int y = parserPanel.get("settings").posY;
        parserPanel.controls.remove(parserPanel.get("settings"));
        parserPanel.addControl(new GuiLittleVecXAnimationLayerSettingsButton("settings", x, y, editing.trigger));
    }

    private void replaceAxisButton(@Nullable LittleDoorBase structure) {
        if (parserPanel == null || parserPanel.get("axis") == null)
            return;

        int x = parserPanel.get("axis").posX;
        int y = parserPanel.get("axis").posY;
        parserPanel.controls.remove(parserPanel.get("axis"));

        if (parserPanel.get("settings") != null) {
            GuiControl settings = parserPanel.get("settings");
            int axisWidth = settings.width;
            int axisHeight = settings.height;
            int axisX = settings.posX + (settings.width - axisWidth) / 2;
            int axisY = settings.posY - axisHeight - 3;
            x = axisX;
            y = axisY;
        }

        int[] axisData = defaultAxis(owner.getPreviews()).write();
        if (structure instanceof LittleAdvancedDoor && ((LittleAdvancedDoor) structure).axisCenter != null)
            axisData = ((LittleAdvancedDoor) structure).axisCenter.write();
        else if (structure instanceof LittleAxisDoor && ((LittleAxisDoor) structure).axisCenter != null)
            axisData = ((LittleAxisDoor) structure).axisCenter.write();

        parserPanel.addControl(new GuiLittleVecXAxisButton("axis",
                CoreControl.translate("gui.littlevecx.axis_button_short"),
                x, y, 40, 7,
                owner.getPreviews().getContext(),
                axisData,
                previewHandler));
    }

    private void replaceEventsButton(@Nullable LittleDoorBase structure) {
        if (parserPanel == null || parserPanel.get("children_activate") == null)
            return;

        int x = parserPanel.get("children_activate").posX;
        int y = parserPanel.get("children_activate").posY;
        parserPanel.controls.remove(parserPanel.get("children_activate"));
        if (owner.isElevatorSoundMode())
            parserPanel.addControl(new GuiLittleVecXElevatorAnimationLayerEventsButton("children_activate", x, y, owner.getPreviews(), structure));
        else
            parserPanel.addControl(new GuiLittleVecXAnimationLayerEventsButton("children_activate", x, y, owner.getPreviews(), structure));
    }

    private void decorateTypeSpecificControls(LittleVecXAnimationLayerDoorType type) {
        if (type == LittleVecXAnimationLayerDoorType.SLIDING) {
            GuiTileViewer viewer = parserPanel != null && parserPanel.get("tileviewer") instanceof GuiTileViewer ? (GuiTileViewer) parserPanel.get("tileviewer") : null;
            GuiDirectionIndicator direction = parserPanel != null && parserPanel.get("relativeDirection") instanceof GuiDirectionIndicator ? (GuiDirectionIndicator) parserPanel.get("relativeDirection") : null;
            GuiStateButton directionState = parserPanel != null && parserPanel.get("direction") instanceof GuiStateButton ? (GuiStateButton) parserPanel.get("direction") : null;
            if (viewer != null && direction != null && directionState != null)
                ((LittleSlidingDoor.LittleSlidingDoorParser) currentParser).updateButtonDirection(viewer, EnumFacing.byIndex(directionState.getState()), direction);
        }
    }

    @Nullable
    private LittleDoorBase parseDoor() {
        if (currentParser instanceof LittleAdvancedDoor.LittleAdvancedDoorParser)
            return (LittleDoorBase) ((LittleAdvancedDoor.LittleAdvancedDoorParser) currentParser).parseStructure(owner.getPreviews());
        if (currentParser instanceof LittleDoorBase.LittleDoorBaseParser)
            return ((LittleDoorBase.LittleDoorBaseParser) currentParser).parseStructure(owner.getPreviews());
        return null;
    }

    private void ensureDoorDefaults(@Nullable LittleDoorBase structure) {
        if (structure == null)
            return;

        LittlePreviews previews = owner.getPreviews();
        if (previews == null)
            return;

        if (structure instanceof LittleAdvancedDoor) {
            LittleAdvancedDoor advanced = (LittleAdvancedDoor) structure;
            if (advanced.axisCenter == null)
                advanced.axisCenter = defaultAxis(previews);
            return;
        }

        if (structure instanceof LittleAxisDoor) {
            LittleAxisDoor axis = (LittleAxisDoor) structure;
            if (axis.axisCenter == null)
                axis.axisCenter = defaultAxis(previews);
            if (axis.axis == null)
                axis.axis = EnumFacing.Axis.Y;
            if (axis.doorRotation == null)
                axis.doorRotation = new LittleAxisDoor.DirectionRotation();
            return;
        }

        if (structure instanceof LittleSlidingDoor) {
            LittleSlidingDoor sliding = (LittleSlidingDoor) structure;
            // LittleTiles' parser dereferences both values while it is creating
            // its controls. A freshly switched layer has no saved sliding-door
            // data yet, so provide the same safe defaults the normal parser uses.
            if (sliding.direction == null)
                sliding.direction = EnumFacing.NORTH;
            if (sliding.moveContext == null)
                sliding.moveContext = previews.getContext();
        }
    }

    private static StructureRelative defaultAxis(LittlePreviews previews) {
        LittleBox box = previews.getSurroundingBox();
        int minX = axisMin(box.minX, box.maxX);
        int minY = axisMin(box.minY, box.maxY);
        int minZ = axisMin(box.minZ, box.maxZ);
        int maxX = axisMax(box.minX, box.maxX);
        int maxY = axisMax(box.minY, box.maxY);
        int maxZ = axisMax(box.minZ, box.maxZ);
        return new StructureRelative(new int[] { minX, minY, minZ, maxX, maxY, maxZ, previews.getContext().size });
    }

    private static int axisMin(int min, int max) {
        return (min + max) / 2;
    }

    private static int axisMax(int min, int max) {
        return ((min + max) / 2) + 1;
    }

    @Nullable
    private GuiLittleVecXAnimationLayerSettingsButton getLayerSettingsButton() {
        return get("settings") instanceof GuiLittleVecXAnimationLayerSettingsButton ? (GuiLittleVecXAnimationLayerSettingsButton) get("settings") : null;
    }

    private void captureUiState(boolean preserveDoorState) {
        if (get("name") instanceof GuiTextfield)
            editing.name = ((GuiTextfield) get("name")).text;
        if (get("doorType") instanceof GuiComboBox)
            editing.doorType = LittleVecXAnimationLayerDoorType.fromIndex(((GuiComboBox) get("doorType")).index);

        GuiLittleVecXAnimationLayerSettingsButton settings = getLayerSettingsButton();
        if (settings != null)
            editing.trigger = settings.trigger;

        if (!preserveDoorState)
            return;

        LittleDoorBase door = parseDoor();
        if (door != null)
            LittleVecXAnimationLayerDoorHelper.applyParsedDoor(editing, door, owner.getPreviews());
    }

    private void rebindControlsAfterRebuild() {
        for (int i = 0; i < controls.size(); i++) {
            GuiControl control = controls.get(i);
            updateControl(control, i);
            control.onOpened();
        }
        refreshControls();
    }

    @Override
    public void onTick() {
        super.onTick();

        if (loadingThread != null && loadingThread.result != null) {
            animationPreview = loadingThread.result;
            loadingThread = null;
            onLoaded(animationPreview);
            if (currentParser != null)
                currentParser.onLoaded(animationPreview);
        }

        if (animationPreview != null)
            previewHandler.tick(animationPreview.previews, animationPreview.animation.structure, animationPreview.animation);
    }

    private void onLoaded(AnimationPreview preview) {
        onLoaded(this, preview);
    }

    private void onLoaded(GuiParent parent, AnimationPreview preview) {
        for (GuiControl control : parent.controls) {
            if (control instanceof IAnimationControl)
                ((IAnimationControl) control).onLoaded(preview);
            if (control instanceof GuiParent)
                onLoaded((GuiParent) control, preview);
        }
    }

    @CustomEventSubscribe
    @SideOnly(Side.CLIENT)
    public void onChanged(GuiControlChangedEvent event) {
        if (event.source.is("doorType")) {
            captureUiState(false);
            reopenEditorNextTick();
            return;
        }

        if (currentParser instanceof LittleAdvancedDoor.LittleAdvancedDoorParser)
            ((LittleAdvancedDoor.LittleAdvancedDoorParser) currentParser).onChange(event);
        else if (currentParser instanceof LittleAxisDoor.LittleAxisDoorParser)
            ((LittleAxisDoor.LittleAxisDoorParser) currentParser).onChanged(event);
        else if (currentParser instanceof LittleSlidingDoor.LittleSlidingDoorParser)
            ((LittleSlidingDoor.LittleSlidingDoorParser) currentParser).onChanged(event);
    }

    @CustomEventSubscribe
    @SideOnly(Side.CLIENT)
    public void onClicked(GuiControlClickEvent event) {
        if (currentParser instanceof LittleAxisDoor.LittleAxisDoorParser)
            ((LittleAxisDoor.LittleAxisDoorParser) currentParser).onButtonClicked(event);
        else if (currentParser instanceof LittleSlidingDoor.LittleSlidingDoorParser)
            ((LittleSlidingDoor.LittleSlidingDoorParser) currentParser).buttonClicked(event);
    }

    @CustomEventSubscribe
    @SideOnly(Side.CLIENT)
    public void onAxisChanged(GuiTileViewerAxisChangedEvent event) {
        if (currentParser instanceof LittleAxisDoor.LittleAxisDoorParser)
            ((LittleAxisDoor.LittleAxisDoorParser) currentParser).onAxisChanged(event);
    }

    @CustomEventSubscribe
    @SideOnly(Side.CLIENT)
    public void onKeySelected(KeySelectedEvent event) {
        if (currentParser instanceof LittleAdvancedDoor.LittleAdvancedDoorParser)
            ((LittleAdvancedDoor.LittleAdvancedDoorParser) currentParser).onKeySelected(event);
    }

    @CustomEventSubscribe
    @SideOnly(Side.CLIENT)
    public void onKeyDeselected(KeyDeselectedEvent event) {
        if (currentParser instanceof LittleAdvancedDoor.LittleAdvancedDoorParser)
            ((LittleAdvancedDoor.LittleAdvancedDoorParser) currentParser).onKeyDeselected(event);
    }

    @CustomEventSubscribe
    @SideOnly(Side.CLIENT)
    public void toolTip(GuiToolTipEvent event) {
        if (currentParser instanceof LittleAdvancedDoor.LittleAdvancedDoorParser)
            ((LittleAdvancedDoor.LittleAdvancedDoorParser) currentParser).toolTip(event);
    }

    private void reopenEditorNextTick() {
        if (reopening)
            return;

        reopening = true;
        Minecraft.getMinecraft().addScheduledTask(() -> {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setBoolean("dialog", true);

            SubGuiLittleVecXAnimationLayerEditor dialog = new SubGuiLittleVecXAnimationLayerEditor(owner, layerIndex);
            dialog.editing = editing.copy();
            dialog.gui = gui;

            PacketHandler.sendPacketToServer(new GuiLayerPacket(nbt, dialog.gui.getLayers().size() - 1, false));
            dialog.container = new SubContainerEmpty(getPlayer());

            onClosed();
            gui.removeLayer(SubGuiLittleVecXAnimationLayerEditor.this);
            dialog.gui.addLayer(dialog);
            dialog.onOpened();
        });
    }

    private static java.util.List<String> doorTypeCaptions() {
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        for (LittleVecXAnimationLayerDoorType type : LittleVecXAnimationLayerDoorType.values())
            ids.add(type.getCaption());
        return ids;
    }

    private static class LoadingThread extends Thread {

        private final LittlePreviews previews;
        @Nullable
        private volatile AnimationPreview result;

        private LoadingThread(LittlePreviews previews) {
            this.previews = previews;
            start();
        }

        @Override
        public void run() {
            result = new AnimationPreview(previews);
        }
    }
}
