package com.integral.littlevecx.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.integral.littlevecx.animation.LittleVecXAnimationLayer;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiLittleVecXAnimationLayersButton extends GuiButton {

    private List<LittleVecXAnimationLayer> layers = new ArrayList<>();
    private final LittlePreviews previews;
    private final int maxLayers;
    private final String[] fixedLayerNames;
    private final boolean elevatorSoundMode;

    public GuiLittleVecXAnimationLayersButton(String name, int x, int y, List<LittleVecXAnimationLayer> initialLayers, LittlePreviews previews) {
        this(name, x, y, initialLayers, previews, Integer.MAX_VALUE, null, false);
    }

    public GuiLittleVecXAnimationLayersButton(String name, int x, int y, List<LittleVecXAnimationLayer> initialLayers, LittlePreviews previews, int maxLayers,
            String[] fixedLayerNames) {
        this(name, x, y, initialLayers, previews, maxLayers, fixedLayerNames, false);
    }

    public GuiLittleVecXAnimationLayersButton(String name, int x, int y, List<LittleVecXAnimationLayer> initialLayers, LittlePreviews previews, int maxLayers,
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
        raiseEvent(new com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent(this));
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

        SubGuiLittleVecXAnimationList dialog = new SubGuiLittleVecXAnimationList(this);
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
