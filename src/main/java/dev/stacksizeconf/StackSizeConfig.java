package dev.stacksizeconf;

public final class StackSizeConfig {
    public static final Value<Boolean> ENABLE_STACK_OVERRIDES = Value.of(true);
    public static final Value<Double> STACK_SIZE_MULTIPLIER = Value.of(2.0D);
    public static final Value<Integer> NON_STACKABLE_BASE_MAX = Value.of(64);
    public static final Value<Integer> MAX_STACK_HARD_CAP = Value.of(1024);

    public static final Value<Boolean> ENABLE_ITEM_MAGNET = Value.of(true);
    public static final Value<Double> ITEM_MAGNET_RANGE = Value.of(6.0D);

    public static final Value<Boolean> ENABLE_HANDHELD_SHULKER_OPEN = Value.of(true);
    public static final Value<Boolean> SHULKER_OPEN_REQUIRE_SNEAK = Value.of(false);
    public static final Value<Boolean> SHULKER_OPEN_ALLOW_OFFHAND = Value.of(true);
    public static final Value<Boolean> SHULKER_OPEN_ALLOW_RIDING_OR_FLYING = Value.of(true);
    public static final Value<Boolean> SHULKER_OPEN_PLAY_SOUND = Value.of(true);
    public static final Value<Boolean> SHULKER_OPEN_SERVER_VALIDATION = Value.of(true);

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
