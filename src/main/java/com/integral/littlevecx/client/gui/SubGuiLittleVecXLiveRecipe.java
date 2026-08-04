package com.integral.littlevecx.client.gui;

import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiComboBoxCategory;
import com.creativemd.creativecore.common.gui.GuiControl;
import com.creativemd.creativecore.common.gui.controls.gui.GuiPanel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiTextfield;
import com.creativemd.creativecore.common.gui.controls.gui.custom.GuiItemComboBox;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.gui.SubGuiRecipe;
import com.creativemd.littletiles.client.gui.controls.GuiAnimationViewer;
import com.creativemd.littletiles.common.tile.math.location.StructureLocation;
import com.creativemd.littletiles.common.tile.preview.LittlePreview;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.integral.littlevecx.network.PacketLittleVecXApplyStructurePreviews;

import net.minecraft.item.ItemStack;

public class SubGuiLittleVecXLiveRecipe extends SubGuiRecipe {

    private static final int LIVE_RECIPE_WIDTH = 468;
    private static final int LIVE_RECIPE_HEIGHT = 252;
    private static final int PANEL_WIDTH = 268;
    private static final int PANEL_HEIGHT = 176;
    private static final int VIEWER_GAP = 8;

    private final StructureLocation location;

    public SubGuiLittleVecXLiveRecipe(StructureLocation location, LittlePreviews previews) {
        super(createRecipeStack(previews));
        this.location = location;
        this.width = LIVE_RECIPE_WIDTH;
        this.height = LIVE_RECIPE_HEIGHT;
    }

    private static ItemStack createRecipeStack(LittlePreviews previews) {
        ItemStack stack = new ItemStack(LittleTiles.recipeAdvanced);
        LittlePreview.savePreview(previews, stack);
        return stack;
    }

    @Override
    public void createControls() {
        super.createControls();

        if (get("save") != null)
            controls.remove(get("save"));

        if (get("clear") != null)
            get("clear").setEnabled(false);

        controls.add(new GuiButton("save", 150, 176, 40) {
            @Override
            public void onClicked(int x, int y, int button) {
                savePreview();
                finializePreview(previews);
                PacketHandler.sendPacketToServer(new PacketLittleVecXApplyStructurePreviews(location, previews.copy()));
                closeGui();
            }
        });

        layoutLiveRecipe();
        lockRootTypeSelector();
    }

    @Override
    public void loadStack(StructureHolder holder) {
        super.loadStack(holder);
        layoutLiveRecipe();
        lockRootTypeSelector();
    }

    @Override
    public void onStructureSelectorChanged() {
        super.onStructureSelectorChanged();
        layoutLiveRecipe();
        lockRootTypeSelector();
    }

    private void layoutLiveRecipe() {
        GuiPanel panel = (GuiPanel) get("panel");
        GuiAnimationViewer viewer = (GuiAnimationViewer) get("renderer");
        GuiItemComboBox hierarchy = (GuiItemComboBox) get("hierarchy");
        GuiControl types = get("types");
        GuiControl clear = get("clear");
        GuiControl tilesCount = get("tilescount");
        GuiControl play = get("play");
        GuiControl pause = get("pause");
        GuiControl stop = get("stop");
        GuiTextfield name = (GuiTextfield) get("name");
        GuiControl save = get("save");

        if (panel == null || viewer == null || hierarchy == null || types == null || clear == null || tilesCount == null || play == null || pause == null || stop == null
                || name == null || save == null)
            return;

        panel.posX = 0;
        panel.posY = 30;
        panel.width = PANEL_WIDTH;
        panel.height = PANEL_HEIGHT;

        types.posX = 0;
        types.posY = 5;
        types.width = PANEL_WIDTH;

        hierarchy.posX = PANEL_WIDTH + VIEWER_GAP;
        hierarchy.posY = 5;
        hierarchy.width = this.width - hierarchy.posX - 6;

        viewer.posX = hierarchy.posX;
        viewer.posY = panel.posY;
        viewer.width = hierarchy.width;
        viewer.height = panel.height;

        int bottomY = this.height - 26;
        name.posX = 2;
        name.posY = bottomY;

        clear.posX = 105;
        clear.posY = bottomY;

        save.posX = PANEL_WIDTH - save.width;
        save.posY = bottomY;

        tilesCount.posX = viewer.posX;
        tilesCount.posY = bottomY - 20;

        int controlsCenter = viewer.posX + (viewer.width / 2);
        int totalControlWidth = play.width + pause.width + stop.width + 4;
        int playX = controlsCenter - (totalControlWidth / 2);
        play.posX = playX;
        pause.posX = play.posX + play.width + 2;
        stop.posX = pause.posX + pause.width + 2;
        play.posY = bottomY;
        pause.posY = bottomY;
        stop.posY = bottomY;
    }

    private void lockRootTypeSelector() {
        if (selected == null || selected.parent != null)
            return;
        if (get("types") instanceof GuiComboBoxCategory)
            ((GuiComboBoxCategory<?>) get("types")).enabled = false;
    }
}
