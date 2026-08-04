package com.integral.littlevecx.client;

import org.lwjgl.input.Keyboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.creativemd.creativecore.common.gui.opener.GuiHandler;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.LittleTilesClient;
import com.integral.littlevecx.item.ItemLittleVecXIndustrialTool;
import com.creativemd.littletiles.client.render.overlay.PreviewRenderer;
import com.creativemd.littletiles.common.action.block.LittleActionPlaceStack;
import com.creativemd.littletiles.common.api.ILittlePlacer;
import com.creativemd.littletiles.common.event.ActionEvent;
import com.creativemd.littletiles.common.util.place.IMarkMode;
import com.creativemd.littletiles.common.util.place.PlacementHelper;
import com.creativemd.littletiles.common.util.place.PlacementMode;
import com.creativemd.littletiles.common.util.place.PlacementPosition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.nbt.NBTTagCompound;

@SideOnly(Side.CLIENT)
public class LittleVecXIndustrialToolClientHandler {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Logger LOGGER = LogManager.getLogger(LittleTiles.modid);

    public static final KeyBinding deleteSelection = new KeyBinding("key.littlevecx.industrial_delete", KeyConflictContext.IN_GAME, Keyboard.KEY_X, "key.categories.littlevecx");
    public static final KeyBinding undoSelectionRegion = new KeyBinding("key.littlevecx.industrial_selection_undo", KeyConflictContext.IN_GAME, Keyboard.KEY_Z, "key.categories.littlevecx");
    public static final KeyBinding redoSelectionRegion = new KeyBinding("key.littlevecx.industrial_selection_redo", KeyConflictContext.IN_GAME, Keyboard.KEY_Y, "key.categories.littlevecx");
    /** Modifier held together with left click to clear all industrial selection regions. */
    public static final KeyBinding clearSelectionModifier = new KeyBinding("key.littlevecx.industrial_clear_selection", KeyConflictContext.IN_GAME, Keyboard.KEY_LSHIFT,
            "key.categories.littlevecx");
    public static final KeyBinding openScrewdriver = new KeyBinding("key.littlevecx.industrial_screwdriver", KeyConflictContext.IN_GAME, Keyboard.KEY_V, "key.categories.littlevecx");
    public static final KeyBinding copySelectionToPreview = new KeyBinding("key.littlevecx.industrial_copy_to_preview", KeyConflictContext.IN_GAME, Keyboard.KEY_LBRACKET,
            "key.categories.littlevecx");
    public static final KeyBinding moveSelectionToPreview = new KeyBinding("key.littlevecx.industrial_move_to_preview", KeyConflictContext.IN_GAME, Keyboard.KEY_RBRACKET,
            "key.categories.littlevecx");
    public static final String INDUSTRIAL_SCREWDRIVER_GUI_ID = "littlevecx_industrial_screwdriver";

    private static boolean registered;
    public static void register() {
        if (registered)
            return;

        ClientRegistry.registerKeyBinding(deleteSelection);
        ClientRegistry.registerKeyBinding(undoSelectionRegion);
        ClientRegistry.registerKeyBinding(redoSelectionRegion);
        ClientRegistry.registerKeyBinding(clearSelectionModifier);
        ClientRegistry.registerKeyBinding(openScrewdriver);
        ClientRegistry.registerKeyBinding(copySelectionToPreview);
        ClientRegistry.registerKeyBinding(moveSelectionToPreview);
        MinecraftForge.EVENT_BUS.register(new LittleVecXIndustrialToolClientHandler());
        registered = true;
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END)
            return;
        if (MC.player == null || MC.world == null || MC.currentScreen != null)
            return;

