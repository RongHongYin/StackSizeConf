package dev.stacksizeconf.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.mojang.blaze3d.platform.InputConstants;

import dev.stacksizeconf.LightOverlayBrightnessMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ListValueSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

/**
 * Config UI: full-width module header (centered title + narrow reset); option rows use tight right-aligned reset buttons.
 */
public final class ToolboxConfigurationSectionScreen extends ConfigurationScreen.ConfigurationSectionScreen {

    /**
     * Full inner width of an options row: left column 160px + right column 160px (see {@code OptionsList.Entry} layout).
     * Slightly wider than {@link net.minecraft.client.gui.components.OptionsList#getRowWidth()} so module header aligns with the right column edge.
     */
    private static final int OPTIONS_ROW_FULL_WIDTH = 320;

    /** Keys belonging to the "自定义堆叠" module (order = display order). */
    private static final List<String> CUSTOM_STACK_KEYS = List.of(
            "enableStackSizeOverrides",
            "stackSizeMultiplier",
            "nonStackableBaseMax",
            "maxStackHardCap"
    );

    /** Keys for the "工具类" module (stored in common config). */
    private static final List<String> TOOLS_MODULE_KEYS = List.of(
            "enableLightLevelOverlay",
            "lightOverlayBrightnessMode",
            "lightLevelOverlayHorizontalRange",
            "lightLevelOverlayVerticalRange"
    );

    private @Nullable KeyMapping keyCaptureTarget;
    private @Nullable LightOverlayHotkeyRowWidget overlayHotkeyRow;

    public ToolboxConfigurationSectionScreen(Screen parent, ModConfig.Type type, ModConfig modConfig, Component title) {
        super(parent, type, modConfig, title);
    }

