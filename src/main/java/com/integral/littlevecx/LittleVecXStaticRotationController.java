package com.integral.littlevecx;

import java.util.List;

import javax.vecmath.Vector3d;

import com.creativemd.creativecore.common.utils.math.box.OrientatedBoundingBox;
import com.creativemd.littletiles.common.entity.EntityAnimationController;
import com.creativemd.littletiles.common.entity.INoPushEntity;
import com.creativemd.littletiles.common.structure.animation.AnimationKey;
import com.creativemd.littletiles.common.structure.animation.AnimationState;
import com.creativemd.littletiles.common.util.vec.LittleTransformation;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.world.WorldAnimationHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

public class LittleVecXStaticRotationController extends EntityAnimationController {

    public static final String CONTROLLER_ID = "static_rotation";

    private static final String STATE_KEY = "static";

    private double rotX;
    private double rotY;
    private double rotZ;

    private double offX;
    private double offY;
    private double offZ;

    public boolean noClip = false;

    public LittleVecXStaticRotationController() {
        addStateAndSelect(STATE_KEY, new AnimationState());
    }

    public LittleVecXStaticRotationController(double rotYDegrees) {
        this(0, rotYDegrees, 0, 0, 0, 0);
    }

    public LittleVecXStaticRotationController(double rotXDegrees, double rotYDegrees, double rotZDegrees) {
        this(rotXDegrees, rotYDegrees, rotZDegrees, 0, 0, 0);
    }

    public LittleVecXStaticRotationController(double rotXDegrees, double rotYDegrees, double rotZDegrees, double offX, double offY, double offZ) {
        this();
        setTransform(rotXDegrees, rotYDegrees, rotZDegrees, offX, offY, offZ);
    }

    public void setTransform(double rotXDegrees, double rotYDegrees, double rotZDegrees, double offX, double offY, double offZ) {
        this.rotX = rotXDegrees;
        this.rotY = rotYDegrees;
        this.rotZ = rotZDegrees;

        this.offX = offX;
        this.offY = offY;
        this.offZ = offZ;

        if (getCurrentState() != null) {
            getCurrentState().state
                    .set(AnimationKey.rotX, rotX)
                    .set(AnimationKey.rotY, rotY)
                    .set(AnimationKey.rotZ, rotZ)
                    .set(AnimationKey.offX, offX)
                    .set(AnimationKey.offY, offY)
                    .set(AnimationKey.offZ, offZ);
        }
    }

    public double getRotX() {
        return rotX;
    }

    public double getRotY() {
        return rotY;
    }

    public double getRotZ() {
        return rotZ;
    }

    public double getOffX() {
        return offX;
    }

    public double getOffY() {
        return offY;
    }

    public double getOffZ() {
        return offZ;
    }

    @Override
    public boolean noClip() {
        return noClip;
    }

    @Override
    public AnimationState tick() {
        AnimationState state = super.tick();

        // LittleTiles animations (doors, etc.) that exist inside this animation live in our fakeWorld. When those
        // inner animations are idle (no movement/rotation this tick), LT does not push entities out, so collision
        // appears to "disappear". We add a lightweight server-side push-out step for inner EntityAnimations only.
        tryApplyStaticInnerAnimationCollision();

        return state;
    }

    private void tryApplyStaticInnerAnimationCollision() {
        if (parent == null || parent.world == null)
            return;
        if (parent.world.isRemote)
            return;
        if (noClip)
            return;
        if (!LittleTiles.CONFIG.general.enableAnimationCollision)
            return;
        if (parent.fakeWorld == null || parent.fakeWorld.loadedEntityList == null || parent.fakeWorld.loadedEntityList.isEmpty())
            return;

        for (int i = 0; i < parent.fakeWorld.loadedEntityList.size(); i++) {
            Entity entity = parent.fakeWorld.loadedEntityList.get(i);
            if (entity instanceof EntityAnimation)
                applyStaticCollisionToInnerAnimations((EntityAnimation) entity, 0);
        }
    }

    private static final int MAX_NESTED_DEPTH = 6;
    private static final int MAX_PUSH_ITERATIONS = 6;
    private static final double PUSH_EPSILON = 1.0E-4;

    private void applyStaticCollisionToInnerAnimations(EntityAnimation animation, int depth) {
        if (animation == null || animation.isDead)
            return;
        if (animation.controller == null || animation.controller.noClip() || animation.preventPush)
            return;
        // Don't double-process during active motion; LT already pushes while the animation is changing.
        if (animation.controller.isChanging())
            return;
        if (animation.origin == null || animation.worldBoundingBox == null || animation.worldCollisionBoxes == null || animation.worldCollisionBoxes.isEmpty())
            return;

        World realWorld = animation.getRealWorld();
        if (realWorld == null)
            return;

        AxisAlignedBB queryBox = animation.origin.getAxisAlignedBox(animation.worldBoundingBox).grow(0.05D);
        List<Entity> entities = realWorld.getEntitiesWithinAABB(Entity.class, queryBox, (x) -> !(x.getLowestRidingEntity() instanceof INoPushEntity));
        if (!entities.isEmpty()) {
            for (Entity entity : entities)
                pushEntityOutOfAnimation(entity, animation);
        }

        if (depth >= MAX_NESTED_DEPTH)
            return;
        if (animation.fakeWorld == null || animation.fakeWorld.loadedEntityList == null || animation.fakeWorld.loadedEntityList.isEmpty())
            return;
        for (int i = 0; i < animation.fakeWorld.loadedEntityList.size(); i++) {
            Entity entity = animation.fakeWorld.loadedEntityList.get(i);
            if (entity instanceof EntityAnimation)
                applyStaticCollisionToInnerAnimations((EntityAnimation) entity, depth + 1);
        }
    }

