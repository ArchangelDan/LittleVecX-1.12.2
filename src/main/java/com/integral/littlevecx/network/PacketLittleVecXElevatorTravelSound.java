package com.integral.littlevecx.network;

import java.util.UUID;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.integral.littlevecx.client.LittleVecXElevatorSoundClientHandler;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

public class PacketLittleVecXElevatorTravelSound extends CreativeCorePacket {

    private UUID animationId;
    private String soundId;
    private float volume;
    private float pitch;
    private int startFloor;
    private int targetFloor;
    private int floorCount;
    private int travelTicks;
    private boolean upwards;

    public PacketLittleVecXElevatorTravelSound() {}

    public PacketLittleVecXElevatorTravelSound(UUID animationId, String soundId, float volume, float pitch, int startFloor,
            int targetFloor, int floorCount, int travelTicks, boolean upwards) {
        this.animationId = animationId;
        this.soundId = soundId;
        this.volume = volume;
        this.pitch = pitch;
        this.startFloor = startFloor;
        this.targetFloor = targetFloor;
        this.floorCount = floorCount;
        this.travelTicks = travelTicks;
        this.upwards = upwards;
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        writeString(buf, animationId.toString());
        writeString(buf, soundId == null ? "" : soundId);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
        buf.writeInt(startFloor);
        buf.writeInt(targetFloor);
        buf.writeInt(floorCount);
        buf.writeInt(travelTicks);
        buf.writeBoolean(upwards);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        animationId = UUID.fromString(readString(buf));
        soundId = readString(buf);
        volume = buf.readFloat();
        pitch = buf.readFloat();
        startFloor = buf.readInt();
        targetFloor = buf.readInt();
        floorCount = buf.readInt();
        travelTicks = buf.readInt();
        upwards = buf.readBoolean();
    }

    @Override
    public void executeClient(EntityPlayer player) {
        if (animationId == null || soundId == null || soundId.isEmpty())
            return;
        LittleVecXElevatorSoundClientHandler.startTravelSound(animationId, soundId, volume, pitch, startFloor, targetFloor,
                floorCount, travelTicks, upwards);
    }

    @Override
    public void executeServer(EntityPlayer player) {}
}
