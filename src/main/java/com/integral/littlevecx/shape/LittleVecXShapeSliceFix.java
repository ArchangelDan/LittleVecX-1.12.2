package com.integral.littlevecx.shape;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.creativecore.common.gui.GuiControl;
import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.creativecore.common.utils.math.Rotation;
import com.creativemd.creativecore.common.utils.math.RotationUtils;
import com.creativemd.littletiles.common.tile.math.box.LittleVecXSliceFixBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxes;
import com.creativemd.littletiles.common.tile.math.box.slice.LittleSlice;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.shape.LittleShape;
import com.creativemd.littletiles.common.util.shape.ShapeSelection;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LittleVecXShapeSliceFix extends LittleShape {

    public LittleVecXShapeSliceFix() {
        super(2);
    }

    @Override
    protected void addBoxes(LittleBoxes boxes, ShapeSelection selection, boolean lowResolution) {
        LittleBox box = selection.getOverallBox();
        Vec3i vec = getVec(selection.getNBT());
        boxes.add(LittleVecXSliceFixBox.create(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, vec));
    }

    @Override
    public void addExtraInformation(NBTTagCompound nbt, List<String> list) {}

    @Override
    @SideOnly(Side.CLIENT)
    public List<GuiControl> getCustomSettings(NBTTagCompound nbt, LittleGridContext context) {
        return new ArrayList<>();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void saveCustomSettings(GuiParent gui, NBTTagCompound nbt, LittleGridContext context) {}

    @Override
    public void rotate(NBTTagCompound nbt, Rotation rotation) {
        setVec(nbt, RotationUtils.rotate(getVec(nbt), rotation));
    }

    @Override
    public void flip(NBTTagCompound nbt, Axis axis) {
        setVec(nbt, RotationUtils.flip(getVec(nbt), axis));
    }

    public static Vec3i getVec(NBTTagCompound nbt) {
        if (nbt != null && nbt.hasKey("vec")) {
            int[] array = nbt.getIntArray("vec");
            if (array.length == 3)
                return new Vec3i(array[0], array[1], array[2]);
        }

        if (nbt != null && nbt.hasKey("slice")) {
            int ordinal = nbt.getInteger("slice");
            LittleSlice[] slices = LittleSlice.values();
            if (ordinal >= 0 && ordinal < slices.length)
                return slices[ordinal].sliceVec;
        }

        return new Vec3i(0, 1, 1);
    }

    public static void setVec(NBTTagCompound nbt, Vec3i vec) {
        nbt.setIntArray("vec", new int[] { vec.getX(), vec.getY(), vec.getZ() });
    }
}
