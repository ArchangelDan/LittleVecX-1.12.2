package com.integral.littlevecx.network;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.integral.littlevecx.item.ItemLittleVecXIndustrialTool;
import com.integral.littlevecx.selection.IndustrialSelectionRegion;
import com.integral.littlevecx.selection.IndustrialSelectionMode;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.List;

public class PacketLittleVecXIndustrialSelection extends CreativeCorePacket {

    private final List<IndustrialSelectionRegion> regions = new ArrayList<>();
    private String selectionStackId = "";

    public PacketLittleVecXIndustrialSelection(List<IndustrialSelectionRegion> regions) {
        this(regions, "");
    }

    public PacketLittleVecXIndustrialSelection(List<IndustrialSelectionRegion> regions, String selectionStackId) {
        if (regions != null)
            this.regions.addAll(regions);
        this.selectionStackId = selectionStackId == null ? "" : selectionStackId;
    }

    public PacketLittleVecXIndustrialSelection() {}

    @Override
    public void writeBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, selectionStackId);
        buf.writeInt(regions.size());
        for (IndustrialSelectionRegion region : regions) {
            writeRegion(buf, region);
        }
    }

    @Override
    public void readBytes(ByteBuf buf) {
        regions.clear();
        selectionStackId = ByteBufUtils.readUTF8String(buf);
        int count = buf.readInt();
        for (int i = 0; i < count; i++)
            regions.add(readRegion(buf));
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
        ItemLittleVecXIndustrialTool.setSelectionStackId(stack, selectionStackId);
        IndustrialSelectionMode.setCurrentRegions(stack, regions);
    }

    private void writeRegion(ByteBuf buf, IndustrialSelectionRegion region) {
        com.creativemd.littletiles.common.action.LittleAction.writeLittlePos(region.first, buf);
        com.creativemd.littletiles.common.action.LittleAction.writeLittlePos(region.second, buf);
        writeFacing(buf, region.facing);
    }

    private IndustrialSelectionRegion readRegion(ByteBuf buf) {
        com.creativemd.littletiles.common.tile.math.vec.LittleAbsoluteVec first = com.creativemd.littletiles.common.action.LittleAction.readLittlePos(buf);
        com.creativemd.littletiles.common.tile.math.vec.LittleAbsoluteVec second = com.creativemd.littletiles.common.action.LittleAction.readLittlePos(buf);
        return new IndustrialSelectionRegion(first, second, readFacing(buf));
    }
}
