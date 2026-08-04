package com.integral.littlevecx.client.gui.controls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.util.Color;

import com.creativemd.creativecore.common.gui.client.style.Style;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButtonHold;
import com.creativemd.creativecore.common.gui.controls.gui.GuiColorPlate;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;

import net.minecraftforge.fml.common.Loader;

public class GuiLittleVecXColorPicker extends com.creativemd.creativecore.common.gui.controls.gui.GuiColorPicker {

    private static final boolean ALET_LOADED = Loader.isModLoaded("alet");
    private final boolean hasAlpha;
    private final boolean hasShadeRow;
    public Color color;
    public GuiColorPickerPalette palette;
    public GuiLittleVecXColoredSteppedSlider sliderR;
    public GuiLittleVecXColoredSteppedSlider sliderG;
    public GuiLittleVecXColoredSteppedSlider sliderB;
    public GuiLittleVecXColoredSteppedSlider sliderS;
    private double oldShaderValue = 0;

    public GuiLittleVecXColorPicker(String name, int x, int y, Color color, boolean hasAlpha, int alphaMin) {
        super(name, x, y, color, hasAlpha, alphaMin);

        this.hasAlpha = hasAlpha;
        this.hasShadeRow = ALET_LOADED;
        this.width = 134 + getContentOffset() * 2;
        this.height = getPickerContentHeight() + getContentOffset() * 2;
        marginWidth = 0;
        this.color = color;
        setStyle(Style.emptyStyle);

        removeControls("null");

        addControl(new GuiButtonHold("r-", "<", 0, 0, 1, 5) {

            @Override
            public void onClicked(int x, int y, int button) {
                onColorChanged();
                sliderR.setValue(sliderR.value - 1);
                updateShadeSlider();
            }

        });
        addControl(new GuiButtonHold("r+", ">", 98, 0, 1, 5) {

            @Override
            public void onClicked(int x, int y, int button) {
                onColorChanged();
                sliderR.setValue(sliderR.value + 1);
                updateShadeSlider();
            }

        });

        addControl(new GuiButtonHold("g-", "<", 0, 10, 1, 5) {

            @Override
            public void onClicked(int x, int y, int button) {
                onColorChanged();
                sliderG.setValue(sliderG.value - 1);
                updateShadeSlider();
            }

        });
        addControl(new GuiButtonHold("g+", ">", 98, 10, 1, 5) {

            @Override
            public void onClicked(int x, int y, int button) {
                onColorChanged();
                sliderG.setValue(sliderG.value + 1);
                updateShadeSlider();
            }

        });

        addControl(new GuiButtonHold("b-", "<", 0, 20, 1, 5) {

            @Override
            public void onClicked(int x, int y, int button) {
                onColorChanged();
                sliderB.setValue(sliderB.value - 1);
                updateShadeSlider();
            }

        });
        addControl(new GuiButtonHold("b+", ">", 98, 20, 1, 5) {

            @Override
            public void onClicked(int x, int y, int button) {
                onColorChanged();
                sliderB.setValue(sliderB.value + 1);
                updateShadeSlider();
            }

        });

        if (hasShadeRow) {
            addControl(new GuiButtonHold("s-", "<", 0, 40, 1, 5) {

                @Override
                public void onClicked(int x, int y, int button) {
                    onColorChanged();
                    if (!isMin(sliderR.value, sliderG.value, sliderB.value)) {
                        sliderR.setValue(sliderR.value - 1);
                        sliderG.setValue(sliderG.value - 1);
                        sliderB.setValue(sliderB.value - 1);
                        sliderS.setValue(sliderS.value - 1);
                        updateShadeSlider();
                    }
                }

            });
            addControl(new GuiButtonHold("s+", ">", 98, 40, 1, 5) {

                @Override
                public void onClicked(int x, int y, int button) {
                    onColorChanged();
                    if (!isMax(sliderR.value, sliderG.value, sliderB.value)) {
                        sliderR.setValue(sliderR.value + 1);
                        sliderG.setValue(sliderG.value + 1);
                        sliderB.setValue(sliderB.value + 1);
                        sliderS.setValue(sliderS.value + 1);
                        updateShadeSlider();
                    }
                }

            });
        }

        if (hasAlpha) {

            addControl(new GuiButtonHold("a-", "<", 0, 30, 1, 5) {

                @Override
                public void onClicked(int x, int y, int button) {
                    onColorChanged();
                    GuiLittleVecXColoredSteppedSlider slider = (GuiLittleVecXColoredSteppedSlider) get("a");
                    slider.setValue(slider.value - 1);
                }

            });
            addControl(new GuiButtonHold("a+", ">", 98, 30, 1, 5) {

                @Override
                public void onClicked(int x, int y, int button) {
                    onColorChanged();
                    GuiLittleVecXColoredSteppedSlider slider = (GuiLittleVecXColoredSteppedSlider) get("a");
                    slider.setValue(slider.value + 1);
                }

            });
        } else
            color.setAlpha(255);

        sliderR = (new GuiLittleVecXColoredSteppedSlider("r", 8, 0, 84, 5, this, LittleVecXColorParts.ColorPart.RED) {
            @Override
            public void mouseMove(int posX, int posY, int button) {
                super.mouseMove(posX, posY, button);
                if (grabbedSlider) {
                    updateShadeSlider();
                }
            }

            @Override
            public boolean mouseScrolled(int x, int y, int scrolled) {
                super.mouseScrolled(x, y, scrolled);
                updateShadeSlider();
                return true;
            }
        });
        addControl(sliderR.setStyle(defaultStyle));

        sliderG = (new GuiLittleVecXColoredSteppedSlider("g", 8, 10, 84, 5, this, LittleVecXColorParts.ColorPart.GREEN) {
            @Override
            public void mouseMove(int posX, int posY, int button) {
                super.mouseMove(posX, posY, button);
                if (grabbedSlider) {
                    updateShadeSlider();
                }
            }

            @Override
            public boolean mouseScrolled(int x, int y, int scrolled) {
                super.mouseScrolled(x, y, scrolled);
                updateShadeSlider();
                return true;
            }
        });
        addControl(sliderG.setStyle(defaultStyle));

        sliderB = (new GuiLittleVecXColoredSteppedSlider("b", 8, 20, 84, 5, this, LittleVecXColorParts.ColorPart.BLUE) {
            @Override
            public void mouseMove(int posX, int posY, int button) {
                super.mouseMove(posX, posY, button);
                if (grabbedSlider) {
                    updateShadeSlider();
                }
            }

            @Override
            public boolean mouseScrolled(int x, int y, int scrolled) {
                super.mouseScrolled(x, y, scrolled);
                updateShadeSlider();
                return true;
            }
        });
        addControl(sliderB.setStyle(defaultStyle));

        if (hasAlpha) {
            GuiLittleVecXColoredSteppedSlider alpha = new GuiLittleVecXColoredSteppedSlider("a", 8, 30, 84, 5, this, LittleVecXColorParts.ColorPart.ALPHA);
            alpha.minValue = alphaMin;
            addControl(alpha.setStyle(defaultStyle));
        }

        if (hasShadeRow) {
            sliderS = (new GuiLittleVecXColoredSteppedSlider("s", 8, 40, 84, 5, this, LittleVecXColorParts.ColorPart.SHADE) {
                @Override
                public void mouseMove(int posX, int posY, int button) {
                    oldShaderValue = this.value;
                    super.mouseMove(posX, posY, button);

                    if (grabbedSlider) {
                        double difference = this.value - oldShaderValue;
                        sliderR.setValue(sliderR.value + difference);
                        sliderG.setValue(sliderG.value + difference);
                        sliderB.setValue(sliderB.value + difference);
                        oldShaderValue = value;
                    }
                }

                @Override
                public boolean mouseScrolled(int x, int y, int scrolled) {
                    oldShaderValue = this.value;
                    super.mouseScrolled(x, y, scrolled);
                    double difference = this.value - oldShaderValue;

                    sliderR.setValue(sliderR.value + difference);
                    sliderG.setValue(sliderG.value + difference);
                    sliderB.setValue(sliderB.value + difference);
                    oldShaderValue = value;

                    return true;
                }
            });
            updateShadeSlider();
            addControl(sliderS.setStyle(defaultStyle));
        } else {
            sliderS = null;
        }

        addControl(new GuiColorPlate("plate", 107, 2, 20, 20, color).setStyle(defaultStyle));
        addControl(new GuiButton("more", "more", 105, 28) {

            @Override
            public void onClicked(int x, int y, int button) {
                if (palette != null)
                    closePalette();
                else
                    openPalette();
            }
        });

    }

