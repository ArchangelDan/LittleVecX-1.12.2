package com.integral.littlevecx;

import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.world.WorldAnimationHandler;
import com.integral.littlevecx.animation.StructureLittleVecXElevator;

import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;

@Mod.EventBusSubscriber(modid = LittleVecXMod.MODID)
public final class LittleVecXElevatorPassengerStabilizer {

    // LittleTiles already carries entities inside EntityAnimation through its CollisionCoordinator.
    // Keep the old compensator in source as a rollback reference, but do not run a second movement pass.
    private static final boolean ENABLED = false;

    private static final double DESCENDING_EPSILON = -0.00001D;
    private static final double HORIZONTAL_MARGIN = 0.08D;
    private static final double VERTICAL_MARGIN = 0.20D;
    private static final double CEILING_KICK_MARGIN = 0.35D;
    private static final double MIN_DESCENT_GRAVITY = -0.02D;
    private static final double CEILING_ESCAPE_GRAVITY = -0.06D;
    private static final double MAX_EXTRA_FALL_SPEED = 0.08D;
    private static final double MAX_CARRY_CORRECTION = 0.20D;

    private LittleVecXElevatorPassengerStabilizer() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (!ENABLED)
            return;
        if (event.phase != Phase.END || event.player == null || event.player.world == null)
            return;

        EntityPlayer player = event.player;
        EntityAnimation elevator = findDescendingElevatorCarrying(player);
        if (elevator == null)
            return;

        boolean nearCeiling = isNearElevatorCeiling(player.getEntityBoundingBox(), elevator.getEntityBoundingBox());
        double elevatorDeltaY = getElevatorDeltaY(elevator);
        double desiredMotionY = Math.min(elevatorDeltaY, MIN_DESCENT_GRAVITY);

        // If the player jitters into the cabin ceiling during the first descending ticks, LT can resolve
        // the overlap as a strong upward push. Give the player a small downward bias instead of pinning
        // him in place, otherwise the descending roof can keep pressing him.
        if (nearCeiling)
            desiredMotionY = Math.min(desiredMotionY - 0.03D, CEILING_ESCAPE_GRAVITY);

        if (player.motionY > desiredMotionY)
            player.motionY = desiredMotionY;
        else if (player.motionY < desiredMotionY - MAX_EXTRA_FALL_SPEED)
            player.motionY = desiredMotionY - MAX_EXTRA_FALL_SPEED;

        player.fallDistance = 0F;
        player.onGround = !nearCeiling;
        player.collidedVertically = !nearCeiling;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWorldTick(WorldTickEvent event) {
        if (!ENABLED)
            return;
        if (event.phase != Phase.END || event.world == null)
            return;

        for (EntityPlayer player : event.world.playerEntities) {
            if (player == null)
                continue;

            EntityAnimation elevator = findDescendingElevatorCarrying(player);
            if (elevator != null)
                carryPassengerDownWithElevator(player, elevator);
        }
    }

    private static void carryPassengerDownWithElevator(EntityPlayer player, EntityAnimation elevator) {
        double elevatorDeltaY = getElevatorDeltaY(elevator);
        if (elevatorDeltaY >= DESCENDING_EPSILON)
            return;

        double playerDeltaY = player.posY - player.prevPosY;
        double missingDeltaY = elevatorDeltaY - playerDeltaY;
        if (missingDeltaY >= DESCENDING_EPSILON)
            return;

        double correction = Math.max(missingDeltaY, -MAX_CARRY_CORRECTION);
        player.move(MoverType.SELF, 0D, correction, 0D);
        player.fallDistance = 0F;
    }

    private static EntityAnimation findDescendingElevatorCarrying(EntityPlayer player) {
        AxisAlignedBB playerBox = player.getEntityBoundingBox();
        if (playerBox == null)
            return null;

        for (EntityAnimation animation : WorldAnimationHandler.getHandler(player.world).openDoors) {
            if (!isDescendingLittleVecXElevator(animation))
                continue;

            AxisAlignedBB animationBox = animation.getEntityBoundingBox();
            if (animationBox != null && isPassengerInsideElevator(playerBox, animationBox))
                return animation;
        }
        return null;
    }

    private static boolean isDescendingLittleVecXElevator(EntityAnimation animation) {
        if (animation == null || animation.isDead || animation.origin == null || animation.controller == null)
            return false;
        if (!(animation.structure instanceof StructureLittleVecXElevator))
            return false;
        if (!animation.controller.isChanging())
            return false;

        return getElevatorDeltaY(animation) < DESCENDING_EPSILON;
    }

    private static boolean isPassengerInsideElevator(AxisAlignedBB playerBox, AxisAlignedBB animationBox) {
        double centerX = (playerBox.minX + playerBox.maxX) * 0.5D;
        double centerZ = (playerBox.minZ + playerBox.maxZ) * 0.5D;
        if (centerX < animationBox.minX - HORIZONTAL_MARGIN || centerX > animationBox.maxX + HORIZONTAL_MARGIN)
            return false;
        if (centerZ < animationBox.minZ - HORIZONTAL_MARGIN || centerZ > animationBox.maxZ + HORIZONTAL_MARGIN)
            return false;

        return playerBox.maxY > animationBox.minY - VERTICAL_MARGIN && playerBox.minY < animationBox.maxY + VERTICAL_MARGIN;
    }

    private static boolean isNearElevatorCeiling(AxisAlignedBB playerBox, AxisAlignedBB animationBox) {
        if (playerBox == null || animationBox == null)
            return false;

        double roofDistance = animationBox.maxY - playerBox.maxY;
        return roofDistance >= -0.05D && roofDistance <= CEILING_KICK_MARGIN;
    }

    private static double getElevatorDeltaY(EntityAnimation animation) {
        double deltaY = animation.origin.offY() - animation.origin.offYLast();
        if (deltaY >= DESCENDING_EPSILON)
            deltaY = animation.posY - animation.prevPosY;
        return deltaY;
    }
}