    private void pushEntityOutOfAnimation(Entity entity, EntityAnimation animation) {
        if (entity == null || entity.isDead)
            return;

        // Avoid repeatedly allocating in the hot path.
        Vector3d moveWorld = new Vector3d();
        Vector3d moveFake = new Vector3d();

        for (int iteration = 0; iteration < MAX_PUSH_ITERATIONS; iteration++) {
            AxisAlignedBB entityBBWorld = entity.getEntityBoundingBox();
            OrientatedBoundingBox entityBBFake = animation.origin.getOrientatedBox(entityBBWorld);

            Vector3d bestPushFake = null;
            double bestPushAbs = Double.MAX_VALUE;

            for (OrientatedBoundingBox box : animation.worldCollisionBoxes) {
                if (box == null)
                    continue;
                if (!box.intersects(entityBBFake))
                    continue;

                Vector3d pushFake = computeMinimalSeparatingPush(box, entityBBFake);
                if (pushFake == null)
                    continue;

                double abs = Math.abs(pushFake.x) + Math.abs(pushFake.y) + Math.abs(pushFake.z);
                if (abs < bestPushAbs) {
                    bestPushAbs = abs;
                    bestPushFake = pushFake;
                }
            }

            if (bestPushFake == null)
                break;

            moveFake.set(bestPushFake);
            moveWorld.set(moveFake);
            animation.origin.rotation().transform(moveWorld);

            double moveX = moveWorld.x;
            double moveY = moveWorld.y;
            double moveZ = moveWorld.z;

            if (moveX == 0 && moveY == 0 && moveZ == 0)
                break;

            boolean collidedHorizontally = entity.collidedHorizontally;
            boolean collidedVertically = entity.collidedVertically;
            boolean onGround = entity.onGround;

            entity.move(MoverType.SELF, moveX, moveY, moveZ);

            if (entity instanceof EntityPlayerMP)
                WorldAnimationHandler.setPushedByDoor((EntityPlayerMP) entity);

            if (LittleTiles.CONFIG.general.enableCollisionMotion) {
                entity.motionX += moveX;
                entity.motionY += moveY;
                entity.motionZ += moveZ;
            }

            if (moveX != 0 || moveZ != 0)
                collidedHorizontally = true;
            if (moveY != 0) {
                collidedVertically = true;
                onGround = true;
            }

            entity.collidedHorizontally = collidedHorizontally;
            entity.collidedVertically = collidedVertically;
            entity.onGround = onGround;
            entity.collided = collidedHorizontally || collidedVertically;
            if (onGround)
                entity.fallDistance = 0;
        }
    }

    private static Vector3d computeMinimalSeparatingPush(AxisAlignedBB box, AxisAlignedBB entity) {
        // The two AABBs are already in the animation's "fake" coordinate system.
        // Compute the smallest translation that moves the entity AABB out of the box AABB.

        double pushPosX = box.maxX - entity.minX;
        double pushNegX = entity.maxX - box.minX;
        double dx = pushPosX < pushNegX ? (pushPosX + PUSH_EPSILON) : -(pushNegX + PUSH_EPSILON);
        double absX = Math.min(pushPosX, pushNegX);

        double pushPosY = box.maxY - entity.minY;
        double pushNegY = entity.maxY - box.minY;
        double dy = pushPosY < pushNegY ? (pushPosY + PUSH_EPSILON) : -(pushNegY + PUSH_EPSILON);
        double absY = Math.min(pushPosY, pushNegY);

        double pushPosZ = box.maxZ - entity.minZ;
        double pushNegZ = entity.maxZ - box.minZ;
        double dz = pushPosZ < pushNegZ ? (pushPosZ + PUSH_EPSILON) : -(pushNegZ + PUSH_EPSILON);
        double absZ = Math.min(pushPosZ, pushNegZ);

        if (absX <= absY && absX <= absZ)
            return new Vector3d(dx, 0, 0);
        if (absY <= absZ)
            return new Vector3d(0, dy, 0);
        return new Vector3d(0, 0, dz);
    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {
        nbt.setDouble("rotX", rotX);
        nbt.setDouble("rotY", rotY);
        nbt.setDouble("rotZ", rotZ);

        nbt.setDouble("offX", offX);
        nbt.setDouble("offY", offY);
        nbt.setDouble("offZ", offZ);

        nbt.setBoolean("noClip", noClip);
    }

    @Override
    protected void readFromNBT(NBTTagCompound nbt) {
        rotX = nbt.getDouble("rotX");
        rotY = nbt.getDouble("rotY");
        rotZ = nbt.getDouble("rotZ");

        offX = nbt.getDouble("offX");
        offY = nbt.getDouble("offY");
        offZ = nbt.getDouble("offZ");

        noClip = nbt.getBoolean("noClip");

        setTransform(rotX, rotY, rotZ, offX, offY, offZ);
    }

    @Override
    public void transform(LittleTransformation transformation) {
        if (currentState == null || transformation == null)
            return;

        currentState.transform(transformation);

        AnimationState state = currentState.state;
        rotX = state.get(AnimationKey.rotX);
        rotY = state.get(AnimationKey.rotY);
        rotZ = state.get(AnimationKey.rotZ);

        offX = state.get(AnimationKey.offX);
        offY = state.get(AnimationKey.offY);
        offZ = state.get(AnimationKey.offZ);
    }
}
