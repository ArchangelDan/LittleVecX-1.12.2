package com.integral.littlevecx.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.controls.gui.GuiCheckBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiComboBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiTextBox;
import com.creativemd.creativecore.common.gui.controls.gui.custom.GuiStackSelectorAll;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.utils.mc.BlockUtils;
import com.creativemd.littletiles.client.gui.LittleSubGuiUtils;
import com.creativemd.littletiles.client.gui.configure.SubGuiConfigure;
import com.creativemd.littletiles.common.item.ItemMultiTiles;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.place.PlacementMode;
import com.creativemd.littletiles.common.util.selection.selector.AnySelector;
import com.creativemd.littletiles.common.util.selection.selector.StateSelector;
import com.creativemd.littletiles.common.util.selection.selector.TileSelector;
import com.creativemd.littletiles.common.util.selection.selector.TileSelectorBlock;
import com.mojang.realmsclient.gui.ChatFormatting;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;

public abstract class SubGuiLittleVecXIndustrialConfigure extends SubGuiConfigure {

    public LittleGridContext context;
    public PlacementMode mode;
    public boolean activeFilter;
    public TileSelector selector;
    private List<String> modeNames;

    public SubGuiLittleVecXIndustrialConfigure(ItemStack stack, LittleGridContext context, PlacementMode mode, boolean activeFilter, TileSelector selector) {
        super(200, 190, stack);
        this.context = context;
        this.mode = mode;
        this.activeFilter = activeFilter;
        this.selector = selector;
    }

    public abstract void saveConfiguration(LittleGridContext context, PlacementMode mode, boolean activeFilter, TileSelector selector);

    @Override
    public void createControls() {
        controls.add(new GuiCheckBox("any", CoreControl.translate("gui.littlevecx.any"), 5, 8, selector == null || selector instanceof AnySelector || !activeFilter));

        GuiStackSelectorAll guiSelector = new GuiStackSelectorAll("filter", 40, 5, 130, container.player, LittleSubGuiUtils.getCollector(getPlayer()), true);
        if (selector instanceof TileSelectorBlock) {
            IBlockState state = ((TileSelectorBlock) selector).getState();
            guiSelector.setSelectedForce(new ItemStack(state.getBlock(), 1, state.getBlock().getMetaFromState(state)));
        }
        controls.add(guiSelector);
        controls.add(new GuiCheckBox("meta", CoreControl.translate("gui.littlevecx.metadata"), 40, 28, selector instanceof StateSelector));

        GuiComboBox contextBox = new GuiComboBox("grid", 5, 40, 28, LittleGridContext.getNames());
        contextBox.select(context.size + "");
        controls.add(contextBox);

        modeNames = new ArrayList<>(PlacementMode.getModeNames());
        GuiComboBox modeBox = new GuiComboBox("mode", 5, 62, 185, new ArrayList<>(PlacementMode.getLocalizedModeNames()));
        modeBox.select(I18n.translateToLocal(mode.name));
        controls.add(modeBox);

        controls.add(new GuiTextBox("text", "", 5, 84, 185));
        onControlChanged(new GuiControlChangedEvent(modeBox));
    }

    private PlacementMode getMode() {
        GuiComboBox box = (GuiComboBox) get("mode");
        if (box.index == -1)
            return PlacementMode.getDefault();
        return PlacementMode.getModeOrDefault(modeNames.get(box.index));
    }

    @Override
    public void saveConfiguration() {
        GuiComboBox contextBox = (GuiComboBox) get("grid");
        try {
            context = LittleGridContext.get(Integer.parseInt(contextBox.getCaption()));
        } catch (NumberFormatException e) {
            context = LittleGridContext.get();
        }

        mode = getMode();
        activeFilter = !((GuiCheckBox) get("any")).value;

        GuiStackSelectorAll filter = (GuiStackSelectorAll) get("filter");
        ItemStack stackFilter = filter.getSelected();
        if (!activeFilter || stackFilter == null || stackFilter.isEmpty()) {
            activeFilter = false;
            selector = new AnySelector();
        } else {
            Block filterBlock = Block.getBlockFromItem(stackFilter.getItem());
            boolean meta = ((GuiCheckBox) get("meta")).value;
            selector = meta ? new StateSelector(BlockUtils.getState(filterBlock, stackFilter.getMetadata())) : new TileSelectorBlock(filterBlock);
        }

        saveConfiguration(context, mode, activeFilter, selector);
    }

    @CustomEventSubscribe
    public void onControlChanged(GuiControlChangedEvent event) {
        if (event.source.is("meta", "filter"))
            ((GuiCheckBox) get("any")).value = false;

        if (event.source.is("mode")) {
            PlacementMode currentMode = getMode();
            ((GuiTextBox) get("text"))
                .setText((currentMode.canPlaceStructures() ? ChatFormatting.BOLD + I18n.translateToLocal("placement.mode.placestructure") + '\n'
                        + ChatFormatting.WHITE : "") + I18n.translateToLocal(currentMode.name + ".tooltip"));
        }
    }
}
