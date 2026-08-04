package com.integral.littlevecx;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

public final class LittleVecXConfig {

    public static final int DEFAULT_MULTI_ANIMATION_SIGNAL_COUNT = 8;
    public static final int MAX_MULTI_ANIMATION_SIGNAL_COUNT = 200;
    public static final int DEFAULT_ELEVATOR_SIGNAL_COUNT = 16;
    public static final int MAX_ELEVATOR_SIGNAL_COUNT = 200;
    public static final String[] DEFAULT_WALLPAPER_ALLOWED_BLOCKS = {
            "example_mod:wallpaper",
            "example_mod:wallpaper_1",
            "example_mod:wallpaper_2"
    };
    public static final String[] DEFAULT_WALLPAPER_ALLOWED_MOD_IDS = { "example_mod" };
    public static final String[] DEFAULT_WALLPAPER_BLOCK_BLACKLIST = { "minecraft:grass" };
    public static final boolean DEFAULT_WALLPAPER_USE_BLACKLIST_MODE = true;

    private static final String CATEGORY_ANIMATIONS = "animations";
    private static final String CATEGORY_WALLPAPER = "wallpaper";
    private static final String CATEGORY_INDUSTRIAL = "industrial";
    private static final String CATEGORY_DEBUG = "debug";
    private static Configuration config;

    public static int multiAnimationSignalCount = DEFAULT_MULTI_ANIMATION_SIGNAL_COUNT;
    public static int elevatorSignalCount = DEFAULT_ELEVATOR_SIGNAL_COUNT;
    public static String[] wallpaperAllowedBlocks = DEFAULT_WALLPAPER_ALLOWED_BLOCKS.clone();
    public static String[] wallpaperAllowedModIds = DEFAULT_WALLPAPER_ALLOWED_MOD_IDS.clone();
    public static String[] wallpaperBlockedBlocks = DEFAULT_WALLPAPER_BLOCK_BLACKLIST.clone();
    public static boolean wallpaperUseBlacklistMode = DEFAULT_WALLPAPER_USE_BLACKLIST_MODE;
    public static int screwdriverQueuedBlockThreshold = 32;
    public static int screwdriverQueuedBoxThreshold = 256;
    public static int screwdriverBlocksPerTick = 8;
    public static int screwdriverBoxesPerTick = 256;
    public static int screwdriverBatchesPerTick = 3;
    public static int screwdriverCombineChangedTileLimit = 512;
    /** Opt-in diagnostic output for animation, elevator and selection troubleshooting. */
    public static boolean enableVerboseLogging;

    private static final Set<String> wallpaperAllowedBlockSet = new LinkedHashSet<>();
    private static final Set<String> wallpaperAllowedModIdSet = new LinkedHashSet<>();
    private static final Set<String> wallpaperBlockedBlockSet = new LinkedHashSet<>();

    private LittleVecXConfig() {
    }

    /**
     * Reads the pre-CreativeCore Forge config once so existing installations keep
     * their values when they are migrated to {@code littlevecx.json}.
     */
    public static void loadLegacy(File file) {
        if (config == null)
            config = new Configuration(file);

        sync();
    }

