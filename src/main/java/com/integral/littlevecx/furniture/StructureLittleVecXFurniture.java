package com.integral.littlevecx.furniture;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.creativecore.common.gui.controls.gui.GuiCheckBox;
import com.creativemd.littletiles.client.gui.handler.LittleStructureGuiHandler;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.animation.AnimationGuiHandler;
import com.creativemd.littletiles.common.structure.registry.LittleStructureGuiParser;
import com.creativemd.littletiles.common.structure.registry.LittleStructureRegistry;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType;
import com.creativemd.littletiles.common.tile.parent.IStructureTileList;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.integral.littlevecx.LittleVecXMod;

import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class StructureLittleVecXFurniture extends LittleStructure {

    public static final String STRUCTURE_ID = "furniture";
    public static final String REQUIRE_FLAT_SURFACE_KEY = "requireFlatSurface";
    public static final String MAGNET_ENABLED_KEY = "magnetEnabled";

    public boolean requireFlatSurface = true;
    public boolean magnetEnabled = true;

    public StructureLittleVecXFurniture(LittleStructureType type, IStructureTileList mainBlock) {
        super(type, mainBlock);
    }

    @Override
    protected void loadFromNBTExtra(NBTTagCompound nbt) {
        requireFlatSurface = !nbt.hasKey(REQUIRE_FLAT_SURFACE_KEY) || nbt.getBoolean(REQUIRE_FLAT_SURFACE_KEY);
        magnetEnabled = !nbt.hasKey(MAGNET_ENABLED_KEY) || nbt.getBoolean(MAGNET_ENABLED_KEY);
    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {
        nbt.setBoolean(REQUIRE_FLAT_SURFACE_KEY, requireFlatSurface);
        nbt.setBoolean(MAGNET_ENABLED_KEY, magnetEnabled);
    }

    public static boolean requiresFlatSurface(LittlePreviews previews) {
        if (previews == null || !STRUCTURE_ID.equals(previews.getStructureId()))
            return false;
        return !previews.structureNBT.hasKey(REQUIRE_FLAT_SURFACE_KEY) || previews.structureNBT.getBoolean(REQUIRE_FLAT_SURFACE_KEY);
    }

    public static boolean isMagnetEnabled(LittlePreviews previews) {
        if (previews == null || !STRUCTURE_ID.equals(previews.getStructureId()))
            return false;
        return !previews.structureNBT.hasKey(MAGNET_ENABLED_KEY) || previews.structureNBT.getBoolean(MAGNET_ENABLED_KEY);
    }

    public static class StructureLittleVecXFurnitureType extends LittleStructureType {

        public StructureLittleVecXFurnitureType() {
            super(STRUCTURE_ID, LittleVecXMod.MODID, StructureLittleVecXFurniture.class, 0);
        }

        @Override
        public void flip(LittlePreviews previews, com.creativemd.littletiles.common.util.grid.LittleGridContext context, Axis axis,
                com.creativemd.littletiles.common.tile.math.vec.LittleVec center) {
            // Furniture recipes are intentionally not mirrored.
        }
    }

    public static class StructureLittleVecXFurnitureParser extends LittleStructureGuiParser {

        public StructureLittleVecXFurnitureParser(GuiParent parent, AnimationGuiHandler handler) {
            super(parent, handler);
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected void createControls(LittlePreviews previews, LittleStructure structure) {
            boolean requireFlatSurface = true;
            boolean magnetEnabled = true;
            if (structure instanceof StructureLittleVecXFurniture) {
                requireFlatSurface = ((StructureLittleVecXFurniture) structure).requireFlatSurface;
                magnetEnabled = ((StructureLittleVecXFurniture) structure).magnetEnabled;
            }

            parent.addControl(new GuiCheckBox("requireFlatSurface", CoreControl.translate("gui.littlevecx.furniture_require_flat_surface"), 0, 0, requireFlatSurface));
            parent.addControl(new GuiCheckBox("magnetEnabled", CoreControl.translate("gui.littlevecx.furniture_magnet"), 0, 20, magnetEnabled));
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructure parseStructure(LittlePreviews previews) {
            StructureLittleVecXFurniture furniture = createStructure(StructureLittleVecXFurniture.class, null);
            furniture.requireFlatSurface = ((GuiCheckBox) parent.get("requireFlatSurface")).value;
            furniture.magnetEnabled = ((GuiCheckBox) parent.get("magnetEnabled")).value;
            return furniture;
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructureType getStructureType() {
            return LittleStructureRegistry.getStructureType(StructureLittleVecXFurniture.class);
        }
    }
}
