package com.integral.littlevecx.client;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.creativecore.common.utils.mc.ColorUtils;
import com.creativemd.littletiles.client.render.overlay.PreviewRenderer;
import com.creativemd.littletiles.client.render.tile.LittleRenderBox;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.tile.combine.BasicCombiner;
import com.creativemd.littletiles.common.tile.math.box.LittleAbsoluteBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxes;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.place.PlacementPosition;
import com.creativemd.littletiles.common.util.shape.ShapeSelection;
import com.creativemd.littletiles.common.util.shape.ShapeSelection.ShapeSelectPos;
import com.integral.littlevecx.item.ItemLittleVecXIndustrialTool;
import com.integral.littlevecx.selection.IndustrialSelectionRegion;
import com.integral.littlevecx.selection.IndustrialSelectionMode;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleVecXIndustrialSelectionHighlightHandler {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final int OUTLINE_COLOR = ColorUtils.WHITE;
    private static final int OUTLINE_ALPHA = 168;
    private static final float OUTLINE_WIDTH = 3.0F;
    private static final double OUTLINE_GROW = 0.002;
    private static final long LIVE_SELECTION_BLOCK_LIMIT = 20000L;
    private static final String KEY_LAST_VISIBLE_SELECTION = "littlevecx_industrial_last_visible_selection";

    private static boolean registered;
    private static World lastWorld;
    private static RenderSelection cachedCommittedSelection;

    public static void register() {
        if (registered)
            return;
        MinecraftForge.EVENT_BUS.register(new LittleVecXIndustrialSelectionHighlightHandler());
        registered = true;
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END)
            return;
        if (MC.world == lastWorld)
            return;

        lastWorld = MC.world;
        resetClientSelections();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null || player.world == null)
            return;
        if (!LittleAction.canPlace(player))
            return;
        if (event.getTarget() == null && PreviewRenderer.marked == null)
            return;
        if (event.getTarget() != null && event.getTarget().typeOfHit != Type.BLOCK && PreviewRenderer.marked == null)
            return;

        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemLittleVecXIndustrialTool))
            return;
        // The point-edit mark mode draws its own blinking region and orange point frame.
        // Suppress the ordinary industrial hover outline so it cannot show through as black.
        if (ItemLittleVecXIndustrialTool.isLastRegionPointMarkModeActive()) {
            event.setCanceled(true);
            return;
        }
        updateVisibleSelectionCache(player, stack);
        if (!shouldRenderLiveSelection(stack)) {
            event.setCanceled(true);
            return;
        }

        World world = player.world;
        ItemLittleVecXIndustrialTool selector = (ItemLittleVecXIndustrialTool) stack.getItem();
        if (shouldSkipLargeLiveSelection(selector, stack, event.getTarget())) {
            event.setCanceled(true);
            return;
        }

        LittleBoxes boxes = resolveLiveSelectionBoxes(selector, world, player, stack, event.getTarget());
        if (boxes == null || boxes.isEmpty())
            return;

        renderSelectionOutline(player, boxes, event.getPartialTicks());
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (MC.player == null || MC.world == null || MC.gameSettings.hideGUI)
            return;

        ItemStack stack = MC.player.getHeldItemMainhand();
        if (stack.getItem() instanceof ItemLittleVecXIndustrialTool)
            updateVisibleSelectionCache(MC.player, stack);

        RenderSelection selection = resolveCommittedSelection(MC.player, stack);
        if (selection == null || selection.isEmpty())
            return;

        renderSelectionOutline(MC.player, selection, event.getPartialTicks(), true);
    }

    private static void renderSelectionOutline(EntityPlayer player, LittleBoxes boxes, float partialTicks) {
        renderSelectionOutline(player, boxes, partialTicks, false);
    }

    private static void renderSelectionOutline(EntityPlayer player, LittleBoxes boxes, float partialTicks, boolean throughBlocks) {
        List<LittleBox> mergedBoxes = mergeBoxes(boxes);
        RenderSelection selection = new RenderSelection("", boxes.pos, boxes.getContext(), mergedBoxes);
        renderSelectionOutline(player, selection, partialTicks, throughBlocks);
    }

    private static void renderSelectionOutline(EntityPlayer player, RenderSelection selection, float partialTicks, boolean throughBlocks) {
        List<LittleBox> mergedBoxes = selection.mergedBoxes;
        if (mergedBoxes.isEmpty())
            return;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.enableTexture2D();
        MC.renderEngine.bindTexture(PreviewRenderer.WHITE_TEXTURE);
        GlStateManager.depthMask(false);
        if (throughBlocks)
            GlStateManager.disableDepth();
        GlStateManager.glLineWidth(OUTLINE_WIDTH);

        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        double posX = x - selection.pos.getX();
        double posY = y - selection.pos.getY();
        double posZ = z - selection.pos.getZ();

        for (LittleBox box : mergedBoxes) {
            LittleRenderBox cube = box.getRenderingCube(selection.context, null, 0);
            if (cube == null)
                continue;
            cube.color = OUTLINE_COLOR;
            cube.renderLines(-posX, -posY, -posZ, OUTLINE_ALPHA, cube.getCenter(), OUTLINE_GROW);
        }

        if (throughBlocks)
            GlStateManager.enableDepth();
        GlStateManager.depthFunc(515);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /** Draws the region being adjusted by Mark Mode with a subtle pulse, independently of hover outlines. */
    public static void renderEditingRegion(LittleBoxes boxes, double cameraX, double cameraY, double cameraZ) {
        if (boxes == null || boxes.isEmpty())
            return;

        List<LittleBox> mergedBoxes = mergeBoxes(boxes);
        if (mergedBoxes.isEmpty())
            return;

        float pulse = (float) ((Math.sin((MC.world == null ? 0L : MC.world.getTotalWorldTime()) * 0.45D) + 1D) * 0.5D);
        int alpha = 64 + (int) (120F * pulse);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.enableTexture2D();
        MC.renderEngine.bindTexture(PreviewRenderer.WHITE_TEXTURE);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(OUTLINE_WIDTH);

        double posX = cameraX - boxes.pos.getX();
        double posY = cameraY - boxes.pos.getY();
        double posZ = cameraZ - boxes.pos.getZ();
        for (LittleBox box : mergedBoxes) {
            LittleRenderBox cube = box.getRenderingCube(boxes.getContext(), null, 0);
            if (cube == null)
                continue;
            cube.color = OUTLINE_COLOR;
            cube.renderLines(-posX, -posY, -posZ, alpha, cube.getCenter(), OUTLINE_GROW);
        }

        GlStateManager.enableDepth();
        GlStateManager.depthFunc(515);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static List<LittleBox> mergeBoxes(LittleBoxes boxes) {
        List<LittleBox> mergedBoxes = new ArrayList<>();
        for (LittleBox box : boxes.all())
            mergedBoxes.add(box.copy());
        BasicCombiner.combineBoxes(mergedBoxes);
        return mergedBoxes;
    }

    private static LittleBoxes resolveLiveSelectionBoxes(ItemLittleVecXIndustrialTool selector, World world, EntityPlayer player, ItemStack stack, RayTraceResult target) {
        if (selector.hasLittlePreview(stack))
            return null;
        if (target == null || target.typeOfHit != Type.BLOCK)
            return null;

        PlacementPosition position = PreviewRenderer.marked != null ? PreviewRenderer.marked.getPosition() : new PlacementPosition(target, selector.getPositionContext(stack));
        if (position == null)
            return null;

        IBlockState state = world.getBlockState(position.getPos());
        if (!selector.hasCustomBoxes(world, stack, player, state, position, target) && PreviewRenderer.marked == null)
            return null;
        if (isPendingLiveSelectionTooLarge(position))
            return null;

        LittleBoxes boxes = selector.getBoxes(world, stack, player, position, target);
        return boxes != null && !boxes.isEmpty() ? boxes : null;
    }

    private static boolean shouldSkipLargeLiveSelection(ItemLittleVecXIndustrialTool selector, ItemStack stack, RayTraceResult target) {
        if (selector.hasLittlePreview(stack))
            return false;
        if (target == null || target.typeOfHit != Type.BLOCK)
            return false;

        PlacementPosition position = PreviewRenderer.marked != null ? PreviewRenderer.marked.getPosition() : new PlacementPosition(target, selector.getPositionContext(stack));
        return isPendingLiveSelectionTooLarge(position);
    }

    private static boolean isPendingLiveSelectionTooLarge(PlacementPosition hoverPosition) {
        ShapeSelection selection = ItemLittleVecXIndustrialTool.selection;
        if (selection == null || hoverPosition == null)
            return false;

        int count = selection.countPositions();
        if (count < 2 || count % 2 != 0)
            return false;

        ShapeSelectPos anchor = getPenultimateSelectionPoint(selection);
        if (anchor == null || anchor.pos == null)
            return false;

        IndustrialSelectionRegion region = new IndustrialSelectionRegion(anchor.pos.copy(), hoverPosition.copy(), hoverPosition.facing);
        LittleAbsoluteBox box = region.toAbsoluteBox();
        if (box == null)
            return false;

        return estimateBlockVolume(box) > LIVE_SELECTION_BLOCK_LIMIT;
    }

    private static ShapeSelectPos getPenultimateSelectionPoint(ShapeSelection selection) {
        ShapeSelectPos previous = null;
        ShapeSelectPos current = null;
        for (ShapeSelectPos point : selection) {
            previous = current;
            current = point;
        }
        return previous;
    }

    private static long estimateBlockVolume(LittleAbsoluteBox box) {
        BlockPos min = box.getMinPos();
        BlockPos max = box.getMaxPos();
        long x = Math.max(1L, (long) max.getX() - min.getX() + 1L);
        long y = Math.max(1L, (long) max.getY() - min.getY() + 1L);
        long z = Math.max(1L, (long) max.getZ() - min.getZ() + 1L);
        return x * y * z;
    }

    private static RenderSelection resolveCommittedSelection(EntityPlayer player, ItemStack stack) {
        if (stack.getItem() instanceof ItemLittleVecXIndustrialTool && ((ItemLittleVecXIndustrialTool) stack.getItem()).hasLittlePreview(stack))
            return null;

        if (stack.getItem() instanceof ItemLittleVecXIndustrialTool) {
            List<IndustrialSelectionRegion> regions = IndustrialSelectionMode.getSavedOrCurrentRegions(stack);
            return getOrBuildCommittedSelection("held:", regions);
        }
        return buildCachedVisibleSelection(player);
    }

    private static void updateVisibleSelectionCache(EntityPlayer player, ItemStack stack) {
        if (player == null || stack.isEmpty() || !(stack.getItem() instanceof ItemLittleVecXIndustrialTool))
            return;
        if (((ItemLittleVecXIndustrialTool) stack.getItem()).hasLittlePreview(stack)) {
            clearVisibleSelectionCache(player);
            return;
        }

        List<IndustrialSelectionRegion> regions = com.integral.littlevecx.selection.IndustrialSelectionMode.getSavedOrCurrentRegions(stack);
        NBTTagCompound data = player.getEntityData();
        if (regions.isEmpty()) {
            data.removeTag(KEY_LAST_VISIBLE_SELECTION);
            return;
        }

        NBTTagList list = new NBTTagList();
        for (IndustrialSelectionRegion region : regions)
            if (region != null && region.isValid())
                list.appendTag(region.writeToNBT(new NBTTagCompound()));
        data.setTag(KEY_LAST_VISIBLE_SELECTION, list);
    }

    public static void clearVisibleSelectionCache(EntityPlayer player) {
        cachedCommittedSelection = null;
        if (player != null)
            player.getEntityData().removeTag(KEY_LAST_VISIBLE_SELECTION);
    }

    private static RenderSelection buildCachedVisibleSelection(EntityPlayer player) {
        if (player == null)
            return null;

        NBTTagList list = player.getEntityData().getTagList(KEY_LAST_VISIBLE_SELECTION, 10);
        if (list.isEmpty())
            return null;

        List<IndustrialSelectionRegion> regions = new ArrayList<>();
        for (int i = 0; i < list.tagCount(); i++) {
            IndustrialSelectionRegion region = IndustrialSelectionRegion.readFromNBT(list.getCompoundTagAt(i));
            if (region != null && region.isValid())
                regions.add(region);
        }
        return getOrBuildCommittedSelection("player:", regions);
    }

    private static RenderSelection getOrBuildCommittedSelection(String source, List<IndustrialSelectionRegion> regions) {
        if (regions == null || regions.isEmpty())
            return null;

        String key = source + buildRegionKey(regions);
        if (cachedCommittedSelection != null && key.equals(cachedCommittedSelection.key))
            return cachedCommittedSelection;

        LittleBoxes boxes = ItemLittleVecXIndustrialTool.INDUSTRIAL_SELECTION_MODE.buildBoxes(regions);
        if (boxes == null || boxes.isEmpty())
            return null;

        cachedCommittedSelection = new RenderSelection(key, boxes.pos, boxes.getContext(), mergeBoxes(boxes));
        return cachedCommittedSelection;
    }

    private static String buildRegionKey(List<IndustrialSelectionRegion> regions) {
        StringBuilder builder = new StringBuilder(regions.size() * 64);
        for (IndustrialSelectionRegion region : regions) {
            if (region == null || !region.isValid())
                continue;
            builder.append(region.writeToNBT(new NBTTagCompound()).toString()).append(';');
        }
        return builder.toString();
    }

    private static boolean shouldRenderLiveSelection(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemLittleVecXIndustrialTool))
            return false;
        if (((ItemLittleVecXIndustrialTool) stack.getItem()).hasLittlePreview(stack))
            return false;

        if (ItemLittleVecXIndustrialTool.selection != null)
            return true;

        List<IndustrialSelectionRegion> regions = IndustrialSelectionMode.getSavedOrCurrentRegions(stack);
        return regions.isEmpty();
    }

    private static void resetClientSelections() {
        ItemLittleVecXIndustrialTool.clearClientRuntimeSelections();
        ItemLittleVecXIndustrialTool.clearRecipeSaveSelectionRestore();
        if (MC.player == null)
            return;

        clearVisibleSelectionCache(MC.player);
        for (int i = 0; i < MC.player.inventory.getSizeInventory(); i++)
            ItemLittleVecXIndustrialTool.clearStoredSelections(MC.player.inventory.getStackInSlot(i));
    }

    private static class RenderSelection {

        private final String key;
        private final BlockPos pos;
        private final LittleGridContext context;
        private final List<LittleBox> mergedBoxes;

        private RenderSelection(String key, BlockPos pos, LittleGridContext context, List<LittleBox> mergedBoxes) {
            this.key = key;
            this.pos = pos;
            this.context = context;
            this.mergedBoxes = mergedBoxes;
        }

        private boolean isEmpty() {
            return mergedBoxes == null || mergedBoxes.isEmpty();
        }
    }
}
