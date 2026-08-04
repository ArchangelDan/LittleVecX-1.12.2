package com.integral.littlevecx.action;

import java.util.UUID;

import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.tile.math.box.LittleAbsoluteBox;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing.Axis;

public class LittleVecXActionRestoreScrewdriverSnapshot extends LittleAction {

    private UUID jobId;
    private boolean restoreAfter;

    public LittleVecXActionRestoreScrewdriverSnapshot(UUID jobId, boolean restoreAfter) {
        this.jobId = jobId;
        this.restoreAfter = restoreAfter;
    }

    public LittleVecXActionRestoreScrewdriverSnapshot() {
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        buf.writeBoolean(jobId != null);
        if (jobId != null) {
            buf.writeLong(jobId.getMostSignificantBits());
            buf.writeLong(jobId.getLeastSignificantBits());
        }
        buf.writeBoolean(restoreAfter);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        if (buf.readBoolean())
            jobId = new UUID(buf.readLong(), buf.readLong());
        else
            jobId = null;
        restoreAfter = buf.readBoolean();
    }

    @Override
    protected boolean action(EntityPlayer player) throws LittleActionException {
        if (player == null || jobId == null)
            return false;
        if (player.world.isRemote)
            return true;
        return LittleVecXQueuedActionHandler.restoreScrewdriverSnapshot(jobId, restoreAfter, player);
    }

    @Override
    public boolean canBeReverted() {
        return true;
    }

    @Override
    public LittleAction revert(EntityPlayer player) throws LittleActionException {
        return new LittleVecXActionRestoreScrewdriverSnapshot(jobId, !restoreAfter);
    }

    @Override
    public LittleAction flip(Axis axis, LittleAbsoluteBox box) {
        return new LittleVecXActionRestoreScrewdriverSnapshot(jobId, restoreAfter);
    }
}
