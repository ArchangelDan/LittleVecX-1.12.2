package com.integral.littlevecx.client.gui;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiLabel;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlClickEvent;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.littletiles.client.LittleTilesClient;
import com.creativemd.littletiles.client.gui.configure.SubGuiConfigure;
import com.integral.littlevecx.client.LittleVecXMoveClientHandler;
import com.integral.littlevecx.client.LittleVecXMoveClientHandler.SelectionSnapshot;
import com.integral.littlevecx.client.gui.layout.LittleVecXGuiLayout;
import com.integral.littlevecx.client.gui.layout.LittleVecXGuiMetrics;
import com.integral.littlevecx.network.PacketLittleVecXDoorAction;
import com.integral.littlevecx.network.PacketLittleVecXEraseStructure;
import com.integral.littlevecx.network.PacketLittleVecXRefreshStructure;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraft.item.ItemStack;

public class SubGuiLittleVecXStructureSettingsMenu extends SubGuiConfigure {

    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 138;
    private static final int BUTTON_HEIGHT = 8;
    private static final int COLUMN_WIDTH = 86;
    private static final int WIDE_BUTTON_WIDTH = COLUMN_WIDTH * 2 + LittleVecXGuiMetrics.COLUMN_GAP;

    private static class ActionButton extends GuiButton {

        public ActionButton(String name, String caption, int x, int y, int width) {
            super(name, caption, x, y, width, BUTTON_HEIGHT);
        }

        @Override
        public void onClicked(int x, int y, int button) {
            // Handled by the parent GUI through GuiControlClickEvent.
        }
    }

    private final SelectionSnapshot snapshot;

    public SubGuiLittleVecXStructureSettingsMenu(ItemStack stack, SelectionSnapshot snapshot) {
        super(GUI_WIDTH, GUI_HEIGHT, stack);
        this.snapshot = snapshot;
    }

    @Override
    public void createControls() {
        LittleVecXGuiLayout layout = new LittleVecXGuiLayout(GUI_WIDTH, GUI_HEIGHT);
        String name = snapshot != null ? snapshot.displayName : CoreControl.translate("gui.littlevecx.no_selection");
        int labelX = layout.left();
        int selectedLabelY = layout.top() + 2;
        int selectedNameY = layout.nextRow(selectedLabelY, 8, LittleVecXGuiMetrics.LABEL_GAP);
        int leftColumnX = layout.pairLeft(COLUMN_WIDTH, COLUMN_WIDTH);
        int rightColumnX = layout.pairRight(COLUMN_WIDTH, COLUMN_WIDTH);
        int buttonRowOneY = layout.nextRow(selectedNameY, 8, LittleVecXGuiMetrics.SECTION_GAP);
        int buttonRowTwoY = layout.nextRow(buttonRowOneY, BUTTON_HEIGHT);
        int buttonRowThreeY = layout.nextRow(buttonRowTwoY, BUTTON_HEIGHT);
        int wideButtonX = layout.center(WIDE_BUTTON_WIDTH);
        int hintY = layout.footerY(8) - 6;

        controls.add(new GuiLabel(CoreControl.translate("gui.littlevecx.selected") + ":", labelX, selectedLabelY));
        controls.add(new GuiLabel(name, labelX, selectedNameY));

        controls.add(new ActionButton("editRecipe", CoreControl.translate("gui.littlevecx.edit_recipe"), leftColumnX, buttonRowOneY, COLUMN_WIDTH));
        controls.add(new ActionButton("openDoors", CoreControl.translate("gui.littlevecx.open_all_doors"), rightColumnX, buttonRowOneY, COLUMN_WIDTH));
        controls.add(new ActionButton("eraseStructure", CoreControl.translate("gui.littlevecx.erase_structure"), leftColumnX, buttonRowTwoY, COLUMN_WIDTH));
        controls.add(new ActionButton("closeDoors", CoreControl.translate("gui.littlevecx.close_all_doors"), rightColumnX, buttonRowTwoY, COLUMN_WIDTH));
        controls.add(new ActionButton("refreshClient", CoreControl.translate("gui.littlevecx.refresh_client"), wideButtonX, buttonRowThreeY, WIDE_BUTTON_WIDTH));

        controls.add(new GuiLabel(LittleTilesClient.mark.getDisplayName() + ": " + CoreControl.translate("gui.littlevecx.move_hint"), labelX, hintY));
    }

    @CustomEventSubscribe
    public void onButtonClicked(GuiControlClickEvent event) {
        if (event.source.is("editRecipe")) {
            LittleVecXMoveClientHandler.queueSelectedRecipeGui();
            closeGui();
            return;
        }

        if (snapshot == null)
            return;

        if (event.source.is("openDoors")) {
            PacketHandler.sendPacketToServer(new PacketLittleVecXDoorAction(snapshot.location, true));
            return;
        }

        if (event.source.is("closeDoors")) {
            PacketHandler.sendPacketToServer(new PacketLittleVecXDoorAction(snapshot.location, false));
            return;
        }

        if (event.source.is("eraseStructure")) {
            PacketHandler.sendPacketToServer(new PacketLittleVecXEraseStructure(snapshot.location));
            closeGui();
            return;
        }

        if (event.source.is("refreshClient"))
            PacketHandler.sendPacketToServer(new PacketLittleVecXRefreshStructure(snapshot.location));
    }

    @Override
    public void saveConfiguration() {
        // Structure settings are applied with dedicated buttons and packets.
    }
}
