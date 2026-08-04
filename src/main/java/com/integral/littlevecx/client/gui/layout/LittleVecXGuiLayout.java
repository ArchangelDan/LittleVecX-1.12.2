package com.integral.littlevecx.client.gui.layout;

public final class LittleVecXGuiLayout {

    private final int guiWidth;
    private final int guiHeight;
    private final int padding;

    public LittleVecXGuiLayout(int guiWidth, int guiHeight) {
        this(guiWidth, guiHeight, LittleVecXGuiMetrics.PADDING);
    }

    public LittleVecXGuiLayout(int guiWidth, int guiHeight, int padding) {
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;
        this.padding = padding;
    }

    public int left() {
        return padding;
    }

    public int top() {
        return padding;
    }

    public int contentWidth() {
        return guiWidth - padding * 2;
    }

    public int center(int width) {
        return (guiWidth - width) / 2;
    }

    public int right(int width) {
        return guiWidth - padding - width;
    }

    public int nextRow(int y, int height) {
        return nextRow(y, height, LittleVecXGuiMetrics.ROW_GAP);
    }

    public int nextRow(int y, int height, int gap) {
        return y + height + gap;
    }

    public int footerY(int height) {
        return guiHeight - padding - height;
    }

    public int pairLeft(int leftWidth, int rightWidth) {
        return pairLeft(leftWidth, rightWidth, LittleVecXGuiMetrics.COLUMN_GAP);
    }

    public int pairLeft(int leftWidth, int rightWidth, int gap) {
        return center(leftWidth + rightWidth + gap);
    }

    public int pairRight(int leftWidth, int rightWidth) {
        return pairRight(leftWidth, rightWidth, LittleVecXGuiMetrics.COLUMN_GAP);
    }

    public int pairRight(int leftWidth, int rightWidth, int gap) {
        return pairLeft(leftWidth, rightWidth, gap) + leftWidth + gap;
    }
}
