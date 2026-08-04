package com.integral.littlevecx.client.gui;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.controls.gui.GuiComboBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiLabel;
import com.creativemd.littletiles.client.gui.configure.SubGuiConfigure;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.integral.littlevecx.client.gui.layout.LittleVecXGuiLayout;
import com.integral.littlevecx.client.gui.layout.LittleVecXGuiMetrics;

import net.minecraft.item.ItemStack;

public class SubGuiLittleVecXPliers extends SubGuiConfigure {

    private static final int GUI_WIDTH = 150;
    private static final int GUI_HEIGHT = 58;
    private static final int GRID_BOX_WIDTH = 48;

    public SubGuiLittleVecXPliers(ItemStack stack) {
        super(GUI_WIDTH, GUI_HEIGHT, stack);
    }

    @Override
    public void createControls() {
        LittleVecXGuiLayout layout = new LittleVecXGuiLayout(GUI_WIDTH, GUI_HEIGHT);
        int labelY = layout.top() + 2;
        int comboY = layout.nextRow(labelY, 8, LittleVecXGuiMetrics.ROW_GAP);
        int comboX = layout.center(GRID_BOX_WIDTH);

        controls.add(new GuiLabel(CoreControl.translate("gui.littlevecx.pliers_grid"), layout.left(), labelY));

        GuiComboBox contextBox = new GuiComboBox("grid", comboX, comboY, GRID_BOX_WIDTH, LittleGridContext.getNames());
        contextBox.select(LittleGridContext.get(stack.getTagCompound()).size + "");
        controls.add(contextBox);
    }

    @Override
    public void saveConfiguration() {
        GuiComboBox contextBox = (GuiComboBox) get("grid");
        LittleGridContext context = LittleGridContext.get();

        try {
            context = LittleGridContext.get(Integer.parseInt(contextBox.getCaption()));
        } catch (RuntimeException e) {
            context = LittleGridContext.get();
        }

        context.set(stack.getTagCompound());
    }
}
