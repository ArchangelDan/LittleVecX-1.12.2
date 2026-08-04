package com.integral.littlevecx.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiListBox;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.integral.littlevecx.animation.LittleVecXAnimationLayer;
import com.integral.littlevecx.animation.LittleVecXAnimationTriggerMode;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SubGuiLittleVecXAnimationList extends SubGui {

    private final GuiLittleVecXAnimationLayersButton activator;
    private final List<LittleVecXAnimationLayer> working;

    public SubGuiLittleVecXAnimationList(GuiLittleVecXAnimationLayersButton activator) {
        super(190, 135);
        this.activator = activator;
        this.working = activator.getLayersCopy();
    }

    @Override
    public void createControls() {
        int actionColumnX = 125;
        int sideButtonWidth = 59;
        int sideButtonHeight = 10;
        int actionButtonX = actionColumnX + 2;
        int actionButtonWidth = sideButtonWidth - 4;
        int actionGap = 3;
        int closeButtonY = 115;
        int moveButtonSize = 8;
        int moveDownY = closeButtonY - 10 - moveButtonSize;
        int moveUpY = moveDownY - actionGap - moveButtonSize - 5;
        int moveButtonX = actionColumnX + (sideButtonWidth - moveButtonSize) / 2 - 22;

        addControl(new GuiButton("add", CoreControl.translate("gui.littlevecx.add_animation"), 0, 0, 110, 10) {
            @Override
            public void onClicked(int x, int y, int button) {
                addLayer();
            }
        });

        addControl(new GuiListBox("list", 0, 15, 120, 115, buildNames()));

        addControl(new GuiButton("edit", CoreControl.translate("gui.littlevecx.edit"), actionButtonX, 15, actionButtonWidth, sideButtonHeight) {
            @Override
            public void onClicked(int x, int y, int button) {
                openSelectedLayer();
            }
        });

        addControl(new GuiButton("remove", CoreControl.translate("gui.littlevecx.delete"), actionButtonX, 15 + sideButtonHeight + actionGap + 5, actionButtonWidth,
                sideButtonHeight) {
            @Override
            public void onClicked(int x, int y, int button) {
                removeSelectedLayer();
            }
        });

        addControl(new GuiButton("up", "+", moveButtonX, moveUpY, moveButtonSize, moveButtonSize) {
            @Override
            public void onClicked(int x, int y, int button) {
                moveSelectedLayer(-1);
            }
        });

        addControl(new GuiButton("down", "-", moveButtonX, moveDownY, moveButtonSize, moveButtonSize) {
            @Override
            public void onClicked(int x, int y, int button) {
                moveSelectedLayer(1);
            }
        });

        addControl(new GuiButton("close", actionButtonX, closeButtonY, actionButtonWidth, sideButtonHeight) {
            @Override
            public void onClicked(int x, int y, int button) {
                activator.setLayers(working);
                onClosed();
                gui.removeLayer(SubGuiLittleVecXAnimationList.this);
            }
        });
    }

    public LittleVecXAnimationLayer getLayerCopy(int index) {
        return working.get(index).copy();
    }

    public LittlePreviews getPreviews() {
        return activator.getPreviews();
    }

    public String getFixedLayerName(int index) {
        return activator.getFixedLayerName(index);
    }

    public boolean isElevatorSoundMode() {
        return activator.isElevatorSoundMode();
    }

    public void updateLayer(int index, LittleVecXAnimationLayer layer) {
        LittleVecXAnimationLayer copy = layer.copy();
        activator.applyFixedLayerName(copy, index);
        working.set(index, copy);
        refreshList(index);
    }

    private void addLayer() {
        if (working.size() >= activator.getMaxLayers())
            return;

        LittleVecXAnimationLayer layer = new LittleVecXAnimationLayer();
        activator.applyFixedLayerName(layer, working.size());
        if (layer.name == null || layer.name.trim().isEmpty())
            layer.name = "Animation " + working.size();
        layer.trigger = working.isEmpty() ? LittleVecXAnimationTriggerMode.RIGHT_CLICK : LittleVecXAnimationTriggerMode.SHIFT_RIGHT_CLICK;
        working.add(layer);
        refreshList(working.size() - 1);
        openLayer(working.size() - 1);
    }

    private void removeSelectedLayer() {
        GuiListBox list = getList();
        if (list.selected < 0 || list.selected >= working.size())
            return;
        working.remove(list.selected);
        int next = Math.min(list.selected, working.size() - 1);
        refreshList(next);
    }

    private void moveSelectedLayer(int delta) {
        GuiListBox list = getList();
        if (list.selected < 0 || list.selected >= working.size())
            return;

        int nextIndex = list.selected + delta;
        if (nextIndex < 0 || nextIndex >= working.size())
            return;

        LittleVecXAnimationLayer layer = working.remove(list.selected);
        working.add(nextIndex, layer);
        refreshList(nextIndex);
    }

    private void openSelectedLayer() {
        GuiListBox list = getList();
        if (list.selected < 0 || list.selected >= working.size())
            return;
        openLayer(list.selected);
    }

    private void openLayer(int index) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("dialog", true);

        SubGuiLittleVecXAnimationLayerEditor dialog = new SubGuiLittleVecXAnimationLayerEditor(this, index);
        dialog.gui = gui;

        PacketHandler.sendPacketToServer(new GuiLayerPacket(nbt, dialog.gui.getLayers().size() - 1, false));
        dialog.container = new SubContainerEmpty(getPlayer());
        dialog.gui.addLayer(dialog);
        dialog.onOpened();
    }

    private GuiListBox getList() {
        return (GuiListBox) get("list");
    }

    private List<String> buildNames() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < working.size(); i++)
            names.add(activator.getDisplayName(working.get(i), i));
        return names;
    }

    private void refreshList(int selectedIndex) {
        GuiListBox list = getList();
        list.clear();
        List<String> names = buildNames();
        for (String name : names)
            list.add(name);

        if (selectedIndex >= 0 && selectedIndex < names.size()) {
            list.selected = selectedIndex;
            list.reloadControls();
        }
    }

    @CustomEventSubscribe
    @SideOnly(Side.CLIENT)
    public void onListChanged(GuiControlChangedEvent event) {
        if (event.source.is("list")) {
            // Selection is already tracked by GuiListBox.
        }
    }

    public static class GuiLittleVecXAnimationListButton extends GuiButton {

        private List<LittleVecXAnimationLayer> layers = new ArrayList<>();
        private final LittlePreviews previews;
        private final int maxLayers;
        private final String[] fixedLayerNames;
        private final boolean elevatorSoundMode;

        public GuiLittleVecXAnimationListButton(String name, int x, int y, List<LittleVecXAnimationLayer> initialLayers, LittlePreviews previews) {
            this(name, x, y, initialLayers, previews, Integer.MAX_VALUE, null, false);
        }

        public GuiLittleVecXAnimationListButton(String name, int x, int y, List<LittleVecXAnimationLayer> initialLayers, LittlePreviews previews, int maxLayers,
                String[] fixedLayerNames) {
            this(name, x, y, initialLayers, previews, maxLayers, fixedLayerNames, false);
        }

        public GuiLittleVecXAnimationListButton(String name, int x, int y, List<LittleVecXAnimationLayer> initialLayers, LittlePreviews previews, int maxLayers,
                String[] fixedLayerNames, boolean elevatorSoundMode) {
            super(name, CoreControl.translate("gui.littlevecx.animations"), x, y, 70, 10);
            this.previews = previews;
            this.maxLayers = maxLayers <= 0 ? Integer.MAX_VALUE : maxLayers;
            this.fixedLayerNames = fixedLayerNames == null ? null : fixedLayerNames.clone();
            this.elevatorSoundMode = elevatorSoundMode;
            setLayers(initialLayers);
        }

        public List<LittleVecXAnimationLayer> getLayersCopy() {
            List<LittleVecXAnimationLayer> copy = new ArrayList<>();
            for (LittleVecXAnimationLayer layer : layers)
                copy.add(layer.copy());
            return copy;
        }

        public void setLayers(List<LittleVecXAnimationLayer> newLayers) {
            layers = new ArrayList<>();
            if (newLayers != null) {
                for (LittleVecXAnimationLayer layer : newLayers)
                    layers.add(layer.copy());
            }
            applyFixedLayerNames(layers);
            raiseEvent(new GuiControlChangedEvent(this));
        }

        public LittlePreviews getPreviews() {
            return previews;
        }

        public int getMaxLayers() {
            return maxLayers;
        }

        public boolean isElevatorSoundMode() {
            return elevatorSoundMode;
        }

        public String getFixedLayerName(int index) {
            if (fixedLayerNames == null || index < 0 || index >= fixedLayerNames.length)
                return null;
            return fixedLayerNames[index];
        }

        public String getDisplayName(LittleVecXAnimationLayer layer, int index) {
            String fixed = getFixedLayerName(index);
            if (fixed != null && !fixed.trim().isEmpty())
                return fixed;
            return layer.getDisplayName(index);
        }

        public void applyFixedLayerName(LittleVecXAnimationLayer layer, int index) {
            String fixed = getFixedLayerName(index);
            if (layer != null && fixed != null && !fixed.trim().isEmpty())
                layer.name = fixed;
        }

        private void applyFixedLayerNames(List<LittleVecXAnimationLayer> target) {
            if (target == null || fixedLayerNames == null)
                return;
            for (int i = 0; i < target.size(); i++)
                applyFixedLayerName(target.get(i), i);
        }

        @Override
        public void onClicked(int x, int y, int button) {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setBoolean("dialog", true);

            GuiLittleVecXAnimationLayersButton proxy = new GuiLittleVecXAnimationLayersButton("animation_layers", posX, posY, layers, previews, maxLayers,
                    fixedLayerNames, elevatorSoundMode);
            SubGuiLittleVecXAnimationList dialog = new SubGuiLittleVecXAnimationList(proxy);
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
