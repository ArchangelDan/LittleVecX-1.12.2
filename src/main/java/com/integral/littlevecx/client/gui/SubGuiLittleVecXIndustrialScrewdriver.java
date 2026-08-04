package com.integral.littlevecx.client.gui;

import java.util.List;

import org.lwjgl.util.Color;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.GuiControl;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiCheckBox;
import com.creativemd.creativecore.common.gui.controls.gui.custom.GuiStackSelectorAll;
import com.creativemd.creativecore.common.utils.mc.BlockUtils;
import com.creativemd.creativecore.common.utils.mc.ColorUtils;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.gui.LittleSubGuiUtils;
import com.creativemd.littletiles.client.gui.configure.SubGuiConfigure;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.action.block.LittleActionDestroyBoxes;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxes;
import com.creativemd.littletiles.common.util.selection.selector.AndSelector;
import com.creativemd.littletiles.common.util.selection.selector.AnySelector;
import com.creativemd.littletiles.common.util.selection.selector.NoStructureSelector;
import com.creativemd.littletiles.common.util.selection.selector.StateSelector;
import com.creativemd.littletiles.common.util.selection.selector.TileSelector;
import com.creativemd.littletiles.common.util.selection.selector.TileSelectorBlock;
import com.integral.littlevecx.client.gui.controls.GuiLittleVecXColorPicker;
import com.integral.littlevecx.action.LittleVecXActionQueuedScrewdriverReplace;
import com.integral.littlevecx.selection.IndustrialSelectionMode;
import com.integral.littlevecx.selection.IndustrialSelectionRegion;
import com.integral.littlevecx.screwdriver.LittleVecXColorSelector;
import com.integral.littlevecx.screwdriver.LittleVecXIndustrialScrewdriverLogic;

import net.minecraft.block.Block;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;

public class SubGuiLittleVecXIndustrialScrewdriver extends SubGuiConfigure {

    private static final String KEY_FILTER_ANY = "littlevecx_screwdriver_filter_any";
    private static final String KEY_FILTER_META = "littlevecx_screwdriver_filter_meta";
    private static final String KEY_FILTER_COLORIZE = "littlevecx_screwdriver_filter_colorize";
    private static final String KEY_FILTER_COLOR_VALUE = "littlevecx_screwdriver_filter_color_value";
    private static final String KEY_COLORIZE_VALUE = "littlevecx_screwdriver_colorize_value";
    private static final String KEY_REMOVE = "littlevecx_screwdriver_remove";
    private static final String KEY_REPLACE = "littlevecx_screwdriver_replace";
    private static final String KEY_META_REPLACEMENT = "littlevecx_screwdriver_meta_replacement";
    private static final String KEY_BLOCK_MODE = "littlevecx_screwdriver_block_mode";

    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 260;
    private static final int BUTTON_X = 148;
    private static final int BUTTON_WIDTH = 45;
    private static final int BUTTON_HEIGHT = 14;
    private static final int MODE_BUTTON_X = 135;
    private static final int MODE_BUTTON_Y = 136;
    private static final int MODE_BUTTON_NORMAL_Y = 100;
    private static final int MODE_BUTTON_WIDTH = 58;

    public static ItemStack lastSelectedSearchStack;
    public static ItemStack lastSelectedReplaceStack;

    public SubGuiLittleVecXIndustrialScrewdriver(ItemStack stack) {
        super(GUI_WIDTH, GUI_HEIGHT, stack);
    }

