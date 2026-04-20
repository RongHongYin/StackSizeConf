package dev.stacksizeconf.client;

import org.jspecify.annotations.Nullable;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import dev.stacksizeconf.BetterTradingMode;
import dev.stacksizeconf.StackSizeConfig;
import dev.stacksizeconf.StackSizeConfigPersistence;

public final class ToolboxConfigScreen {
    private ToolboxConfigScreen() {}

    public static Screen create(@Nullable Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("stacksizeconf.config.title"));
        ConfigEntryBuilder eb = ConfigEntryBuilder.create();

        ConfigCategory stack = builder.getOrCreateCategory(Component.translatable("stacksizeconf.config.category.stack"));
        stack.addEntry(eb.startTextDescription(Component.translatable("stacksizeconf.config.hint.server_authoritative")).build());
        stack.addEntry(eb.startBooleanToggle(Component.translatable("stacksizeconf.config.enable_stack_overrides"), StackSizeConfig.ENABLE_STACK_OVERRIDES.get())
                .setDefaultValue(true)
                .setSaveConsumer(StackSizeConfig.ENABLE_STACK_OVERRIDES::set)
                .build());
        stack.addEntry(eb.startDoubleField(Component.translatable("stacksizeconf.config.stack_size_multiplier"), StackSizeConfig.STACK_SIZE_MULTIPLIER.get())
                .setDefaultValue(2.0D)
                .setMin(0.01D)
                .setMax(1_024D)
                .setSaveConsumer(StackSizeConfig.STACK_SIZE_MULTIPLIER::set)
                .build());
        stack.addEntry(eb.startIntField(Component.translatable("stacksizeconf.config.non_stackable_base_max"), StackSizeConfig.NON_STACKABLE_BASE_MAX.get())
                .setDefaultValue(64)
                .setMin(1)
                .setMax(Math.max(1, StackSizeConfig.MAX_STACK_HARD_CAP.get()))
                .setSaveConsumer(StackSizeConfig.NON_STACKABLE_BASE_MAX::set)
                .build());
        stack.addEntry(eb.startIntField(Component.translatable("stacksizeconf.config.max_stack_hard_cap"), StackSizeConfig.MAX_STACK_HARD_CAP.get())
                .setDefaultValue(1024)
                .setMin(1)
                .setMax(1_000_000)
                .setSaveConsumer(StackSizeConfig.MAX_STACK_HARD_CAP::set)
                .build());

        ConfigCategory hopper = builder.getOrCreateCategory(Component.translatable("stacksizeconf.config.category.hopper"));
        hopper.addEntry(eb.startTextDescription(Component.translatable("stacksizeconf.config.hint.hopper_speed")).build());
        hopper.addEntry(eb.startDoubleField(Component.translatable("stacksizeconf.config.hopper_transfer_speed_multiplier"), StackSizeConfig.HOPPER_TRANSFER_SPEED_MULTIPLIER.get())
                .setDefaultValue(1.0D)
                .setMin(0.1D)
                .setMax(128D)
                .setSaveConsumer(StackSizeConfig.HOPPER_TRANSFER_SPEED_MULTIPLIER::set)
                .build());

        ConfigCategory trading = builder.getOrCreateCategory(Component.translatable("stacksizeconf.config.category.trading"));
        trading.addEntry(eb.startTextDescription(Component.translatable("stacksizeconf.config.hint.trading")).build());
        trading.addEntry(eb.startEnumSelector(Component.translatable("stacksizeconf.config.better_trading_mode"), BetterTradingMode.class, StackSizeConfig.BETTER_TRADING_MODE.get())
                .setDefaultValue(BetterTradingMode.OFF)
                .setEnumNameProvider(mode -> Component.translatable("stacksizeconf.better_trading_mode." + mode.name()))
                .setSaveConsumer(StackSizeConfig.BETTER_TRADING_MODE::set)
                .build());
        trading.addEntry(eb.startIntField(Component.translatable("stacksizeconf.config.infinite_trade_max_per_take"), StackSizeConfig.INFINITE_TRADE_MAX_PER_TAKE.get())
                .setDefaultValue(64)
                .setMin(1)
                .setMax(1_000_000)
                .setSaveConsumer(StackSizeConfig.INFINITE_TRADE_MAX_PER_TAKE::set)
                .build());

