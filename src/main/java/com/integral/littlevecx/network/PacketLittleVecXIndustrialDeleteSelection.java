package com.integral.littlevecx.network;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxes;
import com.integral.littlevecx.action.LittleVecXIndustrialDestroyBoxes;
import com.integral.littlevecx.item.ItemLittleVecXIndustrialTool;
import com.integral.littlevecx.selection.IndustrialSelectionRegion;
import com.integral.littlevecx.selection.IndustrialSelectionMode;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.List;

public class PacketLittleVecXIndustrialDeleteSelection extends CreativeCorePacket {

    private final List<IndustrialSelectionRegion> regions = new ArrayList<>();

    public PacketLittleVecXIndustrialDeleteSelection(List<IndustrialSelectionRegion> regions) {
        if (regions != null)
            this.regions.addAll(regions);
    }

    public PacketLittleVecXIndustrialDeleteSelection() {}

    @Override
    public void writeBytes(ByteBuf buf) {
        buf.writeInt(regions.size());
        for (IndustrialSelectionRegion region : regions) {
            com.creativemd.littletiles.common.action.LittleAction.writeLittlePos(region.first, buf);
            com.creativemd.littletiles.common.action.LittleAction.writeLittlePos(region.second, buf);
            writeFacing(buf, region.facing);
        }
    }

    @Override
    public void readBytes(ByteBuf buf) {
        regions.clear();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            com.creativemd.littletiles.common.tile.math.vec.LittleAbsoluteVec first = com.creativemd.littletiles.common.action.LittleAction.readLittlePos(buf);
            com.creativemd.littletiles.common.tile.math.vec.LittleAbsoluteVec second = com.creativemd.littletiles.common.action.LittleAction.readLittlePos(buf);
            regions.add(new IndustrialSelectionRegion(first, second, readFacing(buf)));
        }
    }

    @Override
    public void executeClient(EntityPlayer player) {}

    @Override
    public void executeServer(EntityPlayer player) {
        if (player == null)
            return;

        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemLittleVecXIndustrialTool))
            return;

        ItemLittleVecXIndustrialTool.ensureIndustrialMode(stack);
        LittleBoxes boxes = regions.isEmpty() ? IndustrialSelectionMode.buildSavedOrCurrentBoxes(stack) : IndustrialSelectionMode.buildBoxes(regions);
        if (boxes.isEmpty()) {
            player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.industrial.no_selection"), true);
            return;
        }

        if (new LittleVecXIndustrialDestroyBoxes(boxes).activateServer(player))
            player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.industrial.deleted"), true);
    }
}