    @Override
    public void createControls() {
        ensureTag();
        boolean blockMode = isBlockMode();

        controls.add(new GuiCheckBox("any", CoreControl.translate("gui.littlevecx.any"), 5, 8, getBoolean(KEY_FILTER_ANY, false)));

        GuiStackSelectorAll selector = new GuiStackSelectorAll("filter", 40, 5, 130, getPlayer(), LittleSubGuiUtils.getCollector(getPlayer()), true);
        if (lastSelectedSearchStack != null)
            selector.setSelectedForce(lastSelectedSearchStack);
        controls.add(selector);

        int actionStartY = 121;
        int replacementY = 148;
        int metaReplacementY = 172;
        int pickerY = 183;

        if (!blockMode) {
            controls.add(new GuiCheckBox("matchColor", CoreControl.translate("gui.littlevecx.screwdriver.match_color"), 5, 48, getBoolean(KEY_FILTER_COLORIZE, true)));
            controls.add(new GuiCheckBox("meta", CoreControl.translate("gui.littlevecx.metadata"), 95, 48, getBoolean(KEY_FILTER_META, true)));
            controls.add(new GuiLittleVecXColorPicker("matchPicker", 5, 60,
                    readStoredColor(KEY_FILTER_COLOR_VALUE, ColorUtils.WHITE),
                    LittleTiles.CONFIG.isTransparencyEnabled(getPlayer()),
                    LittleTiles.CONFIG.getMinimumTransparency(getPlayer())));
        } else {
            controls.add(new GuiCheckBox("meta", CoreControl.translate("gui.littlevecx.metadata"), 40, 48, getBoolean(KEY_FILTER_META, true)));
            actionStartY = 60;
            replacementY = 87;
            metaReplacementY = 111;
        }

        controls.add(new GuiCheckBox("remove", CoreControl.translate("gui.littlevecx.remove"), 5, actionStartY, getBoolean(KEY_REMOVE, false)));
        controls.add(new GuiCheckBox("replace", CoreControl.translate("gui.littlevecx.screwdriver.replace_with"), 5, actionStartY + 13, getBoolean(KEY_REPLACE, true)));

        selector = new GuiStackSelectorAll("replacement", 40, replacementY, 130, getPlayer(), LittleSubGuiUtils.getCollector(getPlayer()), true);
        if (lastSelectedReplaceStack != null)
            selector.setSelectedForce(lastSelectedReplaceStack);
        controls.add(selector);

        controls.add(new GuiCheckBox("metaR", CoreControl.translate("gui.littlevecx.force_metadata"), 40, metaReplacementY, getBoolean(KEY_META_REPLACEMENT, true)));

        if (!blockMode) {
            Color resultColor = readStoredColor(KEY_COLORIZE_VALUE, ColorUtils.WHITE);
            controls.add(new GuiLittleVecXColorPicker("picker", 5, pickerY, resultColor,
                    LittleTiles.CONFIG.isTransparencyEnabled(getPlayer()),
                    LittleTiles.CONFIG.getMinimumTransparency(getPlayer())));
        }

        controls.add(new GuiButton("blockMode", CoreControl.translate(blockMode ? "gui.littlevecx.return" : "gui.littlevecx.screwdriver.block_mode"), MODE_BUTTON_X,
                blockMode ? MODE_BUTTON_Y : MODE_BUTTON_NORMAL_Y, MODE_BUTTON_WIDTH, BUTTON_HEIGHT) {
            @Override
            public void onClicked(int x, int y, int button) {
                saveConfiguration();
                stack.getTagCompound().setBoolean(KEY_BLOCK_MODE, !isBlockMode());
                rebuildControls();
            }
        });

        controls.add(new GuiButton("undo", CoreControl.translate("gui.littlevecx.undo"), BUTTON_X, 204, BUTTON_WIDTH, BUTTON_HEIGHT) {
            @Override
            public void onClicked(int x, int y, int button) {
                try {
                    LittleAction.undo();
                } catch (LittleActionException e) {
                    getPlayer().sendStatusMessage(new TextComponentString(e.getLocalizedMessage()), true);
                }
            }
        });

        controls.add(new GuiButton("redo", CoreControl.translate("gui.littlevecx.redo"), BUTTON_X, 222, BUTTON_WIDTH, BUTTON_HEIGHT) {
            @Override
            public void onClicked(int x, int y, int button) {
                try {
                    LittleAction.redo();
                } catch (LittleActionException e) {
                    getPlayer().sendStatusMessage(new TextComponentString(e.getLocalizedMessage()), true);
                }
            }
        });

        controls.add(new GuiButton("run", CoreControl.translate("gui.littlevecx.do_it"), BUTTON_X, 240, BUTTON_WIDTH, BUTTON_HEIGHT) {
            @Override
            public void onClicked(int x, int y, int button) {
                saveConfiguration();
                LittleAction action = getDesiredAction();
                if (action != null && action.execute())
                    playSound(SoundEvents.BLOCK_LEVER_CLICK);
            }
        });
    }

    public LittleAction getDesiredAction() {
        boolean remove = ((GuiCheckBox) get("remove")).value;
        boolean replace = ((GuiCheckBox) get("replace")).value;
        boolean blockMode = isBlockMode();
        boolean applyReplacementColor = !blockMode;

        if (remove) {
            LittleBoxes boxes = LittleVecXIndustrialScrewdriverLogic.buildFilteredBoxes(getPlayer().world, stack, buildSelector());
            if (boxes.isEmpty()) {
                openButtonDialogDialog(CoreControl.translate("message.littlevecx.screwdriver.no_matching_tiles"), "ok");
                return null;
            }
            return new LittleActionDestroyBoxes(boxes);
        }

        if (replace) {
            GuiStackSelectorAll replacement = (GuiStackSelectorAll) get("replacement");
            ItemStack stackReplace = replacement.getSelected();
            if (stackReplace != null && !stackReplace.isEmpty()) {
                Block replacementBlock = Block.getBlockFromItem(stackReplace.getItem());
                if (!LittleAction.isBlockValid(BlockUtils.getState(replacementBlock, stackReplace.getMetadata()))) {
                    openButtonDialogDialog(CoreControl.translate("message.littlevecx.screwdriver.invalid_replacement_block"), "ok");
                    return null;
                }

                int replacementColor = ColorUtils.WHITE;
                if (applyReplacementColor) {
                    GuiLittleVecXColorPicker picker = (GuiLittleVecXColorPicker) get("picker");
                    replacementColor = ColorUtils.RGBAToInt(picker.color);
                }

                List<IndustrialSelectionRegion> selectionRegions = IndustrialSelectionMode.getSavedOrCurrentRegions(stack);
                if (selectionRegions.isEmpty()) {
                    openButtonDialogDialog(CoreControl.translate("message.littlevecx.industrial.no_selection"), "ok");
                    return null;
                }

                return new LittleVecXActionQueuedScrewdriverReplace(selectionRegions, buildSelector(), replacementBlock, stackReplace.getMetadata(), blockMode, applyReplacementColor, replacementColor);
            }
        }

        openButtonDialogDialog(CoreControl.translate("message.littlevecx.screwdriver.select_task"), "ok");
        return null;
    }