    private static void sync() {
        try {
            config.load();
            multiAnimationSignalCount = config.getInt(
                    "multiAnimationSignalCount",
                    CATEGORY_ANIMATIONS,
                    DEFAULT_MULTI_ANIMATION_SIGNAL_COUNT,
                    1,
                    MAX_MULTI_ANIMATION_SIGNAL_COUNT,
                    "Number of fixed signal ports for multi-animation structures. Requires game restart."
            );
            elevatorSignalCount = config.getInt(
                    "elevatorSignalCount",
                    CATEGORY_ANIMATIONS,
                    DEFAULT_ELEVATOR_SIGNAL_COUNT,
                    3,
                    MAX_ELEVATOR_SIGNAL_COUNT,
                    "Number of fixed signal ports for elevator structures. Requires game restart."
            );
            wallpaperAllowedBlocks = config.getStringList(
                    "allowedBlocks",
                    CATEGORY_WALLPAPER,
                    DEFAULT_WALLPAPER_ALLOWED_BLOCKS,
                    "Exact block registry names used when blacklist mode is false. Example: example_mod:wallpaper_panel"
            );
            wallpaperAllowedModIds = config.getStringList(
                    "allowedModIds",
                    CATEGORY_WALLPAPER,
                    DEFAULT_WALLPAPER_ALLOWED_MOD_IDS,
                    "Exact mod ids used when blacklist mode is false. Example: example_mod"
            );
            wallpaperUseBlacklistMode = config.getBoolean(
                    "useBlacklistMode",
                    CATEGORY_WALLPAPER,
                    DEFAULT_WALLPAPER_USE_BLACKLIST_MODE,
                    "When true, blacklist mode is active and the allowed lists are ignored."
            );
            wallpaperBlockedBlocks = config.getStringList(
                    "blockedBlocks",
                    CATEGORY_WALLPAPER,
                    DEFAULT_WALLPAPER_BLOCK_BLACKLIST,
                    "Exact block registry names rejected when blacklist mode is true. Example: minecraft:grass"
            );
            screwdriverQueuedBlockThreshold = config.getInt(
                    "screwdriverQueuedBlockThreshold",
                    CATEGORY_INDUSTRIAL,
                    32,
                    1,
                    4096,
                    "Block-position count after which industrial screwdriver replacement is queued over multiple server ticks."
            );
            screwdriverBlocksPerTick = config.getInt(
                    "screwdriverBlocksPerTick",
                    CATEGORY_INDUSTRIAL,
                    8,
                    1,
                    256,
                    "How many block positions one queued industrial screwdriver job may process per tick."
            );
            screwdriverQueuedBoxThreshold = config.getInt(
                    "screwdriverQueuedBoxThreshold",
                    CATEGORY_INDUSTRIAL,
                    256,
                    1,
                    100000,
                    "Box count after which industrial screwdriver replacement is queued even if it touches only a few block positions."
            );
            screwdriverBoxesPerTick = config.getInt(
                    "screwdriverBoxesPerTick",
                    CATEGORY_INDUSTRIAL,
                    256,
                    1,
                    100000,
                    "How many selected/replacement boxes one queued industrial screwdriver job may process per tick."
            );
            screwdriverBatchesPerTick = config.getInt(
                    "screwdriverBatchesPerTick",
                    CATEGORY_INDUSTRIAL,
                    2,
                    1,
                    16,
                    "How many queued industrial screwdriver batches may be processed per server tick. Higher values are faster but can cause short freezes on very heavy selections."
            );
            screwdriverCombineChangedTileLimit = config.getInt(
                    "screwdriverCombineChangedTileLimit",
                    CATEGORY_INDUSTRIAL,
                    512,
                    0,
                    100000,
                    "Maximum changed tile pieces after which replace-only skips expensive LittleTiles combineTiles. 0 disables combining for this action."
            );
            enableVerboseLogging = config.getBoolean(
                    "enableVerboseLogging",
                    CATEGORY_DEBUG,
                    false,
                    "Writes detailed LittleVecX diagnostic logs for animations, elevators, selections and network actions. Keep disabled during normal play."
            );
            rebuildWallpaperCaches();
        } finally {
            if (config.hasChanged())
                config.save();
        }
    }

    private static void rebuildWallpaperCaches() {
        wallpaperAllowedBlockSet.clear();
        for (String blockId : wallpaperAllowedBlocks) {
            String normalized = normalize(blockId);
            if (!normalized.isEmpty())
                wallpaperAllowedBlockSet.add(normalized);
        }

        wallpaperAllowedModIdSet.clear();
        for (String modId : wallpaperAllowedModIds) {
            String normalized = normalize(modId);
            if (!normalized.isEmpty())
                wallpaperAllowedModIdSet.add(normalized);
        }

        wallpaperBlockedBlockSet.clear();
        for (String blockId : wallpaperBlockedBlocks) {
            String normalized = normalize(blockId);
            if (!normalized.isEmpty())
                wallpaperBlockedBlockSet.add(normalized);
        }
    }

