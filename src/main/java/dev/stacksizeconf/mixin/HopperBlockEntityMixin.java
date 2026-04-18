package dev.stacksizeconf.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import dev.stacksizeconf.StackSizeConfig;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @ModifyVariable(method = "setCooldown", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int stacksizeconf$scaleHopperCooldown(int cooldownTime) {
        return StackSizeConfig.scaleHopperCooldownTicks(cooldownTime);
    }

    @ModifyExpressionValue(
            method = "tryMoveInItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I", ordinal = 0)
    )
    private static int stacksizeconf$mergeUsesRaisedContainerCap(
            int ignoredVanillaInvoke,
            @Nullable Container source,
            Container destination,
            ItemStack movingStack,
            int slot,
            @Nullable Direction direction
    ) {
        return StackSizeConfig.hopperMergeStackCapacity(destination, movingStack);
    }
}
