package com.integral.littlevecx.mutator;

import java.util.ArrayList;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.littletiles.common.action.block.LittleActionActivated;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.animation.AnimationGuiHandler;
import com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException;
import com.creativemd.littletiles.common.structure.exception.NotYetConnectedException;
import com.creativemd.littletiles.common.structure.registry.LittleStructureGuiParser;
import com.creativemd.littletiles.common.structure.registry.LittleStructureRegistry;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.parent.IStructureTileList;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.integral.littlevecx.LittleVecXConfig;
import com.integral.littlevecx.LittleVecXAnimationSyncHelper;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class StructureLittleVecXWallpaperMutator extends LittleStructure {

    public StructureLittleVecXWallpaperMutator(LittleStructureType type, IStructureTileList mainBlock) {
        super(type, mainBlock);
    }

    @Override
    protected void loadFromNBTExtra(net.minecraft.nbt.NBTTagCompound nbt) {}

    @Override
    protected void writeToNBTExtra(net.minecraft.nbt.NBTTagCompound nbt) {}

    @Override
    public boolean onBlockActivated(World worldIn, LittleTile tile, BlockPos pos, EntityPlayer playerIn, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side,
            float hitX, float hitY, float hitZ, LittleActionActivated action) {
        if (heldItem == null || heldItem.isEmpty() || !(heldItem.getItem() instanceof ItemBlock))
            return false;

        ItemBlock itemBlock = (ItemBlock) heldItem.getItem();
        Block block = itemBlock.getBlock();
        if (block == null)
            return false;
        if (!isWallpaperBlock(block))
            return false;

        IBlockState state = block.getStateFromMeta(heldItem.getMetadata());
        int meta = block.getMetaFromState(state);

        if (worldIn.isRemote)
            return true;

        boolean changed = applyWallpaper(block, meta);
        if (changed && !playerIn.capabilities.isCreativeMode)
            heldItem.shrink(1);
        return true;
    }

    private boolean applyWallpaper(Block block, int meta) {
        boolean changedAny = false;
        ArrayList<TileEntityLittleTiles> changedTiles = new ArrayList<>();
        try {
            for (Entry<BlockPos, ArrayList<IStructureTileList>> entry : collectAllBlocksListSameWorld().entrySet()) {
                if (entry.getValue().isEmpty())
                    continue;

                TileEntityLittleTiles te = entry.getValue().get(0).getTe();
                if (te == null)
                    continue;

                boolean changedTe = false;
                for (IStructureTileList list : entry.getValue()) {
                    for (LittleTile littleTile : list) {
                        if (littleTile.getBlock() == block && littleTile.getMeta() == meta)
                            continue;

                        littleTile.setBlock(block, meta);
                        changedTe = true;
                        changedAny = true;
                    }
                }

                if (changedTe) {
                    te.updateTiles();
                    changedTiles.add(te);
                }
            }
        } catch (CorruptedConnectionException | NotYetConnectedException e) {
            return false;
        }
        if (changedAny)
            LittleVecXAnimationSyncHelper.syncChangedTiles(this, changedTiles);
        return changedAny;
    }

    private static boolean isWallpaperBlock(Block block) {
        if (block == null)
            return false;

        ResourceLocation registryName = block.getRegistryName();
        if (registryName == null)
            return false;
        if (LittleVecXConfig.wallpaperUseBlacklistMode)
            return !LittleVecXConfig.isWallpaperBlockBlocked(registryName);
        return LittleVecXConfig.isWallpaperBlockAllowed(registryName) || LittleVecXConfig.isWallpaperModAllowed(registryName);
    }

    public static class StructureLittleVecXWallpaperMutatorParser extends LittleStructureGuiParser {

        public StructureLittleVecXWallpaperMutatorParser(GuiParent parent, AnimationGuiHandler handler) {
            super(parent, handler);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void createControls(LittlePreviews previews, LittleStructure structure) {}

        @Override
        @SideOnly(Side.CLIENT)
        public LittleStructure parseStructure(LittlePreviews previews) {
            return createStructure(StructureLittleVecXWallpaperMutator.class, null);
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructureType getStructureType() {
            return LittleStructureRegistry.getStructureType(StructureLittleVecXWallpaperMutator.class);
        }
    }
}
