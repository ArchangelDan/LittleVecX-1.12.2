package com.integral.littlevecx.client.gui;

import java.util.ArrayList;
import java.util.Arrays;

import org.lwjgl.util.Color;

import com.creativemd.creativecore.client.avatar.AvatarItemStack;
import com.creativemd.creativecore.common.gui.controls.gui.GuiAvatarLabel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiComboBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiComboBoxTranslated;
import com.creativemd.creativecore.common.gui.controls.gui.GuiScrollBox;
import com.creativemd.creativecore.common.gui.controls.gui.custom.GuiStackSelectorAll;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.utils.mc.ColorUtils;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.gui.LittleSubGuiUtils;
import com.creativemd.littletiles.client.gui.configure.SubGuiConfigure;
import com.creativemd.littletiles.common.api.ILittlePlacer;
import com.creativemd.littletiles.common.item.ItemBlockTiles;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.preview.LittlePreview;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.shape.LittleShape;
import com.creativemd.littletiles.common.util.shape.ShapeRegistry;
import com.integral.littlevecx.client.gui.controls.GuiLittleVecXColorPicker;
import com.integral.littlevecx.item.ItemLittleVecXIndustrialChisel;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class SubGuiLittleVecXIndustrialChisel extends SubGuiConfigure {

    private static final ArrayList<String> ALLOWED_SHAPES = new ArrayList<>(Arrays.asList("slice_fix", "pillar_fix"));

    public SubGuiLittleVecXIndustrialChisel(ItemStack stack) {
        super(140, 180, stack);
        this.stack = stack;
    }

    public LittleGridContext getContext() {
        return ((ILittlePlacer) stack.getItem()).getPositionContext(stack);
    }

    @Override
    public void createControls() {
        LittlePreview preview = ItemLittleVecXIndustrialChisel.getPreview(stack);
        Color color = ColorUtils.IntToRGBA(preview.getColor());
        controls.add(new GuiLittleVecXColorPicker("picker", 2, 2, color, LittleTiles.CONFIG.isTransparencyEnabled(getPlayer()), LittleTiles.CONFIG.getMinimumTransparency(getPlayer())));
        GuiStackSelectorAll selector = new GuiStackSelectorAll("preview", 0, 75, 112, getPlayer(), LittleSubGuiUtils.getCollector(getPlayer()), true);
        selector.setSelectedForce(preview.getBlockStack());
        controls.add(selector);

        GuiComboBox box = new GuiComboBoxTranslated("shape", 0, 96, 134, "shape.", new ArrayList<>(ALLOWED_SHAPES));
        String currentShapeKey = ItemLittleVecXIndustrialChisel.getShape(stack).getKey();
        box.select(ALLOWED_SHAPES.contains(currentShapeKey) ? currentShapeKey : ItemLittleVecXIndustrialChisel.DEFAULT_SHAPE_KEY);
        GuiScrollBox scroll = new GuiScrollBox("settings", 0, 117, 133, 58);
        controls.add(box);
        controls.add(scroll);
        onChange();

        GuiAvatarLabel label = new GuiAvatarLabel("", 115, 35, 0, null);
        label.name = "avatar";
        label.height = 60;
        label.avatarSize = 32;
        controls.add(label);

        updateLabel();
    }

    @CustomEventSubscribe
    public void onComboBoxChange(GuiControlChangedEvent event) {
        if (event.source.is("shape"))
            onChange();
        else if (event.source.is("picker", "preview"))
            updateLabel();
    }

    public void onChange() {
        GuiComboBox box = (GuiComboBox) get("shape");
        GuiScrollBox scroll = (GuiScrollBox) get("settings");

        LittleShape shape = ShapeRegistry.getShape(box.getCaption());
        scroll.controls.clear();
        scroll.controls.addAll(shape.getCustomSettings(stack.getTagCompound(), getContext()));
        scroll.refreshControls();
    }

    public void updateLabel() {
        GuiStackSelectorAll selector = (GuiStackSelectorAll) get("preview");
        ItemStack selected = selector.getSelected();
        LittlePreview preview;

        if (!selected.isEmpty() && selected.getItem() instanceof ItemBlock) {
            LittleTile tile = new LittleTile(((ItemBlock) selected.getItem()).getBlock(), selected.getMetadata());
            tile.setBox(new LittleBox(0, 0, 0, LittleGridContext.get().size, LittleGridContext.get().size, LittleGridContext.get().size));
            preview = tile.getPreviewTile();
        } else
            preview = ItemLittleVecXIndustrialChisel.getPreview(stack);

        GuiLittleVecXColorPicker picker = (GuiLittleVecXColorPicker) get("picker");
        preview.setColor(ColorUtils.RGBAToInt(picker.color));

        GuiAvatarLabel label = (GuiAvatarLabel) get("avatar");
        label.avatar = new AvatarItemStack(ItemBlockTiles.getStackFromPreview(LittleGridContext.get(), preview));
    }

    @Override
    public void saveConfiguration() {
        GuiComboBox box = (GuiComboBox) get("shape");
        GuiScrollBox scroll = (GuiScrollBox) get("settings");
        LittleShape shape = ShapeRegistry.getShape(box.getCaption());

        GuiLittleVecXColorPicker picker = (GuiLittleVecXColorPicker) get("picker");
        LittlePreview preview = ItemLittleVecXIndustrialChisel.getPreview(stack);

        GuiStackSelectorAll selector = (GuiStackSelectorAll) get("preview");
        ItemStack selected = selector.getSelected();

        if (!selected.isEmpty() && selected.getItem() instanceof ItemBlock) {
            LittleTile tile = new LittleTile(((ItemBlock) selected.getItem()).getBlock(), selected.getMetadata());
            tile.setBox(new LittleBox(0, 0, 0, LittleGridContext.get().size, LittleGridContext.get().size, LittleGridContext.get().size));
            preview = tile.getPreviewTile();
        } else
            preview = ItemLittleVecXIndustrialChisel.getPreview(stack);

        preview.setColor(ColorUtils.RGBAToInt(picker.color));
        ItemLittleVecXIndustrialChisel.setPreview(stack, preview);
        ItemLittleVecXIndustrialChisel.setShape(stack, shape);

        shape.saveCustomSettings(scroll, stack.getTagCompound(), getContext());
    }
}