    static void copyToCreativeConfig(LittleVecXCreativeConfig target) {
        target.animations.multiAnimationSignalCount = multiAnimationSignalCount;
        target.animations.elevatorSignalCount = elevatorSignalCount;
        target.wallpaper.allowedBlocks = copyToList(wallpaperAllowedBlocks);
        target.wallpaper.allowedModIds = copyToList(wallpaperAllowedModIds);
        target.wallpaper.blockedBlocks = copyToList(wallpaperBlockedBlocks);
        target.wallpaper.useBlacklistMode = wallpaperUseBlacklistMode;
        target.industrial.screwdriverQueuedBlockThreshold = screwdriverQueuedBlockThreshold;
        target.industrial.screwdriverBlocksPerTick = screwdriverBlocksPerTick;
        target.industrial.screwdriverQueuedBoxThreshold = screwdriverQueuedBoxThreshold;
        target.industrial.screwdriverBoxesPerTick = screwdriverBoxesPerTick;
        target.industrial.screwdriverBatchesPerTick = screwdriverBatchesPerTick;
        target.industrial.screwdriverCombineChangedTileLimit = screwdriverCombineChangedTileLimit;
        target.debug.enableVerboseLogging = enableVerboseLogging;
    }

    static void applyCreativeConfig(LittleVecXCreativeConfig source) {
        multiAnimationSignalCount = clamp(source.animations.multiAnimationSignalCount, 1, MAX_MULTI_ANIMATION_SIGNAL_COUNT);
        elevatorSignalCount = clamp(source.animations.elevatorSignalCount, 3, MAX_ELEVATOR_SIGNAL_COUNT);
        wallpaperAllowedBlocks = copy(source.wallpaper.allowedBlocks);
        wallpaperAllowedModIds = copy(source.wallpaper.allowedModIds);
        wallpaperBlockedBlocks = copy(source.wallpaper.blockedBlocks);
        wallpaperUseBlacklistMode = source.wallpaper.useBlacklistMode;
        screwdriverQueuedBlockThreshold = clamp(source.industrial.screwdriverQueuedBlockThreshold, 1, 4096);
        screwdriverBlocksPerTick = clamp(source.industrial.screwdriverBlocksPerTick, 1, 256);
        screwdriverQueuedBoxThreshold = clamp(source.industrial.screwdriverQueuedBoxThreshold, 1, 100000);
        screwdriverBoxesPerTick = clamp(source.industrial.screwdriverBoxesPerTick, 1, 100000);
        screwdriverBatchesPerTick = clamp(source.industrial.screwdriverBatchesPerTick, 1, 16);
        screwdriverCombineChangedTileLimit = clamp(source.industrial.screwdriverCombineChangedTileLimit, 0, 100000);
        enableVerboseLogging = source.debug.enableVerboseLogging;
        rebuildWallpaperCaches();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String[] copy(String[] values) {
        return values == null ? new String[0] : values.clone();
    }

    private static String[] copy(java.util.List<String> values) {
        return values == null ? new String[0] : values.toArray(new String[0]);
    }

    private static java.util.List<String> copyToList(String[] values) {
        return new java.util.ArrayList<>(java.util.Arrays.asList(copy(values)));
    }

    public static boolean isWallpaperBlockAllowed(ResourceLocation registryName) {
        return registryName != null && wallpaperAllowedBlockSet.contains(normalize(registryName.toString()));
    }

    public static boolean isWallpaperModAllowed(ResourceLocation registryName) {
        return registryName != null && wallpaperAllowedModIdSet.contains(normalize(registryName.getNamespace()));
    }

    public static boolean isWallpaperBlockBlocked(ResourceLocation registryName) {
        return registryName != null && wallpaperBlockedBlockSet.contains(normalize(registryName.toString()));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
