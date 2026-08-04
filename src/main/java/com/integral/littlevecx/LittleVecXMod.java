package com.integral.littlevecx;

import java.io.File;

import com.creativemd.creativecore.CreativeCore;
import com.creativemd.creativecore.common.config.holder.CreativeConfigRegistry;
import com.creativemd.creativecore.common.gui.opener.GuiHandler;
import com.creativemd.creativecore.common.gui.opener.CustomGuiHandler;
import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.creativecore.common.gui.container.SubContainer;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.littletiles.common.entity.EntityAnimationController;
import com.creativemd.littletiles.common.structure.attribute.LittleStructureAttribute;
import com.creativemd.littletiles.common.structure.registry.LittleStructureRegistry;
import com.creativemd.littletiles.common.structure.signal.logic.SignalMode;
import com.creativemd.littletiles.common.tile.math.box.LittleVecXPillarFixBox;
import com.creativemd.littletiles.common.tile.math.box.LittleVecXSliceFixBox;
import com.creativemd.littletiles.common.tile.preview.LittlePreview;
import com.creativemd.littletiles.common.tile.registry.LittleTileRegistry;
import com.creativemd.littletiles.common.util.selection.selector.TileSelector;
import com.creativemd.littletiles.common.util.shape.ShapeRegistry;
import com.creativemd.littletiles.client.gui.handler.LittleStructureGuiHandler;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.integral.littlevecx.LittleVecXStaticRotationController;
import com.integral.littlevecx.StructureLittleVecXRotated;
import com.integral.littlevecx.StructureLittleVecXRotated.StructureLittleVecXRotatedParser;
import com.integral.littlevecx.animation.StructureLittleVecXMultiAnimation;
import com.integral.littlevecx.animation.StructureLittleVecXOverlayAnimation;
import com.integral.littlevecx.animation.StructureLittleVecXAdditiveAnimation;
import com.integral.littlevecx.animation.StructureLittleVecXElevator;
import com.integral.littlevecx.animation.LittleVecXCheckpointController;
import com.integral.littlevecx.animation.LittleVecXLoopDoorController;
import com.integral.littlevecx.animation.LittleVecXRetargetableDoorController;
import com.integral.littlevecx.animation.StructureLittleVecXLoopAnimation;
import com.integral.littlevecx.animation.StructureLittleVecXMultiAnimation.StructureLittleVecXMultiAnimationParser;
import com.integral.littlevecx.animation.StructureLittleVecXOverlayAnimation.StructureLittleVecXOverlayAnimationParser;
import com.integral.littlevecx.animation.StructureLittleVecXAdditiveAnimation.StructureLittleVecXAdditiveAnimationParser;
import com.integral.littlevecx.animation.StructureLittleVecXElevator.StructureLittleVecXElevatorParser;
import com.integral.littlevecx.animation.StructureLittleVecXLoopAnimation.StructureLittleVecXLoopAnimationParser;
import com.integral.littlevecx.client.LittleVecXFurnitureClientHandler;
import com.integral.littlevecx.client.LittleVecXElevatorSoundClientHandler;
import com.integral.littlevecx.client.LittleVecXActivatorHighlightHandler;
import com.integral.littlevecx.client.LittleVecXIndustrialSelectionHighlightHandler;
import com.integral.littlevecx.client.LittleVecXIndustrialToolClientHandler;
import com.integral.littlevecx.client.LittleVecXMoveClientHandler;
import com.integral.littlevecx.client.LittleVecXPlacementFeedbackClientHandler;
import com.integral.littlevecx.client.LittleVecXScrewdriverProgressClientHandler;
import com.integral.littlevecx.client.LittleVecXZoomHandler;
import com.integral.littlevecx.client.gui.SubGuiLittleVecXIndustrialScrewdriver;
import com.integral.littlevecx.client.overlay.LittleVecXSubGuiOverlayHandler;
import com.integral.littlevecx.action.LittleVecXActionReplaceBlockOnly;
import com.integral.littlevecx.action.LittleVecXActionQueuedScrewdriverReplace;
import com.integral.littlevecx.action.LittleVecXActionRestoreScrewdriverSnapshot;
import com.integral.littlevecx.action.LittleVecXQueuedActionHandler;
import com.integral.littlevecx.furniture.StructureLittleVecXFurniture;
import com.integral.littlevecx.furniture.StructureLittleVecXFurniture.StructureLittleVecXFurnitureParser;
import com.integral.littlevecx.furniture.StructureLittleVecXFurniture.StructureLittleVecXFurnitureType;
import com.integral.littlevecx.handler.LittleVecXIndustrialSelectionResetHandler;
import com.integral.littlevecx.handler.LittleVecXActivatorProtectionHandler;
import com.integral.littlevecx.network.PacketLittleVecXApplyStructurePreviews;
import com.integral.littlevecx.network.PacketLittleVecXDoorAction;
import com.integral.littlevecx.client.gui.SubGuiLittleVecXStorage;
import com.integral.littlevecx.network.PacketLittleVecXEraseStructure;
import com.integral.littlevecx.network.PacketLittleVecXElevatorTravelSound;
import com.integral.littlevecx.network.PacketLittleVecXFakeWorldBlocksRefresh;
import com.integral.littlevecx.network.PacketLittleVecXIndustrialDeleteSelection;
import com.integral.littlevecx.network.PacketLittleVecXIndustrialSelection;
import com.integral.littlevecx.network.PacketLittleVecXMoveStructure;
import com.integral.littlevecx.network.PacketLittleVecXPlaceIndustrialPreview;
import com.integral.littlevecx.network.PacketLittleVecXRefreshStructure;
import com.integral.littlevecx.network.PacketLittleVecXScrewdriverProgress;
import com.integral.littlevecx.mutator.StructureLittleVecXStateMutator;
import com.integral.littlevecx.mutator.StructureLittleVecXStateMutator.StructureLittleVecXStateMutatorParser;
import com.integral.littlevecx.mutator.StructureLittleVecXWallpaperMutator;
import com.integral.littlevecx.mutator.StructureLittleVecXWallpaperMutator.StructureLittleVecXWallpaperMutatorParser;
import com.integral.littlevecx.shape.LittleVecXIndustrialShape;
import com.integral.littlevecx.shape.LittleVecXShapePillarFix;
import com.integral.littlevecx.shape.LittleVecXShapeSliceFix;
import com.integral.littlevecx.screwdriver.LittleVecXColorSelector;
import com.integral.littlevecx.preview.LittleVecXPillarFixPreview;
import com.integral.littlevecx.preview.LittleVecXSliceFixPreview;
import com.integral.littlevecx.storage.StructureLittleVecXStorage;
import com.integral.littlevecx.storage.StructureLittleVecXStorage.StructureLittleVecXStorageParser;
import com.integral.littlevecx.storage.StructureLittleVecXStorage.StructureLittleVecXStorageType;
import com.integral.littlevecx.storage.SubContainerLittleVecXStorage;
import com.integral.littlevecx.tile.LittleVecXPillarFixTile;
import com.integral.littlevecx.tile.LittleVecXSliceFixTile;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

