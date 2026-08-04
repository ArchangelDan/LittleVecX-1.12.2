package com.integral.littlevecx.network;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.tile.math.location.StructureLocation;
import com.integral.littlevecx.LittleVecXAnimationSyncHelper;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

public class PacketLittleVecXRefreshStructure extends CreativeCorePacket {

    private StructureLocation location;

    public PacketLittleVecXRefreshStructure() {}

    public PacketLittleVecXRefreshStructure(StructureLocation location) {
        this.location = location;
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        LittleAction.writeStructureLocation(location, buf);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        location = LittleAction.readStructureLocation(buf);
    }

    @Override
    public void executeClient(EntityPlayer player) {}

    @Override
    public void executeServer(EntityPlayer player) {
        if (location == null)
            return;

        try {
            LittleStructure structure = location.find(player.world);
            LittleVecXAnimationSyncHelper.refreshWholeStructure(structure);
        } catch (LittleActionException e) {
            // Ignore stale selections or temporarily disconnected child links.
        }
    }
}
