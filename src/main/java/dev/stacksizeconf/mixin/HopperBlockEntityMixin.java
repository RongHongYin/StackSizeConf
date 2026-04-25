package dev.stacksizeconf.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

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

    @ModifyExpressionValue(
            method = "tryMoveInItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I", ordinal = 0)
    )
    private static int stacksizeconf$hopperMergeCapacityByToggle(
            int computed,
            @Nullable Container source,
            Container destination,
            ItemStack movingStack,
            int slot,
            @Nullable Direction direction
    ) {
        // Hopper stack toggle only controls hopper inventory cap, not transfer/output capacity.
        return StackSizeConfig.hopperMergeStackCapacity(destination, movingStack);
    }
}
