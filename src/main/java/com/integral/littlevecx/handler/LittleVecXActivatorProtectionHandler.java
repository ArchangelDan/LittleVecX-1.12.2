package com.integral.littlevecx.handler;

import com.integral.littlevecx.item.ItemLittleVecXDebugBlazeRod;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The Activator is an interaction tool, never a mining tool.  Cancel block
 * attacks on both sides so a left click cannot damage LittleTiles before the
 * server has a chance to reject the break.
 */
public final class LittleVecXActivatorProtectionHandler {

    public static final LittleVecXActivatorProtectionHandler INSTANCE = new LittleVecXActivatorProtectionHandler();

    private LittleVecXActivatorProtectionHandler() {
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null)
            return;

        ItemStack stack = player.getHeldItemMainhand();
        if (!stack.isEmpty() && stack.getItem() instanceof ItemLittleVecXDebugBlazeRod)
            event.setCanceled(true);
    }
}
