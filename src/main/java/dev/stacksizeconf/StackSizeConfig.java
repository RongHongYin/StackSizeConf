package dev.stacksizeconf;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class StackSizeConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_STACK_OVERRIDES = BUILDER
            .comment("When false, vanilla stack sizes and stackability are used.")
            .translation("stacksizeconf.config.enable_stack_overrides")
            .define("enableStackSizeOverrides", true);

    public static final ModConfigSpec.DoubleValue STACK_SIZE_MULTIPLIER = BUILDER
            .comment(
                    "Multiply each item's vanilla max stack by this (after applying non-stackable base below).",
                    "1.0 keeps normal items at vanilla counts; 2.0 doubles (e.g. 64 -> 128)."
            )
            .translation("stacksizeconf.config.stack_size_multiplier")
            .defineInRange("stackSizeMultiplier", 2.0, 0.05, 1024.0);

    public static final ModConfigSpec.IntValue NON_STACKABLE_BASE_MAX = BUILDER
            .comment(
                    "For items that stack to 1 in vanilla, use this as the base before multiplier.",
                    "Merging still requires identical item data."
            )
            .translation("stacksizeconf.config.non_stackable_base_max")
            .defineInRange("nonStackableBaseMax", 64, 1, 65536);

    public static final ModConfigSpec.IntValue MAX_STACK_HARD_CAP = BUILDER
            .comment("Final stack size is never larger than this (global ceiling).")
            .translation("stacksizeconf.config.max_stack_hard_cap")
            .defineInRange("maxStackHardCap", 1024, 1, 65536);

    public static final ModConfigSpec.BooleanValue ENABLE_LIGHT_LEVEL_OVERLAY = BUILDER
            .comment("When false, the light overlay hotkey does nothing (client-only).")
            .translation("stacksizeconf.config.enable_light_level_overlay")
            .define("enableLightLevelOverlay", true);

    public static final ModConfigSpec.EnumValue<LightOverlayBrightnessMode> LIGHT_OVERLAY_BRIGHTNESS_MODE = BUILDER
            .comment(
                    "BLOCK: show block light only (good for torches). COMBINED: vanilla combined brightness (night surface often ~4 with no torches)."
            )
            .translation("stacksizeconf.config.light_overlay_brightness_mode")
            .defineEnum("lightOverlayBrightnessMode", LightOverlayBrightnessMode.BLOCK);

    public static final ModConfigSpec.IntValue LIGHT_OVERLAY_HORIZONTAL_RANGE = BUILDER
            .comment("Light overlay: horizontal extent in blocks from the player (square, Chebyshev radius).")
            .translation("stacksizeconf.config.light_overlay_horizontal_range")
            .defineInRange("lightLevelOverlayHorizontalRange", 16, 4, 48);

    public static final ModConfigSpec.IntValue LIGHT_OVERLAY_VERTICAL_RANGE = BUILDER
            .comment("Light overlay: blocks above/below feet Y to scan for floor surfaces.")
            .translation("stacksizeconf.config.light_overlay_vertical_range")
            .defineInRange("lightLevelOverlayVerticalRange", 3, 0, 16);

    static final ModConfigSpec SPEC = BUILDER.build();

    private StackSizeConfig() {
    }

    public static boolean stackOverridesEnabled() {
        return ENABLE_STACK_OVERRIDES.get();
    }

    public static int applyToVanillaMax(int vanillaMax) {
        if (!stackOverridesEnabled()) {
            return vanillaMax;
        }
        int base = vanillaMax <= 1 ? Math.max(vanillaMax, NON_STACKABLE_BASE_MAX.get()) : vanillaMax;
        double scaled = base * STACK_SIZE_MULTIPLIER.get();
        int result = (int) Math.round(scaled);
        result = Math.max(1, result);
        return Math.min(result, MAX_STACK_HARD_CAP.get());
    }
}
