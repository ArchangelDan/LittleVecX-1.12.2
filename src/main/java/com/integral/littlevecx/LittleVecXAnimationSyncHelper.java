package com.integral.littlevecx;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.world.SubWorld;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException;
import com.creativemd.littletiles.common.structure.exception.NotYetConnectedException;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.tile.parent.IStructureTileList;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.integral.littlevecx.network.PacketLittleVecXFakeWorldBlocksRefresh;

import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public final class LittleVecXAnimationSyncHelper {

    private LittleVecXAnimationSyncHelper() {}

    public static void syncChangedTiles(LittleStructure structure, Iterable<TileEntityLittleTiles> tileEntities) {
        if (structure == null || !structure.hasWorld())
            return;

        World world = structure.getWorld();
        if (world == null || world.isRemote || !(world instanceof SubWorld))
            return;
        if (!(((SubWorld) world).parent instanceof EntityAnimation))
            return;

        EntityAnimation owner = (EntityAnimation) ((SubWorld) world).parent;
        Entity rootEntity = owner.getAbsoluteParent();
        EntityAnimation root = rootEntity instanceof EntityAnimation ? (EntityAnimation) rootEntity : owner;

        Set<TileEntityLittleTiles> unique = new LinkedHashSet<>();
        if (tileEntities != null) {
            for (TileEntityLittleTiles te : tileEntities) {
                if (te != null && !te.isInvalid())
                    unique.add(te);
            }
        }

        if (unique.isEmpty())
            return;

        List<TileEntityLittleTiles> changed = new ArrayList<>(unique);
        PacketHandler.sendPacketToTrackingPlayers(new PacketLittleVecXFakeWorldBlocksRefresh(world, changed), root, null);
    }

    public static void refreshWholeStructure(LittleStructure structure) {
        if (structure == null)
            return;

        Set<TileEntityLittleTiles> changed = new LinkedHashSet<>();
        try {
            for (Entry<BlockPos, ArrayList<IStructureTileList>> entry : structure.collectAllBlocksListSameWorld().entrySet()) {
                if (entry.getValue().isEmpty())
                    continue;
                TileEntityLittleTiles te = entry.getValue().get(0).getTe();
                if (te != null && !te.isInvalid())
                    changed.add(te);
            }
        } catch (CorruptedConnectionException | NotYetConnectedException e) {
            return;
        }

        syncChangedTiles(structure, changed);
    }
}