        ConfigCategory magnet = builder.getOrCreateCategory(Component.translatable("stacksizeconf.config.category.magnet"));
        magnet.addEntry(eb.startBooleanToggle(Component.translatable("stacksizeconf.config.enable_item_magnet"), StackSizeConfig.ENABLE_ITEM_MAGNET.get())
                .setDefaultValue(true)
                .setSaveConsumer(StackSizeConfig.ENABLE_ITEM_MAGNET::set)
                .build());
        magnet.addEntry(eb.startDoubleField(Component.translatable("stacksizeconf.config.item_magnet_range"), StackSizeConfig.ITEM_MAGNET_RANGE.get())
                .setDefaultValue(6.0D)
                .setMin(0.5D)
                .setMax(64D)
                .setSaveConsumer(StackSizeConfig.ITEM_MAGNET_RANGE::set)
                .build());

        ConfigCategory xpAbsorption = builder.getOrCreateCategory(Component.translatable("stacksizeconf.config.category.xp_absorption"));
        xpAbsorption.addEntry(eb.startBooleanToggle(Component.translatable("stacksizeconf.config.enable_direct_xp_absorption"), StackSizeConfig.ENABLE_DIRECT_XP_ABSORPTION.get())
                .setDefaultValue(true)
                .setSaveConsumer(StackSizeConfig.ENABLE_DIRECT_XP_ABSORPTION::set)
                .build());
        xpAbsorption.addEntry(eb.startDoubleField(Component.translatable("stacksizeconf.config.direct_xp_absorption_range"), StackSizeConfig.DIRECT_XP_ABSORPTION_RANGE.get())
                .setDefaultValue(8.0D)
                .setMin(0.5D)
                .setMax(64D)
                .setSaveConsumer(StackSizeConfig.DIRECT_XP_ABSORPTION_RANGE::set)
                .build());

        ConfigCategory shulker = builder.getOrCreateCategory(Component.translatable("stacksizeconf.config.category.shulker"));
        shulker.addEntry(eb.startBooleanToggle(Component.translatable("stacksizeconf.config.enable_handheld_shulker_open"), StackSizeConfig.ENABLE_HANDHELD_SHULKER_OPEN.get())
                .setDefaultValue(true)
                .setSaveConsumer(StackSizeConfig.ENABLE_HANDHELD_SHULKER_OPEN::set)
                .build());
        shulker.addEntry(eb.startBooleanToggle(Component.translatable("stacksizeconf.config.shulker_open_require_sneak"), StackSizeConfig.SHULKER_OPEN_REQUIRE_SNEAK.get())
                .setDefaultValue(false)
                .setSaveConsumer(StackSizeConfig.SHULKER_OPEN_REQUIRE_SNEAK::set)
                .build());
        shulker.addEntry(eb.startBooleanToggle(Component.translatable("stacksizeconf.config.shulker_open_allow_offhand"), StackSizeConfig.SHULKER_OPEN_ALLOW_OFFHAND.get())
                .setDefaultValue(true)
                .setSaveConsumer(StackSizeConfig.SHULKER_OPEN_ALLOW_OFFHAND::set)
                .build());
        shulker.addEntry(eb.startBooleanToggle(Component.translatable("stacksizeconf.config.shulker_open_allow_riding_or_flying"), StackSizeConfig.SHULKER_OPEN_ALLOW_RIDING_OR_FLYING.get())
                .setDefaultValue(true)
                .setSaveConsumer(StackSizeConfig.SHULKER_OPEN_ALLOW_RIDING_OR_FLYING::set)
                .build());
        shulker.addEntry(eb.startBooleanToggle(Component.translatable("stacksizeconf.config.shulker_open_play_sound"), StackSizeConfig.SHULKER_OPEN_PLAY_SOUND.get())
                .setDefaultValue(true)
                .setSaveConsumer(StackSizeConfig.SHULKER_OPEN_PLAY_SOUND::set)
                .build());
        shulker.addEntry(eb.startBooleanToggle(Component.translatable("stacksizeconf.config.shulker_open_server_validation"), StackSizeConfig.SHULKER_OPEN_SERVER_VALIDATION.get())
                .setDefaultValue(true)
                .setSaveConsumer(StackSizeConfig.SHULKER_OPEN_SERVER_VALIDATION::set)
                .build());

        builder.setSavingRunnable(StackSizeConfigPersistence::save);
        return builder.build();
    }
}
