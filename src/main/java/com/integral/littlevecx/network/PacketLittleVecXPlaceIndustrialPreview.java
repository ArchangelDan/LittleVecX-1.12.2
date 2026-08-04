package com.integral.littlevecx.network;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.action.block.LittleActionPlaceStack;
import com.creativemd.littletiles.common.tile.math.vec.LittleAbsoluteVec;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.util.place.PlacementMode;
import com.creativemd.littletiles.common.util.place.PlacementPosition;
import com.integral.littlevecx.item.ItemLittleVecXIndustrialTool;
import com.integral.littlevecx.selection.IndustrialSelectionRegion;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

public class PacketLittleVecXPlaceIndustrialPreview extends CreativeCorePacket {

    private final List<IndustrialSelectionRegion> regions = new ArrayList<>();
    private PlacementPosition position;
    private boolean centered;
    private boolean fixed;
    private PlacementMode mode;

    public PacketLittleVecXPlaceIndustrialPreview(List<IndustrialSelectionRegion> regions, PlacementPosition position, boolean centered, boolean fixed, PlacementMode mode) {
        if (regions != null)
            this.regions.addAll(regions);
        this.position = position;
        this.centered = centered;
        this.fixed = fixed;
        this.mode = mode;
    }

    public PacketLittleVecXPlaceIndustrialPreview() {}

    @Override
    public void writeBytes(ByteBuf buf) {
        buf.writeInt(regions.size());
        for (IndustrialSelectionRegion region : regions) {
            LittleAction.writeLittlePos(region.first, buf);
            LittleAction.writeLittlePos(region.second, buf);
            writeFacing(buf, region.facing);
        }
        position.writeToBytes(buf);
        buf.writeBoolean(centered);
        buf.writeBoolean(fixed);
        LittleAction.writePlacementMode(mode, buf);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        regions.clear();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            LittleAbsoluteVec first = LittleAction.readLittlePos(buf);
            LittleAbsoluteVec second = LittleAction.readLittlePos(buf);
            regions.add(new IndustrialSelectionRegion(first, second, readFacing(buf)));
        }
        position = PlacementPosition.readFromBytes(buf);
        centered = buf.readBoolean();
        fixed = buf.readBoolean();
        mode = LittleAction.readPlacementMode(buf);
    }

    @Override
    public void executeClient(EntityPlayer player) {}

    @Override
    public void executeServer(EntityPlayer player) {
        if (player == null || position == null || regions.isEmpty())
            return;

        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemLittleVecXIndustrialTool))
            return;

        ItemLittleVecXIndustrialTool.ensureIndustrialMode(stack);
        try {
            LittlePreviews previews = ItemLittleVecXIndustrialTool.INDUSTRIAL_SELECTION_MODE.getPreviews(player.world, player, regions, true, true, true, false);
            if (previews == null || previews.isEmpty()) {
                player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.industrial.server_place_empty"), true);
                return;
            }

            PlacementMode safeMode = mode != null ? mode.place() : ((ItemLittleVecXIndustrialTool) stack.getItem()).getPlacementMode(stack).place();
            boolean placed = new LittleActionPlaceStack(previews, position, centered, fixed, safeMode).activateServer(player);
            player.sendStatusMessage(new TextComponentTranslation(placed ? "message.littlevecx.industrial.server_place_done"
                    : "message.littlevecx.industrial.server_place_failed"), true);
        } catch (LittleActionException e) {
            player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.industrial.server_place_failed"), true);
        }
    }
}