    public void setColor(Color color) {
        this.color.setColor(color);
        ((GuiLittleVecXColoredSteppedSlider) get("r")).value = color.getRed();
        ((GuiLittleVecXColoredSteppedSlider) get("g")).value = color.getGreen();
        ((GuiLittleVecXColoredSteppedSlider) get("b")).value = color.getBlue();
        if (hasAlpha)
            ((GuiLittleVecXColoredSteppedSlider) get("a")).value = color.getAlpha();
        updateShadeSlider();
    }

    public void onColorChanged() {
        if (palette != null)
            palette.onChanged();
        raiseEvent(new GuiControlChangedEvent(this));
    }

    public void openPalette() {
        palette = new GuiColorPickerPalette(name + "palette", this, posX, posY + height, width - getContentOffset() * 2, 100);
        getGui().controls.add(palette);

        palette.parent = getGui();
        palette.moveControlToTop();
        palette.onOpened();
        getGui().refreshControls();
        palette.rotation = rotation;
        palette.posX = getPixelOffsetX() - getGui().getPixelOffsetX() - getContentOffset();
        palette.posY = getPixelOffsetY() - getGui().getPixelOffsetY() - getContentOffset() + height;

        if (palette.posY + palette.height > getParent().height && this.posY >= palette.height)
            palette.posY -= this.height + palette.height;
    }

