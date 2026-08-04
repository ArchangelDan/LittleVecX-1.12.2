package com.integral.littlevecx.network;

import java.util.UUID;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.integral.littlevecx.client.LittleVecXScrewdriverProgressClientHandler;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

public class PacketLittleVecXScrewdriverProgress extends CreativeCorePacket {

    private UUID jobId;
    private int processed;
    private int total;
    private boolean done;

    public PacketLittleVecXScrewdriverProgress(UUID jobId, int processed, int total, boolean done) {
        this.jobId = jobId;
        this.processed = processed;
        this.total = total;
        this.done = done;
    }

    public PacketLittleVecXScrewdriverProgress() {
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        buf.writeBoolean(jobId != null);
        if (jobId != null) {
            buf.writeLong(jobId.getMostSignificantBits());
            buf.writeLong(jobId.getLeastSignificantBits());
        }
        buf.writeInt(processed);
        buf.writeInt(total);
        buf.writeBoolean(done);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        if (buf.readBoolean())
            jobId = new UUID(buf.readLong(), buf.readLong());
        else
            jobId = null;
        processed = buf.readInt();
        total = buf.readInt();
        done = buf.readBoolean();
    }

    @Override
    public void executeClient(EntityPlayer player) {
        LittleVecXScrewdriverProgressClientHandler.update(jobId, processed, total, done);
    }

    @Override
    public void executeServer(EntityPlayer player) {
    }
}
