package com.integral.littlevecx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.creativemd.creativecore.common.config.api.CreativeConfig;
import com.creativemd.creativecore.common.config.api.ICreativeConfig;

import net.minecraftforge.fml.relauncher.Side;

/**
 * CreativeCore-backed configuration shown and synchronized the same way as
 * LittleTiles' settings. Runtime users continue to read LittleVecXConfig.
 */
public class LittleVecXCreativeConfig implements ICreativeConfig {

    @CreativeConfig(name = "Animations", requiresRestart = true)
    public final Animations animations = new Animations();

    @CreativeConfig(name = "Wallpaper")
    public final Wallpaper wallpaper = new Wallpaper();

    @CreativeConfig(name = "Industrial screwdriver")
    public final Industrial industrial = new Industrial();

    @CreativeConfig(name = "Debug")
    public final Debug debug = new Debug();

    @Override
    public void configured() {
        LittleVecXConfig.applyCreativeConfig(this);
    }

    @Override
    public void configured(Side side) {
        configured();
    }

    public static class Animations {

        @CreativeConfig(name = "Multi-animation signal ports", requiresRestart = true)
        @CreativeConfig.IntRange(min = 1, max = LittleVecXConfig.MAX_MULTI_ANIMATION_SIGNAL_COUNT)
        public int multiAnimationSignalCount = LittleVecXConfig.DEFAULT_MULTI_ANIMATION_SIGNAL_COUNT;

        @CreativeConfig(name = "Elevator signal ports", requiresRestart = true)
        @CreativeConfig.IntRange(min = 3, max = LittleVecXConfig.MAX_ELEVATOR_SIGNAL_COUNT)
        public int elevatorSignalCount = LittleVecXConfig.DEFAULT_ELEVATOR_SIGNAL_COUNT;
    }

    public static class Wallpaper {

        @CreativeConfig(name = "Allowed block ids")
        public List<String> allowedBlocks = new ArrayList<>(Arrays.asList(LittleVecXConfig.DEFAULT_WALLPAPER_ALLOWED_BLOCKS));

        @CreativeConfig(name = "Allowed mod ids")
        public List<String> allowedModIds = new ArrayList<>(Arrays.asList(LittleVecXConfig.DEFAULT_WALLPAPER_ALLOWED_MOD_IDS));

        @CreativeConfig(name = "Blacklist mode")
        public boolean useBlacklistMode = LittleVecXConfig.DEFAULT_WALLPAPER_USE_BLACKLIST_MODE;

        @CreativeConfig(name = "Blocked block ids")
        public List<String> blockedBlocks = new ArrayList<>(Arrays.asList(LittleVecXConfig.DEFAULT_WALLPAPER_BLOCK_BLACKLIST));
    }

    public static class Industrial {

        @CreativeConfig(name = "Queue threshold (block positions)")
        @CreativeConfig.IntRange(min = 1, max = 4096)
        public int screwdriverQueuedBlockThreshold = 32;

        @CreativeConfig(name = "Queued block positions per tick")
        @CreativeConfig.IntRange(min = 1, max = 256)
        public int screwdriverBlocksPerTick = 8;

        @CreativeConfig(name = "Queue threshold (boxes)")
        @CreativeConfig.IntRange(min = 1, max = 100000)
        public int screwdriverQueuedBoxThreshold = 256;

        @CreativeConfig(name = "Queued boxes per tick")
        @CreativeConfig.IntRange(min = 1, max = 100000)
        public int screwdriverBoxesPerTick = 256;

        @CreativeConfig(name = "Queued batches per tick")
        @CreativeConfig.IntRange(min = 1, max = 16)
        public int screwdriverBatchesPerTick = 2;

        @CreativeConfig(name = "Combine changed-tile limit")
        @CreativeConfig.IntRange(min = 0, max = 100000)
        public int screwdriverCombineChangedTileLimit = 512;
    }

    public static class Debug {

        @CreativeConfig(name = "Detailed diagnostic logging")
        public boolean enableVerboseLogging = false;
    }
}
