package dev.stacksizeconf;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

public final class StackSizeConfigPersistence {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("stacksizeconf.json");

    private StackSizeConfigPersistence() {}

    public static void load() {
        if (!Files.isRegularFile(PATH)) {
            return;
        }
        try {
            String json = Files.readString(PATH, StandardCharsets.UTF_8);
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            read(o);
        } catch (Exception e) {
            StackSizeMod.LOGGER.warn("Failed to load config from {}, using defaults", PATH, e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(write()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            StackSizeMod.LOGGER.warn("Failed to save config to {}", PATH, e);
        }
    }

    private static JsonObject write() {
        JsonObject o = new JsonObject();
        o.addProperty("enable_stack_overrides", StackSizeConfig.ENABLE_STACK_OVERRIDES.get());
        o.addProperty("stack_size_multiplier", StackSizeConfig.STACK_SIZE_MULTIPLIER.get());
        o.addProperty("non_stackable_base_max", StackSizeConfig.NON_STACKABLE_BASE_MAX.get());
        o.addProperty("max_stack_hard_cap", StackSizeConfig.MAX_STACK_HARD_CAP.get());
        o.addProperty("enable_item_magnet", StackSizeConfig.ENABLE_ITEM_MAGNET.get());
        o.addProperty("item_magnet_range", StackSizeConfig.ITEM_MAGNET_RANGE.get());
        o.addProperty("enable_direct_xp_absorption", StackSizeConfig.ENABLE_DIRECT_XP_ABSORPTION.get());
        o.addProperty("direct_xp_absorption_range", StackSizeConfig.DIRECT_XP_ABSORPTION_RANGE.get());
        o.addProperty("enable_handheld_shulker_open", StackSizeConfig.ENABLE_HANDHELD_SHULKER_OPEN.get());
        o.addProperty("shulker_open_require_sneak", StackSizeConfig.SHULKER_OPEN_REQUIRE_SNEAK.get());
        o.addProperty("shulker_open_allow_offhand", StackSizeConfig.SHULKER_OPEN_ALLOW_OFFHAND.get());
        o.addProperty("shulker_open_allow_riding_or_flying", StackSizeConfig.SHULKER_OPEN_ALLOW_RIDING_OR_FLYING.get());
        o.addProperty("shulker_open_play_sound", StackSizeConfig.SHULKER_OPEN_PLAY_SOUND.get());
        o.addProperty("shulker_open_server_validation", StackSizeConfig.SHULKER_OPEN_SERVER_VALIDATION.get());
        o.addProperty("enable_hopper_stack_overrides", StackSizeConfig.ENABLE_HOPPER_STACK_OVERRIDES.get());
        o.addProperty("hopper_transfer_speed_multiplier", StackSizeConfig.HOPPER_TRANSFER_SPEED_MULTIPLIER.get());
        o.addProperty("better_trading_mode", StackSizeConfig.BETTER_TRADING_MODE.get().name());
        o.addProperty("infinite_trade_max_per_take", StackSizeConfig.INFINITE_TRADE_MAX_PER_TAKE.get());
        return o;
    }

    private static void read(JsonObject o) {
        getBool(o, "enable_stack_overrides", StackSizeConfig.ENABLE_STACK_OVERRIDES);
        getDouble(o, "stack_size_multiplier", StackSizeConfig.STACK_SIZE_MULTIPLIER, 0.01D, 1_000_000D);
        getInt(o, "max_stack_hard_cap", StackSizeConfig.MAX_STACK_HARD_CAP, 1, Integer.MAX_VALUE);
        getInt(o, "non_stackable_base_max", StackSizeConfig.NON_STACKABLE_BASE_MAX, 1, Math.max(1, StackSizeConfig.MAX_STACK_HARD_CAP.get()));
        getBool(o, "enable_item_magnet", StackSizeConfig.ENABLE_ITEM_MAGNET);
        getDouble(o, "item_magnet_range", StackSizeConfig.ITEM_MAGNET_RANGE, 0.1D, 128D);
        getBool(o, "enable_direct_xp_absorption", StackSizeConfig.ENABLE_DIRECT_XP_ABSORPTION);
        getDouble(o, "direct_xp_absorption_range", StackSizeConfig.DIRECT_XP_ABSORPTION_RANGE, 0.5D, 128D);
        getBool(o, "enable_handheld_shulker_open", StackSizeConfig.ENABLE_HANDHELD_SHULKER_OPEN);
        getBool(o, "shulker_open_require_sneak", StackSizeConfig.SHULKER_OPEN_REQUIRE_SNEAK);
        getBool(o, "shulker_open_allow_offhand", StackSizeConfig.SHULKER_OPEN_ALLOW_OFFHAND);
        getBool(o, "shulker_open_allow_riding_or_flying", StackSizeConfig.SHULKER_OPEN_ALLOW_RIDING_OR_FLYING);
        getBool(o, "shulker_open_play_sound", StackSizeConfig.SHULKER_OPEN_PLAY_SOUND);
        getBool(o, "shulker_open_server_validation", StackSizeConfig.SHULKER_OPEN_SERVER_VALIDATION);
        getBool(o, "enable_hopper_stack_overrides", StackSizeConfig.ENABLE_HOPPER_STACK_OVERRIDES);
        getDouble(o, "hopper_transfer_speed_multiplier", StackSizeConfig.HOPPER_TRANSFER_SPEED_MULTIPLIER, 0.1D, 99D);
        getTradingMode(o, "better_trading_mode", StackSizeConfig.BETTER_TRADING_MODE);
        getInt(o, "infinite_trade_max_per_take", StackSizeConfig.INFINITE_TRADE_MAX_PER_TAKE, 1, 2_097_151);
    }

    private static void getTradingMode(JsonObject o, String key, StackSizeConfig.Value<BetterTradingMode> target) {
        if (!o.has(key) || !o.get(key).isJsonPrimitive()) {
            return;
        }
        try {
            target.set(BetterTradingMode.valueOf(o.get(key).getAsString()));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static void getBool(JsonObject o, String key, StackSizeConfig.Value<Boolean> target) {
        if (o.has(key) && o.get(key).isJsonPrimitive()) {
            target.set(o.get(key).getAsBoolean());
        }
    }

    private static void getInt(JsonObject o, String key, StackSizeConfig.Value<Integer> target, int min, int max) {
        if (o.has(key) && o.get(key).isJsonPrimitive()) {
            int v = o.get(key).getAsInt();
            target.set(Math.min(max, Math.max(min, v)));
        }
    }

    private static void getDouble(JsonObject o, String key, StackSizeConfig.Value<Double> target, double min, double max) {
        if (o.has(key) && o.get(key).isJsonPrimitive()) {
            double v = o.get(key).getAsDouble();
            if (!Double.isFinite(v)) {
                return;
            }
            target.set(Math.min(max, Math.max(min, v)));
        }
    }
}