    private static UnmodifiableConfig.Entry findEntryByKey(Context context, String want) {
        for (UnmodifiableConfig.Entry entry : context.entries()) {
            if (entry.getKey().equals(want)) {
                return entry;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected ConfigurationScreen.ConfigurationSectionScreen rebuild() {
        if (list == null) {
            return this;
        }
        list.clearEntries();
        keyCaptureTarget = null;
        overlayHotkeyRow = null;
        boolean hasUndoableElements = false;

        Set<String> inModules = new HashSet<>();

        // --- 自定义堆叠 module ---
        addModuleHeaderRow(Component.translatable("stacksizeconf.module.custom_stack"), CUSTOM_STACK_KEYS);
        for (String key : CUSTOM_STACK_KEYS) {
            inModules.add(key);
            hasUndoableElements |= appendConfigRow(key);
        }

        if (findEntryByKey(context, TOOLS_MODULE_KEYS.getFirst()) != null) {
            addToolsModuleHeaderRow();
            for (String key : TOOLS_MODULE_KEYS) {
                inModules.add(key);
                hasUndoableElements |= appendConfigRow(key);
            }
            hasUndoableElements |= appendElementRow(createLightOverlayHotkeyElement());
        }

        // Keys not in any module (future expansion)
        for (UnmodifiableConfig.Entry entry : context.entries()) {
            String key = entry.getKey();
            if (inModules.contains(key)) {
                continue;
            }
            hasUndoableElements |= appendConfigRow(key);
        }

        for (Element element : createSyntheticValues()) {
            if (element != null) {
                hasUndoableElements |= appendElementRow(element);
            }
        }

        if (hasUndoableElements && undoButton == null) {
            createUndoButton();
            createResetButton();
        }
        return this;
    }

    private Element createLightOverlayHotkeyElement() {
        Component name = Component.translatable("stacksizeconf.config.light_overlay_hotkey");
        Component tip = Component.translatable("stacksizeconf.config.light_overlay_hotkey.tooltip");
        Button bind = Button.builder(Component.empty(), b -> beginOverlayHotkeyCapture()).build();
        int rw = tightButtonWidth(font, ConfigurationScreen.RESET);
        Button reset = Button.builder(ConfigurationScreen.RESET, b -> {
                    KeyMapping k = LightLevelOverlay.toggleKey();
                    k.setToDefault();
                    k.setKey(k.getDefaultKey());
                    KeyMapping.resetMapping();
                    refreshOverlayHotkeyRow();
                })
                .width(rw)
                .tooltip(Tooltip.create(ConfigurationScreen.RESET_TOOLTIP))
                .build();
        LightOverlayHotkeyRowWidget row = new LightOverlayHotkeyRowWidget(160, 20, bind, reset);
        overlayHotkeyRow = row;
        row.refresh();
        return new Element(name, tip, row, false);
    }

    private void beginOverlayHotkeyCapture() {
        keyCaptureTarget = LightLevelOverlay.toggleKey();
        refreshOverlayHotkeyRow();
    }

    private void refreshOverlayHotkeyRow() {
        if (overlayHotkeyRow != null) {
            overlayHotkeyRow.refresh();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (keyCaptureTarget != null) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (keyCaptureTarget != null) {
            if (!net.minecraft.client.input.InputQuirks.ON_OSX || event.scancode() != 63) {
                if (event.isEscape()) {
                    keyCaptureTarget = null;
                    refreshOverlayHotkeyRow();
                } else {
                    InputConstants.Key key = InputConstants.getKey(event);
                    if (key != InputConstants.UNKNOWN) {
                        keyCaptureTarget.setKeyModifierAndCode(KeyModifier.NONE, key);
                        keyCaptureTarget.setKey(key);
                        KeyMapping.resetMapping();
                    }
                    keyCaptureTarget = null;
                    refreshOverlayHotkeyRow();
                }
                return true;
            }
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (keyCaptureTarget != null) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (keyCaptureTarget != null) {
            InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(event.button());
            keyCaptureTarget.setKeyModifierAndCode(KeyModifier.NONE, key);
            keyCaptureTarget.setKey(key);
            KeyMapping.resetMapping();
            keyCaptureTarget = null;
            refreshOverlayHotkeyRow();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void addModuleHeaderRow(Component title, List<String> moduleKeys) {
        Component resetLabel = Component.translatable("stacksizeconf.reset_module");
        int rw = tightButtonWidth(font, resetLabel);
        Button resetModule = Button.builder(resetLabel, b -> resetKeys(moduleKeys))
                .width(rw)
                .tooltip(Tooltip.create(Component.translatable("stacksizeconf.reset_module.tooltip")))
                .build();
        list.addSmall(new ModuleHeaderBarWidget(OPTIONS_ROW_FULL_WIDTH, 22, title, font, resetModule), null);
    }

    /** Light overlay section: reset also restores the key binding (not a config value). */
    private void addToolsModuleHeaderRow() {
        Component resetLabel = Component.translatable("stacksizeconf.reset_module");
        int rw = tightButtonWidth(font, resetLabel);
        Button resetModule = Button.builder(resetLabel, b -> resetToolsModule())
                .width(rw)
                .tooltip(Tooltip.create(Component.translatable("stacksizeconf.reset_module.tooltip")))
                .build();
        list.addSmall(
                new ModuleHeaderBarWidget(
                        OPTIONS_ROW_FULL_WIDTH,
                        22,
                        Component.translatable("stacksizeconf.module.tools"),
                        font,
                        resetModule),
                null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void resetToolsModule() {
        LightLevelOverlay.resetHotkeyToDefault();
        List batch = new ArrayList<>();
        for (String key : TOOLS_MODULE_KEYS) {
            for (UnmodifiableConfig.Entry entry : context.entries()) {
                if (!entry.getKey().equals(key)) {
                    continue;
                }
                if (entry.getRawValue() instanceof final ModConfigSpec.ConfigValue cv
                        && !(getValueSpec(key) instanceof ListValueSpec)) {
                    ValueSpec spec = getValueSpec(key);
                    if (spec != null) {
                        Object def = spec.correct(null);
                        Object raw = cv.getRaw();
                        if (!Objects.equals(raw, def)) {
                            batch.add(
                                    undoManager.step(
                                            v -> {
                                                cv.set(v);
                                                onChanged(key);
                                            },
                                            def,
                                            v -> {
                                                cv.set(v);
                                                onChanged(key);
                                            },
                                            raw));
                        } else {
                            cv.set(def);
                            onChanged(key);
                        }
                    }
                }
                break;
            }
        }
        if (!batch.isEmpty()) {
            undoManager.add(batch);
        }
        rebuild();
    }

    /** ~2 characters wide: label text + small horizontal padding (not {@link Button#SMALL_WIDTH}). */
    private static int tightButtonWidth(Font font, Component label) {
        return Math.max(20, font.width(label.getVisualOrderText()) + 10);
    }

    private boolean appendConfigRow(String key) {
        UnmodifiableConfig.Entry entry = findEntryByKey(context, key);
        if (entry == null) {
            return false;
        }
        Element built = "lightOverlayBrightnessMode".equals(key)
                ? buildLightOverlayBrightnessModeElement(key, entry)
                : buildElement(key, entry);
        if (built == null) {
            return false;
        }
        built = context.filter().filterEntry(context, key, built);
        built = localizeConfigTooltip(key, built);
        built = decorateWithResetRow(key, built);
        if (built == null) {
            return false;
        }
        return appendElementRow(built);
    }

    /**
     * Clickable Chinese labels; NeoForge {@code EnumValue} does not match {@code cv.getClass() == ConfigValue.class}, so it
     * otherwise becomes {@code createOtherValue} (“cannot edit in UI”).
     */
    @SuppressWarnings("unchecked")
    private Element buildLightOverlayBrightnessModeElement(String key, UnmodifiableConfig.Entry entry) {
        Object raw = entry.getRawValue();
        if (!(raw instanceof ModConfigSpec.ConfigValue<?> base)) {
            return null;
        }
        ModConfigSpec.ConfigValue<LightOverlayBrightnessMode> cv = (ModConfigSpec.ConfigValue<LightOverlayBrightnessMode>) base;
        Button modeButton = Button.builder(lightOverlayModeLabel(cv.get()), b -> {
                    LightOverlayBrightnessMode cur = cv.get();
                    LightOverlayBrightnessMode next = cur == LightOverlayBrightnessMode.BLOCK
                            ? LightOverlayBrightnessMode.COMBINED
                            : LightOverlayBrightnessMode.BLOCK;
                    if (!next.equals(cur)) {
                        undoManager.add(
                                v -> {
                                    cv.set(v);
                                    onChanged(key);
                                },
                                next,
                                v -> {
                                    cv.set(v);
                                    onChanged(key);
                                },
                                cur);
                    }
                    rebuild();
                })
                .width(160)
                .build();
        return new Element(
                Component.translatable("stacksizeconf.config.light_overlay_brightness_mode"),
                Component.translatable("stacksizeconf.config.desc.lightOverlayBrightnessMode"),
                modeButton,
                true);
    }

    private static Component lightOverlayModeLabel(LightOverlayBrightnessMode mode) {
        return switch (mode) {
            case BLOCK -> Component.translatable("stacksizeconf.config.light_overlay_brightness_mode.BLOCK");
            case COMBINED -> Component.translatable("stacksizeconf.config.light_overlay_brightness_mode.COMBINED");
        };
    }

    private boolean appendElementRow(Element element) {
        if (element.name() == null) {
            list.addSmall(new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, Component.empty(), font), element.getWidget(options));
        } else {
            StringWidget label = new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, element.name(), font);
            label.setTooltip(Tooltip.create(element.tooltip()));
            list.addSmall(label, element.getWidget(options));
        }
        return element.undoable();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Element buildElement(String key, UnmodifiableConfig.Entry entry) {
        final Object rawValue = entry.getRawValue();
        return switch (rawValue) {
            case ModConfigSpec.ConfigValue cv -> {
                var valueSpec = getValueSpec(key);
                yield switch (valueSpec) {
                    case ListValueSpec listValueSpec -> createList(key, listValueSpec, cv);
                    case ValueSpec spec when cv.getClass() == ModConfigSpec.ConfigValue.class && spec.getDefault() instanceof String ->
                            createStringValue(key, valueSpec::test, () -> (String) cv.getRaw(), cv::set);
                    case ValueSpec spec when cv.getClass() == ModConfigSpec.ConfigValue.class && spec.getDefault() instanceof Integer ->
                            createIntegerValue(key, valueSpec, () -> (Integer) cv.getRaw(), cv::set);
                    case ValueSpec spec when cv.getClass() == ModConfigSpec.ConfigValue.class && spec.getDefault() instanceof Long ->
                            createLongValue(key, valueSpec, () -> (Long) cv.getRaw(), cv::set);
                    case ValueSpec spec when cv.getClass() == ModConfigSpec.ConfigValue.class && spec.getDefault() instanceof Double ->
                            createDoubleValue(key, valueSpec, () -> (Double) cv.getRaw(), cv::set);
                    case ValueSpec spec when cv.getClass() == ModConfigSpec.ConfigValue.class && spec.getDefault() instanceof Enum<?> ->
                            createEnumValue(key, valueSpec, (Supplier) cv::getRaw, (Consumer) cv::set);
                    case null -> null;
                    default -> switch (cv) {
                        case ModConfigSpec.BooleanValue value -> createBooleanValue(key, valueSpec, value::getRaw, value::set);
                        case ModConfigSpec.IntValue value -> createIntegerValue(key, valueSpec, value::getRaw, value::set);
                        case ModConfigSpec.LongValue value -> createLongValue(key, valueSpec, value::getRaw, value::set);
                        case ModConfigSpec.DoubleValue value -> createDoubleValue(key, valueSpec, value::getRaw, value::set);
                        default -> createOtherValue(key, cv);
                    };
                };
            }
            case UnmodifiableConfig subsection when context.valueSpecs().get(key) instanceof UnmodifiableConfig subconfig ->
                    createSection(key, subconfig, subsection);
            default -> createOtherSection(key, rawValue);
        };
    }

    /**
     * Replaces Forge/NeoForge default tooltip (English {@code .comment()} + range) when {@code stacksizeconf.config.desc.<key>}
     * exists for the current language.
     */
    private Element localizeConfigTooltip(String key, @Nullable Element element) {
        if (element == null) {
            return null;
        }
        String descKey = "stacksizeconf.config.desc." + key;
        if (!I18n.exists(descKey)) {
            return element;
        }
        Component tip = Component.translatable(descKey);
        return new Element(element.name(), tip, element.getWidget(options), element.option(), element.undoable());
    }

    private Element decorateWithResetRow(String key, @Nullable Element element) {
        if (element == null || !element.undoable()) {
            return element;
        }
        AbstractWidget valueWidget = element.getWidget(options);
        int rw = tightButtonWidth(font, ConfigurationScreen.RESET);
        Button resetBtn = Button.builder(ConfigurationScreen.RESET, b -> resetKey(key))
                .width(rw)
                .tooltip(Tooltip.create(ConfigurationScreen.RESET_TOOLTIP))
                .build();
        AbstractWidget row = valueWidget instanceof EditBox editBox
                ? new EditBoxWithResetRowWidget(160, 20, editBox, resetBtn)
                : new ValueWithResetRowWidget(160, 20, valueWidget, resetBtn);
        return new Element(element.name(), element.tooltip(), row, element.option(), element.undoable());
    }

    private void resetKey(String key) {
        for (UnmodifiableConfig.Entry entry : context.entries()) {
            if (!entry.getKey().equals(key)) {
                continue;
            }
            if (entry.getRawValue() instanceof final ModConfigSpec.ConfigValue cv
                    && !(getValueSpec(key) instanceof ListValueSpec)) {
                ValueSpec spec = getValueSpec(key);
                if (spec != null) {
                    Object def = spec.correct(null);
                    if (!Objects.equals(cv.getRaw(), def)) {
                        undoManager.add(
                                v -> {
                                    cv.set(v);
                                    onChanged(key);
                                },
                                def,
                                v -> {
                                    cv.set(v);
                                    onChanged(key);
                                },
                                cv.getRaw());
                    }
                }
            }
            break;
        }
        rebuild();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void resetKeys(List<String> keys) {
        List batch = new ArrayList<>();
        for (String key : keys) {
            for (UnmodifiableConfig.Entry entry : context.entries()) {
                if (!entry.getKey().equals(key)) {
                    continue;
                }
                if (entry.getRawValue() instanceof final ModConfigSpec.ConfigValue cv
                        && !(getValueSpec(key) instanceof ListValueSpec)) {
                    ValueSpec spec = getValueSpec(key);
                    if (spec != null) {
                        Object def = spec.correct(null);
                        if (!Objects.equals(cv.getRaw(), def)) {
                            batch.add(undoManager.step(
                                    v -> {
                                        cv.set(v);
                                        onChanged(key);
                                    },
                                    def,
                                    v -> {
                                        cv.set(v);
                                        onChanged(key);
                                    },
                                    cv.getRaw()));
                        }
                    }
                }
                break;
            }
        }
        if (!batch.isEmpty()) {
            undoManager.add(batch);
        }
        rebuild();
    }

    /**
     * One full options row: module title centered in the title band (excluding the reset chip on the right),
     * reset button flush to the same right edge as per-row reset buttons below.
     */
    private static final class ModuleHeaderBarWidget extends AbstractWidget {
        private final Font titleFont;
        private final Button resetButton;

        ModuleHeaderBarWidget(int width, int height, Component title, Font titleFont, Button resetButton) {
            super(0, 0, width, height, title);
            this.titleFont = titleFont;
            this.resetButton = resetButton;
            this.active = true;
        }

        private void layoutChildren() {
            int rw = resetButton.getWidth();
            resetButton.setX(getX() + getWidth() - rw);
            resetButton.setY(getY() + (getHeight() - 20) / 2);
            resetButton.setHeight(20);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            layoutChildren();
            int pad = 6;
            int titleAreaRight = resetButton.getX() - pad;
            int titleCenter = (getX() + titleAreaRight) / 2;
            int cy = getY() + (getHeight() - this.titleFont.lineHeight) / 2;
            // ARGB: alpha must be 0xFF or drawString skips (0xFFFFFF has alpha 0 in 1.21+)
            graphics.drawCenteredString(this.titleFont, getMessage(), titleCenter, cy, 0xFFFFFFFF);
            resetButton.setAlpha(this.alpha);
            resetButton.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (!this.active) {
                return false;
            }
            layoutChildren();
            return resetButton.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            layoutChildren();
            return resetButton.mouseReleased(event);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
            layoutChildren();
            return resetButton.mouseDragged(event, dx, dy);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            out.add(NarratedElementType.TITLE, getMessage());
        }
    }

    private static final class ValueWithResetRowWidget extends AbstractWidget {
        /** 0 = none, 1 = value (e.g. slider), 2 = reset. {@link AbstractWidget#mouseDragged} on buttons always returns true for LMB, so we must route drags explicitly. */
        private int dragTarget;

        private final AbstractWidget valueWidget;
        private final Button resetButton;

        ValueWithResetRowWidget(int width, int height, AbstractWidget valueWidget, Button resetButton) {
            super(0, 0, width, height, Component.empty());
            this.valueWidget = valueWidget;
            this.resetButton = resetButton;
        }

        private void layoutChildren() {
            int rw = resetButton.getWidth();
            int gap = 4;
            resetButton.setX(getX() + this.width - rw);
            resetButton.setY(getY());
            resetButton.setHeight(this.height);
            int vw = Math.max(20, this.width - rw - gap);
            valueWidget.setX(getX());
            valueWidget.setY(getY());
            valueWidget.setWidth(vw);
            valueWidget.setHeight(this.height);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            layoutChildren();
            valueWidget.setAlpha(this.alpha);
            resetButton.setAlpha(this.alpha);
            valueWidget.render(graphics, mouseX, mouseY, partialTick);
            resetButton.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (!this.active) {
                return false;
            }
            layoutChildren();
            dragTarget = 0;
            if (resetButton.mouseClicked(event, doubleClick)) {
                dragTarget = 2;
                return true;
            }
            if (valueWidget.mouseClicked(event, doubleClick)) {
                dragTarget = 1;
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            layoutChildren();
            try {
                return switch (dragTarget) {
                    case 1 -> valueWidget.mouseReleased(event);
                    case 2 -> resetButton.mouseReleased(event);
                    default -> resetButton.mouseReleased(event) || valueWidget.mouseReleased(event);
                };
            } finally {
                dragTarget = 0;
            }
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
            layoutChildren();
            return switch (dragTarget) {
                case 1 -> valueWidget.mouseDragged(event, dx, dy);
                case 2 -> resetButton.mouseDragged(event, dx, dy);
                default -> false;
            };
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            out.add(NarratedElementType.TITLE, valueWidget.getMessage());
        }
    }

    /** EditBox + reset: forwards keyboard input to the edit box. */
    private static final class EditBoxWithResetRowWidget extends AbstractWidget {
        private int dragTarget;

        private final EditBox editBox;
        private final Button resetButton;

        EditBoxWithResetRowWidget(int width, int height, EditBox editBox, Button resetButton) {
            super(0, 0, width, height, Component.empty());
            this.editBox = editBox;
            this.resetButton = resetButton;
        }

        private void layoutChildren() {
            int rw = resetButton.getWidth();
            int gap = 4;
            resetButton.setX(getX() + this.width - rw);
            resetButton.setY(getY());
            resetButton.setHeight(this.height);
            int vw = Math.max(40, this.width - rw - gap);
            editBox.setX(getX());
            editBox.setY(getY());
            editBox.setWidth(vw);
            editBox.setHeight(this.height);
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            if (focused) {
                this.editBox.setFocused(true);
            } else {
                this.editBox.setFocused(false);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            layoutChildren();
            editBox.setAlpha(this.alpha);
            resetButton.setAlpha(this.alpha);
            editBox.render(graphics, mouseX, mouseY, partialTick);
            resetButton.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            layoutChildren();
            return editBox.charTyped(event);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            layoutChildren();
            if (editBox.keyPressed(event)) {
                return true;
            }
            return resetButton.keyPressed(event);
        }

        @Override
        public boolean keyReleased(KeyEvent event) {
            layoutChildren();
            if (editBox.keyReleased(event)) {
                return true;
            }
            return resetButton.keyReleased(event);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (!this.active) {
                return false;
            }
            layoutChildren();
            dragTarget = 0;
            if (resetButton.mouseClicked(event, doubleClick)) {
                editBox.setFocused(false);
                dragTarget = 2;
                return true;
            }
            if (editBox.mouseClicked(event, doubleClick)) {
                setFocused(true);
                editBox.setFocused(true);
                dragTarget = 1;
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            layoutChildren();
            try {
                return switch (dragTarget) {
                    case 1 -> editBox.mouseReleased(event);
                    case 2 -> resetButton.mouseReleased(event);
                    default -> resetButton.mouseReleased(event) || editBox.mouseReleased(event);
                };
            } finally {
                dragTarget = 0;
            }
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
            layoutChildren();
            return switch (dragTarget) {
                case 1 -> editBox.mouseDragged(event, dx, dy);
                case 2 -> resetButton.mouseDragged(event, dx, dy);
                default -> false;
            };
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            layoutChildren();
            return editBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            out.add(NarratedElementType.TITLE, Component.translatable("narration.edit_box", editBox.getMessage()));
        }
    }

    private final class LightOverlayHotkeyRowWidget extends AbstractWidget {
        private int dragTarget;

        private final Button bindButton;
        private final Button resetButton;

        LightOverlayHotkeyRowWidget(int width, int height, Button bindButton, Button resetButton) {
            super(0, 0, width, height, Component.empty());
            this.bindButton = bindButton;
            this.resetButton = resetButton;
        }

        void refresh() {
            KeyMapping k = LightLevelOverlay.toggleKey();
            Component msg = k.getTranslatedKeyMessage();
            if (keyCaptureTarget == k) {
                msg = Component.literal("> ")
                        .append(msg.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
                        .append(" <")
                        .withStyle(ChatFormatting.YELLOW);
            }
            bindButton.setMessage(msg);
            resetButton.active = !k.isDefault();
        }

        private void layoutChildren() {
            int rw = resetButton.getWidth();
            int gap = 4;
            resetButton.setX(getX() + this.width - rw);
            resetButton.setY(getY());
            resetButton.setHeight(this.height);
            int bw = Math.max(20, this.width - rw - gap);
            bindButton.setX(getX());
            bindButton.setY(getY());
            bindButton.setWidth(bw);
            bindButton.setHeight(this.height);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            layoutChildren();
            bindButton.setAlpha(this.alpha);
            resetButton.setAlpha(this.alpha);
            bindButton.render(graphics, mouseX, mouseY, partialTick);
            resetButton.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (!this.active) {
                return false;
            }
            layoutChildren();
            dragTarget = 0;
            if (resetButton.mouseClicked(event, doubleClick)) {
                dragTarget = 2;
                return true;
            }
            if (bindButton.mouseClicked(event, doubleClick)) {
                dragTarget = 1;
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            layoutChildren();
            try {
                return switch (dragTarget) {
                    case 1 -> bindButton.mouseReleased(event);
                    case 2 -> resetButton.mouseReleased(event);
                    default -> resetButton.mouseReleased(event) || bindButton.mouseReleased(event);
                };
            } finally {
                dragTarget = 0;
            }
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
            layoutChildren();
            return switch (dragTarget) {
                case 1 -> bindButton.mouseDragged(event, dx, dy);
                case 2 -> resetButton.mouseDragged(event, dx, dy);
                default -> false;
            };
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            out.add(NarratedElementType.TITLE, Component.translatable("stacksizeconf.config.light_overlay_hotkey"));
        }
    }
}