@Mod(modid = LittleVecXMod.MODID, name = LittleVecXMod.NAME, version = LittleVecXMod.VERSION, guiFactory = "com.integral.littlevecx.client.LittleVecXSettings", dependencies = "required-after:littletiles;required-after:creativecore")
public class LittleVecXMod {
    public static final String MODID = "littlevecx";
    public static final String NAME = "LittleVecX";
    public static final String VERSION = "@VERSION@";
    public static LittleVecXCreativeConfig CONFIG;
    // Future roadmap anchors live in com.integral.littlevecx.future.

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File legacyConfig = event.getSuggestedConfigurationFile();
        if (legacyConfig.exists())
            LittleVecXConfig.loadLegacy(legacyConfig);

        CONFIG = new LittleVecXCreativeConfig();
        LittleVecXConfig.copyToCreativeConfig(CONFIG);
        CreativeConfigRegistry.ROOT.registerValue(MODID, CONFIG);

        if (!CreativeCore.configHandler.modFileExist(MODID, Side.SERVER))
            CreativeCore.configHandler.save(MODID, Side.SERVER);
        CreativeConfigRegistry.load(MODID, Side.SERVER);

        CreativeCorePacket.registerPacket(PacketLittleVecXMoveStructure.class);
        CreativeCorePacket.registerPacket(PacketLittleVecXDoorAction.class);
        CreativeCorePacket.registerPacket(PacketLittleVecXApplyStructurePreviews.class);
        CreativeCorePacket.registerPacket(PacketLittleVecXEraseStructure.class);
        CreativeCorePacket.registerPacket(PacketLittleVecXFakeWorldBlocksRefresh.class);
        CreativeCorePacket.registerPacket(PacketLittleVecXRefreshStructure.class);
        CreativeCorePacket.registerPacket(PacketLittleVecXIndustrialSelection.class);
        CreativeCorePacket.registerPacket(PacketLittleVecXIndustrialDeleteSelection.class);
        CreativeCorePacket.registerPacket(PacketLittleVecXPlaceIndustrialPreview.class);
        CreativeCorePacket.registerPacket(PacketLittleVecXElevatorTravelSound.class);
        CreativeCorePacket.registerPacket(PacketLittleVecXScrewdriverProgress.class);
        CreativeCorePacket.registerPacket(LittleVecXActionReplaceBlockOnly.class);
        CreativeCorePacket.registerPacket(LittleVecXActionQueuedScrewdriverReplace.class);
        CreativeCorePacket.registerPacket(LittleVecXActionRestoreScrewdriverSnapshot.class);
        MinecraftForge.EVENT_BUS.register(LittleVecXQueuedActionHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(LittleVecXIndustrialSelectionResetHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(LittleVecXActivatorProtectionHandler.INSTANCE);
        TileSelector.registerType("littlevecx_color", LittleVecXColorSelector.class);
        ShapeRegistry.registerShape("industrial", new LittleVecXIndustrialShape(), ShapeRegistry.ShapeType.SELECTOR);
        ShapeRegistry.registerShape("slice_fix", new LittleVecXShapeSliceFix(), ShapeRegistry.ShapeType.SHAPE);
        ShapeRegistry.registerShape("pillar_fix", new LittleVecXShapePillarFix(), ShapeRegistry.ShapeType.SHAPE);
        LittlePreview.registerPreviewType("slice_fix_preview", LittleVecXSliceFixPreview.class);
        LittlePreview.registerPreviewType("pillar_fix_preview", LittleVecXPillarFixPreview.class);
        LittleTileRegistry.registerTileType(LittleVecXSliceFixTile.class, LittleVecXSliceFixTile.TILE_ID, LittleVecXSliceFixBox::hasMarker, true);
        LittleTileRegistry.registerTileType(LittleVecXPillarFixTile.class, LittleVecXPillarFixTile.TILE_ID, LittleVecXPillarFixBox::hasMarker, true);

        EntityAnimationController.registerControllerType(LittleVecXStaticRotationController.CONTROLLER_ID, LittleVecXStaticRotationController.class);
        EntityAnimationController.registerControllerType(LittleVecXCheckpointController.CONTROLLER_ID, LittleVecXCheckpointController.class);
        EntityAnimationController.registerControllerType(LittleVecXLoopDoorController.CONTROLLER_ID, LittleVecXLoopDoorController.class);
        EntityAnimationController.registerControllerType(LittleVecXRetargetableDoorController.CONTROLLER_ID, LittleVecXRetargetableDoorController.class);

        LittleStructureRegistry.registerStructureType(
                "rotation",
                MODID,
                StructureLittleVecXRotated.class,
                LittleStructureAttribute.NONE,
                StructureLittleVecXRotatedParser.class
        );

        LittleStructureRegistry.registerStructureType(
                "multi_animation",
                MODID,
                StructureLittleVecXMultiAnimation.class,
                LittleStructureAttribute.NONE,
                StructureLittleVecXMultiAnimationParser.class
        ).addOutput("state", 1, SignalMode.TOGGLE);

        for (int i = 0; i < LittleVecXConfig.multiAnimationSignalCount; i++)
            LittleStructureRegistry.getStructureType(StructureLittleVecXMultiAnimation.class)
                    .addOutput("animation_" + i, 1, SignalMode.EQUAL);

        LittleStructureRegistry.registerStructureType(
                "checkpoint_animation",
                MODID,
                StructureLittleVecXOverlayAnimation.class,
                LittleStructureAttribute.NONE,
                StructureLittleVecXOverlayAnimationParser.class
        ).addOutput("state", 1, SignalMode.TOGGLE);

        for (int i = 0; i < LittleVecXConfig.multiAnimationSignalCount; i++)
            LittleStructureRegistry.getStructureType(StructureLittleVecXOverlayAnimation.class)
                    .addOutput("checkpoint_" + i, 1, SignalMode.EQUAL);

        LittleStructureRegistry.registerStructureType(
                "overlay_animation",
                MODID,
                StructureLittleVecXAdditiveAnimation.class,
                LittleStructureAttribute.NONE,
                StructureLittleVecXAdditiveAnimationParser.class
        ).addOutput("state", 1, SignalMode.TOGGLE);

        for (int i = 0; i < LittleVecXConfig.multiAnimationSignalCount; i++)
            LittleStructureRegistry.getStructureType(StructureLittleVecXAdditiveAnimation.class)
                    .addOutput("animation_" + i, 1, SignalMode.EQUAL);

        LittleStructureRegistry.registerStructureType(
                "loop_animation",
                MODID,
                StructureLittleVecXLoopAnimation.class,
                LittleStructureAttribute.NONE,
                StructureLittleVecXLoopAnimationParser.class
        ).addOutput("state", 1, SignalMode.TOGGLE);

        LittleStructureRegistry.registerStructureType(
                "elevator",
                MODID,
                StructureLittleVecXElevator.class,
                LittleStructureAttribute.NONE,
                StructureLittleVecXElevatorParser.class
        );

        for (int i = 1; i <= LittleVecXConfig.elevatorSignalCount; i++) {
            LittleStructureRegistry.getStructureType(StructureLittleVecXElevator.class)
                    .addInput("button_cabin_" + i, 1)
                    .addInput("button_floor_" + i, 1)
                    .addOutput("button_cabin_" + i, 1, SignalMode.EQUAL)
                    .addOutput("button_floor_" + i, 1, SignalMode.EQUAL)
                    .addOutput("current_floor_" + i, 1, SignalMode.EQUAL);
        }
        LittleStructureRegistry.getStructureType(StructureLittleVecXElevator.class)
                .addInput("up", 1)
                .addInput("down", 1)
                .addInput("stop", 1)
                .addInput("stop_light", 1)
                .addInput("arrival", 1)
                .addOutput("up", 1, SignalMode.EQUAL)
                .addOutput("down", 1, SignalMode.EQUAL)
                .addOutput("stop", 1, SignalMode.EQUAL)
                .addOutput("stop_light", 1, SignalMode.EQUAL)
                .addOutput("arrival", 1, SignalMode.EQUAL);

        LittleStructureRegistry.registerStructureType(
                new StructureLittleVecXStorageType("advanced_storage", MODID, StructureLittleVecXStorage.class, LittleStructureAttribute.NONE)
                        .addInput("accessed", 1)
                        .addInput("filled", 16),
                StructureLittleVecXStorageParser.class
        );

        LittleStructureRegistry.registerStructureType(
                "display",
                MODID,
                StructureLittleVecXStateMutator.class,
                LittleStructureAttribute.NONE,
                StructureLittleVecXStateMutatorParser.class
        ).addOutput("visible", 1, SignalMode.EQUAL);

        LittleStructureRegistry.registerStructureType(
                "wallpaper",
                MODID,
                StructureLittleVecXWallpaperMutator.class,
                LittleStructureAttribute.NONE,
                StructureLittleVecXWallpaperMutatorParser.class
        );

        LittleStructureRegistry.registerStructureType(
                new StructureLittleVecXFurnitureType(),
                StructureLittleVecXFurnitureParser.class
        );
        if (event.getSide() == Side.CLIENT)
            registerClient();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        GuiHandler.registerGuiHandler(LittleVecXIndustrialToolClientHandler.INDUSTRIAL_SCREWDRIVER_GUI_ID, new CustomGuiHandler() {

            @Override
            @SideOnly(Side.CLIENT)
            public SubGui getGui(EntityPlayer player, NBTTagCompound nbt) {
                ItemStack stack = player.getHeldItemMainhand();
                if (stack.isEmpty() || !(stack.getItem() instanceof com.integral.littlevecx.item.ItemLittleVecXIndustrialTool))
                    return null;
                return new SubGuiLittleVecXIndustrialScrewdriver(stack);
            }

            @Override
            public SubContainer getContainer(EntityPlayer player, NBTTagCompound nbt) {
                return new SubContainerEmpty(player);
            }
        });

        GuiHandler.registerGuiHandler(StructureLittleVecXStorage.GUI_ID, new LittleStructureGuiHandler() {

            @Override
            @SideOnly(Side.CLIENT)
            public SubGui getGui(EntityPlayer player, NBTTagCompound nbt, LittleStructure structure) {
                return new SubGuiLittleVecXStorage((StructureLittleVecXStorage) structure);
            }

            @Override
            public SubContainer getContainer(EntityPlayer player, NBTTagCompound nbt, LittleStructure structure) {
                return new SubContainerLittleVecXStorage(player, (StructureLittleVecXStorage) structure);
            }
        });
    }

    @SideOnly(Side.CLIENT)
    private void registerClient() {
        LittleVecXMoveClientHandler.register();
        LittleVecXIndustrialToolClientHandler.register();
        LittleVecXElevatorSoundClientHandler.register();
        LittleVecXActivatorHighlightHandler.register();
        LittleVecXIndustrialSelectionHighlightHandler.register();
        LittleVecXPlacementFeedbackClientHandler.register();
        LittleVecXScrewdriverProgressClientHandler.register();
        LittleVecXFurnitureClientHandler.register();
        LittleVecXSubGuiOverlayHandler.register();
        LittleVecXZoomHandler.register();
    }
}
