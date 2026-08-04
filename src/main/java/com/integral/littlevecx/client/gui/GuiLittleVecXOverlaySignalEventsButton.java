package com.integral.littlevecx.client.gui;

import java.util.Iterator;

import com.creativemd.littletiles.client.gui.dialogs.SubGuiSignalEvents.GuiSignalEvent;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiSignalEvents.GuiSignalEventsButton;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;

public class GuiLittleVecXOverlaySignalEventsButton extends GuiSignalEventsButton {

    public GuiLittleVecXOverlaySignalEventsButton(String name, int x, int y, LittlePreviews previews, LittleStructure structure,
            LittleStructureType type) {
        super(name, x, y, previews, structure, type);
        filterOverlayOutputs();
    }

    private void filterOverlayOutputs() {
        outputs.removeIf(component -> !isAllowedOverlaySignal(component.totalName));

        Iterator<GuiSignalEvent> iterator = events.iterator();
        while (iterator.hasNext()) {
            GuiSignalEvent event = iterator.next();
            if (!isAllowedOverlaySignal(event.component.totalName))
                iterator.remove();
        }
    }

    private static boolean isAllowedOverlaySignal(String totalName) {
        return "state".equals(totalName) || (totalName != null && totalName.startsWith("checkpoint_"));
    }
}
