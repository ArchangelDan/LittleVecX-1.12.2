package com.integral.littlevecx.action;

import java.util.List;

import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.action.block.LittleActionDestroyBoxes;
import com.creativemd.littletiles.common.tile.math.box.LittleAbsoluteBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxes;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LittleVecXIndustrialDestroyBoxes extends LittleActionDestroyBoxes {

    public LittleVecXIndustrialDestroyBoxes(LittleBoxes boxes) {
        super(boxes);
    }

    public LittleVecXIndustrialDestroyBoxes() {}

    @Override
    public void action(World world, EntityPlayer player, BlockPos pos, IBlockState state, List<LittleBox> boxes, LittleGridContext context) throws LittleActionException {
        fireBlockBreakEvent(world, pos, player);

        TileEntity tileEntity = loadTe(player, world, pos, null, true, 0);
        if (!(tileEntity instanceof TileEntityLittleTiles))
            return;

        TileEntityLittleTiles te = (TileEntityLittleTiles) tileEntity;
        if (context != te.getContext()) {
            if (context.size < te.getContext().size) {
                for (LittleBox box : boxes)
                    box.convertTo(context, te.getContext());
                context = te.getContext();
            } else
                te.convertTo(context);
        }

        action(player, te, boxes, false, context);
        te.combineTiles();

        if (!doneSomething)
            te.convertBlockToVanilla();
    }

    @Override
    public boolean canBeReverted() {
        return false;
    }

    @Override
    public LittleAction revert(EntityPlayer player) {
        return null;
    }

    @Override
    public LittleAction flip(Axis axis, LittleAbsoluteBox box) {
        return assignFlip(new LittleVecXIndustrialDestroyBoxes(), axis, box);
    }
}
