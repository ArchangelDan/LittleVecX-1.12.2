package com.integral.littlevecx.client.gui;

import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiCheckBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiIconButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiStateButton;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlClickEvent;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.creativemd.littletiles.client.gui.controls.GuiTileViewer;
import com.creativemd.littletiles.client.gui.controls.GuiTileViewer.GuiTileViewerAxisChangedEvent;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiDialogAxis;
import com.creativemd.littletiles.common.entity.AnimationPreview;
import com.creativemd.littletiles.common.structure.animation.AnimationGuiHandler;
import com.creativemd.littletiles.common.structure.relative.StructureAbsolute;
import com.creativemd.littletiles.common.structure.relative.StructureRelative;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiLittleVecXRotatedAxisButton extends SubGuiDialogAxis.GuiAxisButton {

    private static final BlockPos PREVIEW_ANCHOR = new BlockPos(0, 75, 0);

    public int[] axisData;

    public GuiLittleVecXRotatedAxisButton(String name, String caption, int x, int y, int width, int height, LittleGridContext context, int[] initialAxisData,
            AnimationGuiHandler handler) {
        super(name, caption, x, y, width, height, context, null, handler);
        axisData = initialAxisData;

        if (initialAxisData != null && initialAxisData.length == 7) {
            StructureRelative rel = new StructureRelative(initialAxisData);
            viewer.setEven(rel.isEven());
            viewer.setAxis(rel.getBox(), rel.getContext());
        }
    }

    public void updateAxisFromViewer() {
        if (viewer == null || viewer.getBox() == null || viewer.getAxisContext() == null)
            return;
        axisData = new StructureRelative(viewer.getBox().copy(), viewer.getAxisContext()).write();

        if (handler != null)
            handler.setCenter(new StructureAbsolute(PREVIEW_ANCHOR, viewer.getBox().copy(), viewer.getAxisContext()));
    }

    @Override
    public void onClicked(int x, int y, int button) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("dialog", true);

        SubGuiLittleVecXRotatedAxis dialog = new SubGuiLittleVecXRotatedAxis(this);
        SubGui owner = getGui();
        if (owner == null || owner.gui == null)
            return;
        dialog.gui = owner.gui;

        PacketHandler.sendPacketToServer(new GuiLayerPacket(nbt, dialog.gui.getLayers().size() - 1, false));
        dialog.container = new SubContainerEmpty(getPlayer());
        dialog.gui.addLayer(dialog);
        dialog.onOpened();
    }

    @Override
    public void onLoaded(AnimationPreview animationPreview) {
        super.onLoaded(animationPreview);
        updateAxisFromViewer();
    }

    @SideOnly(Side.CLIENT)
    public static class SubGuiLittleVecXRotatedAxis extends SubGui {

        public final GuiLittleVecXRotatedAxisButton activator;

        public SubGuiLittleVecXRotatedAxis(GuiLittleVecXRotatedAxisButton activator) {
            super(160, 130);
            this.activator = activator;
        }

        @Override
        public void createControls() {
            addControl(activator.viewer);

            addControl(new GuiIconButton("reset view", 20, 107, 8) {
                @Override
                public void onClicked(int x, int y, int button) {
                    activator.viewer.offsetX.set(0);
                    activator.viewer.offsetY.set(0);
                    activator.viewer.scale.set(40);
                }
            }.setCustomTooltip("reset view"));

            addControl(new GuiIconButton("change view", 40, 107, 7) {
                @Override
                public void onClicked(int x, int y, int button) {
                    switch (activator.viewer.getAxis()) {
                    case X:
                        activator.viewer.setViewAxis(EnumFacing.Axis.Y);
                        break;
                    case Y:
                        activator.viewer.setViewAxis(EnumFacing.Axis.Z);
                        break;
                    case Z:
                        activator.viewer.setViewAxis(EnumFacing.Axis.X);
                        break;
                    default:
                        break;
                    }
                }
            }.setCustomTooltip("change view"));

            addControl(new GuiIconButton("flip view", 60, 107, 4) {
                @Override
                public void onClicked(int x, int y, int button) {
                    activator.viewer.setViewDirection(activator.viewer.getViewDirection().getOpposite());
                }
            }.setCustomTooltip("flip view"));

            addControl(new GuiIconButton("up", 124, 33, 1) {
                @Override
                public void onClicked(int x, int y, int button) {
                    activator.viewer.moveY(GuiScreen.isCtrlKeyDown() ? activator.viewer.context.size : 1);
                }
            });

            addControl(new GuiIconButton("right", 141, 50, 0) {
                @Override
                public void onClicked(int x, int y, int button) {
                    activator.viewer.moveX(GuiScreen.isCtrlKeyDown() ? activator.viewer.context.size : 1);
                }
            });

            addControl(new GuiIconButton("left", 107, 50, 2) {
                @Override
                public void onClicked(int x, int y, int button) {
                    activator.viewer.moveX(-(GuiScreen.isCtrlKeyDown() ? activator.viewer.context.size : 1));
                }
            });

            addControl(new GuiIconButton("down", 124, 50, 3) {
                @Override
                public void onClicked(int x, int y, int button) {
                    activator.viewer.moveY(-(GuiScreen.isCtrlKeyDown() ? activator.viewer.context.size : 1));
                }
            });

            addControl(new GuiCheckBox("even", 107, 0, activator.viewer.isEven()));

            GuiStateButton contextBox = new GuiStateButton("grid", LittleGridContext.getNames().indexOf(activator.viewer.getAxisContext() + ""), 107, 80, 20, 12,
                    LittleGridContext.getNames().toArray(new String[0]));
            addControl(contextBox);

            addControl(new GuiButton("close", 125, 110) {
                @Override
                public void onClicked(int x, int y, int button) {
                    activator.updateAxisFromViewer();
                    onClosed();
                    gui.removeLayer(SubGuiLittleVecXRotatedAxis.this);
                }
            });
        }

        @CustomEventSubscribe
        @SideOnly(Side.CLIENT)
        public void onAxisChanged(GuiTileViewerAxisChangedEvent event) {
            if (activator.handler != null) {
                GuiTileViewer viewer = (GuiTileViewer) event.source;
                activator.handler.setCenter(new StructureAbsolute(PREVIEW_ANCHOR, viewer.getBox().copy(), viewer.getAxisContext()));
            }
        }

        @CustomEventSubscribe
        @SideOnly(Side.CLIENT)
        public void onButtonClicked(GuiControlClickEvent event) {
            if (event.source.is("even")) {
                GuiTileViewer viewer = (GuiTileViewer) event.source.parent.get("tileviewer");
                viewer.setEven(((GuiCheckBox) event.source).value);
            }
        }

        @CustomEventSubscribe
        @SideOnly(Side.CLIENT)
        public void onStateChange(GuiControlChangedEvent event) {
            if (event.source.is("grid")) {
                GuiStateButton contextBox = (GuiStateButton) event.source;
                LittleGridContext context;
                try {
                    context = LittleGridContext.get(Integer.parseInt(contextBox.getCaption()));
                } catch (NumberFormatException e) {
                    context = LittleGridContext.get();
                }

                GuiTileViewer viewer = (GuiTileViewer) event.source.parent.get("tileviewer");
                LittleBox box = viewer.getBox();
                box.convertTo(viewer.getAxisContext(), context);

                if (viewer.isEven())
                    box.maxX = box.minX + 2;
                else
                    box.maxX = box.minX + 1;

                if (viewer.isEven())
                    box.maxY = box.minY + 2;
                else
                    box.maxY = box.minY + 1;

                if (viewer.isEven())
                    box.maxZ = box.minZ + 2;
                else
                    box.maxZ = box.minZ + 1;

                viewer.setAxis(box, context);
            }
        }
    }
}
