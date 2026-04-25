package dev.stacksizeconf.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.stacksizeconf.StackSizeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Unique
    private static double stacksizeconf$extraTickCarry;
    @Unique
    private static final ThreadLocal<Boolean> stacksizeconf$EXTRA_TICK_GUARD = ThreadLocal.withInitial(() -> false);
    @Unique
    private static final ThreadLocal<Boolean> stacksizeconf$TRY_MOVE_LIMIT_ACTIVE = ThreadLocal.withInitial(() -> false);
    @Unique
    private static final ThreadLocal<Integer> stacksizeconf$TRY_MOVE_LIMIT_MAX = ThreadLocal.withInitial(() -> 0);
    @Unique
    private static final ThreadLocal<Integer> stacksizeconf$TRY_MOVE_TAIL_COUNT = ThreadLocal.withInitial(() -> 0);
    @Unique
    private static final ThreadLocal<ItemStack> stacksizeconf$TRY_MOVE_TAIL_TEMPLATE = ThreadLocal.withInitial(() -> ItemStack.EMPTY);

    @ModifyVariable(method = "setCooldown", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int stacksizeconf$scaleHopperCooldown(int cooldownTime) {
        return StackSizeConfig.scaleHopperCooldownTicks(cooldownTime);
    }

    @Inject(
            method = "pushItemsTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)V",
            at = @At("TAIL")
    )
    private static void stacksizeconf$extraHighMultiplierTicks(
            Level level,
            BlockPos pos,
            BlockState state,
            HopperBlockEntity hopper,
            CallbackInfo ci
    ) {
        if (Boolean.TRUE.equals(stacksizeconf$EXTRA_TICK_GUARD.get())) {
            return;
        }
        double mult = StackSizeConfig.HOPPER_TRANSFER_SPEED_MULTIPLIER.get();
        if (!Double.isFinite(mult) || mult <= 8.0D) {
            return;
        }

        double perTick = mult / 8.0D;
        int guaranteedExtra = Math.max(0, (int) Math.floor(perTick) - 1);
        stacksizeconf$extraTickCarry += perTick - Math.floor(perTick);
        int bonus = 0;
        if (stacksizeconf$extraTickCarry >= 1.0D) {
            bonus = (int) stacksizeconf$extraTickCarry;
            stacksizeconf$extraTickCarry -= bonus;
        }
        int attempts = guaranteedExtra + bonus;
        if (attempts <= 0) {
            return;
        }
        stacksizeconf$EXTRA_TICK_GUARD.set(true);
        try {
            for (int i = 0; i < attempts; i++) {
                HopperBlockEntityAccess.stacksizeconf$invokePushItemsTick(level, pos, state, hopper);
            }
        } finally {
            stacksizeconf$EXTRA_TICK_GUARD.set(false);
        }
    }

    @Redirect(
            method = "tryMoveInItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I")
    )
    private static int stacksizeconf$hopperMergeCapacityByToggle(
            ItemStack receiver,
            @Nullable Container source,
            Container destination,
            ItemStack movingStack,
            int slot,
            @Nullable Direction direction
    ) {
        // Route all max-stack checks in hopper merge math through one policy function so disabled
        // hopper overrides never inherit large global stack limits.
        return StackSizeConfig.hopperTransferStackCapacity(source, destination, movingStack);
    }

    @Inject(
            method = "tryMoveInItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD")
    )
    private static void stacksizeconf$resetEntityPullTail(
            @Nullable Container source,
            Container destination,
            ItemStack movingStack,
            int slot,
            @Nullable Direction direction,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (source != null || StackSizeConfig.hopperStackOverridesEnabled() || movingStack.isEmpty()) {
            stacksizeconf$TRY_MOVE_LIMIT_ACTIVE.set(false);
            stacksizeconf$TRY_MOVE_LIMIT_MAX.set(0);
        } else {
            stacksizeconf$TRY_MOVE_LIMIT_ACTIVE.set(true);
            stacksizeconf$TRY_MOVE_LIMIT_MAX.set(StackSizeConfig.vanillaItemMaxStackSize(movingStack));
        }
        stacksizeconf$TRY_MOVE_TAIL_COUNT.set(0);
        stacksizeconf$TRY_MOVE_TAIL_TEMPLATE.set(ItemStack.EMPTY);
    }

    @ModifyVariable(
            method = "tryMoveInItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private static ItemStack stacksizeconf$limitEntityPullInputPerAttempt(ItemStack originalMovingStack) {
        if (!Boolean.TRUE.equals(stacksizeconf$TRY_MOVE_LIMIT_ACTIVE.get()) || originalMovingStack.isEmpty()) {
            return originalMovingStack;
        }
        int maxPerAttempt = stacksizeconf$TRY_MOVE_LIMIT_MAX.get();
        if (originalMovingStack.getCount() <= maxPerAttempt) {
            return originalMovingStack;
        }
        int tail = originalMovingStack.getCount() - maxPerAttempt;
        stacksizeconf$TRY_MOVE_TAIL_COUNT.set(tail);
        stacksizeconf$TRY_MOVE_TAIL_TEMPLATE.set(originalMovingStack.copyWithCount(1));
        return originalMovingStack.copyWithCount(maxPerAttempt);
    }

    @Inject(
            method = "tryMoveInItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void stacksizeconf$appendEntityPullTailBack(
            @Nullable Container source,
            Container destination,
            ItemStack movingStack,
            int slot,
            @Nullable Direction direction,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        stacksizeconf$TRY_MOVE_LIMIT_ACTIVE.set(false);
        stacksizeconf$TRY_MOVE_LIMIT_MAX.set(0);
        int tail = stacksizeconf$TRY_MOVE_TAIL_COUNT.get();
        ItemStack template = stacksizeconf$TRY_MOVE_TAIL_TEMPLATE.get();
        stacksizeconf$TRY_MOVE_TAIL_COUNT.set(0);
        stacksizeconf$TRY_MOVE_TAIL_TEMPLATE.set(ItemStack.EMPTY);
        if (tail <= 0 || template.isEmpty()) {
            return;
        }
        ItemStack remainder = cir.getReturnValue();
        if (remainder.isEmpty()) {
            cir.setReturnValue(template.copyWithCount(tail));
            return;
        }
        if (ItemStack.isSameItemSameComponents(remainder, template)) {
            remainder.grow(tail);
            cir.setReturnValue(remainder);
        }
    }
}
