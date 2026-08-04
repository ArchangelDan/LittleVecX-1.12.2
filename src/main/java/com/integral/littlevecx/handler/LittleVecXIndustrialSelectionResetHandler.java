package com.integral.littlevecx.handler;

import com.integral.littlevecx.item.ItemLittleVecXIndustrialTool;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

public class LittleVecXIndustrialSelectionResetHandler {

    public static final LittleVecXIndustrialSelectionResetHandler INSTANCE = new LittleVecXIndustrialSelectionResetHandler();

    private LittleVecXIndustrialSelectionResetHandler() {
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (player == null || player.world == null || player.world.isRemote)
            return;

        boolean changed = false;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!(stack.getItem() instanceof ItemLittleVecXIndustrialTool))
                continue;

            // Keep the actual selected regions. Clearing them on every join both broke
            // undo/redo and left LittleTiles' transient preview data behind as a white ghost.
            changed |= ItemLittleVecXIndustrialTool.clearInterruptedIndustrialPreview(stack);
        }

        if (changed)
            player.inventory.markDirty();
    }
}
