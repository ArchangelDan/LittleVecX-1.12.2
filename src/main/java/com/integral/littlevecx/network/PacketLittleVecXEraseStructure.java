package com.integral.littlevecx.network;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.packet.LittleAnimationDestroyPacket;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.tile.math.location.StructureLocation;
import com.creativemd.littletiles.common.world.LittleNeighborUpdateCollector;
import com.creativemd.littletiles.common.world.WorldAnimationHandler;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class PacketLittleVecXEraseStructure extends CreativeCorePacket {

    private StructureLocation location;

    public PacketLittleVecXEraseStructure() {}

    public PacketLittleVecXEraseStructure(StructureLocation location) {
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
            if (location.worldUUID != null) {
                EntityAnimation animation = WorldAnimationHandler.findAnimation(false, location.worldUUID);
                if (animation != null) {
                    Entity rootEntity = animation.getAbsoluteParent();
                    if (rootEntity instanceof EntityAnimation)
                        animation = (EntityAnimation) rootEntity;

                    if (animation.structure != null)
                        animation.structure.callStructureDestroyedToSameWorld();

                    animation.destroyAnimation();
                    PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationDestroyPacket(animation.getUniqueID(), false), animation, null);
                    return;
                }
            }

            LittleStructure structure = location.find(player.world);
            LittleStructure top = structure.findTopStructure();
            LittleNeighborUpdateCollector neighbor = new LittleNeighborUpdateCollector(top.getWorld());
            top.removeStructure(neighbor);
            neighbor.process();
        } catch (LittleActionException e) {
            // Ignore stale, broken, or already-removed selections.
        }
    }
}
