package com.integral.littlevecx;

import java.util.List;

import com.creativemd.creativecore.common.utils.math.box.OrientatedBoundingBox;
import com.creativemd.creativecore.common.utils.math.vec.ChildVecOrigin;
import com.creativemd.creativecore.common.utils.math.vec.IVecOrigin;
import com.creativemd.creativecore.common.utils.type.Pair;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.entity.DoorController;
import com.creativemd.littletiles.common.structure.type.door.LittleDoorBase;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.parent.IParentTileList;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.world.WorldAnimationHandler;
import com.integral.littlevecx.LittleVecXMod;

import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.world.GetCollisionBoxesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;

@Mod.EventBusSubscriber(modid = LittleVecXMod.MODID)
public final class LittleVecXNestedAnimationCollisionHandler {

    private LittleVecXNestedAnimationCollisionHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCollision(GetCollisionBoxesEvent event) {
        if (event.getWorld() == null)
            return;

        List<AxisAlignedBB> collisions = event.getCollisionBoxesList();
        if (collisions == null)
            return;

        for (EntityAnimation animation : WorldAnimationHandler.getHandler(event.getWorld()).openDoors) {
            if (animation == null || animation.isDead)
                continue;

            Entity rootEntity;
            try {
                rootEntity = animation.getAbsoluteParent();
            } catch (Throwable ignored) {
                // Some nested LT animations briefly exist without a fully wired SubWorld parent chain.
                // We skip collision injection for that tick instead of crashing the player tick loop.
                continue;
            }
            if (!(rootEntity instanceof EntityAnimation))
                continue;

            EntityAnimation root = (EntityAnimation) rootEntity;
            if (!(root.controller instanceof LittleVecXStaticRotationController))
                continue;
            if (root == animation)
                continue;

            try {
                appendAnimationCollision(animation, event.getAabb(), collisions);
            } catch (Throwable ignored) {
                // Nested LT animations can exist in a half-initialized state for a tick while the parent
                // rotated structure is being placed. We skip collision injection until their ChildVecOrigin
                // chain is fully connected instead of crashing the whole integrated server.
            }
        }
    }

    private static void appendAnimationCollision(EntityAnimation animation, AxisAlignedBB query, List<AxisAlignedBB> collisions) {
        if (animation == null || animation.isDead)
            return;
        syncDoorNoClipState(animation);
        if (animation.controller == null || animation.controller.noClip() || animation.noCollision)
            return;
        if (animation.origin == null || animation.fakeWorld == null || animation.fakeWorld.loadedTileEntityList == null)
            return;
        if (!isOriginChainReady(animation.origin))
            return;
        if (animation.getEntityBoundingBox() == null || !animation.getEntityBoundingBox().intersects(query))
            return;
        if (appendPreciseDoorCollisionIfNeeded(animation, query, collisions))
            return;

        for (TileEntity tileEntity : animation.fakeWorld.loadedTileEntityList) {
            if (!(tileEntity instanceof TileEntityLittleTiles))
                continue;

            TileEntityLittleTiles te = (TileEntityLittleTiles) tileEntity;
            for (Pair<IParentTileList, LittleTile> pair : te.allTiles()) {
                LittleBox littleBox = pair.value.getCollisionBox();
                if (littleBox == null)
                    continue;

                AxisAlignedBB localBox = littleBox.getBox(te.getContext(), te.getPos());
                AxisAlignedBB worldBox = animation.origin.getAxisAlignedBox(localBox);
                if (worldBox != null && worldBox.intersects(query))
                    collisions.add(worldBox);
            }
        }
    }

    private static void syncDoorNoClipState(EntityAnimation animation) {
        if (!(animation.controller instanceof DoorController))
            return;

        DoorController controller = (DoorController) animation.controller;
        controller.noClip = isDoorConfiguredNoClip(animation) || isDoorOpeningOrOpened(controller);
    }

    private static boolean isDoorConfiguredNoClip(EntityAnimation animation) {
        return animation.structure instanceof LittleDoorBase && ((LittleDoorBase) animation.structure).noClip;
    }

    private static boolean appendPreciseDoorCollisionIfNeeded(EntityAnimation animation, AxisAlignedBB query, List<AxisAlignedBB> collisions) {
        if (!(animation.controller instanceof DoorController))
            return false;

        DoorController controller = (DoorController) animation.controller;
        boolean opened = isDoorOpeningOrOpened(controller);
        boolean opening = controller.isChanging() && controller.getAimedState() != null && DoorController.openedState.equals(controller.getAimedState().name);

        if (!opened && !opening)
            return false;

        // Open nested doors inside rotated structures should be fully passable.
        // A thin residual collision feels more "physical", but in practice it still snags the player
        // because the rotated parent already makes movement through the opening less forgiving.
        return true;
    }

    private static boolean isDoorOpeningOrOpened(DoorController controller) {
        if (controller == null)
            return false;
        if (controller.getCurrentState() != null && DoorController.openedState.equals(controller.getCurrentState().name))
            return true;
        return controller.isChanging() && controller.getAimedState() != null && DoorController.openedState.equals(controller.getAimedState().name);
    }

    private static boolean isOriginChainReady(IVecOrigin origin) {
        int depth = 0;
        IVecOrigin current = origin;
        while (current != null && depth++ < 8) {
            if (current.center() == null || current.rotation() == null || current.rotationInv() == null || current.translation() == null)
                return false;
            if (current instanceof ChildVecOrigin && ((ChildVecOrigin) current).parent == null)
                return false;
            current = current.getParent();
        }
        return depth < 8;
    }
}
