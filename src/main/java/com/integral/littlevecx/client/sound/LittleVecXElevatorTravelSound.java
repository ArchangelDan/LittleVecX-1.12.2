package com.integral.littlevecx.client.sound;

import java.util.UUID;

import javax.annotation.Nullable;

import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.world.WorldAnimationHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleVecXElevatorTravelSound {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final int MAX_ANIMATION_WAIT_TICKS = 40;

    private final UUID animationId;
    private final String soundId;
    private final float volume;
    private final float pitch;
    private final int travelTicks;

    private int waitTicks;
    private long playbackStartNanoTime = -1L;
    @Nullable
    private ElevatorMovingSound activeSound;

    public LittleVecXElevatorTravelSound(UUID animationId, String soundId, float volume, float pitch, int startFloor, int targetFloor,
            int floorCount, int travelTicks, boolean upwards) {
        this.animationId = animationId;
        this.soundId = soundId;
        this.volume = Math.max(0.0F, volume);
        this.pitch = Math.max(0.01F, pitch);
        this.travelTicks = Math.max(1, travelTicks);
    }

    public boolean tick() {
        if (MC.world == null || MC.player == null) {
            stop();
            return true;
        }

        if (activeSound != null)
            return tickActiveSound();

        EntityAnimation animation = WorldAnimationHandler.findAnimation(true, animationId);
        if (animation == null) {
            waitTicks++;
            return waitTicks > MAX_ANIMATION_WAIT_TICKS;
        }

        SoundEvent soundEvent = resolveSoundEvent(soundId);
        if (soundEvent == null)
            return true;

        activeSound = new ElevatorMovingSound(animationId, soundEvent, volume, pitch, travelTicks);
        MC.getSoundHandler().playSound(activeSound);
        playbackStartNanoTime = System.nanoTime();
        return false;
    }

    public void stop() {
        if (activeSound != null) {
            MC.getSoundHandler().stopSound(activeSound);
            activeSound.stopNow();
            activeSound = null;
        }
        playbackStartNanoTime = -1L;
    }

    private boolean tickActiveSound() {
        if (activeSound == null)
            return true;

        if (activeSound.isDone())
            return true;

        if (playbackStartNanoTime > 0L) {
            long elapsedMs = (System.nanoTime() - playbackStartNanoTime) / 1_000_000L;
            if (elapsedMs >= Math.max(1L, travelTicks) * 50L) {
                stop();
                return true;
            }
        }

        return false;
    }

    @Nullable
    private static SoundEvent resolveSoundEvent(@Nullable String soundId) {
        if (soundId == null || soundId.isEmpty())
            return null;
        try {
            return SoundEvent.REGISTRY.getObject(new ResourceLocation(soundId));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @SideOnly(Side.CLIENT)
    private static class ElevatorMovingSound extends MovingSound {

        private final UUID animationId;
        private final long durationMs;
        private final long startNanoTime;

        protected ElevatorMovingSound(UUID animationId, SoundEvent soundEvent, float volume, float pitch, int travelTicks) {
            super(soundEvent, SoundCategory.NEUTRAL);
            this.animationId = animationId;
            this.volume = volume;
            this.pitch = pitch;
            this.repeat = false;
            this.repeatDelay = 0;
            this.attenuationType = ISound.AttenuationType.LINEAR;
            this.durationMs = Math.max(1L, travelTicks) * 50L;
            this.startNanoTime = System.nanoTime();

            EntityAnimation animation = WorldAnimationHandler.findAnimation(true, animationId);
            if (animation != null) {
                this.xPosF = (float) animation.posX;
                this.yPosF = (float) animation.posY;
                this.zPosF = (float) animation.posZ;
            }
        }

        @Override
        public void update() {
            if (MC.world == null || MC.player == null) {
                donePlaying = true;
                return;
            }

            EntityAnimation animation = WorldAnimationHandler.findAnimation(true, animationId);
            if (animation == null) {
                donePlaying = true;
                return;
            }

            xPosF = (float) animation.posX;
            yPosF = (float) animation.posY;
            zPosF = (float) animation.posZ;

            long elapsedMs = (System.nanoTime() - startNanoTime) / 1_000_000L;
            if (elapsedMs >= durationMs)
                donePlaying = true;
        }

        public boolean isDone() {
            return donePlaying;
        }

        public void stopNow() {
            donePlaying = true;
        }
    }
}
