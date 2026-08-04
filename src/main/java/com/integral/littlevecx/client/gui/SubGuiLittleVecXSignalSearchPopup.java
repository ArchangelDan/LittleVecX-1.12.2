package com.integral.littlevecx.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiComboBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiLabel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiTextfield;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.integral.littlevecx.client.gui.layout.LittleVecXGuiLayout;
import com.creativemd.littletiles.client.gui.signal.GuiSignalController;
import com.creativemd.littletiles.client.gui.signal.SubGuiDialogSignal.GuiSignalComponent;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SubGuiLittleVecXSignalSearchPopup extends SubGui {

    private static final int POPUP_WIDTH = 270;
    private static final int POPUP_HEIGHT = 92;
    private static final int BUTTON_WIDTH = 56;
    private static final int FORM_WIDTH = 230;
    private static final int BUTTON_GAP = 12;

    private final GuiSignalController controller;
    private final List<GuiSignalComponent> possibleInputs = new ArrayList<>();
    private final List<GuiSignalComponent> filteredInputs = new ArrayList<>();

    public SubGuiLittleVecXSignalSearchPopup(GuiSignalController controller, List<GuiSignalComponent> inputs) {
        super(POPUP_WIDTH, POPUP_HEIGHT);
        this.controller = controller;
        if (inputs != null) {
            for (GuiSignalComponent input : inputs) {
                if (input == null)
                    continue;
                if ("[]".equals(input.name) || "number".equals(input.name))
                    continue;
                possibleInputs.add(input);
            }
        }
    }

    @Override
    public void createControls() {
        LittleVecXGuiLayout layout = new LittleVecXGuiLayout(POPUP_WIDTH, POPUP_HEIGHT);
        int contentWidth = FORM_WIDTH;
        int contentX = layout.center(contentWidth);
        int titleY = layout.top() - 1;
        int searchY = 18;
        int resultsY = 36;
        int resultsHeight = 12;
        int footerY = layout.footerY(10);
        int buttonRowWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
        int cancelButtonX = layout.center(buttonRowWidth);
        int addButtonX = cancelButtonX + BUTTON_WIDTH + BUTTON_GAP;

        GuiLabel title = new GuiLabel("title", CoreControl.translate("gui.signal.configuration.search"), contentX, titleY);
        addControl(title);
        addControl(new GuiTextfield("search", "", contentX, searchY, contentWidth, 12));
        GuiComboBox results = new GuiComboBox("results", contentX, resultsY, contentWidth, new ArrayList<>());
        results.height = resultsHeight;
        addControl(results);
        addControl(new GuiButton("add", CoreControl.translate("gui.signal.configuration.add"), addButtonX, footerY, BUTTON_WIDTH, 10) {

            @Override
            public void onClicked(int x, int y, int button) {
                GuiSignalComponent selected = getSelectedInput();
                if (selected != null)
                    controller.addInput(selected);
                closeGui();
            }
        });
        addControl(new GuiButton("cancel", cancelButtonX, footerY, BUTTON_WIDTH, 10) {

            @Override
            public void onClicked(int x, int y, int button) {
                closeGui();
            }
        });
        onChanged(new GuiControlChangedEvent((GuiTextfield) get("search")));
    }

    @CustomEventSubscribe
    public void onChanged(GuiControlChangedEvent event) {
        if (event.source == null)
            return;
        if (!event.source.is("search"))
            return;

        GuiTextfield search = (GuiTextfield) get("search");
        GuiComboBox results = (GuiComboBox) get("results");
        String filter = search.text == null ? "" : search.text.trim().toLowerCase();

        filteredInputs.clear();
        List<String> lines = new ArrayList<>();
        for (GuiSignalComponent input : possibleInputs) {
            String line = input.info();
            if (filter.isEmpty() || line.toLowerCase().contains(filter)) {
                filteredInputs.add(input);
                lines.add(line);
            }
        }

        results.lines = lines;
        if (!lines.isEmpty())
            results.select(0);
    }

    private GuiSignalComponent getSelectedInput() {
        GuiComboBox results = (GuiComboBox) get("results");
        if (results == null)
            return null;
        if (results.index < 0 || results.index >= filteredInputs.size())
            return null;
        return filteredInputs.get(results.index);
    }
}
