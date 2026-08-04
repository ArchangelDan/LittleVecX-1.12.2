package com.integral.littlevecx.network;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.packet.LittleAnimationControllerPacket;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException;
import com.creativemd.littletiles.common.structure.exception.NotYetConnectedException;
import com.creativemd.littletiles.common.tile.math.location.StructureLocation;
import com.creativemd.littletiles.common.tile.parent.IStructureTileList;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.util.place.Placement;
import com.creativemd.littletiles.common.util.place.PlacementHelper;
import com.creativemd.littletiles.common.util.place.PlacementMode;
import com.creativemd.littletiles.common.util.place.PlacementResult;
import com.creativemd.littletiles.common.world.LittleNeighborUpdateCollector;
import com.creativemd.littletiles.common.world.WorldAnimationHandler;
import com.integral.littlevecx.LittleVecXAnimationSyncHelper;
import com.integral.littlevecx.StructureLittleVecXRotated;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PacketLittleVecXApplyStructurePreviews extends CreativeCorePacket {

    private StructureLocation location;
    private LittlePreviews previews;

    public PacketLittleVecXApplyStructurePreviews() {}

    public PacketLittleVecXApplyStructurePreviews(StructureLocation location, LittlePreviews previews) {
        this.location = location;
        this.previews = previews;
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        LittleAction.writeStructureLocation(location, buf);
        LittleAction.writePreviews(previews, buf);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        location = LittleAction.readStructureLocation(buf);
        previews = LittleAction.readPreviews(buf);
    }

    @Override
    public void executeClient(EntityPlayer player) {}

    @Override
    public void executeServer(EntityPlayer player) {
        if (location == null || previews == null || !previews.hasStructure())
            return;

        String previewStructureId = previews.getStructureId();

        try {
            LittleStructure root = location.find(player.world);
            if (root == null || !root.type.id.equals(previewStructureId))
                return;

            Set<TileEntityLittleTiles> dirtyTiles = new LinkedHashSet<>();
            LittleStructure updatedRoot;
            if (root instanceof StructureLittleVecXRotated)
                updatedRoot = applyPreviewTree(root, previews, true, dirtyTiles);
            else
                updatedRoot = replaceStructureTree(root, previews, player, dirtyTiles);

            for (TileEntityLittleTiles te : dirtyTiles) {
                if (te != null && !te.isInvalid())
                    te.updateTiles();
            }

            if (location.worldUUID != null) {
                EntityAnimation animation = WorldAnimationHandler.findAnimation(false, location.worldUUID);
                if (animation != null) {
                    Entity rootEntity = animation.getAbsoluteParent();
                    if (rootEntity instanceof EntityAnimation)
                        animation = (EntityAnimation) rootEntity;

                    if (updatedRoot instanceof StructureLittleVecXRotated) {
                        ((StructureLittleVecXRotated) updatedRoot).refreshPlacedAnimation(animation);
                        PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationControllerPacket(animation), animation, null);
                    }

                    animation.onUpdateForReal();
                }
            }

            LittleVecXAnimationSyncHelper.syncChangedTiles(updatedRoot, dirtyTiles);
        } catch (LittleActionException e) {
            // Ignore stale selections or temporarily disconnected child links.
        }
    }

    private static LittleStructure applyPreviewTree(LittleStructure structure, LittlePreviews previews, boolean root, Set<TileEntityLittleTiles> dirtyTiles)
            throws CorruptedConnectionException, NotYetConnectedException {
        if (previews == null || structure == null)
            return structure;

        if (previews.hasStructure()) {
            if (root && !structure.type.id.equals(previews.getStructureId()))
                return structure;

            TileEntityLittleTiles te = structure.mainBlock.getTe();
            final com.creativemd.littletiles.common.tile.parent.IStructureTileList mainBlock = structure.mainBlock;
            te.updateTilesSecretly((interactor) -> interactor.get(mainBlock).setStructureNBT(previews.structureNBT.copy()));
            dirtyTiles.add(te);
            structure = structure.mainBlock.getStructure();
        }

        int childCount = Math.min(previews.childrenCount(), structure.countChildren());
        for (int i = 0; i < childCount; i++) {
            LittlePreviews childPreview = previews.getChild(i);
            if (childPreview == null || !childPreview.hasStructureIncludeChildren())
                continue;

            LittleStructure childStructure = structure.getChild(i).getStructure();
            applyPreviewTree(childStructure, childPreview, false, dirtyTiles);
        }

        return structure;
    }

    private static LittleStructure replaceStructureTree(LittleStructure structure, LittlePreviews previews, EntityPlayer player, Set<TileEntityLittleTiles> dirtyTiles)
            throws CorruptedConnectionException, NotYetConnectedException, LittleActionException {
        if (structure == null || previews == null || !previews.hasStructure())
            return structure;

        collectTouchedTiles(structure, dirtyTiles);

        World world = structure.getWorld();
        BlockPos pos = structure.getPos();
        LittlePreviews originalPreviews = structure.getAbsolutePreviews(pos);

        LittleNeighborUpdateCollector neighbor = new LittleNeighborUpdateCollector(world);
        structure.removeStructure(neighbor);
        neighbor.process();

        Placement placement = new Placement(player, PlacementHelper.getAbsolutePreviews(world, previews.copy(), pos, PlacementMode.all));
        PlacementResult result = placement.tryPlace();
        if (result == null) {
            Placement restore = new Placement(player, PlacementHelper.getAbsolutePreviews(world, originalPreviews, pos, PlacementMode.all));
            PlacementResult restored = restore.tryPlace();
            if (restored != null)
                dirtyTiles.addAll(restored.tileEntities);
            return structure;
        }

        dirtyTiles.addAll(result.tileEntities);
        if (result.parentStructure != null)
            return result.parentStructure;

        LittleStructure replaced = findStructureInTouchedTiles(result, previews.getStructureId());
        return replaced != null ? replaced : structure;
    }

    private static void collectTouchedTiles(LittleStructure structure, Set<TileEntityLittleTiles> dirtyTiles) throws CorruptedConnectionException, NotYetConnectedException {
        for (Entry<BlockPos, ArrayList<IStructureTileList>> entry : structure.collectAllBlocksListSameWorld().entrySet()) {
            if (entry.getValue().isEmpty())
                continue;
            TileEntityLittleTiles te = entry.getValue().get(0).getTe();
            if (te != null && !te.isInvalid())
                dirtyTiles.add(te);
        }
    }

    private static LittleStructure findStructureInTouchedTiles(PlacementResult result, String structureId) {
        if (result == null || structureId == null)
            return null;

        for (TileEntityLittleTiles te : result.tileEntities) {
            if (te == null || te.isInvalid())
                continue;
            for (IStructureTileList list : te.structures()) {
                if (list == null)
                    continue;
                try {
                    LittleStructure candidate = list.getStructure();
                    if (candidate != null && structureId.equals(candidate.type.id))
                        return candidate;
                } catch (CorruptedConnectionException | NotYetConnectedException e) {
                    // Ignore partially connected structures while the replacement settles.
                }
            }
        }
        return null;
    }
}
