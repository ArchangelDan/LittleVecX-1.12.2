package com.integral.littlevecx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.creativecore.common.gui.controls.gui.GuiLabel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiTextfield;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.GuiTimeline;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.GuiTimeline.KeyDeselectedEvent;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.GuiTimeline.KeySelectedEvent;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.KeyControl;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.TimelineChannel;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.TimelineChannel.TimelineChannelDouble;
import com.creativemd.creativecore.common.gui.controls.gui.timeline.TimelineChannel.TimelineChannelInteger;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.gui.event.gui.GuiToolTipEvent;
import com.creativemd.creativecore.common.utils.math.collision.MatrixUtils;
import com.creativemd.creativecore.common.utils.type.HashMapList;
import com.creativemd.creativecore.common.utils.type.PairList;
import com.creativemd.creativecore.common.utils.type.UUIDSupplier;
import com.creativemd.creativecore.common.world.IOrientatedWorld;
import com.creativemd.creativecore.common.world.SubWorld;
import com.creativemd.littletiles.client.gui.controls.GuiLTDistance;
import com.creativemd.littletiles.common.entity.DoorController;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.packet.LittleAnimationDataPacket;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.animation.AnimationGuiHandler;
import com.creativemd.littletiles.common.structure.animation.AnimationKey;
import com.creativemd.littletiles.common.structure.animation.AnimationTimeline;
import com.creativemd.littletiles.common.structure.animation.ValueTimeline;
import com.creativemd.littletiles.common.structure.attribute.LittleStructureAttribute;
import com.creativemd.littletiles.common.structure.connection.StructureChildConnection;
import com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException;
import com.creativemd.littletiles.common.structure.exception.NotYetConnectedException;
import com.creativemd.littletiles.common.structure.relative.StructureAbsolute;
import com.creativemd.littletiles.common.structure.relative.StructureRelative;
import com.creativemd.littletiles.common.structure.registry.LittleStructureGuiParser;
import com.creativemd.littletiles.common.structure.registry.LittleStructureRegistry;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType;
import com.creativemd.littletiles.common.structure.type.door.LittleDoorBase;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.location.LocalStructureLocation;
import com.creativemd.littletiles.common.tile.math.vec.LittleAbsoluteVec;
import com.creativemd.littletiles.common.tile.parent.IStructureTileList;
import com.creativemd.littletiles.common.tile.preview.LittleAbsolutePreviews;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.place.Placement;
import com.creativemd.littletiles.common.util.place.PlacementHelper;
import com.creativemd.littletiles.common.util.place.PlacementMode;
import com.creativemd.littletiles.common.util.place.PlacementResult;
import com.integral.littlevecx.animation.StructureLittleVecXMultiAnimation;
import com.integral.littlevecx.client.gui.GuiLittleVecXRotatedAxisButton;
import com.integral.littlevecx.client.gui.SubGuiLittleVecXSettings.GuiLittleVecXSettingsButton;
import com.integral.littlevecx.client.gui.layout.LittleVecXGuiLayout;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class StructureLittleVecXRotated extends LittleStructure {

    private static final Logger LOGGER = LogManager.getLogger("LittleVecX");
    private static final int LEGACY_STEPS = 24; // 360 / 15
    private static final int DEFAULT_DURATION = 10;
    private static final int DEFAULT_OFFGRID = 16;

    public int duration = DEFAULT_DURATION;

    @Nullable
    public ValueTimeline rotX = null;
    @Nullable
    public ValueTimeline rotY = null;
    @Nullable
    public ValueTimeline rotZ = null;

    public int offGrid = DEFAULT_OFFGRID;

    @Nullable
    public ValueTimeline offX = null;
    @Nullable
    public ValueTimeline offY = null;
    @Nullable
    public ValueTimeline offZ = null;

    private double placedOffX = 0.0;
    private double placedOffY = 0.0;
    private double placedOffZ = 0.0;

    public boolean noClip = false;

    public boolean forceStayAnimatedDoors = false;

    /** StructureRelative.write() format: [minX,minY,minZ,maxX,maxY,maxZ,gridSize] */
    @Nullable
    public int[] axis = null;

    public StructureLittleVecXRotated(LittleStructureType type, IStructureTileList mainBlock) {
        super(type, mainBlock);
    }

    private static double normalizeDegrees(double degrees) {
        degrees %= 360.0;
        if (degrees < 0)
            degrees += 360.0;
        return degrees;
    }

    private static boolean isZero(double value) {
        return Math.abs(value) < 1.0E-9;
    }

    private static int sanitizeDuration(int duration) {
        if (duration <= 0)
            return DEFAULT_DURATION;
        return duration;
    }

    private static int sanitizeOffGrid(int offGrid) {
        if (offGrid <= 0)
            return DEFAULT_OFFGRID;
        return offGrid;
    }

    @Nullable
    private static ValueTimeline tryReadTimeline(NBTTagCompound nbt, String key) {
        if (nbt == null || !nbt.hasKey(key))
            return null;
        try {
            return ValueTimeline.read(nbt.getIntArray(key));
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Nullable
    private static ValueTimeline legacyLinearTimeline(double endValue, int duration) {
        if (isZero(endValue))
            return null;
        ValueTimeline timeline = ValueTimeline.create(0);
        timeline.addPoint(0, 0D);
        timeline.addPoint(duration, endValue);
        return timeline;
    }

    @Override
    protected void loadFromNBTExtra(NBTTagCompound nbt) {
        duration = sanitizeDuration(nbt.hasKey("duration") ? nbt.getInteger("duration") : DEFAULT_DURATION);

        noClip = nbt.getBoolean("noClip");
        forceStayAnimatedDoors = nbt.getBoolean("forceStayAnimatedDoors");
        placedOffX = nbt.getDouble("placedOffX");
        placedOffY = nbt.getDouble("placedOffY");
        placedOffZ = nbt.getDouble("placedOffZ");

        if (nbt.hasKey("axis")) {
            int[] data = nbt.getIntArray("axis");
            if (data != null && data.length == 7)
                axis = data;
            else
                axis = null;
        } else {
            axis = null;
        }

        if (nbt.hasKey("animation")) {
            NBTTagCompound animation = nbt.getCompoundTag("animation");
            rotX = tryReadTimeline(animation, "rotX");
            rotY = tryReadTimeline(animation, "rotY");
            rotZ = tryReadTimeline(animation, "rotZ");

            if (animation.hasKey("offGrid")) {
                offGrid = sanitizeOffGrid(animation.getInteger("offGrid"));
                offX = tryReadTimeline(animation, "offX");
                offY = tryReadTimeline(animation, "offY");
                offZ = tryReadTimeline(animation, "offZ");
            } else {
                offGrid = DEFAULT_OFFGRID;
                offX = null;
                offY = null;
                offZ = null;
            }
            return;
        }

        // Backwards compatibility for old recipes.
        double legacyX;
        double legacyY;
        double legacyZ;
        if (nbt.hasKey("rotX") || nbt.hasKey("rotY") || nbt.hasKey("rotZ")) {
            legacyX = nbt.getDouble("rotX");
            legacyY = nbt.getDouble("rotY");
            legacyZ = nbt.getDouble("rotZ");
        } else {
            int step = nbt.getInteger("angleStep");
            int normalizedStep = step % LEGACY_STEPS;
            if (normalizedStep < 0)
                normalizedStep += LEGACY_STEPS;

            legacyX = 0.0;
            legacyY = normalizedStep * (360.0 / LEGACY_STEPS);
            legacyZ = 0.0;
        }

        rotX = legacyLinearTimeline(legacyX, duration);
        rotY = legacyLinearTimeline(legacyY, duration);
        rotZ = legacyLinearTimeline(legacyZ, duration);

        offGrid = DEFAULT_OFFGRID;
        offX = null;
        offY = null;
        offZ = null;
    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {
        nbt.setInteger("duration", sanitizeDuration(duration));

        if (noClip)
            nbt.setBoolean("noClip", true);
        else
            nbt.removeTag("noClip");

        if (forceStayAnimatedDoors)
            nbt.setBoolean("forceStayAnimatedDoors", true);
        else
            nbt.removeTag("forceStayAnimatedDoors");

        if (axis != null && axis.length == 7)
            nbt.setIntArray("axis", axis);
        else
            nbt.removeTag("axis");

        NBTTagCompound animation = new NBTTagCompound();

        if (rotX != null)
            animation.setIntArray("rotX", rotX.write());
        if (rotY != null)
            animation.setIntArray("rotY", rotY.write());
        if (rotZ != null)
            animation.setIntArray("rotZ", rotZ.write());

        offGrid = sanitizeOffGrid(offGrid);
        animation.setInteger("offGrid", offGrid);

        if (offX != null)
            animation.setIntArray("offX", offX.write());
        if (offY != null)
            animation.setIntArray("offY", offY.write());
        if (offZ != null)
            animation.setIntArray("offZ", offZ.write());

        nbt.setTag("animation", animation);

        if (!isZero(placedOffX))
            nbt.setDouble("placedOffX", placedOffX);
        else
            nbt.removeTag("placedOffX");

        if (!isZero(placedOffY))
            nbt.setDouble("placedOffY", placedOffY);
        else
            nbt.removeTag("placedOffY");

        if (!isZero(placedOffZ))
            nbt.setDouble("placedOffZ", placedOffZ);
        else
            nbt.removeTag("placedOffZ");
    }

    @Override
    public int getAttribute() {
        if (noClip)
            return super.getAttribute() | LittleStructureAttribute.NOCOLLISION;
        return super.getAttribute();
    }

    @Override
    public void afterPlaced() {
        super.afterPlaced();
        syncAttribute();
    }

    private static final class ResolvedTransform {
        private final double rotX;
        private final double rotY;
        private final double rotZ;
        private final double offX;
        private final double offY;
        private final double offZ;

        private ResolvedTransform(double rotX, double rotY, double rotZ, double offX, double offY, double offZ) {
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.offX = offX;
            this.offY = offY;
            this.offZ = offZ;
        }

        private boolean isIdentity() {
            return isZero(rotX) && isZero(rotY) && isZero(rotZ) && isZero(offX) && isZero(offY) && isZero(offZ);
        }
    }

    private LittleGridContext getOffsetContext() {
        try {
            return LittleGridContext.get(sanitizeOffGrid(offGrid));
        } catch (RuntimeException e) {
            return LittleGridContext.get();
        }
    }

    private ResolvedTransform resolveBasePlacedTransform(BlockPos anchor, LittleAbsolutePreviews previews) {
        int duration = sanitizeDuration(this.duration);

        double x = normalizeDegrees(rotX != null ? rotX.value(duration) : 0.0);
        double y = normalizeDegrees(rotY != null ? rotY.value(duration) : 0.0);
        double z = normalizeDegrees(rotZ != null ? rotZ.value(duration) : 0.0);

        LittleGridContext offContext = getOffsetContext();
        double userOffX = offX != null ? offContext.toVanillaGrid(offX.value(duration)) : 0.0;
        double userOffY = offY != null ? offContext.toVanillaGrid(offY.value(duration)) : 0.0;
        double userOffZ = offZ != null ? offContext.toVanillaGrid(offZ.value(duration)) : 0.0;

        StructureAbsolute absolute = new StructureAbsolute(previews.pos, previews.getSurroundingBox(), previews.getContext());

        double pivotOffX = 0.0;
        double pivotOffY = 0.0;
        double pivotOffZ = 0.0;
        if (axis != null && axis.length == 7) {
            try {
                StructureRelative axisRel = new StructureRelative(axis);
                StructureAbsolute axisAbs = new StructureAbsolute(new LittleAbsoluteVec(previews.pos, previews.getContext()), axisRel);

                Vector3d delta = new Vector3d(axisAbs.rotationCenter);
                delta.sub(absolute.rotationCenter);

                if (!isZero(delta.x) || !isZero(delta.y) || !isZero(delta.z)) {
                    Matrix3d rot = MatrixUtils.createRotationMatrix(x, y, z);
                    Vector3d rotated = new Vector3d(delta);
                    rot.transform(rotated);

                    Vector3d translation = new Vector3d(delta);
                    translation.sub(rotated);

                    pivotOffX = translation.x;
                    pivotOffY = translation.y;
                    pivotOffZ = translation.z;
                }
            } catch (RuntimeException e) {
                // Invalid axis, ignore.
            }
        }

        return new ResolvedTransform(x, y, z, userOffX + pivotOffX, userOffY + pivotOffY, userOffZ + pivotOffZ);
    }

    private ResolvedTransform resolvePlacedTransform(BlockPos anchor, LittleAbsolutePreviews previews) {
        ResolvedTransform base = resolveBasePlacedTransform(anchor, previews);
        return new ResolvedTransform(
                base.rotX,
                base.rotY,
                base.rotZ,
                base.offX + placedOffX,
                base.offY + placedOffY,
                base.offZ + placedOffZ
        );
    }

    public boolean applyPersistentPlacedOffset(@Nullable EntityAnimation animation, double absoluteOffX, double absoluteOffY, double absoluteOffZ) {
        if (animation == null)
            return false;

        try {
            load();
            BlockPos anchor = animation.absolutePreviewPos != null ? animation.absolutePreviewPos : getPos();
            LittleAbsolutePreviews previews = getAbsolutePreviewsSameWorldOnly(anchor);
            ResolvedTransform base = resolveBasePlacedTransform(anchor, previews);

            placedOffX = absoluteOffX - base.offX;
            placedOffY = absoluteOffY - base.offY;
            placedOffZ = absoluteOffZ - base.offZ;

            TileEntityLittleTiles te = mainBlock != null ? mainBlock.getTe() : null;
            if (te != null && !te.isInvalid()) {
                te.markDirty();
                if (te.getWorld() instanceof WorldServer)
                    ((WorldServer) te.getWorld()).getPlayerChunkMap().markBlockForUpdate(te.getPos());
            }

            updateStructure();
            return true;
        } catch (CorruptedConnectionException | NotYetConnectedException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public void refreshPlacedAnimation(@Nullable EntityAnimation animation) {
        if (animation == null || !(animation.controller instanceof LittleVecXStaticRotationController))
            return;

        try {
            load();
            BlockPos anchor = animation.absolutePreviewPos != null ? animation.absolutePreviewPos : getPos();
            LittleAbsolutePreviews previews = getAbsolutePreviewsSameWorldOnly(anchor);
            ResolvedTransform transform = resolvePlacedTransform(anchor, previews);

            LittleVecXStaticRotationController controller = (LittleVecXStaticRotationController) animation.controller;
            controller.noClip = noClip;
            controller.setTransform(transform.rotX, transform.rotY, transform.rotZ, transform.offX, transform.offY, transform.offZ);

            animation.updateTickState();
            animation.updateBoundingBox();
            syncAttribute();
        } catch (CorruptedConnectionException | NotYetConnectedException e) {
            // Ignore stale/broken fake-world connections during live editing.
        }
    }

    @Override
    public void finishedPlacement(Placement placement) {
        World world = getWorld();
        if (world == null || world.isRemote)
            return;

        // If this structure is being placed into a fake/orientated world, do not recurse.
        if (world instanceof IOrientatedWorld)
            return;

        BlockPos anchor = placement != null ? placement.pos : getPos();

        try {
            load();

            HashMapList<BlockPos, IStructureTileList> blocks = collectAllBlocksListSameWorld();
            LittleAbsolutePreviews previews = getAbsolutePreviewsSameWorldOnly(anchor);
            ResolvedTransform transform = resolvePlacedTransform(anchor, previews);
            if (transform.isIdentity())
                return;

            SubWorld fakeWorld = SubWorld.createFakeWorld(world);
            fakeWorld.preventNeighborUpdate = true;

            Placement fakePlacement = new Placement(null, PlacementHelper.getAbsolutePreviews(fakeWorld, previews, previews.pos, PlacementMode.all))
                    .setAfterNotifyPlace(false)
                    .setPlaySounds(false);

            PlacementResult result = fakePlacement.tryPlace();

            fakeWorld.preventNeighborUpdate = false;

            if (result == null || result.parentStructure == null)
                return;

            if (forceStayAnimatedDoors)
                forceStayAnimatedOnDoors(result.parentStructure);

            // Doors inside a rotated structure would normally convert into a nested EntityAnimation on first open.
            // That conversion triggers a massive "top animation" resync packet and (in practice) can make the whole
            // rotated build disappear client-side for a long time. It also breaks closed-state collision for nested
            // animations because their origin parent isn't wired up until the top animation is created.
            //
            // When "keep doors animated" is enabled we pre-convert all doors into *closed* animations right here,
            // before the top animation is spawned. This makes first activation send only a small controller packet
            // and ensures nested door collision works immediately.
            if (forceStayAnimatedDoors)
                preAnimateDoorsClosed(result.parentStructure);

            StructureAbsolute absolute = new StructureAbsolute(previews.pos, previews.getSurroundingBox(), previews.getContext());

            LittleVecXStaticRotationController controller = new LittleVecXStaticRotationController(
                    transform.rotX,
                    transform.rotY,
                    transform.rotZ,
                    transform.offX,
                    transform.offY,
                    transform.offZ
            );
            controller.noClip = noClip;

            EntityAnimation animation = new EntityAnimation(world, fakeWorld, controller, previews.pos, UUID.randomUUID(), absolute,
                    new LocalStructureLocation(result.parentStructure));

            // Remove original blocks first to avoid a one-tick double-render.
            for (Entry<BlockPos, ArrayList<IStructureTileList>> entry : blocks.entrySet()) {
                if (entry.getValue().isEmpty())
                    continue;

                TileEntityLittleTiles te = entry.getValue().get(0).getTe();
                te.updateTiles((interactor) -> {
                    for (IStructureTileList list : entry.getValue())
                        interactor.get(list).remove();
                });

                if (world instanceof WorldServer)
                    ((WorldServer) world).getPlayerChunkMap().markBlockForUpdate(te.getPos());
            }

            world.spawnEntity(animation);
            animation.updateTickState();
            if (forceStayAnimatedDoors)
                animation.onUpdateForReal();
            if (forceStayAnimatedDoors && world instanceof WorldServer) {
                WorldServer serverWorld = (WorldServer) world;
                serverWorld.getMinecraftServer().addScheduledTask(() -> {
                    if (!animation.isDead)
                        PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationDataPacket(animation), animation, null);
                });
            }
        } catch (CorruptedConnectionException | NotYetConnectedException e) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX rotation animation skipped because the structure is not fully connected", e);
        } catch (RuntimeException e) {
            LOGGER.warn("LittleVecX rotation animation failed", e);
        }
    }

    private void syncAttribute() {
        if (!hasWorld() || getWorld().isRemote)
            return;
        try {
            tryAttributeChangeForBlocks();
        } catch (CorruptedConnectionException | NotYetConnectedException e) {
            // Ignore: structure may not be fully connected yet.
        }
    }

    private static void forceStayAnimatedOnDoors(@Nullable LittleStructure root) {
        if (root == null)
            return;

        if (root instanceof LittleDoorBase) {
            LittleDoorBase door = (LittleDoorBase) root;
            if (!door.stayAnimated)
                door.stayAnimated = true;
        }

        for (StructureChildConnection child : root.getChildren()) {
            try {
                forceStayAnimatedOnDoors(child.getStructure());
            } catch (CorruptedConnectionException | NotYetConnectedException e) {
                // Ignore broken connections inside the fake world.
            }
        }
    }

    private static void preAnimateDoorsClosed(@Nullable LittleStructure root) {
        if (root == null)
            return;

        // Collect first to avoid modifying the structure tree while iterating it.
        List<LittleDoorBase> doors = new ArrayList<>();
        collectDoors(root, doors);

        for (LittleDoorBase door : doors) {
            try {
                preAnimateDoorClosed(door);
            } catch (Exception e) {
                com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX rotation could not pre-animate a child door before spawning", e);
            }
        }
    }

    private static void collectDoors(@Nullable LittleStructure structure, List<LittleDoorBase> out) {
        if (structure == null)
            return;

        if (structure instanceof LittleDoorBase)
            out.add((LittleDoorBase) structure);

        for (StructureChildConnection child : structure.getChildren()) {
            try {
                collectDoors(child.getStructure(), out);
            } catch (CorruptedConnectionException | NotYetConnectedException e) {
                // Ignore broken connections inside the fake world.
            }
        }
    }

    private static void preAnimateDoorClosed(LittleDoorBase door) throws CorruptedConnectionException, NotYetConnectedException {
        if (door == null)
            return;
        if (door.isAnimated())
            return;
        if (door.mainBlock == null || door.mainBlock.isRemoved())
            return;

        World doorWorld = door.getWorld();
        if (doorWorld == null || doorWorld.isRemote)
            return;

        // Make sure we're in a fake world (doors are nested inside the rotated structure's fakeWorld).
        // If someone places this structure directly, we do not want to create sub-animations.
        if (!(doorWorld instanceof SubWorld))
            return;

        LittleAbsolutePreviews previews = door.getDoorPreviews();
        StructureAbsolute absolute = door.getAbsoluteAxis();
        HashMapList<BlockPos, IStructureTileList> blocks = door.collectAllBlocksListSameWorld();

        // Place the door into its own nested fake world, then wrap it as an EntityAnimation in the parent fake world.
        SubWorld doorFakeWorld = SubWorld.createFakeWorld(doorWorld);
        doorFakeWorld.preventNeighborUpdate = true;

        Placement placement = new Placement(null, PlacementHelper.getAbsolutePreviews(doorFakeWorld, previews, previews.pos, PlacementMode.all))
                .setIgnoreWorldBoundaries(false)
                .setAfterNotifyPlace(false)
                .setPlaySounds(false);

        PlacementResult result = placement.tryPlace();
        doorFakeWorld.preventNeighborUpdate = false;

        if (result == null || result.parentStructure == null)
            return;
        if (!(result.parentStructure instanceof LittleDoorBase))
            return;

        LittleDoorBase newDoor = (LittleDoorBase) result.parentStructure;
        newDoor.stayAnimated = true;

        UUIDSupplier supplier = new UUIDSupplier(UUID.randomUUID(), UUID.randomUUID());
        DoorController controller = door.createController(supplier, placement, door.getCompleteDuration());
        controller.noClip = door.noClip;
        controller.activator = null;

        EntityAnimation animation = new EntityAnimation(doorWorld, doorFakeWorld, controller, placement.pos, supplier.next(), absolute,
                new LocalStructureLocation(newDoor));
        newDoor.setAnimation(animation);

        // Move animated worlds
        newDoor.transferChildrenToAnimation(animation);

        // Reconnect the door to its parent (if it had one) so the structure tree stays valid.
        if (door.getParent() != null) {
            LittleStructure parentStructure = door.getParent().getStructure();
            boolean dynamic = door.getParent().dynamic;
            parentStructure.updateChildConnection(door.getParent().getChildId(), newDoor, dynamic);
            newDoor.updateParentConnection(door.getParent().getChildId(), parentStructure, dynamic);
            parentStructure.updateStructure();
        }

        newDoor.notifyAfterPlaced();
        animation.updateTickState();
        animation.updateBoundingBox();

        // IMPORTANT: do NOT start an "opened" transition. We want the door to start closed, but already be animated.

        doorWorld.spawnEntity(animation);
        animation.onUpdateForReal();

        // Remove the original static door tiles from the parent fake world. They are now represented by the nested animation.
        for (Entry<BlockPos, ArrayList<IStructureTileList>> entry : blocks.entrySet()) {
            if (entry.getValue().isEmpty())
                continue;

            TileEntityLittleTiles te = entry.getValue().get(0).getTe();
            if (te == null || te.isInvalid())
                continue;

            te.updateTiles((x) -> {
                for (IStructureTileList list : entry.getValue())
                    x.get(list).remove();
            });
        }
    }

    public static class StructureLittleVecXRotatedParser extends LittleStructureGuiParser {

        public StructureLittleVecXRotatedParser(GuiParent parent, AnimationGuiHandler handler) {
            super(parent, handler);
        }

        public LittleGridContext context;

        @Override
        @SideOnly(Side.CLIENT)
        protected void createControls(LittlePreviews previews, @Nullable LittleStructure structure) {
            StructureLittleVecXRotated rotated = structure instanceof StructureLittleVecXRotated ? (StructureLittleVecXRotated) structure : null;
            LittleVecXGuiLayout layout = new LittleVecXGuiLayout(190, 130, 0);
            int timelineHeight = 67;
            int valueRowY = layout.nextRow(0, timelineHeight, 5);
            int positionRowY = layout.nextRow(valueRowY, 10, 5);
            int actionRowY = layout.nextRow(positionRowY, 10, 6);
            int valueFieldWidth = 42;
            int positionFieldX = 52;
            int axisButtonX = positionFieldX + valueFieldWidth + 8;
            int axisButtonWidth = 56;

            int duration = rotated != null ? sanitizeDuration(rotated.duration) : DEFAULT_DURATION;

            List<TimelineChannel> channels = new ArrayList<>();

            PairList<Integer, Double> rotXKeys = rotated != null && rotated.rotX != null ? rotated.rotX.getPointsCopy() : null;
            if (rotXKeys == null || rotXKeys.isEmpty())
                rotXKeys = defaultDoubleKeys(duration);
            channels.add(new TimelineChannelDouble("rot X").addKeys(rotXKeys));

            PairList<Integer, Double> rotYKeys = rotated != null && rotated.rotY != null ? rotated.rotY.getPointsCopy() : null;
            if (rotYKeys == null || rotYKeys.isEmpty())
                rotYKeys = defaultDoubleKeys(duration);
            channels.add(new TimelineChannelDouble("rot Y").addKeys(rotYKeys));

            PairList<Integer, Double> rotZKeys = rotated != null && rotated.rotZ != null ? rotated.rotZ.getPointsCopy() : null;
            if (rotZKeys == null || rotZKeys.isEmpty())
                rotZKeys = defaultDoubleKeys(duration);
            channels.add(new TimelineChannelDouble("rot Z").addKeys(rotZKeys));

            PairList<Integer, Integer> offXKeys = rotated != null && rotated.offX != null ? rotated.offX.getRoundedPointsCopy() : null;
            if (offXKeys == null || offXKeys.isEmpty())
                offXKeys = defaultIntKeys(duration);
            channels.add(new TimelineChannelInteger("off X").addKeys(offXKeys));

            PairList<Integer, Integer> offYKeys = rotated != null && rotated.offY != null ? rotated.offY.getRoundedPointsCopy() : null;
            if (offYKeys == null || offYKeys.isEmpty())
                offYKeys = defaultIntKeys(duration);
            channels.add(new TimelineChannelInteger("off Y").addKeys(offYKeys));

            PairList<Integer, Integer> offZKeys = rotated != null && rotated.offZ != null ? rotated.offZ.getRoundedPointsCopy() : null;
            if (offZKeys == null || offZKeys.isEmpty())
                offZKeys = defaultIntKeys(duration);
            channels.add(new TimelineChannelInteger("off Z").addKeys(offZKeys));

            parent.controls.add(new GuiTimeline("timeline", 0, 0, 190, timelineHeight, duration, channels, handler).setSidebarWidth(30));
            parent.controls.add(new GuiLabel("tick", "0", layout.right(40), valueRowY + 1));

            int offGrid = rotated != null ? sanitizeOffGrid(rotated.offGrid) : DEFAULT_OFFGRID;
            try {
                context = LittleGridContext.get(offGrid);
            } catch (RuntimeException e) {
                context = LittleGridContext.get();
            }

            parent.controls.add(new GuiTextfield("keyValue", "", 0, valueRowY, valueFieldWidth, 10).setFloatOnly().setEnabled(false));
            parent.controls.add(new GuiLTDistance("keyDistance", 0, valueRowY, context, 0).setVisible(false));

            parent.controls.add(new GuiLabel("Position:", 0, positionRowY + 1));
            parent.controls.add(new GuiTextfield("keyPosition", "", positionFieldX, positionRowY, valueFieldWidth, 10).setNumbersOnly().setEnabled(false));

            int[] axisData = rotated != null && rotated.axis != null && rotated.axis.length == 7 ? rotated.axis : defaultAxis(previews);
            parent.controls.add(new GuiLittleVecXRotatedAxisButton("axis", CoreControl.translate("gui.littlevecx.axis_button"), axisButtonX, positionRowY,
                    axisButtonWidth, 10, previews.getContext(), axisData, handler));

            boolean noClip = rotated != null && rotated.noClip;
            boolean forceStayAnimatedDoors = rotated != null && rotated.forceStayAnimatedDoors;
            parent.controls.add(new GuiLittleVecXSettingsButton("settings", 0, actionRowY, noClip, forceStayAnimatedDoors));

            updateTimeline();
        }

        private static PairList<Integer, Double> defaultDoubleKeys(int duration) {
            PairList<Integer, Double> list = new PairList<>();
            list.add(0, 0D);
            list.add(duration, 0D);
            return list;
        }

        private static PairList<Integer, Integer> defaultIntKeys(int duration) {
            PairList<Integer, Integer> list = new PairList<>();
            list.add(0, 0);
            list.add(duration, 0);
            return list;
        }

        public void updateTimeline() {
            GuiTimeline timeline = (GuiTimeline) parent.get("timeline");
            AnimationTimeline animation = new AnimationTimeline(timeline.getDuration(), new PairList<>());

            int interpolation = 0; // linear

            ValueTimeline rotX = ValueTimeline.create(interpolation, timeline.channels.get(0).getPairs());
            if (rotX != null)
                animation.values.add(AnimationKey.rotX, rotX);

            ValueTimeline rotY = ValueTimeline.create(interpolation, timeline.channels.get(1).getPairs());
            if (rotY != null)
                animation.values.add(AnimationKey.rotY, rotY);

            ValueTimeline rotZ = ValueTimeline.create(interpolation, timeline.channels.get(2).getPairs());
            if (rotZ != null)
                animation.values.add(AnimationKey.rotZ, rotZ);

            ValueTimeline offX = ValueTimeline.create(interpolation, timeline.channels.get(3).getPairs());
            if (offX != null)
                animation.values.add(AnimationKey.offX, offX.factor(context.pixelSize));

            ValueTimeline offY = ValueTimeline.create(interpolation, timeline.channels.get(4).getPairs());
            if (offY != null)
                animation.values.add(AnimationKey.offY, offY.factor(context.pixelSize));

            ValueTimeline offZ = ValueTimeline.create(interpolation, timeline.channels.get(5).getPairs());
            if (offZ != null)
                animation.values.add(AnimationKey.offZ, offZ.factor(context.pixelSize));

            handler.setTimeline(animation, null);
        }

        @SideOnly(Side.CLIENT)
        private KeyControl selected;

        @CustomEventSubscribe
        @SideOnly(Side.CLIENT)
        public void onKeySelected(KeySelectedEvent event) {
            GuiTextfield textfield = (GuiTextfield) parent.get("keyValue");
            GuiLTDistance distance = (GuiLTDistance) parent.get("keyDistance");

            selected = (KeyControl) event.source;

            if (((KeyControl) event.source).value instanceof Double) {
                distance.setVisible(false);
                textfield.setEnabled(true);
                textfield.setVisible(true);
                textfield.text = "" + selected.value;
            } else {
                distance.setEnabled(true);
                distance.setVisible(true);
                textfield.setVisible(false);

                distance.setDistance(context, (int) selected.value);
            }

            GuiTextfield position = (GuiTextfield) parent.get("keyPosition");
            position.setEnabled(true);
            position.text = "" + selected.tick;
        }

        @CustomEventSubscribe
        @SideOnly(Side.CLIENT)
        public void onChange(GuiControlChangedEvent event) {
            if (event.source.is("timeline")) {
                updateTimeline();
                return;
            }

            if (selected == null)
                return;

            if (event.source.is("keyDistance")) {

                if (!selected.modifiable)
                    return;

                GuiLTDistance distance = (GuiLTDistance) event.source;
                LittleGridContext newContext = distance.getDistanceContext();
                if (newContext.size > context.size) {
                    int scale = newContext.size / context.size;
                    GuiTimeline timeline = (GuiTimeline) parent.get("timeline");
                    for (TimelineChannel channel : timeline.channels) {
                        if (channel instanceof TimelineChannelInteger) {
                            for (Object control : channel.controls) {
                                ((KeyControl<Integer>) control).value *= scale;
                            }
                        }
                    }
                }

                context = newContext;
                selected.value = distance.getDistance();
                updateTimeline();
            } else if (event.source.is("keyValue")) {
                if (!selected.modifiable)
                    return;

                try {
                    selected.value = Double.parseDouble(((GuiTextfield) event.source).text);
                } catch (NumberFormatException e) {

                }
                updateTimeline();
            } else if (event.source.is("keyPosition")) {
                if (!selected.modifiable)
                    return;

                try {
                    GuiTimeline timeline = (GuiTimeline) parent.get("timeline");

                    int tick = selected.tick;
                    int newTick = Integer.parseInt(((GuiTextfield) event.source).text);
                    if (selected.channel.isSpaceFor(selected, newTick)) {
                        selected.tick = newTick;
                        selected.channel.movedKey(selected);
                        if (tick != selected.tick)
                            timeline.adjustKeysPositionX();
                    }
                } catch (NumberFormatException e) {

                }
                updateTimeline();
            } else if (event.source.is("timeline")) {
                updateTimeline();
            }
        }

        @CustomEventSubscribe
        @SideOnly(Side.CLIENT)
        public void onKeyDeselected(KeyDeselectedEvent event) {
            selected = null;
            GuiTextfield textfield = (GuiTextfield) parent.get("keyValue");
            textfield.setEnabled(false);
            textfield.text = "";
            textfield.setCursorPositionZero();

            textfield = (GuiTextfield) parent.get("keyPosition");
            textfield.setEnabled(false);
            textfield.text = "";
            textfield.setCursorPositionZero();

            GuiLTDistance distance = (GuiLTDistance) parent.get("keyDistance");
            distance.setEnabled(false);
            distance.resetTextfield();

            updateTimeline();
        }

        @CustomEventSubscribe
        @SideOnly(Side.CLIENT)
        public void toolTip(GuiToolTipEvent event) {
            if (event.source.is("timeline")) {
                ((GuiLabel) parent.get("tick")).setCaption(event.tooltip.get(0));
                event.CancelEvent();
            }
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructure parseStructure(LittlePreviews previews) {
            StructureLittleVecXRotated rotated = createStructure(StructureLittleVecXRotated.class, null);

            GuiLittleVecXRotatedAxisButton axisButton = (GuiLittleVecXRotatedAxisButton) parent.get("axis");
            rotated.axis = axisButton.axisData;

            GuiTimeline timeline = (GuiTimeline) parent.get("timeline");
            rotated.duration = sanitizeDuration(timeline.getDuration());

            GuiLittleVecXSettingsButton settings = (GuiLittleVecXSettingsButton) parent.get("settings");
            rotated.noClip = settings.noClip;
            rotated.forceStayAnimatedDoors = settings.forceStayAnimatedDoors;

            int interpolation = 0; // linear

            rotated.rotX = ValueTimeline.create(interpolation, timeline.channels.get(0).getPairs());
            rotated.rotY = ValueTimeline.create(interpolation, timeline.channels.get(1).getPairs());
            rotated.rotZ = ValueTimeline.create(interpolation, timeline.channels.get(2).getPairs());

            rotated.offGrid = context.size;
            rotated.offX = ValueTimeline.create(interpolation, timeline.channels.get(3).getPairs());
            rotated.offY = ValueTimeline.create(interpolation, timeline.channels.get(4).getPairs());
            rotated.offZ = ValueTimeline.create(interpolation, timeline.channels.get(5).getPairs());

            return rotated;
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructureType getStructureType() {
            return LittleStructureRegistry.getStructureType(StructureLittleVecXRotated.class);
        }

        @SideOnly(Side.CLIENT)
        private static int[] defaultAxis(LittlePreviews previews) {
            LittleBox box = previews.getSurroundingBox();
            int minX = axisMin(box.minX, box.maxX);
            int minY = axisMin(box.minY, box.maxY);
            int minZ = axisMin(box.minZ, box.maxZ);
            int maxX = axisMax(box.minX, box.maxX);
            int maxY = axisMax(box.minY, box.maxY);
            int maxZ = axisMax(box.minZ, box.maxZ);
            return new int[] { minX, minY, minZ, maxX, maxY, maxZ, previews.getContext().size };
        }

        @SideOnly(Side.CLIENT)
        private static int axisMin(int min, int max) {
            int sum = min + max;
            if ((sum & 1) == 0) {
                int center = sum / 2;
                return center - 1;
            }
            return Math.floorDiv(sum, 2);
        }

        @SideOnly(Side.CLIENT)
        private static int axisMax(int min, int max) {
            int sum = min + max;
            if ((sum & 1) == 0) {
                int center = sum / 2;
                return center + 1;
            }
            return Math.floorDiv(sum, 2) + 1;
        }
    }
}
