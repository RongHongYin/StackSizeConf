package dev.stacksizeconf.mixin.compat.lithium;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.stacksizeconf.StackSizeConfig;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Lithium replaces hopper insertion with {@code HopperHelper.tryMoveSingleItem}. That path compares
 * {@link Container#getMaxStackSize()} ({@link net.minecraft.world.item.Item#ABSOLUTE_MAX_STACK_SIZE}, 99 for chests)
 * instead of {@link Container#getMaxStackSize(ItemStack)}, so stacks stall at 99 despite raised item caps.
 *
 * @see dev.stacksizeconf.mixin.ContainerMixin
 */
@Mixin(targets = "net.caffeinemc.mods.lithium.common.hopper.HopperHelper")
public abstract class HopperHelperMixin {
    /**
     * Mojmap strings in {@code method}/{@code target} do not resolve for third-party targets when no mixin refmap
     * applies (“No refMap loaded”). Match Fabric intermediary names instead (same mapping set as Loom 1.21.11).
     * {@code Container.getMaxStackSize()} → {@code class_1263.method_5444}; {@code ItemStack.getMaxStackSize()} is
     * {@code class_1799.method_7914} so this redirect does not collide.
     */
    @Redirect(
            method = "tryMoveSingleItem(Lnet/minecraft/class_1263;Lnet/minecraft/class_1278;Lnet/minecraft/class_1799;Lnet/minecraft/class_1799;ILnet/minecraft/class_2350;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1263;method_5444()I")
    )
    private static int stacksizeconf$useStackAwareContainerCap(
            Container invokeOwner,
            @SuppressWarnings("unused") Container outerMethodTo,
            @Nullable WorldlyContainer toSided,
            ItemStack transferStack,
            ItemStack transferChecker,
            int targetSlot,
            @Nullable Direction fromDirection
    ) {
        if (!StackSizeConfig.stackOverridesEnabled()) {
            return invokeOwner.getMaxStackSize();
        }
        return invokeOwner.getMaxStackSize(transferStack);
    }
}
