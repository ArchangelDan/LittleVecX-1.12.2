package com.integral.littlevecx.client;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.integral.littlevecx.client.sound.LittleVecXElevatorTravelSound;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleVecXElevatorSoundClientHandler {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Map<UUID, LittleVecXElevatorTravelSound> ACTIVE_SOUNDS = new LinkedHashMap<>();

    private static boolean registered;

    public static void register() {
        if (registered)
            return;
        MinecraftForge.EVENT_BUS.register(new LittleVecXElevatorSoundClientHandler());
        registered = true;
    }

    public static void startTravelSound(UUID animationId, String soundId, float volume, float pitch, int startFloor, int targetFloor,
            int floorCount, int travelTicks, boolean upwards) {
        if (animationId == null || soundId == null || soundId.isEmpty())
            return;

        LittleVecXElevatorTravelSound existing = ACTIVE_SOUNDS.remove(animationId);
        if (existing != null)
            existing.stop();

        ACTIVE_SOUNDS.put(animationId, new LittleVecXElevatorTravelSound(animationId, soundId, volume, pitch, startFloor,
                targetFloor, floorCount, travelTicks, upwards));
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END)
            return;

        if (MC.world == null || MC.player == null) {
            stopAll();
            return;
        }

        Iterator<Map.Entry<UUID, LittleVecXElevatorTravelSound>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, LittleVecXElevatorTravelSound> entry = iterator.next();
            LittleVecXElevatorTravelSound sound = entry.getValue();
            if (sound == null || sound.tick()) {
                if (sound != null)
                    sound.stop();
                iterator.remove();
            }
        }
    }

    private static void stopAll() {
        if (ACTIVE_SOUNDS.isEmpty())
            return;

        for (LittleVecXElevatorTravelSound sound : ACTIVE_SOUNDS.values())
            if (sound != null)
                sound.stop();
        ACTIVE_SOUNDS.clear();
    }
}