        ItemStack stack = MC.player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemLittleVecXIndustrialTool))
            return;

        while (copySelectionToPreview.isPressed())
            startSelectionPreview(stack, false);
        while (moveSelectionToPreview.isPressed())
            startSelectionPreview(stack, true);
        while (deleteSelection.isPressed())
            ItemLittleVecXIndustrialTool.deleteCurrentSelection(MC.player, stack);
        drainKeyPresses(undoSelectionRegion);
        drainKeyPresses(redoSelectionRegion);
        while (openScrewdriver.isPressed())
            openIndustrialScrewdriverGui(MC.player, stack);
    }

    /**
     * Z/Y are handled as direct keyboard events. The KeyBinding press queue can
     * be consumed by another client handler before the end-of-tick recipe code
     * reads it, which made selection undo/redo intermittently stop working.
     */
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!Keyboard.getEventKeyState() || Keyboard.isRepeatEvent() || GuiScreen.isCtrlKeyDown())
            return;
        if (MC.player == null || MC.world == null || MC.currentScreen != null)
            return;

        ItemStack stack = MC.player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemLittleVecXIndustrialTool))
            return;

        int keyCode = Keyboard.getEventKey();
        if (undoSelectionRegion.isActiveAndMatches(keyCode)) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX industrial selection key: undo, key={}", keyCode);
            ItemLittleVecXIndustrialTool.undoLastSelectionRegion(MC.player, stack);
        } else if (redoSelectionRegion.isActiveAndMatches(keyCode)) {
            com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER, "LittleVecX industrial selection key: redo, key={}", keyCode);
            ItemLittleVecXIndustrialTool.redoLastSelectionRegion(MC.player, stack);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld() == null || !event.getWorld().isRemote || event.getHand() != EnumHand.MAIN_HAND)
            return;
        RayTraceResult result = new RayTraceResult(event.getHitVec(), event.getFace(), event.getPos());
        if (tryPlacePreviewWithLittleTilesUndo(event.getEntityPlayer(), result)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld() == null || !event.getWorld().isRemote || event.getHand() != EnumHand.MAIN_HAND)
            return;
        if (PreviewRenderer.marked == null)
            return;
        if (tryPlacePreviewWithLittleTilesUndo(event.getEntityPlayer(), null)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (event.getWorld() == null || !event.getWorld().isRemote || event.getHand() != EnumHand.MAIN_HAND)
            return;
        if (PreviewRenderer.marked == null)
            return;
        if (tryPlacePreviewWithLittleTilesUndo(event.getEntityPlayer(), null)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }

    /**
     * Keep industrial placement on LittleTiles' regular action path. It performs
     * the normal client/server action sync and records an undo snapshot; the old
     * bespoke packet bypassed that action history completely.
     */
    private static boolean tryPlacePreviewWithLittleTilesUndo(EntityPlayer player, RayTraceResult result) {
        if (player == null || player.world == null || !player.world.isRemote)
            return false;

        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemLittleVecXIndustrialTool))
            return false;
        ItemLittleVecXIndustrialTool tool = (ItemLittleVecXIndustrialTool) stack.getItem();
        ItemLittleVecXIndustrialTool.restorePendingPreviewAfterServerSync(stack);
        if (!tool.hasLittlePreview(stack))
            return false;

        PlacementPosition position = PreviewRenderer.marked != null ? PreviewRenderer.marked.getPosition()
                : PlacementHelper.getPosition(player.world, result, tool.getPositionContext(stack), tool, stack);
        if (position == null)
            return false;

        if (!LittleTilesClient.INTERACTION.start(true))
            return false;

        PlacementMode mode = tool.getPlacementMode(stack).place();
        boolean centered = PreviewRenderer.isCentered(player, stack, (ILittlePlacer) tool);
        boolean fixed = PreviewRenderer.isFixed(player, stack, (ILittlePlacer) tool);
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX industrial preview place: marked={}, position={}, movePreview={}, centered={}, fixed={}, mode={}",
                PreviewRenderer.marked == null ? "none" : PreviewRenderer.marked.getClass().getSimpleName(), position, ItemLittleVecXIndustrialTool.isMovePreviewActive(stack),
                centered, fixed, mode);
        if (!new LittleActionPlaceStack(tool.getLittlePreview(stack, false), position, centered, fixed, mode).execute())
            return false;

        PreviewRenderer.marked = null;
        ItemLittleVecXIndustrialTool.finishSelectionPreviewPlacement(player, stack);
        return true;
    }

    private static void drainKeyPresses(KeyBinding keyBinding) {
        while (keyBinding.isPressed()) {
            // Intentionally empty: Ctrl+Z/Ctrl+Y belongs to LittleTiles undo/redo.
        }
    }

    private static void startSelectionPreview(ItemStack stack, boolean deleteOriginal) {
        if (PreviewRenderer.marked != null || MC.objectMouseOver == null)
            return;

        ItemLittleVecXIndustrialTool tool = (ItemLittleVecXIndustrialTool) stack.getItem();
        PlacementPosition position = PlacementHelper.getPosition(MC.world, MC.objectMouseOver, tool.getPositionContext(stack), tool, stack);
        if (position == null)
            return;

        IMarkMode mark = tool.beginSelectionPreview(MC.player, stack, position, MC.objectMouseOver, deleteOriginal);
        com.integral.littlevecx.LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX industrial preview start: move={}, regions={}, rayPosition={}, mark={}, hasPreview={}", deleteOriginal,
                ItemLittleVecXIndustrialTool.getServerPlacementRegions(stack).size(), position, mark == null ? "none" : mark.getClass().getSimpleName(), tool.hasLittlePreview(stack));
        if (mark != null)
            PreviewRenderer.marked = mark;
    }

    @SubscribeEvent
    public void onAction(ActionEvent event) {
        if (event == null || event.type != ActionEvent.ActionType.normal)
            return;
        if (!(event.action instanceof LittleActionPlaceStack))
            return;
        if (MC.player == null || event.player != MC.player)
            return;

        ItemStack stack = MC.player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemLittleVecXIndustrialTool))
            return;
        if (!ItemLittleVecXIndustrialTool.isMovePreviewActive(stack))
            return;

        ItemLittleVecXIndustrialTool.finishMovePreview(MC.player, stack);
    }

    public static void openIndustrialScrewdriverGui(net.minecraft.entity.player.EntityPlayer player, ItemStack stack) {
        if (player == null || stack.isEmpty() || !(stack.getItem() instanceof ItemLittleVecXIndustrialTool))
            return;

        ItemLittleVecXIndustrialTool.ensureIndustrialMode(stack);
        ItemLittleVecXIndustrialTool.syncSelectionToStack(stack);
        GuiHandler.openGui(INDUSTRIAL_SCREWDRIVER_GUI_ID, new NBTTagCompound(), player);
    }

    public static void openIndustrialRecipeGui(net.minecraft.entity.player.EntityPlayer player, ItemStack stack) {
        if (player == null || stack.isEmpty() || !(stack.getItem() instanceof ItemLittleVecXIndustrialTool))
            return;

        GuiHandler.openGui("configure", new NBTTagCompound(), player);
    }
}
