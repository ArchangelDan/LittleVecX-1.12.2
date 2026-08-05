package com.integral.littlevecx.item;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.lang.reflect.Field;

import javax.annotation.Nullable;

import com.creativemd.creativecore.client.rendering.RenderBox;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.LittleTilesClient;
import com.creativemd.littletiles.client.gui.configure.SubGuiConfigure;
import com.creativemd.littletiles.client.render.overlay.PreviewRenderer;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.action.block.LittleActionDestroyBoxes;
import com.creativemd.littletiles.common.action.block.LittleActionDestroyBoxes.LittleActionDestroyBoxesFiltered;
import com.creativemd.littletiles.common.api.ILittleEditor;
import com.creativemd.littletiles.common.item.ItemLittleRecipeAdvanced;
import com.creativemd.littletiles.common.item.ItemMultiTiles;
import com.creativemd.littletiles.common.tile.math.box.LittleAbsoluteBox;
import com.creativemd.littletiles.common.tile.math.vec.LittleAbsoluteVec;
import com.creativemd.littletiles.common.tile.math.vec.LittleVec;
import com.creativemd.littletiles.common.tile.math.vec.LittleVecContext;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxes;
import com.creativemd.littletiles.common.tile.place.PlacePreview;
import com.creativemd.littletiles.common.tile.preview.LittlePreview;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.place.IMarkMode;
import com.creativemd.littletiles.common.util.place.MarkMode;
import com.creativemd.littletiles.common.util.place.PlacementHelper;
import com.creativemd.littletiles.common.util.place.PlacementMode;
import com.creativemd.littletiles.common.util.place.PlacementPosition;
import com.creativemd.littletiles.common.util.place.PlacementPreview;
import com.creativemd.littletiles.common.util.selection.selector.TileSelector;
import com.creativemd.littletiles.common.util.shape.ShapeSelection;
import com.creativemd.littletiles.common.util.shape.ShapeSelection.ShapeSelectPos;
import com.integral.littlevecx.LittleVecXMod;
import com.integral.littlevecx.LittleVecXDebugLog;
import com.integral.littlevecx.backup.LittleVecXCrashBackup;
import com.integral.littlevecx.client.LittleVecXIndustrialSelectionHighlightHandler;
import com.integral.littlevecx.client.LittleVecXIndustrialToolClientHandler;
import com.integral.littlevecx.client.gui.SubGuiLittleVecXIndustrialConfigure;
import com.integral.littlevecx.network.PacketLittleVecXIndustrialSelection;
import com.integral.littlevecx.selection.IndustrialSelectionRegion;
import com.integral.littlevecx.selection.IndustrialSelectionMode;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ItemLittleVecXIndustrialTool extends ItemLittleRecipeAdvanced implements ILittleEditor {

    public static final IndustrialSelectionMode INDUSTRIAL_SELECTION_MODE = new IndustrialSelectionMode();
    private static final Logger LOGGER = LogManager.getLogger(LittleTiles.modid);
    public static final String INDUSTRIAL_SHAPE = "industrial";
    private static final String KEY_MOVE_PREVIEW_REGIONS = "littlevecx_industrial_move_preview_regions";
    private static final String KEY_MOVE_PREVIEW_DELETE_ORIGINAL = "littlevecx_industrial_move_preview_delete_original";
    private static final String KEY_PREVIEW_SOURCE_REGIONS = "littlevecx_industrial_preview_source_regions";
    private static final String KEY_PREVIEW_SOURCE_DIMENSION = "littlevecx_industrial_preview_source_dimension";
    private static final String KEY_PREVIEW_SOURCE_WORLD = "littlevecx_industrial_preview_source_world";
    private static final String KEY_SELECTION_STACK_ID = "littlevecx_industrial_selection_stack_id";
    @SideOnly(Side.CLIENT)
    private static final Deque<List<IndustrialSelectionRegion>> selectionRedoHistory = new ArrayDeque<>();
    @SideOnly(Side.CLIENT)
    private static final List<IndustrialSelectionRegion> recipeSaveSelectionRestore = new ArrayList<>();
    /** True only after a local selection mutation that must immediately resume the hover point. */
    @SideOnly(Side.CLIENT)
    private static boolean restoreRuntimeHoverAfterLocalChange;
    @SideOnly(Side.CLIENT)
    private static String lastHoverDebugState;
    /**
     * A selection packet sent by point-edit mode may return from the server after '[' or ']'
     * has already created the client-only LittlePreview. Keep that preview NBT until it is
     * placed or cancelled so the stale inventory update cannot turn it into a white mark box.
     */
    @SideOnly(Side.CLIENT)
    private static NBTTagCompound pendingPreviewTag;
    @SideOnly(Side.CLIENT)
    private static String pendingPreviewStackId;
    @SideOnly(Side.CLIENT)
    private static Field shapeSelectionPositionsField;
    @SideOnly(Side.CLIENT)
    private static Field shapeSelectionLastField;
    @SideOnly(Side.CLIENT)
    private static Field shapeSelectionPosField;
    @SideOnly(Side.CLIENT)
    private static Field shapeSelectionContextField;

    @SideOnly(Side.CLIENT)
    public static ShapeSelection selection;
    @SideOnly(Side.CLIENT)
    private static String activeSelectionStackId;
    private static boolean activeFilter = false;
    private static TileSelector currentFilter = null;
    @SideOnly(Side.CLIENT)
    private static IBakedModel backgroundModel;

    public ItemLittleVecXIndustrialTool() {
        setRegistryName(LittleVecXMod.MODID, "industrial_tool");
        setTranslationKey(LittleVecXMod.MODID + ".industrial_tool");
        setCreativeTab(LittleTiles.littleTab);
        setMaxStackSize(1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        ensureIndustrialMode(stack);
        tooltip.add("ЛКМ: добавить точку выделения");
        tooltip.add(getKeyName(LittleTilesClient.mark, "M") + ": редактировать точки");
        tooltip.add(getKeyName(LittleTilesClient.configure, "C") + ": сохранить выделение в рецепт");
        tooltip.add(getKeyName(LittleTilesClient.configureAdvanced, "Ctrl+C") + ": настроить грид");
        tooltip.add(getKeyName(LittleVecXIndustrialToolClientHandler.copySelectionToPreview, "[") + ": копировать выделение в превью");
        tooltip.add(getKeyName(LittleVecXIndustrialToolClientHandler.moveSelectionToPreview, "]") + ": переместить выделение в превью");
        tooltip.add(getKeyName(LittleVecXIndustrialToolClientHandler.deleteSelection, "X") + ": удалить выделенное из мира");
        tooltip.add(getKeyName(LittleVecXIndustrialToolClientHandler.clearSelectionModifier, "Shift") + "+ЛКМ: очистить все области");
        tooltip.add(getKeyName(LittleVecXIndustrialToolClientHandler.openScrewdriver, "V") + ": industrial-отвертка");
    }

    @Override
    public void onCreated(ItemStack stack, World worldIn, EntityPlayer playerIn) {
        ensureIndustrialMode(stack);
    }

    @Override
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        ensureIndustrialMode(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
      public void applyCustomOpenGLHackery(ItemStack stack, TransformType cameraTransformType) {
        GlStateManager.pushMatrix();
        Minecraft mc = Minecraft.getMinecraft();

        if (cameraTransformType == TransformType.GUI || !hasLittlePreview(stack)) {
            if (cameraTransformType == TransformType.GUI)
                GlStateManager.disableDepth();
            if (backgroundModel == null)
                backgroundModel = mc.getRenderItem().getItemModelMesher().getModelManager()
                    .getModel(new ModelResourceLocation(LittleVecXMod.MODID + ":industrial_recipe_background", "inventory"));
            ForgeHooksClient.handleCameraTransforms(backgroundModel, cameraTransformType, false);
            mc.getRenderItem().renderItem(new ItemStack(Items.PAPER), backgroundModel);
            if (cameraTransformType == TransformType.GUI)
                GlStateManager.enableDepth();
        }

          GlStateManager.popMatrix();
      }

    /**
     * The industrial recipe saves a selection by mutating the same ItemStack. The shared
     * item-model cache can then retain quads created before the saved tile colours arrived.
     * Its compact preview is cheap to rebuild and must always reflect the current NBT.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public List<RenderBox> getRenderingCubes(IBlockState state, TileEntity te, ItemStack stack) {
        if (!hasLittlePreview(stack))
            return Collections.emptyList();
        return LittlePreview.getCubesForStackRendering(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public List<BakedQuad> getCachedModel(EnumFacing facing, BlockRenderLayer layer, IBlockState state, TileEntity te, ItemStack stack, boolean threaded) {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void saveCachedModel(EnumFacing facing, BlockRenderLayer layer, List<BakedQuad> cachedQuads, IBlockState state, TileEntity te, ItemStack stack,
            boolean threaded) {}

    @Override
    public boolean hasCustomBoxes(World world, ItemStack stack, EntityPlayer player, IBlockState state, PlacementPosition pos, RayTraceResult result) {
        activateSelectionStack(stack);
        restorePendingPreviewAfterServerSync(stack);
        if (hasLittlePreview(stack))
            return false;
        if (selection != null)
            return true;
        if (!IndustrialSelectionMode.getSavedOrCurrentRegions(stack).isEmpty())
            return true;
        return LittleAction.isBlockValid(state) || (result != null && world.getTileEntity(result.getBlockPos()) instanceof TileEntityLittleTiles);
    }

    @Override
    public LittleBoxes getBoxes(World world, ItemStack stack, EntityPlayer player, PlacementPosition pos, RayTraceResult result) {
        ensureIndustrialMode(stack);
        activateSelectionStack(stack);
        restorePendingPreviewAfterServerSync(stack);
        // LittleTiles renders saved LittlePreview data itself while a copy/move preview is
        // active. Returning the old selected regions here makes its editor renderer draw a
        // second, generic white hover box over (or instead of) the real structure preview.
        if (hasLittlePreview(stack))
            return IndustrialSelectionMode.buildBoxes(Collections.emptyList());
        PlacementPosition hoverPosition = pos;
        List<IndustrialSelectionRegion> currentRegions = IndustrialSelectionMode.getCurrentRegions(stack);
        logHoverState("render", hoverPosition, result, currentRegions.size());
        if (selection == null && restoreRuntimeHoverAfterLocalChange && !currentRegions.isEmpty()) {
            restoreRuntimeHoverAfterLocalChange = !restoreRuntimeSelectionFromRegions(player, stack, hoverPosition, result);
            logHoverState("render-after-restore", hoverPosition, result, currentRegions.size());
        }
        // Rendering must be read-only. Reconstructing ShapeSelection here mutates LT's hidden
        // hover state merely by looking at a block, which is what produced the stray white box
        // after joining a world. Restore it only when the player actually clicks to continue it.
        if (selection == null && !currentRegions.isEmpty())
            return IndustrialSelectionMode.buildCurrentBoxes(stack);
        if (selection == null && !IndustrialSelectionMode.getSavedOrCurrentRegions(stack).isEmpty())
            return IndustrialSelectionMode.buildSavedOrCurrentBoxes(stack);
        if (selection == null)
            selection = new ShapeSelection(stack, true);
        // The white hover point must be exactly where the following left click will place a
        // selection point. Otherwise positive block faces produce an invisible one-grid jump.
        selection.setLast(player, stack, hoverPosition, result);
        return selection.getBoxes(true);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void tick(EntityPlayer player, ItemStack stack, PlacementPosition position, RayTraceResult result) {
        // ILittleEditor preview already updates the hover point through getBoxes().
        // Using a second LT position pipeline here introduces face-dependent offsets.
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean onClickBlock(World world, EntityPlayer player, ItemStack stack, PlacementPosition position, RayTraceResult result) {
        ensureIndustrialMode(stack);
        if (confirmLastRegionPointMarkMode())
            return true;
        activateSelectionStack(stack);
        restorePendingPreviewAfterServerSync(stack);
        if (hasLittlePreview(stack))
            return true;

        // This must be LittleVecX's own configurable modifier, not LittleTiles' global
        // second-mode setting. Otherwise changing LT's modifier silently changes the
        // destructive "clear all regions" gesture as well.
        if (LittleVecXIndustrialToolClientHandler.clearSelectionModifier.isKeyDown()) {
            clearRuntimeSelection(stack, true);
            return true;
        }

        PlacementPosition applyPosition = position;
        LittleVecXDebugLog.debug(LOGGER, "LittleVecX industrial selection click: position={}, face={}, selectionPoints={}, regions={}, restoreHover={}", applyPosition,
                result == null ? "none" : result.sideHit, getSelectionPoints().size(), IndustrialSelectionMode.getCurrentRegions(stack).size(), restoreRuntimeHoverAfterLocalChange);
        if (selection == null)
            restoreRuntimeSelectionFromRegions(player, stack, applyPosition, result);
        if (selection == null)
            selection = new ShapeSelection(stack, true);
        clearRecipeSaveSelectionRestore();
        // ShapeSelection.addAndCheckIfPlace consumes the unmodified ray position itself.
        // Calling setLast first mutates that very PlacementPosition inside a positive face,
        // so the subsequent first point of a new region was one grid cell too low.
        selection.addAndCheckIfPlace(player, applyPosition, result);
        clearSelectionRedoHistory();
        syncSelectionToStack(stack);
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onClickAir(EntityPlayer player, ItemStack stack) {
        if (stack.getItem() instanceof ItemLittleVecXIndustrialTool)
            confirmLastRegionPointMarkMode();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean onRightClick(World world, EntityPlayer player, ItemStack stack, PlacementPosition position, RayTraceResult result) {
        ensureIndustrialMode(stack);
        activateSelectionStack(stack);
        if (hasLittlePreview(stack))
            return true;

        if (selection != null)
            selection.click(player);
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IMarkMode onMark(EntityPlayer player, ItemStack stack, PlacementPosition position, RayTraceResult result, PlacementPreview previews) {
        activateSelectionStack(stack);
        // The first point of the next region is already fixed; mark-mode must not replace or
        // move that unfinished selection. It becomes available again after the second click.
        if (!hasLittlePreview(stack) && hasUnfinishedSelectionRegion())
            return null;
        // M is reserved for correcting the final point of a completed region. Copying and
        // moving the selection into a preview use their dedicated configurable key bindings.
        if (!hasLittlePreview(stack) && !GuiScreen.isShiftKeyDown()) {
            IMarkMode editLastPoint = createLastRegionPointMarkMode(stack);
            if (editLastPoint != null)
                return editLastPoint;
        }
        return null;
    }

    /** Starts the copy or move-to-preview flow from its dedicated key binding. */
    @SideOnly(Side.CLIENT)
    @Nullable
    public IMarkMode beginSelectionPreview(EntityPlayer player, ItemStack stack, PlacementPosition position, RayTraceResult result, boolean deleteOriginal) {
        activateSelectionStack(stack);
        if (hasLittlePreview(stack) || hasUnfinishedSelectionRegion())
            return null;
        if (!prepareMovePreview(player, stack, position, deleteOriginal))
            return null;

        PlacementMode mode = getPlacementMode(stack);
        // A copied selection must start over the original area too. The mark mode then lets
        // the player drag it away; using the ray-trace position here made '[' appear under
        // the crosshair while ']' used a separate, incompatible path.
        PlacementPosition previewPosition = getMovePreviewInitialPosition(stack, position);
        PlacementPreview activePreview = PlacementHelper.getPreviews(player.world, stack, previewPosition, PreviewRenderer.isCentered(player, stack, this),
                PreviewRenderer.isFixed(player, stack, this), true, mode);
        IMarkMode movePreviewMark = createMovePreviewMarkMode(player, stack, previewPosition, activePreview);
        if (movePreviewMark != null) {
            // PreviewRenderer switches to centred/non-fixed placement as soon as a mark mode is
            // installed. Record both coordinate systems once so a source-aligned industrial
            // preview can be calibrated from the values LittleTiles actually renders with.
            PlacementPreview markedPreview = PlacementHelper.getPreviews(player.world, stack, movePreviewMark.getPosition(), true, false, true, mode);
            LittleVecXDebugLog.debug(LOGGER,
                    "LittleVecX industrial preview geometry: move={}, sourcePosition={}, source={}, markedPosition={}, marked={}",
                    deleteOriginal, previewPosition, describePlacementPreview(activePreview), movePreviewMark.getPosition(), describePlacementPreview(markedPreview));
            return movePreviewMark;
        }
        return ILittleEditor.super.onMark(player, stack, previewPosition, result, activePreview);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onDeselect(World world, ItemStack stack, EntityPlayer player) {
        if (isMovePreviewActive(stack)) {
            cancelMovePreview(stack);
            sendSelectionToServer(stack);
            return;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public SubGuiConfigure getConfigureGUI(EntityPlayer player, ItemStack stack) {
        ensureIndustrialMode(stack);
        activateSelectionStack(stack);
        boolean copiedSelectionToRecipe = !hasLittlePreview(stack);
        List<IndustrialSelectionRegion> restoreRegions = new ArrayList<>();
        if (!hasLittlePreview(stack)) {
            if (selection != null)
                syncSelectionToStack(stack);
            restoreRegions = copyRegions(IndustrialSelectionMode.getSavedOrCurrentRegions(stack));
            sendSelectionToServer(stack);
        }
        SubGuiConfigure gui = super.getConfigureGUI(player, stack);
        if (copiedSelectionToRecipe && hasLittlePreview(stack)) {
            rememberRecipeSaveSelectionRestore(restoreRegions);
            clearStoredSelections(stack);
            storePreviewSourceRegions(stack, restoreRegions, player.world);
            clearClientRuntimeSelections();
            LittleVecXIndustrialSelectionHighlightHandler.clearVisibleSelectionCache(player);
            sendCurrentSelectionToServer();
        }
        return gui;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public SubGuiConfigure getConfigureGUIAdvanced(EntityPlayer player, ItemStack stack) {
        activateSelectionStack(stack);
        return new SubGuiLittleVecXIndustrialConfigure(stack, getPositionContext(stack), ItemMultiTiles.currentMode, isFiltered(), getFilter()) {

            @Override
            public void saveConfiguration(LittleGridContext context, PlacementMode mode, boolean activeFilter, TileSelector selector) {
                if (selection != null)
                    selection.convertTo(context);
                ItemMultiTiles.currentContext = context;
                ItemMultiTiles.currentMode = mode;
                setFilter(activeFilter, selector);
            }
        };
    }

    @Override
    public LittleGridContext getPositionContext(ItemStack stack) {
        return ItemMultiTiles.currentContext == null ? LittleGridContext.get() : ItemMultiTiles.currentContext;
    }

    public static void ensureIndustrialMode(ItemStack stack) {
        if (stack.isEmpty())
            return;
        if (!stack.hasTagCompound())
            stack.setTagCompound(new NBTTagCompound());
        if (!INDUSTRIAL_SELECTION_MODE.name.equals(stack.getTagCompound().getString("selmode")))
            setSelectionMode(stack, INDUSTRIAL_SELECTION_MODE);
        if (!INDUSTRIAL_SHAPE.equals(stack.getTagCompound().getString("shape")))
            stack.getTagCompound().setString("shape", INDUSTRIAL_SHAPE);
    }

    @SideOnly(Side.CLIENT)
    private static void activateSelectionStack(ItemStack stack) {
        if (stack == null || stack.isEmpty())
            return;

        String id = getOrCreateSelectionStackId(stack);
        if (id.equals(activeSelectionStackId))
            return;

        LittleVecXDebugLog.debug(LOGGER, "LittleVecX industrial selection stack switch: oldId={}, newId={}, selectionPoints={}, storedRegions={}", activeSelectionStackId, id,
                selection == null ? 0 : getSelectionPoints().size(), IndustrialSelectionMode.getCurrentRegions(stack).size());
        selection = null;
        clearSelectionRedoHistory();
        clearRecipeSaveSelectionRestore();
        restoreRuntimeHoverAfterLocalChange = false;
        lastHoverDebugState = null;
        activeSelectionStackId = id;
    }

    @SideOnly(Side.CLIENT)
    private static boolean isActiveSelectionStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound())
            return false;
        return stack.getTagCompound().getString(KEY_SELECTION_STACK_ID).equals(activeSelectionStackId);
    }

    private static String getOrCreateSelectionStackId(ItemStack stack) {
        ensureIndustrialMode(stack);
        NBTTagCompound tag = stack.getTagCompound();
        String id = tag.getString(KEY_SELECTION_STACK_ID);
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
            tag.setString(KEY_SELECTION_STACK_ID, id);
        }
        return id;
    }

    @SideOnly(Side.CLIENT)
    private static void rememberPendingPreview(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemLittleVecXIndustrialTool)
                || !((ItemLittleVecXIndustrialTool) stack.getItem()).hasLittlePreview(stack)) {
            clearPendingPreview();
            return;
        }

        pendingPreviewStackId = getOrCreateSelectionStackId(stack);
        pendingPreviewTag = stack.getTagCompound().copy();
        LittleVecXDebugLog.debug(LOGGER, "LittleVecX industrial preview cached: stackId={}, movePreview={}", pendingPreviewStackId, isMovePreviewActive(stack));
    }

    /** Restores only a preview that was displaced by an older server selection-sync packet. */
    @SideOnly(Side.CLIENT)
    public static boolean restorePendingPreviewAfterServerSync(ItemStack stack) {
        if (pendingPreviewTag == null || pendingPreviewStackId == null || pendingPreviewStackId.isEmpty() || stack == null || stack.isEmpty()
                || !(stack.getItem() instanceof ItemLittleVecXIndustrialTool) || PreviewRenderer.marked == null
                || !(PreviewRenderer.marked instanceof IndustrialPreviewMarkMode))
            return false;
        if (((ItemLittleVecXIndustrialTool) stack.getItem()).hasLittlePreview(stack))
            return false;
        if (!pendingPreviewStackId.equals(getOrCreateSelectionStackId(stack)))
            return false;

        stack.setTagCompound(pendingPreviewTag.copy());
        LittleVecXDebugLog.debug(LOGGER, "LittleVecX industrial preview restored after stale selection sync: stackId={}, movePreview={}", pendingPreviewStackId,
                isMovePreviewActive(stack));
        return true;
    }

    @SideOnly(Side.CLIENT)
    private static void clearPendingPreview() {
        pendingPreviewTag = null;
        pendingPreviewStackId = null;
    }

    /** Persists the client identity of this concrete tool stack through server inventory syncs. */
    public static void setSelectionStackId(ItemStack stack, String id) {
        if (stack == null || stack.isEmpty() || id == null || id.isEmpty())
            return;
        ensureIndustrialMode(stack);
        stack.getTagCompound().setString(KEY_SELECTION_STACK_ID, id);
    }

    @SideOnly(Side.CLIENT)
    public static void syncSelectionToStack(ItemStack stack) {
        ensureIndustrialMode(stack);
        IndustrialSelectionMode.syncSelectionToStack(stack, selection);
    }

    @SideOnly(Side.CLIENT)
    public static void sendCurrentSelectionToServer() {
        PacketHandler.sendPacketToServer(new PacketLittleVecXIndustrialSelection(IndustrialSelectionMode.extractCommittedRegions(selection), activeSelectionStackId));
    }

    @SideOnly(Side.CLIENT)
    public static void sendSelectionToServer(ItemStack stack) {
        if (selection != null && isActiveSelectionStack(stack)) {
            sendCurrentSelectionToServer();
            return;
        }
        PacketHandler.sendPacketToServer(new PacketLittleVecXIndustrialSelection(IndustrialSelectionMode.getCurrentRegions(stack), getOrCreateSelectionStackId(stack)));
    }

    @SideOnly(Side.CLIENT)
    public static void clearRuntimeSelection(ItemStack stack, boolean sendStatus) {
        activateSelectionStack(stack);
        selection = null;
        clearSelectionRedoHistory();
        restoreRuntimeHoverAfterLocalChange = false;
        syncSelectionToStack(stack);
        sendCurrentSelectionToServer();
        if (sendStatus && net.minecraft.client.Minecraft.getMinecraft().player != null)
            net.minecraft.client.Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.industrial.selection_cleared"), true);
    }

    public static void clearSavedSelection(ItemStack stack) {
        ensureIndustrialMode(stack);
        if (stack.hasTagCompound())
            stack.getTagCompound().removeTag(IndustrialSelectionMode.KEY_SAVED_REGIONS);
    }

    public static void clearStoredSelections(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemLittleVecXIndustrialTool))
            return;
        ensureIndustrialMode(stack);
        if (!stack.hasTagCompound())
            return;

        NBTTagCompound tag = stack.getTagCompound();
        tag.removeTag(IndustrialSelectionMode.KEY_PENDING);
        tag.removeTag(IndustrialSelectionMode.KEY_PENDING_FACE);
        tag.removeTag(IndustrialSelectionMode.KEY_REGIONS);
        tag.removeTag(IndustrialSelectionMode.KEY_SAVED_REGIONS);
        tag.removeTag(KEY_MOVE_PREVIEW_REGIONS);
        tag.removeTag(KEY_MOVE_PREVIEW_DELETE_ORIGINAL);
        tag.removeTag(KEY_PREVIEW_SOURCE_REGIONS);
        tag.removeTag(KEY_PREVIEW_SOURCE_DIMENSION);
        tag.removeTag(KEY_PREVIEW_SOURCE_WORLD);
    }

    @SideOnly(Side.CLIENT)
    public static void clearClientRuntimeSelections() {
        selection = null;
        activeSelectionStackId = null;
        clearSelectionRedoHistory();
        restoreRuntimeHoverAfterLocalChange = false;
        lastHoverDebugState = null;
        clearPendingPreview();
    }

    @SideOnly(Side.CLIENT)
    public static void undoLastSelectionRegion(EntityPlayer player, ItemStack stack) {
        cancelLastRegionPointMarkMode();
        activateSelectionStack(stack);
        if (restoreRecipeSaveSelection(player, stack))
            return;
        if (hasActivePreview(stack)) {
            LittleVecXDebugLog.debug(LOGGER, "LittleVecX industrial selection undo skipped: activePreview=true");
            return;
        }

        // Regions live in the item NBT while ShapeSelection is client-only and disappears on a
        // world reload. Undo must therefore operate on the durable representation.
        List<IndustrialSelectionRegion> regions = copyRegions(IndustrialSelectionMode.getCurrentRegions(stack));
        LittleVecXDebugLog.debug(LOGGER, "LittleVecX industrial selection undo: currentRegions={}, redoDepth={}", regions.size(), selectionRedoHistory.size());
        if (regions.isEmpty()) {
            player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.industrial.no_selection"), true);
            return;
        }

        List<IndustrialSelectionRegion> removed = new ArrayList<>(1);
        removed.add(regions.remove(regions.size() - 1));

        selectionRedoHistory.push(removed);
        selection = null;
        IndustrialSelectionMode.setCurrentRegions(stack, regions);
        restoreRuntimeHoverAfterLocalChange = !regions.isEmpty();
        sendSelectionToServer(stack);
        player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.industrial.selection_undo"), true);
    }

    @SideOnly(Side.CLIENT)
    public static void redoLastSelectionRegion(EntityPlayer player, ItemStack stack) {
        activateSelectionStack(stack);
        if (hasActivePreview(stack)) {
            LittleVecXDebugLog.debug(LOGGER, "LittleVecX industrial selection redo skipped: activePreview=true");
            return;
        }
        LittleVecXDebugLog.debug(LOGGER, "LittleVecX industrial selection redo: currentRegions={}, redoDepth={}",
                IndustrialSelectionMode.getCurrentRegions(stack).size(), selectionRedoHistory.size());
        if (selectionRedoHistory.isEmpty()) {
            player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.industrial.no_selection_redo"), true);
            return;
        }

        List<IndustrialSelectionRegion> restore = selectionRedoHistory.pop();
        List<IndustrialSelectionRegion> regions = copyRegions(IndustrialSelectionMode.getCurrentRegions(stack));
        regions.addAll(copyRegions(restore));

        selection = null;
        IndustrialSelectionMode.setCurrentRegions(stack, regions);
        restoreRuntimeHoverAfterLocalChange = !regions.isEmpty();
        sendSelectionToServer(stack);
        player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.industrial.selection_redo"), true);
    }

    /**
     * A selected area is durable tool state. A preview started by '[' or ']' is not: after a
     * reconnect there is no client mark-mode to control it, so retaining only its LittleTiles
     * preview data leaves the one-block white ghost seen on world load.
     */
    public static boolean clearInterruptedIndustrialPreview(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemLittleVecXIndustrialTool) || !isMovePreviewActive(stack))
            return false;

        com.creativemd.littletiles.common.tile.preview.LittlePreview.removePreviewTiles(stack);
        clearMovePreviewRegions(stack);
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag(KEY_PREVIEW_SOURCE_REGIONS);
            stack.getTagCompound().removeTag(KEY_PREVIEW_SOURCE_DIMENSION);
            stack.getTagCompound().removeTag(KEY_PREVIEW_SOURCE_WORLD);
        }
        return true;
    }

    public static boolean isMovePreviewActive(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey(KEY_MOVE_PREVIEW_REGIONS);
    }

    @SideOnly(Side.CLIENT)
    public static void finishMovePreview(EntityPlayer player, ItemStack stack) {
        activateSelectionStack(stack);
        if (!(stack.getItem() instanceof ItemLittleVecXIndustrialTool) || !isMovePreviewActive(stack))
            return;

        if (shouldDeleteMovePreviewOriginal(stack)) {
            cancelMovePreview(stack);
            if (selection != null)
                syncSelectionToStack(stack);
            sendSelectionToServer(stack);
        }
    }

    /** Consumes the source selection only after '[' or ']' was successfully placed. */
    @SideOnly(Side.CLIENT)
    public static void finishSelectionPreviewPlacement(EntityPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemLittleVecXIndustrialTool))
            return;

        activateSelectionStack(stack);
        com.creativemd.littletiles.common.tile.preview.LittlePreview.removePreviewTiles(stack);
        clearMovePreviewRegions(stack);
        clearPendingPreview();
        selection = null;
        clearSelectionRedoHistory();
        restoreRuntimeHoverAfterLocalChange = false;
        syncSelectionToStack(stack);
        sendCurrentSelectionToServer();
        LittleVecXIndustrialSelectionHighlightHandler.clearVisibleSelectionCache(player);
    }

    public static boolean isFiltered() {
        return activeFilter;
    }

    public static void setFilter(boolean active, @Nullable TileSelector filter) {
        activeFilter = active;
        currentFilter = filter;
    }

    @Nullable
    public static TileSelector getFilter() {
        return currentFilter;
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    private static IMarkMode createLastRegionPointMarkMode(ItemStack stack) {
        if (selection == null)
            return null;

        List<ShapeSelectPos> points = getSelectionPoints();
        if (points.size() < 2)
            return null;

        List<ShapeSelectPos> snapshot = new ArrayList<>(points.size());
        for (ShapeSelectPos point : points)
            snapshot.add(point.copy());
        int lastIndex = points.size() - 1;
        return new IndustrialLastRegionPointMarkMode(stack, snapshot, points.size() - 2, lastIndex, selection.allowLowResolution(), snapshot.get(lastIndex).getContext());
    }

    @SideOnly(Side.CLIENT)
    public static boolean isLastRegionPointMarkModeActive() {
        return PreviewRenderer.marked instanceof IndustrialLastRegionPointMarkMode;
    }

    /** Undo/redo supersedes point editing; discard the temporary mark UI before mutating the selection. */
    @SideOnly(Side.CLIENT)
    private static void cancelLastRegionPointMarkMode() {
        if (isLastRegionPointMarkModeActive())
            PreviewRenderer.marked = null;
    }

    /** A normal left click confirms the edited point instead of starting a new selection point. */
    @SideOnly(Side.CLIENT)
    private static boolean confirmLastRegionPointMarkMode() {
        if (!(PreviewRenderer.marked instanceof IndustrialLastRegionPointMarkMode))
            return false;

        IndustrialLastRegionPointMarkMode mark = (IndustrialLastRegionPointMarkMode) PreviewRenderer.marked;
        mark.done();
        PreviewRenderer.marked = null;
        restoreRuntimeHoverAfterLocalChange = true;
        lastHoverDebugState = null;
        LittleVecXDebugLog.debug(LOGGER, "LittleVecX industrial mark confirm: selectionPoints={}, regions={}, restoreHover={}", getSelectionPoints().size(),
                IndustrialSelectionMode.getCurrentRegions(mark.stack).size(), restoreRuntimeHoverAfterLocalChange);
        return true;
    }

    /** Mark-mode adapter that exposes only the final point of the last completed industrial region. */
    @SideOnly(Side.CLIENT)
    private static class IndustrialLastRegionPointMarkMode implements IMarkMode {

        private final ItemStack stack;
        private final List<ShapeSelectPos> points;
        private final int firstIndex;
        private final int pointIndex;
        private final boolean allowLowResolution;
        private final LittleGridContext editContext;

        private IndustrialLastRegionPointMarkMode(ItemStack stack, List<ShapeSelectPos> points, int firstIndex, int pointIndex, boolean allowLowResolution,
                LittleGridContext editContext) {
            this.stack = stack;
            this.points = points;
            this.firstIndex = firstIndex;
            this.pointIndex = pointIndex;
            this.allowLowResolution = allowLowResolution;
            this.editContext = editContext;
        }

        @Override
        public boolean allowLowResolution() {
            return allowLowResolution;
        }

        @Override
        public PlacementPosition getPosition() {
            // pointIndex is validated when this snapshot is created and the snapshot is immutable in size.
            return points.get(pointIndex).pos.copy();
        }

        @Override
        public SubGui getConfigurationGui() {
            return selection == null ? null : selection.getConfigurationGui();
        }

        @Override
        public void render(LittleGridContext context, double x, double y, double z) {
            ShapeSelectPos first = getPoint(firstIndex);
            ShapeSelectPos last = getPoint(pointIndex);
            if (first == null || last == null)
                return;

            IndustrialSelectionRegion region = new IndustrialSelectionRegion(first.pos.copy(), last.pos.copy(), last.pos.facing);
            LittleBoxes boxes = INDUSTRIAL_SELECTION_MODE.buildBoxes(Collections.singletonList(region));
            LittleVecXIndustrialSelectionHighlightHandler.renderEditingRegion(boxes, x, y, z);
            // Keep the outline on the selection's configured recipe grid, not the renderer's block grid.
            last.render(editContext, x, y, z, true);
        }

        @Override
        public void move(LittleGridContext context, EnumFacing facing) {
            ShapeSelectPos point = getPoint(pointIndex);
            if (point == null)
                return;

            // Coordinates are stored in units of editContext: one unit is one configured grid cell.
            LittleVec offset = new LittleVec(facing);
            point.pos.subVec(offset);
            applySelectionPointEdit(stack, points, false);
        }

        @Override
        public void done() {
            applySelectionPointEdit(stack, points, true);
        }

        @Nullable
        private ShapeSelectPos getPoint(int index) {
            return index >= 0 && index < points.size() ? points.get(index) : null;
        }
    }

    @SideOnly(Side.CLIENT)
    public static void deleteCurrentSelection(EntityPlayer player, ItemStack stack) {
        ensureIndustrialMode(stack);
        activateSelectionStack(stack);
        if (selection != null)
            syncSelectionToStack(stack);
        LittleBoxes boxes = IndustrialSelectionMode.buildSavedOrCurrentBoxes(stack);
        if (boxes.isEmpty()) {
            player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.industrial.no_selection"), true);
            return;
        }

        saveCrashBackup(player, IndustrialSelectionMode.getSavedOrCurrentRegions(stack), "industrial_delete");
        if (isFiltered() && getFilter() != null)
            new LittleActionDestroyBoxesFiltered(boxes, getFilter()).execute();
        else
            new LittleActionDestroyBoxes(boxes).execute();
        player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.industrial.deleted"), true);
        if (selection != null)
            syncSelectionToStack(stack);
        sendSelectionToServer(stack);
    }

    @SideOnly(Side.CLIENT)
    private boolean prepareMovePreview(EntityPlayer player, ItemStack stack, PlacementPosition position, boolean deleteOriginal) {
        ensureIndustrialMode(stack);
        activateSelectionStack(stack);
        if (selection != null)
            syncSelectionToStack(stack);
        List<IndustrialSelectionRegion> currentRegions = IndustrialSelectionMode.getCurrentRegions(stack);
        List<IndustrialSelectionRegion> savedOrCurrentRegions = IndustrialSelectionMode.getSavedOrCurrentRegions(stack);
        List<IndustrialSelectionRegion> regions = currentRegions.isEmpty() ? savedOrCurrentRegions : currentRegions;
        LittleVecXDebugLog.debug(LOGGER,
                "LittleVecX industrial preview source: deleteOriginal={}, currentRegions={}, currentMin={}, selectedRegions={}, selectedMin={}, selectedBounds={}, rayPosition={}",
                deleteOriginal, currentRegions.size(), describeRegionMin(currentRegions), regions.size(), describeRegionMin(regions), describeRegionBounds(regions), position);
        if (regions.isEmpty())
            return false;

        try {
            markMovePreviewRegions(stack, regions, deleteOriginal, player.world);
            LittlePreviews previews = INDUSTRIAL_SELECTION_MODE.getPreviews(player.world, player, stack, true, true, true, false);
            saveLittlePreview(stack, previews);
            if (deleteOriginal && hasLittlePreview(stack)) {
                LittleVecXCrashBackup.save(player, previews, "industrial_move");
                destroyRegions(regions);
                selection = null;
                clearSelectionRedoHistory();
                syncSelectionToStack(stack);
                rememberPendingPreview(stack);
                sendCurrentSelectionToServer();
                LittleVecXIndustrialSelectionHighlightHandler.clearVisibleSelectionCache(player);
            } else {
                rememberPendingPreview(stack);
            }
            return hasLittlePreview(stack);
        } catch (LittleActionException e) {
            clearMovePreviewRegions(stack);
            clearPendingPreview();
            LittleAction.handleExceptionClient(e);
            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    private static void cancelMovePreview(ItemStack stack) {
        ensureIndustrialMode(stack);
        com.creativemd.littletiles.common.tile.preview.LittlePreview.removePreviewTiles(stack);
        clearMovePreviewRegions(stack);
        clearPendingPreview();
    }

    private static void markMovePreviewRegions(ItemStack stack, List<IndustrialSelectionRegion> regions, boolean deleteOriginal, World world) {
        ensureTag(stack);
        net.minecraft.nbt.NBTTagList list = new net.minecraft.nbt.NBTTagList();
        for (IndustrialSelectionRegion region : regions)
            if (region != null && region.isValid())
                list.appendTag(region.writeToNBT(new NBTTagCompound()));
        stack.getTagCompound().setTag(KEY_MOVE_PREVIEW_REGIONS, list);
        stack.getTagCompound().setBoolean(KEY_MOVE_PREVIEW_DELETE_ORIGINAL, deleteOriginal);
        storePreviewSourceWorld(stack, world);
    }

    private static void storePreviewSourceRegions(ItemStack stack, List<IndustrialSelectionRegion> regions, World world) {
        ensureTag(stack);
        net.minecraft.nbt.NBTTagList list = new net.minecraft.nbt.NBTTagList();
        for (IndustrialSelectionRegion region : regions)
            if (region != null && region.isValid())
                list.appendTag(region.writeToNBT(new NBTTagCompound()));
        stack.getTagCompound().setTag(KEY_PREVIEW_SOURCE_REGIONS, list);
        storePreviewSourceWorld(stack, world);
    }

    private static void storePreviewSourceWorld(ItemStack stack, World world) {
        ensureTag(stack);
        if (world == null || world.provider == null)
            return;

        stack.getTagCompound().setInteger(KEY_PREVIEW_SOURCE_DIMENSION, world.provider.getDimension());
        stack.getTagCompound().setString(KEY_PREVIEW_SOURCE_WORLD, getWorldName(world));
    }

    private static List<IndustrialSelectionRegion> getMovePreviewRegions(ItemStack stack) {
        List<IndustrialSelectionRegion> regions = new ArrayList<>();
        if (!stack.hasTagCompound())
            return regions;

        net.minecraft.nbt.NBTTagList list = stack.getTagCompound().getTagList(KEY_MOVE_PREVIEW_REGIONS, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            IndustrialSelectionRegion region = IndustrialSelectionRegion.readFromNBT(list.getCompoundTagAt(i));
            if (region != null && region.isValid())
                regions.add(region);
        }
        return regions;
    }

    private static void clearMovePreviewRegions(ItemStack stack) {
        ensureTag(stack);
        stack.getTagCompound().removeTag(KEY_MOVE_PREVIEW_REGIONS);
        stack.getTagCompound().removeTag(KEY_MOVE_PREVIEW_DELETE_ORIGINAL);
    }

    private static String describeRegionMin(List<IndustrialSelectionRegion> regions) {
        return regions == null || regions.isEmpty() ? "none" : String.valueOf(IndustrialSelectionMode.getGlobalMinPosition(regions));
    }

    /** Logs the durable selection coordinates, not LittleTiles' later normalisation offsets. */
    private static String describeRegionBounds(List<IndustrialSelectionRegion> regions) {
        if (regions == null || regions.isEmpty())
            return "none";

        StringBuilder result = new StringBuilder("[");
        for (IndustrialSelectionRegion region : regions) {
            if (region == null)
                continue;
            LittleAbsoluteBox box = region.toAbsoluteBox();
            if (box == null)
                continue;
            if (result.length() > 1)
                result.append(", ");
            result.append("{first=").append(region.first).append(", second=").append(region.second).append(", pos=").append(box.pos).append(", box=").append(box.box)
                    .append(", context=").append(box.context).append('}');
        }
        return result.append(']').toString();
    }

    @SideOnly(Side.CLIENT)
    private static String describePlacementPreview(@Nullable PlacementPreview preview) {
        if (preview == null)
            return "none";
        return "{pos=" + preview.pos + ", cachedOffset=" + preview.cachedOffset + ", box=" + preview.box + ", size=" + preview.size + ", context=" + preview.context
                + ", placedBounds=" + describePlacedBounds(preview) + ", fixed=" + preview.fixed + "}";
    }

    @SideOnly(Side.CLIENT)
    private static String describePlacedBounds(PlacementPreview preview) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (PlacePreview placed : preview.getPreviews()) {
            if (placed == null || placed.box == null)
                continue;
            minX = Math.min(minX, placed.box.minX);
            minY = Math.min(minY, placed.box.minY);
            minZ = Math.min(minZ, placed.box.minZ);
            maxX = Math.max(maxX, placed.box.maxX);
            maxY = Math.max(maxY, placed.box.maxY);
            maxZ = Math.max(maxZ, placed.box.maxZ);
        }
        if (minX == Integer.MAX_VALUE)
            return "none";
        return "[" + minX + ',' + minY + ',' + minZ + " -> " + maxX + ',' + maxY + ',' + maxZ + ']';
    }

    private static boolean shouldDeleteMovePreviewOriginal(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().getBoolean(KEY_MOVE_PREVIEW_DELETE_ORIGINAL);
    }

    public static boolean canPlacePreviewFromServerSource(ItemStack stack, World world) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemLittleVecXIndustrialTool))
            return false;
        if (!((ItemLittleVecXIndustrialTool) stack.getItem()).hasLittlePreview(stack))
            return false;
        if (isMovePreviewActive(stack) && shouldDeleteMovePreviewOriginal(stack))
            return false;
        if (!isPreviewSourceWorld(stack, world))
            return false;
        return !getServerPlacementRegions(stack).isEmpty();
    }

    public static boolean isPreviewSourceWorld(ItemStack stack, World world) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound() || world == null || world.provider == null)
            return false;
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey(KEY_PREVIEW_SOURCE_DIMENSION) || !tag.hasKey(KEY_PREVIEW_SOURCE_WORLD))
            return false;
        return tag.getInteger(KEY_PREVIEW_SOURCE_DIMENSION) == world.provider.getDimension()
                && tag.getString(KEY_PREVIEW_SOURCE_WORLD).equals(getWorldName(world));
    }

    public static List<IndustrialSelectionRegion> getServerPlacementRegions(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound())
            return new ArrayList<>();
        if (isMovePreviewActive(stack))
            return getMovePreviewRegions(stack);
        return readRegionList(stack, KEY_PREVIEW_SOURCE_REGIONS);
    }

    private static List<IndustrialSelectionRegion> readRegionList(ItemStack stack, String key) {
        List<IndustrialSelectionRegion> regions = new ArrayList<>();
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound())
            return regions;

        net.minecraft.nbt.NBTTagList list = stack.getTagCompound().getTagList(key, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            IndustrialSelectionRegion region = IndustrialSelectionRegion.readFromNBT(list.getCompoundTagAt(i));
            if (region != null && region.isValid())
                regions.add(region);
        }
        return regions;
    }

    private static String getWorldName(World world) {
        if (world == null || world.getWorldInfo() == null)
            return "";
        String name = world.getWorldInfo().getWorldName();
        return name == null ? "" : name;
    }

    private static void saveCrashBackup(EntityPlayer player, List<IndustrialSelectionRegion> regions, String reason) {
        if (player == null || player.world == null || regions == null || regions.isEmpty())
            return;

        LittlePreviews previews = INDUSTRIAL_SELECTION_MODE.getCrashBackupPreviews(player.world, regions, true, true, true, false);
        LittleVecXCrashBackup.save(player, previews, reason);
    }

    private static void destroyRegions(List<IndustrialSelectionRegion> regions) {
        LittleBoxes boxes = IndustrialSelectionMode.buildBoxes(regions);
        if (boxes.isEmpty())
            return;

        if (isFiltered() && getFilter() != null)
            new LittleActionDestroyBoxesFiltered(boxes, getFilter()).execute();
        else
            new LittleActionDestroyBoxes(boxes).execute();
    }

    @SideOnly(Side.CLIENT)
    private static boolean hasActivePreview(ItemStack stack) {
        return stack != null && (isMovePreviewActive(stack) || (stack.getItem() instanceof ItemLittleVecXIndustrialTool && ((ItemLittleVecXIndustrialTool) stack.getItem()).hasLittlePreview(stack)));
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    private IMarkMode createMovePreviewMarkMode(EntityPlayer player, ItemStack stack, PlacementPosition position, @Nullable PlacementPreview preview) {
        if (!isMovePreviewActive(stack) || preview == null)
            return null;

        // Keep the positioning calculation used by the stable move implementation. The
        // mark mode then switches LittleTiles into centred/non-fixed placement, so compensate
        // against the actual selected bounds rather than against its normalisation offsets.
        PlacementPosition correctedPosition = MarkMode.loadPosition(position.copy(), preview);
        PlacementPreview markedPreview = PlacementHelper.getPreviews(player.world, stack, correctedPosition, true, false, true, getPlacementMode(stack));
        if (markedPreview != null) {
            PreviewBounds sourceBounds = getPreviewSourceBounds(stack, markedPreview.context);
            PreviewBounds placedBounds = getPlacedBounds(markedPreview);
            if (sourceBounds != null && placedBounds != null) {
                // A selection box may include empty space. For example, a ceiling tile selected
                // down to a lower point has a box whose minimum is below the tile. Aligning to
                // that empty minimum mirrored the preview to the bottom of the selection.
                // LittlePreviews contains the actual copied tiles, so its bounds are the only
                // valid source anchor for copy and move previews.
                LittleAbsoluteVec expectedOrigin = sourceBounds.getMinimum();
                LittleAbsoluteVec actualOrigin = placedBounds.getMinimum();
                LittleAbsoluteVec delta = actualOrigin.copy();
                delta.sub(expectedOrigin);
                correctedPosition.sub(delta);
            }
        }
        correctedPosition.removeInternalBlockOffset();
        return new IndustrialPreviewMarkMode(correctedPosition);
    }

    @Nullable
    private static PlacementPosition getMovePreviewInitialPosition(ItemStack stack, PlacementPosition fallback) {
        List<IndustrialSelectionRegion> regions = getMovePreviewRegions(stack);
        if (regions.isEmpty())
            return fallback;

        BlockPos globalMin = IndustrialSelectionMode.getGlobalMinPosition(regions);
        return new PlacementPosition(globalMin, fallback.getContext(), LittleVec.ZERO.copy(), fallback.facing);
    }

    @Nullable
    private PreviewBounds getPreviewSourceBounds(ItemStack stack, LittleGridContext context) {
        List<IndustrialSelectionRegion> regions = getMovePreviewRegions(stack);
        if (regions.isEmpty())
            return null;

        BlockPos globalMin = IndustrialSelectionMode.getGlobalMinPosition(regions);
        LittlePreviews previews = getLittlePreview(stack, false);
        if (previews == null || previews.isEmptyIncludeChildren())
            return null;

        previews = previews.copy();
        previews.convertTo(context);
        PreviewBounds bounds = new PreviewBounds(globalMin, context);
        for (com.creativemd.littletiles.common.tile.preview.LittlePreview preview : previews.allPreviews()) {
            if (preview == null || preview.box == null)
                continue;
            bounds.include(preview.box.minX, preview.box.minY, preview.box.minZ, preview.box.maxX, preview.box.maxY, preview.box.maxZ);
        }
        return bounds.isValid() ? bounds : null;
    }

    @Nullable
    private static PreviewBounds getPlacedBounds(PlacementPreview preview) {
        PreviewBounds bounds = new PreviewBounds(preview.pos, preview.context);
        for (PlacePreview placed : preview.getPreviews()) {
            if (placed == null || placed.box == null)
                continue;
            bounds.include(placed.box.minX, placed.box.minY, placed.box.minZ, placed.box.maxX, placed.box.maxY, placed.box.maxZ);
        }
        return bounds.isValid() ? bounds : null;
    }

    private static class PreviewBounds {

        private final BlockPos origin;
        private final LittleGridContext context;
        private int minX = Integer.MAX_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int minZ = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE;
        private int maxY = Integer.MIN_VALUE;
        private int maxZ = Integer.MIN_VALUE;

        private PreviewBounds(BlockPos origin, LittleGridContext context) {
            this.origin = origin;
            this.context = context;
        }

        private void include(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = Math.min(this.minX, minX);
            this.minY = Math.min(this.minY, minY);
            this.minZ = Math.min(this.minZ, minZ);
            this.maxX = Math.max(this.maxX, maxX);
            this.maxY = Math.max(this.maxY, maxY);
            this.maxZ = Math.max(this.maxZ, maxZ);
        }

        private boolean isValid() {
            return minX != Integer.MAX_VALUE && maxX > minX && maxY > minY && maxZ > minZ;
        }

        private LittleAbsoluteVec getMinimum() {
            return new LittleAbsoluteVec(origin, context, new LittleVec(minX, minY, minZ));
        }
    }

    @SideOnly(Side.CLIENT)
    private static void clearSelectionRedoHistory() {
        selectionRedoHistory.clear();
    }

    /**
     * Dedicated preview controller for copying with '['. Moving with ']' retains LT's
     * offset-aware MarkMode from the known-good implementation above.
     * This controller deliberately does not use
     * LittleTiles' MarkMode: that mode rewrites the point to the preview centre and
     * renders its own white cube, which conflicts with industrial selection editing.
     */
    @SideOnly(Side.CLIENT)
    private static class IndustrialPreviewMarkMode implements IMarkMode {

        private final PlacementPosition position;

        private IndustrialPreviewMarkMode(PlacementPosition position) {
            this.position = position.copy();
            // These cubes belong to the block ray-traced before '[' or ']'. PreviewRenderer
            // renders them separately from the structure as a white hover cube, so they must
            // not follow the frozen preview position.
            this.position.positingCubes = null;
        }

        @Override
        public boolean allowLowResolution() {
            return true;
        }

        @Override
        public PlacementPosition getPosition() {
            return position.copy();
        }

        @Override
        public SubGui getConfigurationGui() {
            return null;
        }

        @Override
        public void render(LittleGridContext context, double x, double y, double z) {
            // The preview itself is the visual feedback. Do not draw MarkMode's white anchor box.
        }

        @Override
        public void move(LittleGridContext context, EnumFacing facing) {
            LittleVec offset = new LittleVec(facing.getOpposite());
            offset.scale(GuiScreen.isCtrlKeyDown() ? context.size : 1);
            position.sub(new LittleVecContext(offset, context));
        }

        @Override
        public void done() {
            // Placement is confirmed by the normal mouse action, not by LittleTiles' mark key.
        }
    }

    @SideOnly(Side.CLIENT)
    private static void rememberRecipeSaveSelectionRestore(List<IndustrialSelectionRegion> regions) {
        recipeSaveSelectionRestore.clear();
        recipeSaveSelectionRestore.addAll(copyRegions(regions));
    }

    @SideOnly(Side.CLIENT)
    public static void clearRecipeSaveSelectionRestore() {
        recipeSaveSelectionRestore.clear();
    }

    @SideOnly(Side.CLIENT)
    private static boolean restoreRecipeSaveSelection(EntityPlayer player, ItemStack stack) {
        if (recipeSaveSelectionRestore.isEmpty())
            return false;

        com.creativemd.littletiles.common.tile.preview.LittlePreview.removePreviewTiles(stack);
        clearMovePreviewRegions(stack);
        ensureIndustrialMode(stack);
        IndustrialSelectionMode.setCurrentRegions(stack, copyRegions(recipeSaveSelectionRestore));
        selection = null;
        clearSelectionRedoHistory();
        clearRecipeSaveSelectionRestore();
        sendSelectionToServer(stack);
        LittleVecXIndustrialSelectionHighlightHandler.clearVisibleSelectionCache(player);
        player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.industrial.selection_redo"), true);
        return true;
    }

    private static List<IndustrialSelectionRegion> copyRegions(List<IndustrialSelectionRegion> regions) {
        List<IndustrialSelectionRegion> copy = new ArrayList<>();
        if (regions == null)
            return copy;

        for (IndustrialSelectionRegion region : regions)
            if (region != null && region.isValid())
                copy.add(new IndustrialSelectionRegion(region.first, region.second, region.facing));
        return copy;
    }

    @SideOnly(Side.CLIENT)
    private static List<ShapeSelectPos> getSelectionPoints() {
        if (selection == null)
            return new ArrayList<>();

        try {
            Field field = getShapeSelectionPositionsField();
            @SuppressWarnings("unchecked")
            List<ShapeSelectPos> live = (List<ShapeSelectPos>) field.get(selection);
            return live;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to access industrial selection points", e);
        }
    }

    @SideOnly(Side.CLIENT)
    private static boolean hasUnfinishedSelectionRegion() {
        return getSelectionPoints().size() % 2 != 0;
    }

    /**
     * The selected stack can be re-synchronised by the server while the client-side
     * ShapeSelection is static state. Restore its committed point pairs from the item NBT so
     * the normal LT hover and the next region both continue from the existing selection.
     */
    @SideOnly(Side.CLIENT)
    private static boolean restoreRuntimeSelectionFromRegions(EntityPlayer player, ItemStack stack, PlacementPosition hoverPosition, RayTraceResult hoverResult) {
        List<IndustrialSelectionRegion> regions = IndustrialSelectionMode.getCurrentRegions(stack);
        LittleVecXDebugLog.debug(LOGGER, "LittleVecX industrial selection restore: hover={}, face={}, regions={}", hoverPosition, hoverResult == null ? "none" : hoverResult.sideHit,
                regions.size());
        if (regions.isEmpty() || hoverPosition == null || hoverResult == null)
            return false;

        selection = new ShapeSelection(stack, true);
        selection.setLast(player, stack, hoverPosition, hoverResult);
        ShapeSelectPos template = selection.getLast().copy();
        List<ShapeSelectPos> points = new ArrayList<>(regions.size() * 2);
        for (IndustrialSelectionRegion region : regions) {
            if (region == null || !region.isValid())
                continue;
            points.add(createSelectionPointFromRegion(template, region.first, region.facing));
            points.add(createSelectionPointFromRegion(template, region.second, region.facing));
        }
        if (!points.isEmpty())
            applySelectionPointEdit(stack, points, false);
        else
            selection = null;
        LittleVecXDebugLog.debug(LOGGER, "LittleVecX industrial selection restore result: selectionPoints={}, selectionPresent={}", getSelectionPoints().size(), selection != null);
        return selection != null;
    }

    @SideOnly(Side.CLIENT)
    private static void logHoverState(String phase, PlacementPosition position, @Nullable RayTraceResult result, int regionCount) {
        String state = phase + "|selection=" + (selection == null ? "none" : getSelectionPoints().size()) + "|regions=" + regionCount + "|restore="
                + restoreRuntimeHoverAfterLocalChange;
        if (state.equals(lastHoverDebugState))
            return;
        lastHoverDebugState = state;
        LittleVecXDebugLog.debug(LOGGER, "LittleVecX industrial hover {}: position={}, face={}, selectionPoints={}, regions={}, restoreHover={}", phase, position,
                result == null ? "none" : result.sideHit, selection == null ? 0 : getSelectionPoints().size(), regionCount, restoreRuntimeHoverAfterLocalChange);
    }

    @SideOnly(Side.CLIENT)
    private static ShapeSelectPos createSelectionPointFromRegion(ShapeSelectPos template, LittleAbsoluteVec point, EnumFacing facing) {
        ShapeSelectPos restored = template.copy();
        restored.pos.assign(point);
        restored.pos.facing = facing;
        return restored;
    }

    @SideOnly(Side.CLIENT)
    private static void syncSelectionAfterPointEdit(ItemStack stack, List<ShapeSelectPos> points) {
        applySelectionPointEdit(stack, points, true);
    }

    /** Keeps arrow-key edits client-local until mark mode is confirmed, avoiding a selection reset per key repeat. */
    @SideOnly(Side.CLIENT)
    private static void applySelectionPointEdit(ItemStack stack, List<ShapeSelectPos> points, boolean sendToServer) {
        try {
            if (points.isEmpty()) {
                selection = null;
            } else {
                // ShapeSelection and undo are allowed to mutate their own list. Mark mode retains
                // its snapshot, so an undo cannot empty it while the renderer still uses it.
                List<ShapeSelectPos> appliedPoints = copySelectionPoints(points);
                if (selection == null)
                    selection = new ShapeSelection(stack, true);

                getShapeSelectionPositionsField().set(selection, appliedPoints);
                getShapeSelectionLastField().set(selection, appliedPoints.get(appliedPoints.size() - 1).copy());
                getShapeSelectionPosField().set(selection, appliedPoints.get(0).pos.getPos());
                getShapeSelectionContextField().set(selection, appliedPoints.get(0).getContext());
                selection.deleteCache();
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to update industrial selection points", e);
        }

        syncSelectionToStack(stack);
        if (sendToServer)
            sendCurrentSelectionToServer();
    }

    @SideOnly(Side.CLIENT)
    private static List<ShapeSelectPos> copySelectionPoints(List<ShapeSelectPos> points) {
        List<ShapeSelectPos> copies = new ArrayList<>(points.size());
        for (ShapeSelectPos point : points)
            copies.add(point.copy());
        return copies;
    }

    @SideOnly(Side.CLIENT)
    private static Field getShapeSelectionPositionsField() throws NoSuchFieldException {
        if (shapeSelectionPositionsField == null) {
            shapeSelectionPositionsField = ShapeSelection.class.getDeclaredField("positions");
            shapeSelectionPositionsField.setAccessible(true);
        }
        return shapeSelectionPositionsField;
    }

    @SideOnly(Side.CLIENT)
    private static Field getShapeSelectionLastField() throws NoSuchFieldException {
        if (shapeSelectionLastField == null) {
            shapeSelectionLastField = ShapeSelection.class.getDeclaredField("last");
            shapeSelectionLastField.setAccessible(true);
        }
        return shapeSelectionLastField;
    }

    @SideOnly(Side.CLIENT)
    private static Field getShapeSelectionPosField() throws NoSuchFieldException {
        if (shapeSelectionPosField == null) {
            shapeSelectionPosField = ShapeSelection.class.getDeclaredField("pos");
            shapeSelectionPosField.setAccessible(true);
        }
        return shapeSelectionPosField;
    }

    @SideOnly(Side.CLIENT)
    private static Field getShapeSelectionContextField() throws NoSuchFieldException {
        if (shapeSelectionContextField == null) {
            shapeSelectionContextField = ShapeSelection.class.getDeclaredField("context");
            shapeSelectionContextField.setAccessible(true);
        }
        return shapeSelectionContextField;
    }

    private static void ensureTag(ItemStack stack) {
        if (!stack.hasTagCompound())
            stack.setTagCompound(new NBTTagCompound());
    }

    @SideOnly(Side.CLIENT)
    private static String getKeyName(@Nullable KeyBinding keyBinding, String fallback) {
        if (keyBinding == null)
            return fallback;
        String name = keyBinding.getDisplayName();
        return name != null && !name.isEmpty() ? name : fallback;
    }
}
