package com.integral.littlevecx.item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.utils.type.Pair;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException;
import com.creativemd.littletiles.common.structure.exception.NotYetConnectedException;
import com.creativemd.littletiles.common.structure.signal.output.InternalSignalOutput;
import com.creativemd.littletiles.common.structure.type.door.LittleDoor;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.parent.IParentTileList;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.util.vec.LittleRayTraceResult;
import com.creativemd.littletiles.common.world.WorldAnimationHandler;
import com.integral.littlevecx.LittleVecXMod;
import com.integral.littlevecx.animation.StructureLittleVecXElevator;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemLittleVecXDebugBlazeRod extends Item {

    public ItemLittleVecXDebugBlazeRod() {
        setRegistryName(LittleVecXMod.MODID, "debug_blaze_rod");
        setTranslationKey(LittleVecXMod.MODID + ".debug_blaze_rod");
        setCreativeTab(LittleTiles.littleTab);
        setMaxStackSize(1);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
            float hitX, float hitY, float hitZ) {
        if (world.isRemote)
            return EnumActionResult.SUCCESS;
        return activateFocusedStructure(player, world, pos) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (!world.isRemote) {
            TileEntity tileEntity = getLookedLittleTileEntity(player, world);
            boolean activated = false;
            if (tileEntity instanceof TileEntityLittleTiles)
                activated = activateFocusedStructure(player, world, tileEntity.getPos());
            if (!activated)
                activateStructure(player, findLookedAnimationStructure(player, world));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("tooltip.littlevecx.activator.structure"));
        tooltip.add(I18n.format("tooltip.littlevecx.activator.elevator_up"));
        tooltip.add(I18n.format("tooltip.littlevecx.activator.elevator_down"));
    }

    private boolean activateFocusedStructure(EntityPlayer player, World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileEntityLittleTiles))
            return activateStructure(player, findLookedAnimationStructure(player, world));

        LittleStructure structure = getFocusedStructure(player, (TileEntityLittleTiles) tileEntity);
        return activateStructure(player, structure);
    }

    private boolean activateStructure(EntityPlayer player, LittleStructure structure) {
        if (structure == null)
            return true;

        try {
            if (structure instanceof StructureLittleVecXElevator) {
                StructureLittleVecXElevator elevator = (StructureLittleVecXElevator) structure;
                boolean activated = player.isSneaking() ? elevator.activateDownFromActivator() : elevator.activateUpFromActivator();
                if (activated)
                    return true;
            }

            if (structure instanceof LittleDoor) {
                InternalSignalOutput stateOutput = getStateOutput(structure);
                if (stateOutput != null) {
                    stateOutput.toggle();
                    structure.notifyChange();
                    return true;
                }

                // Compatibility fallback for doors which do not expose a toggleable state output.
                ((LittleDoor) structure).activate(LittleDoor.DoorActivator.SIGNAL, player, null);
                return true;
            }

            InternalSignalOutput output = structure.getOutput(0);
            if (output != null) {
                output.toggle();
                structure.notifyChange();
                return true;
            }
        } catch (LittleActionException e) {
            return true;
        }

        return true;
    }

    private InternalSignalOutput getStateOutput(LittleStructure structure) {
        InternalSignalOutput output = structure.getOutput(0);
        return output != null && "state".equals(output.component.identifier) ? output : null;
    }

    private LittleStructure getFocusedStructure(EntityPlayer player, TileEntityLittleTiles tileEntity) {
        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        double reach = player.capabilities.isCreativeMode ? 5.0D : 4.5D;
        Vec3d end = new Vec3d(start.x + look.x * reach, start.y + look.y * reach, start.z + look.z * reach);
        return getFocusedStructure(tileEntity, start, end);
    }

    private LittleStructure getFocusedStructure(TileEntityLittleTiles tileEntity, Vec3d start, Vec3d end) {
        Pair<IParentTileList, LittleTile> focused = tileEntity.getFocusedTile(start, end);
        LittleStructure structure = getStructure(focused != null ? focused.key : null);
        if (structure != null)
            return structure;

        for (Pair<IParentTileList, LittleTile> pair : tileEntity.allTiles()) {
            structure = getStructure(pair != null ? pair.key : null);
            if (structure != null)
                return structure;
        }
        return null;
    }

    private LittleStructure getStructure(IParentTileList parent) {
        if (parent == null || !parent.isStructure())
            return null;

        try {
            return parent.getStructure();
        } catch (CorruptedConnectionException | NotYetConnectedException e) {
            return null;
        }
    }

    private LittleStructure findLookedAnimationStructure(EntityPlayer player, World world) {
        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        double reach = player.capabilities.isCreativeMode ? 5.0D : 4.5D;
        Vec3d end = new Vec3d(start.x + look.x * reach, start.y + look.y * reach, start.z + look.z * reach);

        EntityAnimation best = null;
        LittleRayTraceResult bestHit = null;
        double bestDistance = Double.MAX_VALUE;
        Set<UUID> seen = new HashSet<>();

        for (EntityAnimation animation : WorldAnimationHandler.getHandler(world).openDoors) {
            EntityAnimation root = getRootAnimation(animation);
            if (root == null || root.isDead || !seen.add(root.getUniqueID()))
                continue;

            AxisAlignedBB box = root.getEntityBoundingBox();
            if (box == null)
                continue;

            RayTraceResult intercept = box.grow(0.05D).calculateIntercept(start, end);
            if (intercept == null)
                continue;

            LittleRayTraceResult preciseHit = root.getRayTraceResult(start, end);
            if (preciseHit == null)
                continue;

            double distance = start.distanceTo(intercept.hitVec);
            if (best == null || distance < bestDistance) {
                best = root;
                bestHit = preciseHit;
                bestDistance = distance;
            }
        }

        if (best == null)
            return null;

        LittleStructure focused = getFocusedAnimationStructure(bestHit, start, end);
        if (focused != null)
            return focused;
        return best.structure;
    }

    private LittleStructure getFocusedAnimationStructure(LittleRayTraceResult hit, Vec3d start, Vec3d end) {
        if (hit == null || hit.world == null || hit.getBlockPos() == null)
            return null;

        TileEntity tileEntity = hit.world.getTileEntity(hit.getBlockPos());
        if (!(tileEntity instanceof TileEntityLittleTiles))
            return null;

        Vec3d transformedStart = hit.world.getOrigin().transformPointToFakeWorld(start);
        Vec3d transformedEnd = hit.world.getOrigin().transformPointToFakeWorld(end);
        return getFocusedStructure((TileEntityLittleTiles) tileEntity, transformedStart, transformedEnd);
    }

    private EntityAnimation getRootAnimation(EntityAnimation animation) {
        if (animation == null)
            return null;

        try {
            Entity root = animation.getAbsoluteParent();
            return root instanceof EntityAnimation ? (EntityAnimation) root : animation;
        } catch (Throwable ignored) {
            return animation;
        }
    }

    private TileEntity getLookedLittleTileEntity(EntityPlayer player, World world) {
        RayTraceResult result = rayTrace(world, player, false);
        if (result == null || result.getBlockPos() == null)
            return null;
        return world.getTileEntity(result.getBlockPos());
    }
}