    public void closePalette() {
        if (palette != null) {
            palette.savePalette();
            getGui().controls.remove(palette);
            removeListener(palette);
            palette = null;
        }
    }

    public double getShadeLimit(double r, double g, double b) {
        List<Double> rgb = new ArrayList<Double>();
        rgb.add(r);
        rgb.add(g);
        rgb.add(b);

        double min = Collections.min(rgb);
        double max = Collections.max(rgb);
        double a = 255 - max;

        return min + a;
    }

    public double getMinColor(double r, double g, double b) {
        List<Double> rgb = new ArrayList<Double>();
        rgb.add(r);
        rgb.add(g);
        rgb.add(b);
        return Collections.min(rgb);
    }

    public double getMaxColor(double r, double g, double b) {
        List<Double> rgb = new ArrayList<Double>();
        rgb.add(r);
        rgb.add(g);
        rgb.add(b);
        return Collections.max(rgb);
    }

    public boolean isMin(double r, double g, double b) {
        List<Double> rgb = new ArrayList<Double>();
        rgb.add(r);
        rgb.add(g);
        rgb.add(b);
        double min = Collections.min(rgb);
        return min == 0;
    }

    public boolean isMax(double r, double g, double b) {
        List<Double> rgb = new ArrayList<Double>();
        rgb.add(r);
        rgb.add(g);
        rgb.add(b);
        double max = Collections.max(rgb);
        return max == 255;
    }

    public void updateShadeSlider() {
        if (!hasShadeRow || sliderS == null)
            return;
        sliderS.maxValue = getShadeLimit(sliderR.value, sliderG.value, sliderB.value);
        sliderS.value = getMinColor(sliderR.value, sliderG.value, sliderB.value);
        oldShaderValue = sliderS.value;
    }

    private int getPickerContentHeight() {
        if (hasShadeRow)
            return 55;
        if (hasAlpha)
            return 45;
        return 35;
    }
}
