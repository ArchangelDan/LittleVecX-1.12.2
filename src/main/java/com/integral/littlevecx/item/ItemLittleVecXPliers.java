package com.integral.littlevecx.item;

import java.util.List;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.utils.math.Rotation;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.LittleTilesClient;
import com.creativemd.littletiles.client.gui.configure.SubGuiConfigure;
import com.creativemd.littletiles.common.api.ILittleTool;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.place.PlacementPosition;
import com.integral.littlevecx.LittleVecXMod;
import com.integral.littlevecx.client.LittleVecXMoveClientHandler;
import com.integral.littlevecx.client.gui.SubGuiLittleVecXStructureSettingsMenu;
import com.integral.littlevecx.client.gui.SubGuiLittleVecXPliers;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemLittleVecXPliers extends Item implements ILittleTool {

    public ItemLittleVecXPliers() {
        setRegistryName(LittleVecXMod.MODID, "pliers");
        setTranslationKey(LittleVecXMod.MODID + ".pliers");
        setCreativeTab(LittleTiles.littleTab);
        setMaxStackSize(1);
    }

    @Override
    public void rotate(EntityPlayer player, ItemStack stack, Rotation rotation, boolean client) {
        // This tool uses LT move keys for pixel offsets, not for transform editing.
    }

    @Override
    public void flip(EntityPlayer player, ItemStack stack, Axis axis, boolean client) {
        // No flip mode for the first movement MVP.
    }

    @Override
    public boolean sendTransformationUpdate() {
        return false;
    }

    @Override
    public LittleGridContext getPositionContext(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null)
            return LittleGridContext.get();
        try {
            return LittleGridContext.get(nbt);
        } catch (RuntimeException e) {
            return LittleGridContext.get();
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public SubGuiConfigure getConfigureGUI(EntityPlayer player, ItemStack stack) {
        return new SubGuiLittleVecXStructureSettingsMenu(stack, LittleVecXMoveClientHandler.getSelectionSnapshot());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public SubGuiConfigure getConfigureGUIAdvanced(EntityPlayer player, ItemStack stack) {
        return new SubGuiLittleVecXPliers(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onClickAir(EntityPlayer player, ItemStack stack) {
        LittleVecXMoveClientHandler.cancelCurrentSession();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean onClickBlock(World world, EntityPlayer player, ItemStack stack, PlacementPosition position, RayTraceResult result) {
        LittleVecXMoveClientHandler.cancelCurrentSession();
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onDeselect(World world, ItemStack stack, EntityPlayer player) {
        LittleVecXMoveClientHandler.cancelCurrentSession();
    }

    @Override
    public boolean canDestroyBlockInCreative(World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
        return false;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, IBlockState state) {
        return 0F;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(I18n.format("tooltip.littlevecx.pliers.select_rotated_structure"));
        tooltip.add(I18n.format("tooltip.littlevecx.pliers.recipe_rotated_structure"));
        tooltip.add(I18n.format("tooltip.littlevecx.pliers.recipe_object"));
        tooltip.add(I18n.format("tooltip.littlevecx.pliers.move_mode", getKeyName(LittleTilesClient.mark, "M")));
        tooltip.add(I18n.format("tooltip.littlevecx.pliers.selected_settings", getKeyName(LittleTilesClient.configure, "C")));
        tooltip.add(I18n.format("tooltip.littlevecx.pliers.move_pixels"));
        tooltip.add(I18n.format("tooltip.littlevecx.pliers.move_vertical"));
        tooltip.add(I18n.format("tooltip.littlevecx.pliers.move_grid", getKeyName(LittleTilesClient.configureAdvanced, "Ctrl+C")));
        tooltip.add(I18n.format("tooltip.littlevecx.pliers.clear_selection"));
    }

    @SideOnly(Side.CLIENT)
    private static String getKeyName(@Nullable KeyBinding keyBinding, String fallback) {
        if (keyBinding == null)
            return fallback;
        String name = keyBinding.getDisplayName();
        return name != null && !name.isEmpty() ? name : fallback;
    }
}
