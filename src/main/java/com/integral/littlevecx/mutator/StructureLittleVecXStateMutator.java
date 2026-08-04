package com.integral.littlevecx.mutator;

import java.util.ArrayList;
import java.util.Map.Entry;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.controls.gui.GuiCheckBox;
import com.creativemd.creativecore.common.utils.type.HashMapList;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.animation.AnimationGuiHandler;
import com.creativemd.littletiles.common.structure.attribute.LittleStructureAttribute;
import com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException;
import com.creativemd.littletiles.common.structure.exception.NotYetConnectedException;
import com.creativemd.littletiles.common.structure.registry.LittleStructureGuiParser;
import com.creativemd.littletiles.common.structure.registry.LittleStructureRegistry;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType;
import com.creativemd.littletiles.common.structure.signal.output.InternalSignalOutput;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.parent.IStructureTileList;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.integral.littlevecx.LittleVecXAnimationSyncHelper;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A simple "state mutator" that toggles tile visibility via {@link LittleTile#invisible}.
 * Output bit: true => visible, false => invisible.
 */
public class StructureLittleVecXStateMutator extends LittleStructure {

    private static final int OUTPUT_VISIBLE = 0;

    public boolean noClip = false;

    public StructureLittleVecXStateMutator(LittleStructureType type, IStructureTileList mainBlock) {
        super(type, mainBlock);
    }

    @Override
    protected void loadFromNBTExtra(NBTTagCompound nbt) {
        noClip = nbt.getBoolean("noClip");
        if (hasWorld() && !getWorld().isRemote)
            queueForNextTick();
    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {
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
        if (!isClient())
            applyVisibilityFromOutput();
    }

    @Override
    public void performInternalOutputChange(InternalSignalOutput output) {
        if (output.component.is("visible"))
            applyVisibilityFromOutput();
    }

    private void applyVisibilityFromOutput() {
        if (getOutput(OUTPUT_VISIBLE) == null)
            return;
        boolean visible = getOutput(OUTPUT_VISIBLE).getState()[0];
        setTilesInvisible(!visible);
    }

    private void setTilesInvisible(boolean invisible) {
        boolean changedAny = false;
        ArrayList<TileEntityLittleTiles> changedTiles = new ArrayList<>();
        try {
            HashMapList<BlockPos, IStructureTileList> blocks = collectAllBlocksListSameWorld();

            for (Entry<BlockPos, ArrayList<IStructureTileList>> entry : blocks.entrySet()) {
                if (entry.getValue().isEmpty())
                    continue;

                TileEntityLittleTiles te = entry.getValue().get(0).getTe();
                if (te == null)
                    continue;

                boolean changed = false;
                for (IStructureTileList list : entry.getValue()) {
                    for (LittleTile tile : list) {
                        if (tile.invisible != invisible) {
                            tile.invisible = invisible;
                            changed = true;
                            changedAny = true;
                        }
                    }
                }

                if (changed) {
                    te.updateTiles();
                    changedTiles.add(te);
                }
            }
        } catch (CorruptedConnectionException | NotYetConnectedException e) {
            // Ignore: structure may not be fully connected yet.
            return;
        }

        if (changedAny)
            LittleVecXAnimationSyncHelper.syncChangedTiles(this, changedTiles);
    }

    private void syncAttribute() {
        if (!hasWorld() || isClient())
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
        return false;
    }

    public static class StructureLittleVecXStateMutatorParser extends LittleStructureGuiParser {

        public StructureLittleVecXStateMutatorParser(com.creativemd.creativecore.common.gui.container.GuiParent parent, AnimationGuiHandler handler) {
            super(parent, handler);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void createControls(LittlePreviews previews, LittleStructure structure) {
            boolean noClip = false;
            if (structure instanceof StructureLittleVecXStateMutator)
                noClip = ((StructureLittleVecXStateMutator) structure).noClip;

            parent.addControl(new GuiCheckBox("noClip", CoreControl.translate("gui.littlevecx.noclip"), 0, 0, noClip));
        }

        @Override
        @SideOnly(Side.CLIENT)
        public LittleStructure parseStructure(LittlePreviews previews) {
            StructureLittleVecXStateMutator mutator = createStructure(StructureLittleVecXStateMutator.class, null);
            mutator.noClip = ((GuiCheckBox) parent.get("noClip")).value;
            return mutator;
        }

        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructureType getStructureType() {
            return LittleStructureRegistry.getStructureType(StructureLittleVecXStateMutator.class);
        }
    }
}
