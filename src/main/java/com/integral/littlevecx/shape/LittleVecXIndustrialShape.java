package com.integral.littlevecx.shape;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.creativemd.creativecore.common.gui.GuiControl;
import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.creativecore.common.utils.math.Rotation;
import com.creativemd.creativecore.common.utils.type.HashMapList;
import com.creativemd.littletiles.common.tile.math.box.LittleAbsoluteBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxes;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.shape.LittleShape;
import com.creativemd.littletiles.common.util.shape.ShapeSelection;
import com.creativemd.littletiles.common.util.shape.ShapeSelection.ShapeSelectPos;
import com.integral.littlevecx.selection.IndustrialSelectionRegion;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LittleVecXIndustrialShape extends LittleShape {

    public LittleVecXIndustrialShape() {
        super(Integer.MAX_VALUE);
    }

    @Override
    public boolean requiresNoOverlap() {
        return true;
    }

    @Override
    protected void addBoxes(LittleBoxes boxes, ShapeSelection selection, boolean lowResolution) {
        int totalCount = selection.countPositions();
        if (totalCount <= 0)
            return;

        if (totalCount == 1) {
            boxes.add(new LittleBox(selection.getFirst().pos.getRelative(boxes.pos)));
            return;
        }

        int effectiveCount = totalCount;
        if (effectiveCount % 2 == 1)
            effectiveCount--;

        List<ShapeSelectPos> points = new ArrayList<>(effectiveCount);
        Iterator<ShapeSelectPos> iterator = selection.iterator();
        while (iterator.hasNext() && points.size() < effectiveCount)
            points.add(iterator.next());

        for (int i = 0; i + 1 < points.size(); i += 2) {
            ShapeSelectPos first = points.get(i);
            ShapeSelectPos second = points.get(i + 1);
            IndustrialSelectionRegion region = new IndustrialSelectionRegion(first.pos.copy(), second.pos.copy(), second.pos.facing);
            LittleAbsoluteBox absoluteBox = region.toAbsoluteBox();
            if (absoluteBox == null)
                continue;

            HashMapList<BlockPos, LittleBox> split = absoluteBox.splitted();
            for (Entry<BlockPos, ArrayList<LittleBox>> entry : split.entrySet())
                for (LittleBox box : entry.getValue())
                    boxes.addBox(absoluteBox.context, entry.getKey(), box.copy());
        }

        if (totalCount % 2 == 1)
            boxes.add(new LittleBox(selection.getLast().pos.getRelative(boxes.pos)));
    }

    @Override
    public void addExtraInformation(NBTTagCompound nbt, List<String> list) {
        list.add("mode: industrial");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public List<GuiControl> getCustomSettings(NBTTagCompound nbt, LittleGridContext context) {
        return new ArrayList<>();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void saveCustomSettings(GuiParent gui, NBTTagCompound nbt, LittleGridContext context) {}

    @Override
    public void rotate(NBTTagCompound nbt, Rotation rotation) {}

    @Override
    public void flip(NBTTagCompound nbt, Axis axis) {}
}
