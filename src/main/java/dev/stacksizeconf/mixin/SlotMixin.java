package dev.stacksizeconf.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.stacksizeconf.StackSizeConfig;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Vanilla {@link net.minecraft.world.Container#getMaxStackSize()} defaults to {@link Item#ABSOLUTE_MAX_STACK_SIZE}
 * (99). {@link Slot#getMaxStackSize(ItemStack)} uses {@code min(containerCap, stack.getMaxStackSize())}, so stacks
 * never exceed 99 in inventories even when {@link ItemStack#getMaxStackSize()} is higher.
 */
@Mixin(Slot.class)
public abstract class SlotMixin {
    @Shadow @Final public Container container;

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private void stacksizeconf$raiseSlotStackLimit(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (!StackSizeConfig.shouldApplyContainerStackOverride(this.container)) {
            return;
        }
        Slot self = (Slot) (Object) this;
        int containerCap = self.getMaxStackSize();
        if (containerCap != Item.ABSOLUTE_MAX_STACK_SIZE) {
            return;
        }
        int stackMax = stack.getMaxStackSize();
        int vanillaBlend = cir.getReturnValue();
        if (stackMax <= vanillaBlend) {
            return;
        }
        cir.setReturnValue(Math.min(StackSizeConfig.MAX_STACK_HARD_CAP.get(), stackMax));
    }
}
