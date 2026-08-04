package com.integral.littlevecx.network;

import java.util.UUID;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.packet.LittleAnimationControllerPacket;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.tile.math.location.StructureLocation;
import com.creativemd.littletiles.common.world.WorldAnimationHandler;
import com.integral.littlevecx.LittleVecXStaticRotationController;
import com.integral.littlevecx.StructureLittleVecXRotated;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentTranslation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PacketLittleVecXMoveStructure extends CreativeCorePacket {

    private static final Logger LOGGER = LogManager.getLogger("LittleVecX");

    private UUID animationId;
    private StructureLocation location;
    private double offX;
    private double offY;
    private double offZ;

    public PacketLittleVecXMoveStructure() {}

    public PacketLittleVecXMoveStructure(UUID animationId, StructureLocation location, double offX, double offY, double offZ) {
        this.animationId = animationId;
        this.location = location;
        this.offX = offX;
        this.offY = offY;
        this.offZ = offZ;
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        writeString(buf, animationId.toString());
        LittleAction.writeStructureLocation(location, buf);
        buf.writeDouble(offX);
        buf.writeDouble(offY);
        buf.writeDouble(offZ);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        animationId = UUID.fromString(readString(buf));
        location = LittleAction.readStructureLocation(buf);
        offX = buf.readDouble();
        offY = buf.readDouble();
        offZ = buf.readDouble();
    }

    @Override
    public void executeClient(EntityPlayer player) {}

    @Override
    public void executeServer(EntityPlayer player) {
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX move packet: player={}, anim={}, location={}, off=({}, {}, {})",
                player != null ? player.getName() : "null",
                animationId,
                location != null ? (location.pos + " idx=" + location.index + " world=" + location.worldUUID) : "null",
                offX, offY, offZ);

        StructureLittleVecXRotated rotated = null;
        boolean foundByLocation = false;
        if (location != null) {
            try {
                LittleStructure structure = location.find(player.world);
                if (structure instanceof StructureLittleVecXRotated) {
                    rotated = (StructureLittleVecXRotated) structure;
                    foundByLocation = true;
                }
                com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX move location resolve: foundByLocation={}, structure={}",
                        foundByLocation,
                        structure != null ? structure.getClass().getName() : "null");
            } catch (LittleActionException e) {
                LOGGER.warn("LittleVecX move location resolve failed", e);
            }
        }

        EntityAnimation animation = WorldAnimationHandler.findAnimation(false, animationId);
        if (animation == null && player != null && player.world != null) {
            for (Entity entity : player.world.loadedEntityList) {
                if (entity instanceof EntityAnimation && animationId.equals(entity.getUniqueID())) {
                    animation = (EntityAnimation) entity;
                    break;
                }
            }
        }
        if (animation == null) {
            LOGGER.warn("LittleVecX move: animation not found on server for {}", animationId);
            if (player != null)
                player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.move.animation_not_found"), true);
            return;
        }

        Entity root = animation.getAbsoluteParent();
        if (root instanceof EntityAnimation)
            animation = (EntityAnimation) root;

        if (!(animation.controller instanceof LittleVecXStaticRotationController)) {
            LOGGER.warn("LittleVecX move: controller is not LittleVecXStaticRotationController, got {}",
                    animation.controller != null ? animation.controller.getClass().getName() : "null");
            if (player != null)
                player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.move.bad_controller"), true);
            return;
        }

        if (rotated == null && animation.structure instanceof StructureLittleVecXRotated)
            rotated = (StructureLittleVecXRotated) animation.structure;
        boolean savedOffset = rotated != null && rotated.applyPersistentPlacedOffset(animation, offX, offY, offZ);

        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX move apply: foundByLocation={}, rotatedResolved={}, savedOffset={}, animStructure={}, animPos={}",
                foundByLocation,
                rotated != null,
                savedOffset,
                animation.structure != null ? animation.structure.getClass().getName() : "null",
                animation.getPosition());

        LittleVecXStaticRotationController controller = (LittleVecXStaticRotationController) animation.controller;
        controller.setTransform(controller.getRotX(), controller.getRotY(), controller.getRotZ(), offX, offY, offZ);
        animation.updateTickState();
        animation.updateBoundingBox();
        animation.onUpdateForReal();

        if (animation.world != null) {
            if (animation.absolutePreviewPos != null)
                animation.world.markChunkDirty(animation.absolutePreviewPos, null);
            animation.world.markChunkDirty(animation.getPosition(), null);
        }

        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX move done: controllerOff=({}, {}, {})", controller.getOffX(), controller.getOffY(), controller.getOffZ());

        PacketHandler.sendPacketToTrackingPlayers(new LittleAnimationControllerPacket(animation), animation, null);
    }
}

