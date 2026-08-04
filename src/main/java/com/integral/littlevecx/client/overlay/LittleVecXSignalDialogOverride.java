package com.integral.littlevecx.client.overlay;

import com.creativemd.littletiles.client.gui.signal.GuiSignalController;
import com.creativemd.littletiles.client.gui.signal.SubGuiDialogSignal;
import com.integral.littlevecx.client.gui.GuiLittleVecXSearchIconButton;
import com.integral.littlevecx.client.gui.SubGuiLittleVecXSignalSearchPopup;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleVecXSignalDialogOverride extends LittleVecXSubGuiOverride<SubGuiDialogSignal> {

    private static final String SEARCH_BUTTON_ID = "littlevecx_overlay_search";
    private static final int SEARCH_RIGHT_MARGIN = 8;
    private static final int SEARCH_MODE_GAP = 20;
    private static final int SEARCH_RAISE_OFFSET = 3;

    public LittleVecXSignalDialogOverride() {
        super(SubGuiDialogSignal.class);
    }

    @Override
    protected void overrideGui(SubGuiDialogSignal gui) {
        if (gui.get(SEARCH_BUTTON_ID) != null)
            return;

        GuiSignalController controller = (GuiSignalController) gui.get("controller");
        if (controller == null || gui.inputs == null)
            return;

        int posX = gui.width - GuiLittleVecXSearchIconButton.BUTTON_SIZE - SEARCH_RIGHT_MARGIN;
        int posY = -2;
        com.creativemd.creativecore.common.gui.controls.gui.GuiButton modeButton =
                (com.creativemd.creativecore.common.gui.controls.gui.GuiButton) gui.get("mode");
        if (modeButton != null) {
            posX = modeButton.posX - GuiLittleVecXSearchIconButton.BUTTON_SIZE - SEARCH_MODE_GAP;
            int modeCenterY = modeButton.posY + (modeButton.height / 2);
            posY = modeCenterY - (GuiLittleVecXSearchIconButton.BUTTON_SIZE / 2) - SEARCH_RAISE_OFFSET;
        }

        gui.addControl(new GuiLittleVecXSearchIconButton(SEARCH_BUTTON_ID, posX, posY) {

            @Override
            public void onClicked(int x, int y, int button) {
                gui.openClientLayer(new SubGuiLittleVecXSignalSearchPopup(controller, gui.inputs));
            }
        });
    }
}
