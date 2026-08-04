package com.integral.littlevecx.client.gui.controls;

import org.lwjgl.util.Color;

import com.creativemd.creativecore.common.utils.mc.ColorUtils;

public class LittleVecXColorParts extends ColorUtils {

    public enum ColorPart {
        RED {
            @Override
            public int getColor(Color color) {
                return color.getRed();
            }

            @Override
            public void setColor(Color color, int intensity) {
                color.setRed(intensity);
            }

            @Override
            public int getBrightest() {
                return 0xFF0000;
            }
        },
        GREEN {
            @Override
            public int getColor(Color color) {
                return color.getGreen();
            }

            @Override
            public void setColor(Color color, int intensity) {
                color.setGreen(intensity);
            }

            @Override
            public int getBrightest() {
                return 0x00FF00;
            }
        },
        BLUE {
            @Override
            public int getColor(Color color) {
                return color.getBlue();
            }

            @Override
            public void setColor(Color color, int intensity) {
                color.setBlue(intensity);
            }

            @Override
            public int getBrightest() {
                return 0x0000FF;
            }
        },
        ALPHA {
            @Override
            public int getColor(Color color) {
                return color.getAlpha();
            }

            @Override
            public void setColor(Color color, int intensity) {
                color.setAlpha(intensity);
            }

            @Override
            public int getBrightest() {
                return 0x000000FF;
            }
        },
        SHADE {
            @Override
            public int getColor(Color color) {
                return RGBAToInt(color.getRed(), color.getGreen(), color.getBlue(), 255);
            }

            @Override
            public void setColor(Color color, int intensity) {}

            @Override
            public int getBrightest() {
                return 0xFFFFFFFF;
            }
        };

        public abstract int getColor(Color color);

        public abstract void setColor(Color color, int intensity);

        public abstract int getBrightest();
    }
}
