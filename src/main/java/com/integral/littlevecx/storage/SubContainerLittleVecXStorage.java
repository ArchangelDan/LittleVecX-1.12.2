package com.integral.littlevecx.storage;

import com.creativemd.creativecore.common.gui.container.SubContainer;
import com.creativemd.creativecore.common.slots.SlotStackLimit;
import com.creativemd.creativecore.common.utils.mc.InventoryUtils;
import com.creativemd.littletiles.common.container.SubContainerStorage.StorageSize;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class SubContainerLittleVecXStorage extends SubContainer {

    public StructureLittleVecXStorage storage;
    public final StorageSize size;

    public SubContainerLittleVecXStorage(EntityPlayer player, StructureLittleVecXStorage storage) {
        super(player);
        this.storage = storage;
        this.size = StorageSize.getSizeFromInventory(storage.inventory);
        if (!player.world.isRemote)
            this.storage.openContainer(this);
    }

    @Override
    public void createControls() {
        if (storage.inventory == null)
            return;

        int slotsPerRow = size.width / 18;
        int rows = (int) Math.ceil(storage.inventory.getSizeInventory() / (double) slotsPerRow);
        int rowWidth = Math.min(slotsPerRow, storage.inventory.getSizeInventory()) * 18;
        int offsetX = (size.width - rowWidth) / 2;

        for (int i = 0; i < storage.inventory.getSizeInventory(); i++) {
            int row = i / slotsPerRow;
            int rowIndex = i - row * slotsPerRow;
            addSlotToContainer(new SlotStackLimit(storage.inventory, i, offsetX + rowIndex * 18, 5 + row * 18, storage.inventory.getInventoryStackLimit()));
        }

        addPlayerSlotsToContainer(player, size.playerOffsetX, size.playerOffsetY);
    }

    @Override
    public void writeOpeningNBT(NBTTagCompound nbt) {
        nbt.setTag("inventory", InventoryUtils.saveInventoryBasic(storage.inventory));
    }

    @Override
    public void onPacketReceive(NBTTagCompound nbt) {
        if (isRemote() && nbt.hasKey("inventory")) {
            ItemStack[] stacks = InventoryUtils.loadInventory(nbt.getCompoundTag("inventory"));
            for (int i = 0; i < stacks.length; i++)
                storage.inventory.setInventorySlotContents(i, stacks[i]);
        }
        if (nbt.getBoolean("sort")) {
            InventoryUtils.sortInventory(storage.inventory, false);
            NBTTagCompound update = new NBTTagCompound();
            update.setTag("inventory", InventoryUtils.saveInventoryBasic(storage.inventory));
            sendNBTUpdate(update);
        }
    }

    @Override
    public void onClosed() {
        super.onClosed();
        if (storage != null && !player.world.isRemote)
            storage.closeContainer(this);
    }
}
