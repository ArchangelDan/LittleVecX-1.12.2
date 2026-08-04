package com.integral.littlevecx.network;

import java.util.LinkedHashSet;
import java.util.Set;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.connection.StructureChildConnection;
import com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException;
import com.creativemd.littletiles.common.structure.exception.NotYetConnectedException;
import com.creativemd.littletiles.common.structure.type.door.LittleDoor;
import com.creativemd.littletiles.common.structure.type.door.LittleDoor.DoorActivator;
import com.creativemd.littletiles.common.tile.math.location.StructureLocation;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

public class PacketLittleVecXDoorAction extends CreativeCorePacket {

    private StructureLocation location;
    private boolean open;

    public PacketLittleVecXDoorAction() {}

    public PacketLittleVecXDoorAction(StructureLocation location, boolean open) {
        this.location = location;
        this.open = open;
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        LittleAction.writeStructureLocation(location, buf);
        buf.writeBoolean(open);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        location = LittleAction.readStructureLocation(buf);
        open = buf.readBoolean();
    }

    @Override
    public void executeClient(EntityPlayer player) {}

    @Override
    public void executeServer(EntityPlayer player) {
        if (location == null)
            return;

        try {
            LittleStructure root = location.find(player.world);
            Set<LittleDoor> doors = new LinkedHashSet<>();
            collectDoors(root, doors);

            for (LittleDoor door : doors) {
                if (door == null || door.mainBlock == null || door.mainBlock.isRemoved())
                    continue;
                if (door.opened == open || door.isInMotion())
                    continue;
                try {
                    door.activate(DoorActivator.COMMAND, player, null);
                } catch (LittleActionException e) {
                    // Skip doors that cannot change right now without aborting the batch.
                }
            }
        } catch (LittleActionException e) {
            // Selected structure disappeared; ignore.
        }
    }

    private static void collectDoors(LittleStructure structure, Set<LittleDoor> doors) {
        if (structure == null)
            return;

        if (structure instanceof LittleDoor) {
            try {
                doors.add(((LittleDoor) structure).getParentDoor());
            } catch (CorruptedConnectionException | NotYetConnectedException e) {
                doors.add((LittleDoor) structure);
            }
        }

        for (StructureChildConnection child : structure.getChildren()) {
            try {
                collectDoors(child.getStructure(), doors);
            } catch (CorruptedConnectionException | NotYetConnectedException e) {
                // Ignore broken child links during bulk operations.
            }
        }
    }
}
