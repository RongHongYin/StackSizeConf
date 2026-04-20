package dev.stacksizeconf;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class StackSizeConfig {
    public static final Value<Boolean> ENABLE_STACK_OVERRIDES = Value.of(true);
    public static final Value<Double> STACK_SIZE_MULTIPLIER = Value.of(2.0D);
    public static final Value<Integer> NON_STACKABLE_BASE_MAX = Value.of(64);
    public static final Value<Integer> MAX_STACK_HARD_CAP = Value.of(1024);

    public static final Value<Boolean> ENABLE_ITEM_MAGNET = Value.of(true);
    public static final Value<Double> ITEM_MAGNET_RANGE = Value.of(6.0D);

    public static final Value<Boolean> ENABLE_DIRECT_XP_ABSORPTION = Value.of(true);
    public static final Value<Double> DIRECT_XP_ABSORPTION_RANGE = Value.of(8.0D);

    public static final Value<Boolean> ENABLE_HANDHELD_SHULKER_OPEN = Value.of(true);
    public static final Value<Boolean> SHULKER_OPEN_REQUIRE_SNEAK = Value.of(false);
    public static final Value<Boolean> SHULKER_OPEN_ALLOW_OFFHAND = Value.of(true);
    public static final Value<Boolean> SHULKER_OPEN_ALLOW_RIDING_OR_FLYING = Value.of(true);
    public static final Value<Boolean> SHULKER_OPEN_PLAY_SOUND = Value.of(true);
    public static final Value<Boolean> SHULKER_OPEN_SERVER_VALIDATION = Value.of(true);

    /**
     * Vanilla hopper waits {@code TransferCooldown} ticks (typically 8) after each transfer; higher values move
     * items in and out more often. {@code 1.0} matches vanilla.
     */
    public static final Value<Double> HOPPER_TRANSFER_SPEED_MULTIPLIER = Value.of(1.0D);

    /** Villager trading helper: off / fast restock on GUI reopen / infinite at master tier. */
    public static final Value<BetterTradingMode> BETTER_TRADING_MODE = Value.of(BetterTradingMode.OFF);
    /** Only {@link BetterTradingMode#INFINITE}: max result items moved from the merchant result slot per take. */
    public static final Value<Integer> INFINITE_TRADE_MAX_PER_TAKE = Value.of(64);

    private StackSizeConfig() {}

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

    /**
     * Capacity used in {@link net.minecraft.world.level.block.entity.HopperBlockEntity} merge math
     * ({@code getMaxStackSize() - destCount}). Vanilla uses only {@link ItemStack#getMaxStackSize()} on the
     * moving stack; {@link Container#getMaxStackSize(ItemStack)} must match {@code setItem}/{@code limitSize}
     * or stacks stall at the 99 inventory ceiling.
     */
    public static int hopperMergeStackCapacity(Container destination, ItemStack movingStack) {
        if (!stackOverridesEnabled()) {
            return movingStack.getMaxStackSize();
        }
        int movingMax = movingStack.getMaxStackSize();
        int blended = destination.getMaxStackSize(movingStack);
        if (blended >= movingMax) {
            return movingMax;
        }
        if (destination.getMaxStackSize() == Item.ABSOLUTE_MAX_STACK_SIZE) {
            return Math.min(MAX_STACK_HARD_CAP.get(), movingMax);
        }
        return blended;
    }

    /** Scales vanilla hopper cooldown ticks (1–8) after transfers; values {@code > 8} are left unchanged. */
    public static int scaleHopperCooldownTicks(int vanillaTicks) {
        double mult = HOPPER_TRANSFER_SPEED_MULTIPLIER.get();
        if (!Double.isFinite(mult) || Math.abs(mult - 1.0D) < 1e-6) {
            return vanillaTicks;
        }
        if (vanillaTicks <= 0 || vanillaTicks > 8) {
            return vanillaTicks;
        }
        return Math.max(1, Mth.ceil(vanillaTicks / mult));
    }

    public static final class Value<T> {
        private T value;

        private Value(T value) {
            this.value = value;
        }

        public static <T> Value<T> of(T value) {
            return new Value<>(value);
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
        }
    }
}
