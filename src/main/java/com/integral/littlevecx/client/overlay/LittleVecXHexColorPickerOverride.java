package com.integral.littlevecx.client.overlay;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.creativemd.creativecore.common.gui.GuiControl;
import com.creativemd.creativecore.common.gui.GuiRenderHelper;
import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.client.style.Style;
import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.gui.controls.gui.GuiButton;
import com.creativemd.creativecore.common.gui.controls.gui.GuiColorPicker;
import com.creativemd.creativecore.common.gui.controls.gui.GuiLabel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiTextfield;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleVecXHexColorPickerOverride<K extends SubGui> extends LittleVecXSubGuiOverride<K> {

    private static final String HEX_BUTTON_PREFIX = "littlevecx_color_hex_button_";
    private static final String HEX_FIELD_PREFIX = "littlevecx_color_hex_field_";
    private static final int HEX_BUTTON_WIDTH = 9;
    private static final int HEX_BUTTON_HEIGHT = 11;
    private static final int HEX_FIELD_WIDTH = 20;
    private static final int HEX_FIELD_HEIGHT = 4;
    private static final int HEX_FIELD_HORIZONTAL_GAP = 0;
    private static final int CONTROL_DECORATION = 6;
    private static final int HEX_BUTTON_CONTENT_WIDTH = HEX_BUTTON_WIDTH - CONTROL_DECORATION;
    private static final int HEX_BUTTON_CONTENT_HEIGHT = HEX_BUTTON_HEIGHT - CONTROL_DECORATION;
    private static final int HEX_FIELD_OUTER_HEIGHT = HEX_FIELD_HEIGHT + CONTROL_DECORATION;
    private static final int HEX_ANCHOR_X = 108;
    private static final int HEX_ANCHOR_Y = 27;
    private static final String ALET_TUTORIAL_BOX = "com.alet.client.gui.controls.tutorial.GuiTutorialBox";
    private static final String ALET_TUTORIAL_DATA = "com.alet.client.gui.controls.tutorial.TutorialData";

    public LittleVecXHexColorPickerOverride(Class<K> key) {
        super(key);
    }

    @Override
    protected void overrideGui(K gui) {
        List<GuiControl> controls = new ArrayList<GuiControl>(gui.controls);
        for (GuiControl control : controls) {
            if (!(control instanceof GuiColorPicker))
                continue;

            GuiColorPicker picker = (GuiColorPicker) control;
            String buttonName = HEX_BUTTON_PREFIX + picker.name;
            String fieldName = HEX_FIELD_PREFIX + picker.name;
            if (gui.get(buttonName) != null || gui.get(fieldName) != null)
                continue;

            int x = picker.posX + HEX_ANCHOR_X;
            int y = picker.posY + HEX_ANCHOR_Y;
            GuiHexColorField field = new GuiHexColorField(fieldName, picker, x + HEX_BUTTON_WIDTH + HEX_FIELD_HORIZONTAL_GAP,
                    y + (HEX_BUTTON_HEIGHT - HEX_FIELD_OUTER_HEIGHT) / 2);
            gui.addControl(field);
            field.addListener(field);
            GuiHexButton button = new GuiHexButton(buttonName, x, y, field);
            gui.addControl(button);
            field.moveControlToTop();
            button.moveControlToTop();
        }
        attachAletTutorials(gui);
        keepHexControlsInFront(gui);
    }

    public static void keepHexControlsInFront(SubGui gui) {
        List<GuiControl> hexControls = new ArrayList<GuiControl>();
        for (GuiControl control : gui.controls) {
            if (control instanceof GuiHexButton) {
                ((GuiHexButton) control).syncPosition();
                hexControls.add(control);
            }
        }
        for (GuiControl control : gui.controls) {
            if (control instanceof GuiHexColorField)
                hexControls.add(control);
        }
        if (hexControls.isEmpty())
            return;

        for (int i = 0; i < hexControls.size(); i++) {
            if (gui.controls.get(i) != hexControls.get(i)) {
                gui.controls.removeAll(hexControls);
                gui.controls.addAll(0, hexControls);
                gui.refreshControls();
                return;
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void attachAletTutorials(SubGui gui) {
        GuiParent tutorial = null;
        for (GuiControl candidate : gui.controls) {
            if (ALET_TUTORIAL_BOX.equals(candidate.getClass().getName())) {
                tutorial = (GuiParent) candidate;
                break;
            }
        }
        if (tutorial == null)
            return;

        try {
            List tutorialMap = (List) tutorial.getClass().getField("tutorialMap").get(tutorial);
            Class<?> tutorialData = Class.forName(ALET_TUTORIAL_DATA);
            Field highlightedControl = tutorialData.getField("control");
            Constructor<?> entryConstructor = tutorialData.getConstructor(GuiControl.class, String.class, String.class);
            boolean added = false;
            int nextHexPage = Math.min(8, tutorialMap.size());
            for (int i = 0; i < tutorialMap.size(); i++) {
                if (highlightedControl.get(tutorialMap.get(i)) instanceof GuiHexButton)
                    nextHexPage = i + 1;
            }

            for (GuiControl control : gui.controls) {
                if (!(control instanceof GuiHexButton))
                    continue;

                boolean exists = false;
                for (Object entry : tutorialMap) {
                    if (highlightedControl.get(entry) == control) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    tutorialMap.add(nextHexPage++, entryConstructor.newInstance(control, "leftout", CoreControl.translate("gui.littlevecx.color.hex_tutorial")));
                    added = true;
                }
            }

            GuiControl maxPages = tutorial.get("maxPages");
            if (added && maxPages instanceof GuiLabel) {
                GuiLabel label = (GuiLabel) maxPages;
                String caption = label.getCaption();
                int separator = caption.lastIndexOf('/');
                if (separator >= 0)
                    label.setCaption(caption.substring(0, separator + 1) + tutorialMap.size());
            }
        } catch (ReflectiveOperationException ignored) {}
    }

    private static class GuiHexButton extends GuiButton {

        private final GuiHexColorField field;

        private GuiHexButton(String name, int x, int y, GuiHexColorField field) {
            super(name, "#", x, y, HEX_BUTTON_CONTENT_WIDTH, HEX_BUTTON_CONTENT_HEIGHT);
            this.field = field;
        }

        private void syncPosition() {
            GuiColorPicker current = field.currentPicker();
            posX = current.posX + HEX_ANCHOR_X;
            posY = current.posY + HEX_ANCHOR_Y;
            field.posX = posX + HEX_BUTTON_WIDTH + HEX_FIELD_HORIZONTAL_GAP;
            field.posY = posY + (HEX_BUTTON_HEIGHT - HEX_FIELD_OUTER_HEIGHT) / 2;
        }

        @Override
        public void onClicked(int mouseX, int mouseY, int button) {
            field.toggle();
            moveControlToTop();
        }

        @Override
        public boolean canOverlap() {
            return true;
        }
    }

    private static class GuiHexColorField extends GuiTextfield {

        private static final char[] HEX_CHARACTERS = "0123456789abcdefABCDEF".toCharArray();

        private final GuiColorPicker picker;
        private boolean applyingHexColor;

        private GuiHexColorField(String name, GuiColorPicker picker, int x, int y) {
            super(name, toHex(picker), x, y, HEX_FIELD_WIDTH, HEX_FIELD_HEIGHT);
            this.picker = picker;
            this.maxLength = 6;
            this.allowedChars = HEX_CHARACTERS;
            setVisible(false);
            setEnabled(false);
        }

        private void toggle() {
            boolean open = !visible;
            setVisible(open);
            setEnabled(open);
            if (open) {
                syncFromPicker();
                moveControlToTop();
            }
        }

        @CustomEventSubscribe
        public void onChanged(GuiControlChangedEvent event) {
            if (event.source == currentPicker()) {
                if (!applyingHexColor)
                    syncFromPicker();
            }
        }

        @Override
        public void writeText(String value) {
            String previous = text;
            super.writeText(value);
            if (!previous.equals(text))
                applyToPicker();
        }

        @Override
        public void deleteFromCursor(int amount) {
            String previous = text;
            super.deleteFromCursor(amount);
            if (!previous.equals(text))
                applyToPicker();
        }

        @Override
        public boolean onKeyPressed(char character, int keyCode) {
            if (focused && isInteractable() && GuiScreen.isKeyComboCtrlV(keyCode)) {
                String clipboard = GuiScreen.getClipboardString().trim();
                if (clipboard.startsWith("#"))
                    clipboard = clipboard.substring(1);
                if (isHexColor(clipboard)) {
                    text = clipboard;
                    setCursorPositionEnd();
                    applyToPicker();
                    return true;
                }
            }
            return super.onKeyPressed(character, keyCode);
        }

        private static boolean isHexColor(String value) {
            if (value.length() != 6)
                return false;
            for (int i = 0; i < value.length(); i++) {
                if (Character.digit(value.charAt(i), 16) < 0)
                    return false;
            }
            return true;
        }

        private void syncFromPicker() {
            text = toHex(currentPicker());
            setCursorPositionEnd();
        }

        private GuiColorPicker currentPicker() {
            GuiParent parent = getParent();
            GuiControl current = parent == null ? null : parent.get(picker.name);
            return current instanceof GuiColorPicker ? (GuiColorPicker) current : picker;
        }

        private void applyToPicker() {
            if (text == null || !isHexColor(text))
                return;

            try {
                int rgb = Integer.parseInt(text, 16);
                applyingHexColor = true;
                GuiColorPicker current = currentPicker();
                current.color.setRed((rgb >> 16) & 255);
                current.color.setGreen((rgb >> 8) & 255);
                current.color.setBlue(rgb & 255);
                current.setColor(current.color);
                current.onColorChanged();
            } catch (NumberFormatException ignored) {
                // Only the six-digit hexadecimal RGB form is accepted.
            } finally {
                applyingHexColor = false;
            }
        }

        @Override
        protected void renderContent(GuiRenderHelper helper, Style style, int width, int height) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(-5, -1, 0);
            super.renderContent(helper, style, width, height);
            GlStateManager.popMatrix();
        }

        @Override
        public boolean canOverlap() {
            return true;
        }

        private static String toHex(GuiColorPicker picker) {
            return String.format("%02x%02x%02x", picker.color.getRed(), picker.color.getGreen(), picker.color.getBlue());
        }
    }
}
