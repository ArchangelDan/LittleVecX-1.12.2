package com.integral.littlevecx.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiComboBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiLabel;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.littletiles.client.gui.signal.GuiSignalController;
import com.creativemd.littletiles.client.gui.signal.GuiSignalController.GeneratePatternException;
import com.creativemd.littletiles.client.gui.signal.SubGuiDialogSignal;
import com.creativemd.littletiles.client.gui.signal.SubGuiDialogSignalMode;
import com.creativemd.littletiles.common.structure.signal.input.SignalInputCondition;
import com.creativemd.littletiles.common.structure.signal.logic.SignalLogicOperator;

public class SubGuiLittleVecXDialogSignal extends SubGuiDialogSignal {

    private static final int DIALOG_WIDTH = 420;
    private static final int DIALOG_HEIGHT = 200;
    private static final int CONTROLLER_WIDTH = 414;
    private static final int INPUTS_WIDTH = 150;
    private static final int ADD_INPUT_X = INPUTS_WIDTH + 8;
    private static final int ADD_INPUT_WIDTH = 30;
    private static final int SEARCH_BUTTON_X = ADD_INPUT_X + ADD_INPUT_WIDTH + 6;
    private static final int SEARCH_BUTTON_Y = 176;
    private static final int OPERATORS_X = SEARCH_BUTTON_X + GuiLittleVecXSearchIconButton.BUTTON_SIZE + 8;
    private static final int OPERATORS_WIDTH = 60;
    
    private final boolean openSearchOnCreate;

    public SubGuiLittleVecXDialogSignal(List<GuiSignalComponent> inputs, IConditionConfiguration event) {
        this(inputs, event, false);
    }

    public SubGuiLittleVecXDialogSignal(List<GuiSignalComponent> inputs, IConditionConfiguration event,
            boolean openSearchOnCreate) {
        super(inputs, event);
        this.width = DIALOG_WIDTH;
        this.height = DIALOG_HEIGHT;
        this.openSearchOnCreate = openSearchOnCreate;
    }

    @Override
    public void createControls() {
        controls.add(new GuiLabel("result", translate("gui.signal.configuration.result"), 0, 0));

        GuiSignalController controller = new GuiSignalController("controller", 0, 22, CONTROLLER_WIDTH, 150, event.getOutput(), inputs);
        controls.add(controller);

        List<String> inputLines = new ArrayList<>();
        for (GuiSignalComponent entry : inputs)
            inputLines.add(entry.info());
        inputLines.add("[]");
        inputLines.add("number");

        controls.add(new GuiComboBox("inputs", 0, 180, INPUTS_WIDTH, inputLines));
        controls.add(new GuiButton("add", translate("gui.signal.configuration.add"), ADD_INPUT_X, 180, ADD_INPUT_WIDTH) {

            @Override
            public void onClicked(int x, int y, int button) {
                GuiComboBox inputsBox = (GuiComboBox) SubGuiLittleVecXDialogSignal.this.get("inputs");
                if (inputsBox.index < inputs.size())
                    controller.addInput(inputs.get(inputsBox.index));
                else if (inputsBox.index == inputs.size())
                    controller.addVirtualInput();
                else
                    controller.addVirtualNumberInput();
            }
        });
        controls.add(new GuiLittleVecXSearchIconButton("search", SEARCH_BUTTON_X, SEARCH_BUTTON_Y) {

            @Override
            public void onClicked(int x, int y, int button) {
                openSearch(controller);
            }
        });

        List<String> operatorLines = new ArrayList<>();
        operatorLines.add(SignalLogicOperator.AND.display);
        operatorLines.add(SignalLogicOperator.OR.display);
        operatorLines.add(SignalLogicOperator.XOR.display);
        operatorLines.add("not");
        operatorLines.add(SignalLogicOperator.BITWISE_AND.display);
        operatorLines.add(SignalLogicOperator.BITWISE_OR.display);
        operatorLines.add(SignalLogicOperator.BITWISE_XOR.display);
        operatorLines.add("b-not");
        operatorLines.add(SignalLogicOperator.ADD.display);
        operatorLines.add(SignalLogicOperator.SUB.display);
        operatorLines.add(SignalLogicOperator.MUL.display);
        operatorLines.add(SignalLogicOperator.DIV.display);

        controls.add(new GuiComboBox("operators", OPERATORS_X, 180, OPERATORS_WIDTH, operatorLines));
        controls.add(new GuiButton("add", translate("gui.signal.configuration.addop"), OPERATORS_X + OPERATORS_WIDTH + 8, 180) {

            @Override
            public void onClicked(int x, int y, int button) {
                GuiComboBox operatorsBox = (GuiComboBox) SubGuiLittleVecXDialogSignal.this.get("operators");
                int index = operatorsBox.index;
                if (index < 3)
                    controller.addOperator(SignalLogicOperator.values()[index]);
                else if (index == 3)
                    controller.addNotOperator(false);
                else if (index == 7)
                    controller.addNotOperator(true);
                else if (index > 7)
                    controller.addOperator(SignalLogicOperator.values()[index - 2]);
                else
                    controller.addOperator(SignalLogicOperator.values()[index - 1]);
            }
        });

        if (event.getCondition() != null)
            controller.setCondition(event.getCondition(), this);

        controls.add(new GuiLabel("delay", DIALOG_WIDTH - 120, 182));

        changed(new GuiControlChangedEvent(controller));

        if (event.hasModeConfiguration())
            controls.add(new GuiButton("mode", 0, 0) {

                @Override
                public void onClicked(int x, int y, int button) {
                    openClientLayer(new SubGuiDialogSignalMode(SubGuiLittleVecXDialogSignal.this, event));
                }
            });

        controls.add(new GuiButton("save", translate("gui.signal.configuration.save"), DIALOG_WIDTH - 30, 180) {

            @Override
            public void onClicked(int x, int y, int button) {
                try {
                    event.setCondition(controller.generatePattern());
                    event.update();
                    closeGui();
                } catch (GeneratePatternException e) {}
            }
        });
        modeChanged();
        
        if (openSearchOnCreate)
            openSearch(controller);
    }

    @Override
    public void modeChanged() {
        if (event.hasModeConfiguration()) {
            GuiButton button = (GuiButton) get("mode");
            button.setCaption(translate(event.getModeConfiguration().getMode().translateKey));
            button.posX = DIALOG_WIDTH - button.width;
        }
    }

    protected void openSearch(GuiSignalController controller) {
        openClientLayer(new SubGuiLittleVecXSignalSearchPopup(controller, inputs));
    }
}
