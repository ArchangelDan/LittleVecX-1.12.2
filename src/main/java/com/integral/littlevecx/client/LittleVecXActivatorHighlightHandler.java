package com.integral.littlevecx.client;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.creativemd.creativecore.common.utils.type.Pair;
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
import com.integral.littlevecx.item.ItemLittleVecXDebugBlazeRod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Shows the exact target selected by the Activator before it is used.
 *
 * The resolver intentionally follows ItemLittleVecXDebugBlazeRod's current
 * server-side lookup order. This makes the overlay a diagnostic aid as well as
 * a user-facing preview for structures that live in an animation subworld.
 */
@SideOnly(Side.CLIENT)
public final class LittleVecXActivatorHighlightHandler {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static boolean registered;

    private LittleVecXActivatorHighlightHandler() {
    }

    public static void register() {
        if (registered)
            return;
        MinecraftForge.EVENT_BUS.register(new LittleVecXActivatorHighlightHandler());
        registered = true;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        EntityPlayer player = MC.player;
        if (player == null || MC.world == null || MC.gameSettings.hideGUI)
            return;

        ItemStack stack = player.getHeldItemMainhand();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemLittleVecXDebugBlazeRod))
            return;

        Target target = findTarget(player);
        if (target == null || !isActivatable(target.structure))
            return;

        AxisAlignedBB box = target.animation != null ? target.animation.getEntityBoundingBox() : getStructureBox(target.structure);
        if (box == null)
            return;

        renderOutline(player, box.grow(0.01D), event.getPartialTicks());
    }

    private static Target findTarget(EntityPlayer player) {
        RayTraceResult mouseOver = MC.objectMouseOver;
        if (mouseOver != null && mouseOver.getBlockPos() != null) {
            TileEntity tileEntity = player.world.getTileEntity(mouseOver.getBlockPos());
            if (tileEntity instanceof TileEntityLittleTiles) {
                LittleStructure structure = getFocusedStructure(player, (TileEntityLittleTiles) tileEntity);
                if (structure != null)
                    return new Target(null, structure);
            }
        }

        return findLookedAnimationTarget(player);
    }

    private static Target findLookedAnimationTarget(EntityPlayer player) {
        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        double reach = player.capabilities.isCreativeMode ? 5.0D : 4.5D;
        Vec3d end = new Vec3d(start.x + look.x * reach, start.y + look.y * reach, start.z + look.z * reach);

        EntityAnimation best = null;
        LittleRayTraceResult bestHit = null;
        double bestDistance = Double.MAX_VALUE;
        Set<UUID> seen = new HashSet<>();

        for (EntityAnimation animation : WorldAnimationHandler.getHandlerClient().openDoors) {
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

        if (best == null || best.structure == null)
            return null;

        LittleStructure focused = getFocusedAnimationStructure(player, bestHit);
        return new Target(best, focused != null ? focused : best.structure);
    }

    private static LittleStructure getFocusedAnimationStructure(EntityPlayer player, LittleRayTraceResult hit) {
        if (hit == null || hit.world == null || hit.getBlockPos() == null)
            return null;
        TileEntity tileEntity = hit.world.getTileEntity(hit.getBlockPos());
        return tileEntity instanceof TileEntityLittleTiles ? getFocusedStructure(player, (TileEntityLittleTiles) tileEntity) : null;
    }

    private static EntityAnimation getRootAnimation(EntityAnimation animation) {
        if (animation == null)
            return null;
        try {
            Entity root = animation.getAbsoluteParent();
            return root instanceof EntityAnimation ? (EntityAnimation) root : animation;
        } catch (Throwable ignored) {
            return animation;
        }
    }

    private static LittleStructure getFocusedStructure(EntityPlayer player, TileEntityLittleTiles tileEntity) {
        Pair<IParentTileList, LittleTile> focused = tileEntity.getFocusedTile(player, 1.0F);
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

    private static LittleStructure getStructure(IParentTileList parent) {
        if (parent == null || !parent.isStructure())
            return null;
        try {
            return parent.getStructure();
        } catch (CorruptedConnectionException | NotYetConnectedException ignored) {
            return null;
        }
    }

    private static boolean isActivatable(LittleStructure structure) {
        if (structure instanceof LittleDoor)
            return true;
        InternalSignalOutput output = structure.getOutput(0);
        return output != null;
    }

    private static AxisAlignedBB getStructureBox(LittleStructure structure) {
        try {
            return structure.getSurroundingBox().getAABB();
        } catch (CorruptedConnectionException | NotYetConnectedException | RuntimeException ignored) {
            return null;
        }
    }

    private static void renderOutline(EntityPlayer player, AxisAlignedBB worldBox, float partialTicks) {
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        AxisAlignedBB box = worldBox.offset(-x, -y, -z);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        GlStateManager.glLineWidth(4.0F);
        RenderGlobal.drawSelectionBoundingBox(box, 0.0F, 0.0F, 0.0F, 0.82F);

        GlStateManager.disableDepth();
        GlStateManager.glLineWidth(2.0F);
        RenderGlobal.drawSelectionBoundingBox(box, 0.15F, 0.85F, 1.0F, 1.0F);
        GlStateManager.enableDepth();

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static final class Target {

        private final EntityAnimation animation;
        private final LittleStructure structure;

        private Target(EntityAnimation animation, LittleStructure structure) {
            this.animation = animation;
            this.structure = structure;
        }
    }
}