    @Override
    public void closeGui() {
        saveConfiguration();
        super.closeGui();
        lastSelectedSearchStack = ((GuiStackSelectorAll) get("filter")).getSelected();
        lastSelectedReplaceStack = ((GuiStackSelectorAll) get("replacement")).getSelected();
    }

    @Override
    public void saveConfiguration() {
        ensureTag();
        stack.getTagCompound().setBoolean(KEY_FILTER_ANY, ((GuiCheckBox) get("any")).value);
        stack.getTagCompound().setBoolean(KEY_FILTER_META, ((GuiCheckBox) get("meta")).value);
        if (get("matchColor") instanceof GuiCheckBox)
            stack.getTagCompound().setBoolean(KEY_FILTER_COLORIZE, ((GuiCheckBox) get("matchColor")).value);
        stack.getTagCompound().setBoolean(KEY_REMOVE, ((GuiCheckBox) get("remove")).value);
        stack.getTagCompound().setBoolean(KEY_REPLACE, ((GuiCheckBox) get("replace")).value);
        stack.getTagCompound().setBoolean(KEY_META_REPLACEMENT, ((GuiCheckBox) get("metaR")).value);
        stack.getTagCompound().setBoolean(KEY_BLOCK_MODE, isBlockMode());
        if (get("matchPicker") instanceof GuiLittleVecXColorPicker)
            stack.getTagCompound().setInteger(KEY_FILTER_COLOR_VALUE, ColorUtils.RGBAToInt(((GuiLittleVecXColorPicker) get("matchPicker")).color));
        if (get("picker") instanceof GuiLittleVecXColorPicker)
            stack.getTagCompound().setInteger(KEY_COLORIZE_VALUE, ColorUtils.RGBAToInt(((GuiLittleVecXColorPicker) get("picker")).color));
    }

    private TileSelector buildSelector() {
        TileSelector selector;
        if (((GuiCheckBox) get("any")).value) {
            selector = new AnySelector();
        } else {
            GuiStackSelectorAll filter = (GuiStackSelectorAll) get("filter");
            ItemStack stackFilter = filter.getSelected();
            if (stackFilter == null || stackFilter.isEmpty()) {
                selector = new AnySelector();
            } else {
                Block filterBlock = Block.getBlockFromItem(stackFilter.getItem());
                boolean meta = ((GuiCheckBox) get("meta")).value;
                selector = meta ? new StateSelector(BlockUtils.getState(filterBlock, stackFilter.getMetadata())) : new TileSelectorBlock(filterBlock);
            }
        }

        if (isBlockMode())
            return new AndSelector(new NoStructureSelector(), selector);

        boolean matchColor = !(get("matchColor") instanceof GuiCheckBox) || ((GuiCheckBox) get("matchColor")).value;
        if (!matchColor)
            return new AndSelector(new NoStructureSelector(), selector);

        return new AndSelector(new NoStructureSelector(), selector,
                new LittleVecXColorSelector(ColorUtils.RGBAToInt(((GuiLittleVecXColorPicker) get("matchPicker")).color)));
    }

    private void ensureTag() {
        if (!stack.hasTagCompound())
            stack.setTagCompound(new NBTTagCompound());
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey(key) ? stack.getTagCompound().getBoolean(key) : defaultValue;
    }

    private boolean isBlockMode() {
        return getBoolean(KEY_BLOCK_MODE, false);
    }

    private void rebuildControls() {
        controls.clear();
        createControls();
        for (int i = 0; i < controls.size(); i++) {
            GuiControl control = controls.get(i);
            updateControl(control, i);
            control.onOpened();
        }
        refreshControls();
    }

    private Color readStoredColor(String key, int fallback) {
        int color = stack.hasTagCompound() && stack.getTagCompound().hasKey(key) ? stack.getTagCompound().getInteger(key) : fallback;
        return ColorUtils.IntToRGBA(color);
    }
}
