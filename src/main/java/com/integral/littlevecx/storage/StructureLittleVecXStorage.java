package com.integral.littlevecx.storage;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.creativecore.common.gui.controls.gui.GuiCheckBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiLabel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiTextfield;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.creativemd.creativecore.common.utils.mc.InventoryUtils;
import com.creativemd.creativecore.common.utils.math.BooleanUtils;
import com.creativemd.littletiles.client.gui.handler.LittleStructureGuiHandler;
import com.creativemd.littletiles.common.action.block.LittleActionActivated;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.animation.AnimationGuiHandler;
import com.creativemd.littletiles.common.structure.attribute.LittleStructureAttribute;
import com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException;
import com.creativemd.littletiles.common.structure.exception.NotYetConnectedException;
import com.creativemd.littletiles.common.structure.registry.LittleStructureGuiParser;
import com.creativemd.littletiles.common.structure.registry.LittleStructureRegistry;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.parent.IStructureTileList;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.util.ingredient.LittleIngredients;
import com.creativemd.littletiles.common.util.ingredient.StackIngredient;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class StructureLittleVecXStorage extends LittleStructure {

    public static final String GUI_ID = "advanced_storage_structure";
    public static final String INVENTORY_ID = "advanced_storage";

    private static final int INPUT_ACCESSED = 0;
    private static final int INPUT_FILLED = 1;

    private static final int DEFAULT_SLOTS = 27;
    private static final int MAX_SLOTS = 9 * 9 * 9; // 729, still usable with scrollbox; keep a sane limit.

    private final List<SubContainerLittleVecXStorage> openContainers = new ArrayList<>();

    public int slots = DEFAULT_SLOTS;
    public InventoryBasic inventory = null;
    public boolean noClip = false;

    public StructureLittleVecXStorage(LittleStructureType type, IStructureTileList mainBlock) {
        super(type, mainBlock);
    }

    @Override
    protected void loadFromNBTExtra(NBTTagCompound nbt) {
        slots = sanitizeSlots(nbt.getInteger("slots"));
        noClip = nbt.getBoolean("noClip");

        if (nbt.hasKey("inventory"))
            inventory = InventoryUtils.loadInventoryBasic(nbt.getCompoundTag("inventory"));
        else
            inventory = null;

        if (inventory == null)
            inventory = new InventoryBasic(INVENTORY_ID, false, slots);
        else
            slots = sanitizeSlots(inventory.getSizeInventory());

        inventory.addInventoryChangeListener((x) -> onInventoryChanged());

        // When structures are loaded in a preview/recipe GUI there is no world (mainBlock can be null).
        // Only touch signals when we actually exist in a real world on the server.
        if (hasWorld() && !getWorld().isRemote) {
            updateAccessedInput();
            updateFilledInput();
            queueForNextTick();
        }
    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {
        nbt.setInteger("slots", sanitizeSlots(slots));
        if (inventory != null)
            nbt.setTag("inventory", InventoryUtils.saveInventoryBasic(inventory));
        if (noClip)
            nbt.setBoolean("noClip", true);
        else
            nbt.removeTag("noClip");
    }

    @Override
    public int getAttribute() {
        if (noClip)
            return super.getAttribute() | LittleStructureAttribute.NOCOLLISION;
        return super.getAttribute();
    }

    @Override
    public void afterPlaced() {
        super.afterPlaced();
        syncAttribute();
    }

    @Override
    public void onStructureDestroyed() {
        super.onStructureDestroyed();

        // `mainBlock` can already be removed at this point, so avoid calling getWorld() (it may return null).
        // `openContainers` is only populated server-side, so this is a safe proxy for the server check.
        for (SubContainerLittleVecXStorage container : openContainers) {
            container.storage = null;
            if (container.player instanceof EntityPlayerMP) {
                NBTTagCompound nbt = new NBTTagCompound();
                PacketHandler.sendPacketToPlayer(new GuiLayerPacket(nbt, container.getLayerID(), true), (EntityPlayerMP) container.player);
                container.closeLayer(nbt, true);
            }
        }
        openContainers.clear();
        if (hasWorld() && !getWorld().isRemote)
            updateAccessedInput();
    }

    private void syncAttribute() {
        if (!hasWorld() || getWorld().isRemote)
            return;
        try {
            tryAttributeChangeForBlocks();
        } catch (CorruptedConnectionException | NotYetConnectedException e) {
            // Ignore: structure may not be fully connected yet.
        }
    }

    @Override
    public boolean queueTick() {
        syncAttribute();
        if (hasWorld() && !getWorld().isRemote) {
            updateAccessedInput();
            updateFilledInput();
            // Inputs are restored before the parent signal graph is fully linked.
            // Propagate once on the next server tick even when their values did
            // not change, otherwise a pre-filled storage stays silent.
            notifyChange();
        }
        return false;
    }

    private static int sanitizeSlots(int slots) {
        if (slots <= 0)
            return DEFAULT_SLOTS;
        if (slots > MAX_SLOTS)
            return MAX_SLOTS;
        return slots;
    }

    private void updateAccessedInput() {
        getInput(INPUT_ACCESSED).updateState(new boolean[] { !openContainers.isEmpty() });
    }

    private void updateFilledInput() {
        if (inventory == null || inventory.getSizeInventory() <= 0) {
            getInput(INPUT_FILLED).updateState(BooleanUtils.toBits(0, 16));
            return;
        }

        int used = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++)
            used += inventory.getStackInSlot(i).getCount();

        int capacity = inventory.getSizeInventory() * inventory.getInventoryStackLimit();
        int filled = capacity <= 0 ? 0 : (int) (((double) used / capacity) * 65535);
        getInput(INPUT_FILLED).updateState(BooleanUtils.toBits(filled, 16));
    }

    public void onInventoryChanged() {
        if (!hasWorld() || getWorld().isRemote)
            return;
        updateFilledInput();
    }

    public void openContainer(SubContainerLittleVecXStorage container) {
        openContainers.add(container);
        updateAccessedInput();
    }

    public void closeContainer(SubContainerLittleVecXStorage container) {
        openContainers.remove(container);
        updateAccessedInput();
    }

    public boolean hasPlayerOpened(EntityPlayer player) {
        for (SubContainerLittleVecXStorage container : openContainers)
            if (container.getPlayer() == player)
                return true;
        return false;
    }

    @Override
    public boolean onBlockActivated(World worldIn, LittleTile tile, BlockPos pos, EntityPlayer playerIn, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side,
            float hitX, float hitY, float hitZ, LittleActionActivated action) {
        if (!worldIn.isRemote && !hasPlayerOpened(playerIn))
            LittleStructureGuiHandler.openGui(GUI_ID, new NBTTagCompound(), playerIn, this);
        return true;
    }

    public static class StructureLittleVecXStorageParser extends LittleStructureGuiParser {

        public StructureLittleVecXStorageParser(GuiParent parent, AnimationGuiHandler handler) {
            super(parent, handler);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void createControls(LittlePreviews previews, LittleStructure structure) {
            int slots = DEFAULT_SLOTS;
            boolean noClip = false;
            if (structure instanceof StructureLittleVecXStorage)
            {
                slots = ((StructureLittleVecXStorage) structure).slots;
                noClip = ((StructureLittleVecXStorage) structure).noClip;
            }

            parent.addControl(new GuiLabel(CoreControl.translate("gui.littlevecx.storage_slots"), 0, 0));
            parent.addControl(new GuiTextfield("slots", String.valueOf(slots), 0, 12, 60, 12).setNumbersOnly());
            parent.addControl(new GuiCheckBox("noClip", CoreControl.translate("gui.littlevecx.noclip"), 0, 30, noClip));
        }

        @Override
        @SideOnly(Side.CLIENT)
        public StructureLittleVecXStorage parseStructure(LittlePreviews previews) {
            StructureLittleVecXStorage storage = createStructure(StructureLittleVecXStorage.class, null);

            int slots = DEFAULT_SLOTS;
            try {
                slots = Integer.parseInt(((GuiTextfield) parent.get("slots")).text);
            } catch (NumberFormatException ignored) {
            }
            storage.slots = sanitizeSlots(slots);
            storage.inventory = new InventoryBasic(INVENTORY_ID, false, storage.slots);
            storage.noClip = ((GuiCheckBox) parent.get("noClip")).value;

            return storage;
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructureType getStructureType() {
            return LittleStructureRegistry.getStructureType(StructureLittleVecXStorage.class);
        }
    }

    public static class StructureLittleVecXStorageType extends LittleStructureType {

        public StructureLittleVecXStorageType(String id, String category, Class<? extends LittleStructure> structureClass, int attribute) {
            super(id, category, structureClass, attribute);
        }

        @Override
        public void addIngredients(LittlePreviews previews, LittleIngredients ingredients) {
            super.addIngredients(previews, ingredients);

            IInventory inventory = InventoryUtils.loadInventoryBasic(previews.structureNBT.getCompoundTag("inventory"));
            if (inventory != null)
                ingredients.add(new StackIngredient(inventory));
        }
    }
}
