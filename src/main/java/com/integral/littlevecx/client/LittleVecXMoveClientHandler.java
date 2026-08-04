package com.integral.littlevecx.client;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.gui.mc.GuiContainerSub;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.type.Pair;
import com.creativemd.littletiles.client.LittleTilesClient;
import com.creativemd.littletiles.client.render.overlay.PreviewRenderer;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException;
import com.creativemd.littletiles.common.structure.exception.NotYetConnectedException;
import com.creativemd.littletiles.common.structure.type.LittleItemHolder;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.math.location.StructureLocation;
import com.creativemd.littletiles.common.tile.parent.IParentTileList;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.tile.preview.LittlePreviewsStructureHolder;
import com.creativemd.littletiles.common.tile.preview.LittleVecXPreviewFixHelper;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.place.IMarkMode;
import com.creativemd.littletiles.common.util.place.PlacementPosition;
import com.creativemd.littletiles.common.util.vec.LittleRayTraceResult;
import com.creativemd.littletiles.common.world.WorldAnimationHandler;
import com.integral.littlevecx.LittleVecXStaticRotationController;
import com.integral.littlevecx.StructureLittleVecXRotated;
import com.integral.littlevecx.animation.StructureLittleVecXElevator;
import com.integral.littlevecx.client.gui.SubGuiLittleVecXLiveRecipe;
import com.integral.littlevecx.item.ItemLittleVecXPliers;
import com.integral.littlevecx.network.PacketLittleVecXMoveStructure;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleVecXMoveClientHandler {

    private static final Minecraft MC = Minecraft.getMinecraft();

    private static boolean registered = false;
    private static UUID selectedAnimationId;
    private static StructureLocation selectedLocation;
    private static MoveSession currentMoveSession;
    private static int pendingRecipeGuiTicks;
    private static StructureLocation pendingRecipeLocation;
    private static LittlePreviews pendingRecipePreviews;

    public static void register() {
        if (registered)
            return;
        MinecraftForge.EVENT_BUS.register(new LittleVecXMoveClientHandler());
        registered = true;
    }

    public static void cancelCurrentSession() {
        clearSelection();
    }

    public static void clearSelection() {
        cancelMoveSession(false);
        selectedAnimationId = null;
        selectedLocation = null;
    }

    public static boolean hasSelectedStructure() {
        return hasSelection() && isSelectedValid();
    }

    public static boolean hasActiveMoveSession() {
        return currentMoveSession != null;
    }

    public static SelectionSnapshot getSelectionSnapshot() {
        if (!hasSelectedStructure())
            return null;
        return buildSelectionSnapshot();
    }

    public static void openSelectedRecipeGui() {
        if (MC.player == null)
            return;

        if (pendingRecipeLocation != null && pendingRecipePreviews != null) {
            openRecipeGui(pendingRecipeLocation, pendingRecipePreviews);
            pendingRecipeLocation = null;
            pendingRecipePreviews = null;
            return;
        }

        if (!hasSelectedStructure())
            return;

        LittleStructure structure = resolveSelectedStructure();
        RecipeTarget recipeTarget = buildRecipeTarget(structure);
        if (recipeTarget == null)
            return;

        openRecipeGui(recipeTarget.location, recipeTarget.previews);
    }

    private static void openRecipeGui(StructureLocation location, LittlePreviews previews) {
        SubGuiLittleVecXLiveRecipe gui = new SubGuiLittleVecXLiveRecipe(location, previews);
        FMLClientHandler.instance().displayGuiScreen(MC.player, new GuiContainerSub(MC.player, gui, new SubContainerEmpty(MC.player)));
    }

    public static void queueSelectedRecipeGui() {
        LittleStructure structure = resolveSelectedStructure();
        if (structure != null)
            queueRecipeGui(structure);
        pendingRecipeGuiTicks = 2;
    }

    private static void queueRecipeGui(LittleStructure structure) {
        RecipeTarget target = buildRecipeTarget(structure);
        if (target == null) {
            pendingRecipeLocation = null;
            pendingRecipePreviews = null;
            return;
        }
        pendingRecipeLocation = target.location;
        pendingRecipePreviews = target.previews;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        handleRightClick(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        handleRightClick(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        handleRightClick(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickEntity(PlayerInteractEvent.EntityInteractSpecific event) {
        handleRightClick(event);
    }

    private void handleRightClick(PlayerInteractEvent event) {
        if (event.getWorld() == null || !event.getWorld().isRemote)
            return;
        if (event.getHand() != EnumHand.MAIN_HAND)
            return;

        EntityPlayer player = event.getEntityPlayer();
        if (player == null)
            return;

        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemLittleVecXPliers))
            return;

        if (currentMoveSession != null)
        {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
            return;
        }

        EntityAnimation animation = findTargetAnimation(player);
        LittleStructure structure = findTargetStructure(player, animation);
        if (structure == null)
            return;
        if (structure instanceof LittleItemHolder)
            return;

        // Elevators own their moving cabin and signal state; treating them as a movable pliers target
        // can detach the cabin from its control logic. Consume the click without selecting anything.
        if (structure instanceof StructureLittleVecXElevator) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);

        setSelection(animation, structure);

        if (canMove(animation, structure)) {
            if (player.isSneaking()) {
                queueRecipeGui(structure);
                pendingRecipeGuiTicks = 2;
            }
            return;
        }

        queueRecipeGui(structure);
        pendingRecipeGuiTicks = 2;
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END)
            return;

        if (MC.world == null || MC.player == null) {
            clearSelection();
            pendingRecipeGuiTicks = 0;
            pendingRecipeLocation = null;
            pendingRecipePreviews = null;
            return;
        }

        ItemStack stack = MC.player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemLittleVecXPliers)) {
            clearSelection();
            pendingRecipeGuiTicks = 0;
            pendingRecipeLocation = null;
            pendingRecipePreviews = null;
            return;
        }

        if (currentMoveSession == null) {
            if (hasSelection() && !isSelectedValid()) {
                clearSelection();
                return;
            }
        } else if (!currentMoveSession.isValid()) {
            cancelMoveSession(false);
            if (hasSelection() && !isSelectedValid())
                clearSelection();
            return;
        }

        if (pendingRecipeGuiTicks > 0 && --pendingRecipeGuiTicks == 0)
            openSelectedRecipeGui();

        if (MC.currentScreen == null) {
            while (LittleTilesClient.mark.isPressed()) {
                if (currentMoveSession == null) {
                    if (!hasSelection())
                        break;
                    startMoveSession(stack);
                } else {
                    applyMoveSession();
                }
            }
        }

        if (currentMoveSession != null)
            currentMoveSession.refreshPreviewIfDirty();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote)
            clearSelection();
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (currentMoveSession != null)
            return;

        if (hasSelection())
            renderSelectedSelection(event.getPartialTicks());
    }

    private static void startMoveSession(ItemStack stack) {
        if (!canMoveSelectedRoot())
            return;

        EntityAnimation animation = resolveSelectedAnimation();
        if (animation == null)
            return;

        currentMoveSession = new MoveSession(animation, stack);
        PreviewRenderer.marked = currentMoveSession;
    }

    private static void cancelMoveSession(boolean restoreOriginalPosition) {
        if (currentMoveSession == null) {
            clearMarkedMoveSession();
            return;
        }

        if (restoreOriginalPosition)
            currentMoveSession.cancel();
        currentMoveSession = null;
        clearMarkedMoveSession();
    }

    private static void applyMoveSession() {
        if (currentMoveSession == null)
            return;

        currentMoveSession.apply();
        currentMoveSession = null;
        clearMarkedMoveSession();
    }

    private static void clearMarkedMoveSession() {
        if (!isMarkedMoveSession())
            return;
        PreviewRenderer.marked = null;
    }

    private static boolean isMarkedMoveSession() {
        if (PreviewRenderer.marked == null)
            return false;
        if (currentMoveSession != null && PreviewRenderer.marked == currentMoveSession)
            return true;
        return PreviewRenderer.marked.getClass().getName().equals(LittleVecXMoveClientHandler.class.getName() + "$MoveSession");
    }

    private static EntityAnimation findTargetAnimation(EntityPlayer player) {
        float partialTicks = MC.getRenderPartialTicks();
        Vec3d start = player.getPositionEyes(partialTicks);

        RayTraceResult target = MC.objectMouseOver != null && MC.objectMouseOver.typeOfHit == Type.BLOCK ? MC.objectMouseOver : null;
        double reach = target != null ? start.distanceTo(target.hitVec) : (player.capabilities.isCreativeMode ? 5.0D : 4.5D);
        Vec3d look = player.getLook(partialTicks);
        Vec3d end = new Vec3d(start.x + look.x * reach, start.y + look.y * reach, start.z + look.z * reach);

        AxisAlignedBB rayBox = new AxisAlignedBB(start, target != null ? target.hitVec : end).grow(0.05D);
        List<EntityAnimation> animations = WorldAnimationHandler.getHandlerClient().findAnimations(rayBox.grow(1.0D));
        if (animations.isEmpty()) {
            if (MC.objectMouseOver != null && MC.objectMouseOver.entityHit instanceof EntityAnimation) {
                Entity entity = ((EntityAnimation) MC.objectMouseOver.entityHit).getAbsoluteParent();
                if (entity instanceof EntityAnimation && ((EntityAnimation) entity).controller instanceof LittleVecXStaticRotationController)
                    return (EntityAnimation) entity;
            }
            return null;
        }

        EntityAnimation best = null;
        double bestDistance = Double.MAX_VALUE;
        Set<UUID> seen = new HashSet<>();

        for (EntityAnimation animation : animations) {
            Entity entity = animation.getAbsoluteParent();
            if (!(entity instanceof EntityAnimation))
                continue;

            EntityAnimation root = (EntityAnimation) entity;
            if (!seen.add(root.getUniqueID()))
                continue;

            RayTraceResult intercept = root.getEntityBoundingBox().grow(0.05D).calculateIntercept(start, end);
            if (intercept == null)
                continue;

            double distance = start.distanceTo(intercept.hitVec);
            if (best == null || distance < bestDistance) {
                best = root;
                bestDistance = distance;
            }
        }

        return best;
    }

    private static LittleStructure findTargetStructure(EntityPlayer player, @Nullable EntityAnimation rootAnimation) {
        float partialTicks = MC.getRenderPartialTicks();
        Vec3d start = player.getPositionEyes(partialTicks);
        RayTraceResult target = MC.objectMouseOver != null && MC.objectMouseOver.typeOfHit == Type.BLOCK ? MC.objectMouseOver : null;
        double reach = target != null ? start.distanceTo(target.hitVec) : (player.capabilities.isCreativeMode ? 5.0D : 4.5D);
        Vec3d look = player.getLook(partialTicks);
        Vec3d end = new Vec3d(start.x + look.x * reach, start.y + look.y * reach, start.z + look.z * reach);

        if (rootAnimation != null) {
            LittleRayTraceResult hit = rootAnimation.getRayTraceResult(start, end);
            LittleStructure structure = findFocusedStructure(player, partialTicks, hit != null ? hit.world : null, hit != null ? hit.getBlockPos() : null);
            if (structure != null)
                return structure;
            return rootAnimation.structure;
        }

        if (target != null && target.typeOfHit == Type.BLOCK)
            return findFocusedStructure(player, partialTicks, player.world, target.getBlockPos());

        return null;
    }

    private static boolean hasSelection() {
        return selectedLocation != null;
    }

    private static void setSelection(@Nullable EntityAnimation animation, LittleStructure structure) {
        if (structure == null) {
            clearSelection();
            return;
        }

        try {
            selectedAnimationId = animation != null ? animation.getUniqueID() : null;
            selectedLocation = new StructureLocation(structure);
        } catch (RuntimeException e) {
            clearSelection();
        }
    }

    private static boolean matchesSelectedTarget(@Nullable EntityAnimation animation, LittleStructure structure) {
        if (!hasSelection() || structure == null)
            return false;

        try {
            StructureLocation currentLocation = new StructureLocation(structure);
            UUID animationId = animation != null ? animation.getUniqueID() : null;
            return Objects.equals(selectedAnimationId, animationId)
                    && Objects.equals(selectedLocation.pos, currentLocation.pos)
                    && selectedLocation.index == currentLocation.index
                    && Objects.equals(selectedLocation.worldUUID, currentLocation.worldUUID);
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Nullable
    private static EntityAnimation resolveSelectedAnimation() {
        if (!hasSelection() || selectedAnimationId == null)
            return null;

        EntityAnimation animation = WorldAnimationHandler.findAnimation(true, selectedAnimationId);
        if (animation == null)
            return null;

        Entity entity = animation.getAbsoluteParent();
        if (entity instanceof EntityAnimation)
            animation = (EntityAnimation) entity;
        return animation;
    }

    private static LittleStructure resolveSelectedStructure() {
        if (MC.world == null || selectedLocation == null)
            return null;
        try {
            return selectedLocation.find(MC.world);
        } catch (RuntimeException | com.creativemd.littletiles.common.action.LittleActionException e) {
            return null;
        }
    }

    private static boolean canMoveSelectedRoot() {
        LittleStructure structure = resolveSelectedStructure();
        EntityAnimation animation = resolveSelectedAnimation();
        return canMove(animation, structure);
    }

    private static boolean isSelectedValid() {
        LittleStructure structure = resolveSelectedStructure();
        if (structure == null)
            return false;
        return selectedAnimationId == null || resolveSelectedAnimation() != null;
    }

    private static SelectionSnapshot buildSelectionSnapshot() {
        LittleStructure structure = resolveSelectedStructure();
        if (structure == null)
            return null;

        try {
            String name = structure.name != null ? structure.name : structure.type.id;
            EntityAnimation animation = resolveSelectedAnimation();
            return new SelectionSnapshot(animation != null ? animation.getUniqueID() : null, new StructureLocation(structure), name, canMove(animation, structure));
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Nullable
    private static RecipeTarget buildRecipeTarget(@Nullable LittleStructure structure) {
        if (structure == null)
            return null;

        try {
            LittleStructure topStructure = structure.findTopStructure();
            topStructure.load();
            structure.load();
            return new RecipeTarget(new StructureLocation(structure), sanitizeRecipePreviews(structure.getPreviewsSameWorldOnly(structure.getPos())));
        } catch (CorruptedConnectionException | NotYetConnectedException | RuntimeException e) {
            try {
                LittleStructure topStructure = structure.findTopStructure();
                topStructure.load();
                return new RecipeTarget(new StructureLocation(topStructure), sanitizeRecipePreviews(topStructure.getPreviewsSameWorldOnly(topStructure.getPos())));
            } catch (CorruptedConnectionException | NotYetConnectedException | RuntimeException ignored) {
                return null;
            }
        }
    }

    private static LittlePreviews sanitizeRecipePreviews(LittlePreviews previews) {
        if (previews == null)
            return null;

        for (int i = 0; i < previews.childrenCount(); i++) {
            LittlePreviews child = previews.getChild(i);
            if (child instanceof LittlePreviewsStructureHolder) {
                LittlePreviews materialized = materializeStructureHolder((LittlePreviewsStructureHolder) child, previews.getContext());
                previews.updateChild(i, materialized);
                child = materialized;
            }

            sanitizeRecipePreviews(child);
        }

        LittleVecXPreviewFixHelper.convertSlicesToSliceFix(previews);
        return previews;
    }

    private static LittlePreviews materializeStructureHolder(LittlePreviewsStructureHolder holder, LittleGridContext fallbackContext) {
        try {
            LittleStructure structure = holder.structure;
            LittleStructure topStructure = structure.findTopStructure();
            topStructure.load();
            structure.load();
            return sanitizeRecipePreviews(structure.getPreviewsSameWorldOnly(structure.getPos()));
        } catch (CorruptedConnectionException | NotYetConnectedException | RuntimeException e) {
            return new LittlePreviews(fallbackContext);
        }
    }

    private static LittlePreviews buildSelectedPreviews() {
        RecipeTarget target = buildRecipeTarget(resolveSelectedStructure());
        if (target == null)
            return null;
        return target.previews;
    }

    private static void renderSelectedSelection(float partialTicks) {
        EntityAnimation animation = resolveSelectedAnimation();
        if (animation == null || animation.getEntityBoundingBox() == null || MC.player == null)
            return;

        double x = MC.player.lastTickPosX + (MC.player.posX - MC.player.lastTickPosX) * partialTicks;
        double y = MC.player.lastTickPosY + (MC.player.posY - MC.player.lastTickPosY) * partialTicks;
        double z = MC.player.lastTickPosZ + (MC.player.posZ - MC.player.lastTickPosZ) * partialTicks;
        AxisAlignedBB box = animation.getEntityBoundingBox().grow(0.01D).offset(-x, -y, -z);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        GlStateManager.glLineWidth(3.0F);
        RenderGlobal.drawSelectionBoundingBox(box, 0.0F, 0.0F, 0.0F, 0.75F);

        GlStateManager.disableDepth();
        GlStateManager.glLineWidth(2.0F);
        RenderGlobal.drawSelectionBoundingBox(box, 0.20F, 1.0F, 0.45F, 1.0F);
        GlStateManager.enableDepth();

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static final class SelectionSnapshot {
        @Nullable
        public final UUID animationId;
        public final StructureLocation location;
        public final String displayName;
        public final boolean canMoveRoot;

        public SelectionSnapshot(@Nullable UUID animationId, StructureLocation location, String displayName, boolean canMoveRoot) {
            this.animationId = animationId;
            this.location = location;
            this.displayName = displayName;
            this.canMoveRoot = canMoveRoot;
        }
    }

    private static final class RecipeTarget {
        private final StructureLocation location;
        private final LittlePreviews previews;

        private RecipeTarget(StructureLocation location, LittlePreviews previews) {
            this.location = location;
            this.previews = previews;
        }
    }

    private static final class MoveSession implements IMarkMode {

        private final UUID animationId;
        private final StructureLocation location;
        private final double rotX;
        private final double rotY;
        private final double rotZ;
        private final double originalOffX;
        private final double originalOffY;
        private final double originalOffZ;

        private double currentOffX;
        private double currentOffY;
        private double currentOffZ;
        private double syncedOffX;
        private double syncedOffY;
        private double syncedOffZ;
        private boolean previewDirty = true;

        private MoveSession(EntityAnimation animation, ItemStack stack) {
            this.animationId = animation.getUniqueID();
            this.location = selectedLocation != null ? new StructureLocation(selectedLocation.pos, selectedLocation.index, selectedLocation.worldUUID) : null;

            LittleVecXStaticRotationController controller = (LittleVecXStaticRotationController) animation.controller;
            this.rotX = controller.getRotX();
            this.rotY = controller.getRotY();
            this.rotZ = controller.getRotZ();

            this.originalOffX = controller.getOffX();
            this.originalOffY = controller.getOffY();
            this.originalOffZ = controller.getOffZ();

            this.currentOffX = originalOffX;
            this.currentOffY = originalOffY;
            this.currentOffZ = originalOffZ;
            this.syncedOffX = originalOffX;
            this.syncedOffY = originalOffY;
            this.syncedOffZ = originalOffZ;
        }

        private EntityAnimation resolveAnimation() {
            EntityAnimation animation = WorldAnimationHandler.findAnimation(true, animationId);
            if (animation == null)
                return null;
            Entity entity = animation.getAbsoluteParent();
            if (entity instanceof EntityAnimation)
                animation = (EntityAnimation) entity;
            if (!(animation.controller instanceof LittleVecXStaticRotationController))
                return null;
            return animation;
        }

        private boolean isValid() {
            return resolveAnimation() != null;
        }

        private void updatePreview(double offX, double offY, double offZ) {
            EntityAnimation animation = resolveAnimation();
            if (animation == null)
                return;

            LittleVecXStaticRotationController controller = (LittleVecXStaticRotationController) animation.controller;
            controller.setTransform(rotX, rotY, rotZ, offX, offY, offZ);
            animation.updateTickState();
            animation.updateBoundingBox();
        }

        private void refreshPreview() {
            updatePreview(currentOffX, currentOffY, currentOffZ);
            syncServerPositionIfNeeded();
            previewDirty = false;
        }

        private void refreshPreviewIfDirty() {
            if (previewDirty)
                refreshPreview();
        }

        private void cancel() {
            updatePreview(originalOffX, originalOffY, originalOffZ);
            sendMovePacket(originalOffX, originalOffY, originalOffZ);
            previewDirty = false;
        }

        private void apply() {
            refreshPreview();
            sendMovePacket(currentOffX, currentOffY, currentOffZ);
        }

        private void syncServerPositionIfNeeded() {
            if (currentOffX == syncedOffX && currentOffY == syncedOffY && currentOffZ == syncedOffZ)
                return;
            sendMovePacket(currentOffX, currentOffY, currentOffZ);
        }

        private void sendMovePacket(double offX, double offY, double offZ) {
            PacketHandler.sendPacketToServer(new PacketLittleVecXMoveStructure(animationId, location, offX, offY, offZ));
            syncedOffX = offX;
            syncedOffY = offY;
            syncedOffZ = offZ;
        }

        @Override
        public boolean allowLowResolution() {
            return true;
        }

        @Override
        public PlacementPosition getPosition() {
            EntityAnimation animation = resolveAnimation();
            BlockPos pos = animation != null ? animation.getPosition() : BlockPos.ORIGIN;
            return new PlacementPosition(pos, LittleGridContext.get(), com.creativemd.littletiles.common.tile.math.vec.LittleVec.ZERO, EnumFacing.UP);
        }

        @Override
        public com.creativemd.creativecore.common.gui.container.SubGui getConfigurationGui() {
            return null;
        }

        @Override
        public void render(LittleGridContext positionContext, double x, double y, double z) {
            EntityAnimation animation = resolveAnimation();
            if (animation == null)
                return;

            AxisAlignedBB box = animation.getEntityBoundingBox().grow(0.01D).offset(-x, -y, -z);

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO
            );
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);

            GlStateManager.glLineWidth(4.0F);
            RenderGlobal.drawSelectionBoundingBox(box, 0.0F, 0.0F, 0.0F, 0.8F);

            GlStateManager.disableDepth();
            GlStateManager.glLineWidth(2.0F);
            RenderGlobal.drawSelectionBoundingBox(box, 0.10F, 0.80F, 1.0F, 1.0F);
            GlStateManager.enableDepth();

            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }

        @Override
        public void move(LittleGridContext positionContext, EnumFacing facing) {
            double step = positionContext.pixelSize;
            if (GuiScreen.isCtrlKeyDown())
                step *= positionContext.size;

            switch (facing) {
            case EAST:
                currentOffX += step;
                break;
            case WEST:
                currentOffX -= step;
                break;
            case UP:
                currentOffY += step;
                break;
            case DOWN:
                currentOffY -= step;
                break;
            case SOUTH:
                currentOffZ += step;
                break;
            case NORTH:
                currentOffZ -= step;
                break;
            default:
                break;
            }

            previewDirty = true;
        }

        @Override
        public void done() {
            applyMoveSession();
        }
    }

    @Nullable
    private static LittleStructure findFocusedStructure(EntityPlayer player, float partialTicks, @Nullable net.minecraft.world.World world, @Nullable BlockPos pos) {
        if (world == null || pos == null)
            return null;
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityLittleTiles))
            return null;
        Pair<IParentTileList, LittleTile> pair = ((TileEntityLittleTiles) te).getFocusedTile(player, partialTicks);
        if (pair == null || pair.key == null || !pair.key.isStructure())
            return null;
        try {
            return pair.key.getStructure();
        } catch (CorruptedConnectionException | NotYetConnectedException e) {
            return null;
        }
    }

    private static boolean canMove(@Nullable EntityAnimation animation, @Nullable LittleStructure structure) {
        return structure instanceof StructureLittleVecXRotated
                && animation != null
                && animation.controller instanceof LittleVecXStaticRotationController
                && animation.structure == structure;
    }
}
