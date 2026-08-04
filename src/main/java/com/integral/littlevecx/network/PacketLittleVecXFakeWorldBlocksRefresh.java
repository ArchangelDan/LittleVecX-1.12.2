package com.integral.littlevecx.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.creativecore.common.world.CreativeWorld;
import com.creativemd.creativecore.common.world.IOrientatedWorld;
import com.creativemd.littletiles.client.render.entity.LittleRenderChunk;
import com.creativemd.littletiles.client.render.world.RenderUtils;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.world.WorldAnimationHandler;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class PacketLittleVecXFakeWorldBlocksRefresh extends CreativeCorePacket {

    private static final java.lang.reflect.Field CHUNK_MODIFIED = ReflectionHelper.findField(LittleRenderChunk.class, "modified");
    private static final java.lang.reflect.Field CHUNK_COMPLETE = ReflectionHelper.findField(LittleRenderChunk.class, "complete");

    private List<BlockPos> positions;
    private List<IBlockState> states;
    private List<SPacketUpdateTileEntity> packets;
    private UUID uuid;

    public PacketLittleVecXFakeWorldBlocksRefresh() {}

    public PacketLittleVecXFakeWorldBlocksRefresh(World world, Iterable<? extends TileEntityLittleTiles> tileEntities) {
        positions = new ArrayList<>();
        states = new ArrayList<>();
        packets = new ArrayList<>();

        for (TileEntityLittleTiles te : tileEntities) {
            if (te == null || te.isInvalid())
                continue;
            positions.add(te.getPos());
            states.add(world.getBlockState(te.getPos()));
            packets.add(te.getUpdatePacket());
        }

        if (world instanceof CreativeWorld && ((CreativeWorld) world).parent != null)
            uuid = ((CreativeWorld) world).parent.getUniqueID();
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        buf.writeInt(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            writePos(buf, positions.get(i));
            writeState(buf, states.get(i));
            if (packets.get(i) != null) {
                buf.writeBoolean(true);
                writePacket(buf, packets.get(i));
            } else {
                buf.writeBoolean(false);
            }
        }

        if (uuid != null) {
            buf.writeBoolean(true);
            writeString(buf, uuid.toString());
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public void readBytes(ByteBuf buf) {
        int size = buf.readInt();
        positions = new ArrayList<>(size);
        states = new ArrayList<>(size);
        packets = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            positions.add(readPos(buf));
            states.add(readState(buf));
            if (buf.readBoolean())
                packets.add((SPacketUpdateTileEntity) readPacket(buf));
            else
                packets.add(null);
        }

        if (buf.readBoolean())
            uuid = UUID.fromString(readString(buf));
        else
            uuid = null;
    }

    @Override
    public void executeClient(EntityPlayer player) {
        if (uuid == null || positions == null || positions.isEmpty())
            return;

        EntityAnimation animation = WorldAnimationHandler.findAnimation(true, uuid);
        if (animation == null || animation.fakeWorld == null)
            return;

        World world = animation.fakeWorld;

        for (BlockPos pos : positions) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityLittleTiles) {
                TileEntityLittleTiles littleTe = (TileEntityLittleTiles) te;
                littleTe.render.getBufferCache().setEmpty();
                littleTe.render.getBoxCache().clear();
                littleTe.render.resetRenderingState();
            }
        }

        for (int i = 0; i < positions.size(); i++) {
            BlockPos pos = positions.get(i);
            world.setBlockState(pos, states.get(i), 3);
            TileEntity te = world.getTileEntity(pos);
            SPacketUpdateTileEntity packet = packets.get(i);
            if (packet != null && te != null)
                te.onDataPacket(((EntityPlayerSP) player).connection.getNetworkManager(), packet);

            markOwningChunkDirty(world, pos);
        }
    }

    private static void markOwningChunkDirty(World world, BlockPos pos) {
        if (!(world instanceof IOrientatedWorld))
            return;

        LittleRenderChunk chunk = RenderUtils.getRenderChunk((IOrientatedWorld) world, pos);
        if (chunk == null)
            return;

        try {
            CHUNK_MODIFIED.setBoolean(chunk, true);
            CHUNK_COMPLETE.setBoolean(chunk, false);
        } catch (IllegalAccessException ignored) {}
    }

    @Override
    public void executeServer(EntityPlayer player) {}
}
