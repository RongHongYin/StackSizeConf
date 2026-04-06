package dev.stacksizeconf;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class StackSizeConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_STACK_OVERRIDES = BUILDER
            .comment("关闭后使用原版堆叠数量与可否堆叠规则。")
            .translation("stacksizeconf.config.enable_stack_overrides")
            .define("enableStackSizeOverrides", true);

    public static final ModConfigSpec.DoubleValue STACK_SIZE_MULTIPLIER = BUILDER
            .comment(
                    "应用“原不可堆叠基准数量”后，再乘以该倍率。",
                    "1.0 保持原版；2.0 约等于翻倍（如 64 -> 128）。"
            )
            .translation("stacksizeconf.config.stack_size_multiplier")
            .defineInRange("stackSizeMultiplier", 2.0, 0.05, 1024.0);

    public static final ModConfigSpec.IntValue NON_STACKABLE_BASE_MAX = BUILDER
            .comment(
                    "对原版最大堆叠为 1 的物品，先按该基准计算，再应用倍率。",
                    "物品合并仍要求 NBT 等数据一致。"
            )
            .translation("stacksizeconf.config.non_stackable_base_max")
            .defineInRange("nonStackableBaseMax", 64, 1, 65536);

    public static final ModConfigSpec.IntValue MAX_STACK_HARD_CAP = BUILDER
            .comment("最终堆叠数量不会超过此上限（全局封顶）。")
            .translation("stacksizeconf.config.max_stack_hard_cap")
            .defineInRange("maxStackHardCap", 1024, 1, 65536);

    public static final ModConfigSpec.BooleanValue ENABLE_ITEM_MAGNET = BUILDER
            .comment("关闭后，不会应用自动拾取逻辑。")
            .translation("stacksizeconf.config.enable_item_magnet")
            .define("enableItemMagnet", true);

    public static final ModConfigSpec.DoubleValue ITEM_MAGNET_RANGE = BUILDER
            .comment("自动拾取：玩家周围的拾取范围（单位：格）。")
            .translation("stacksizeconf.config.item_magnet_range")
            .defineInRange("itemMagnetRange", 6.0, 1.0, 64.0);

    public static final ModConfigSpec.BooleanValue ENABLE_HANDHELD_SHULKER_OPEN = BUILDER
            .comment("启用后，手持潜影盒可直接打开，不必放置。")
            .translation("stacksizeconf.config.enable_handheld_shulker_open")
            .define("enableHandheldShulkerOpen", true);

    public static final ModConfigSpec.BooleanValue SHULKER_OPEN_REQUIRE_SNEAK = BUILDER
            .comment("启用后，只有潜行 + 右键才会打开手持潜影盒。")
            .translation("stacksizeconf.config.shulker_open_require_sneak")
            .define("shulkerOpenRequireSneak", false);

    public static final ModConfigSpec.BooleanValue SHULKER_OPEN_ALLOW_OFFHAND = BUILDER
            .comment("启用后，副手拿潜影盒也可直接打开。")
            .translation("stacksizeconf.config.shulker_open_allow_offhand")
            .define("shulkerOpenAllowOffhand", true);

    public static final ModConfigSpec.BooleanValue SHULKER_OPEN_ALLOW_RIDING_OR_FLYING = BUILDER
            .comment("启用后，骑乘或飞行状态也允许打开。")
            .translation("stacksizeconf.config.shulker_open_allow_riding_or_flying")
            .define("shulkerOpenAllowRidingOrFlying", true);

    public static final ModConfigSpec.BooleanValue SHULKER_OPEN_PLAY_SOUND = BUILDER
            .comment("打开手持潜影盒时播放容器音效。")
            .translation("stacksizeconf.config.shulker_open_play_sound")
            .define("shulkerOpenPlaySound", true);

    public static final ModConfigSpec.BooleanValue SHULKER_OPEN_SERVER_VALIDATION = BUILDER
            .comment("启用后，服务端会再次校验打开条件，保证联机一致性。")
            .translation("stacksizeconf.config.shulker_open_server_validation")
            .define("shulkerOpenServerValidation", true);

    public static final ModConfigSpec.BooleanValue ENABLE_LIGHT_LEVEL_OVERLAY = BUILDER
            .comment("关闭后，光照覆盖层快捷键无效（仅客户端）。")
            .translation("stacksizeconf.config.enable_light_level_overlay")
            .define("enableLightLevelOverlay", true);

    public static final ModConfigSpec.EnumValue<LightOverlayBrightnessMode> LIGHT_OVERLAY_BRIGHTNESS_MODE = BUILDER
            .comment(
                    "BLOCK：仅方块光（便于判断是否需要火把）。",
                    "COMBINED：原版综合亮度（夜间地表无火把常见约 4）。"
            )
            .translation("stacksizeconf.config.light_overlay_brightness_mode")
            .defineEnum("lightOverlayBrightnessMode", LightOverlayBrightnessMode.BLOCK);

    public static final ModConfigSpec.IntValue LIGHT_OVERLAY_HORIZONTAL_RANGE = BUILDER
            .comment("光照覆盖层：以玩家为中心的水平扫描范围（正方形，切比雪夫半径）。")
            .translation("stacksizeconf.config.light_overlay_horizontal_range")
            .defineInRange("lightLevelOverlayHorizontalRange", 16, 4, 48);

    public static final ModConfigSpec.IntValue LIGHT_OVERLAY_VERTICAL_RANGE = BUILDER
            .comment("光照覆盖层：相对脚下方块向上/向下扫描层数，用于寻找地面顶面。")
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
