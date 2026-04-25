package dev.stacksizeconf.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.stacksizeconf.StackSizeConfig;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * {@link Container} is an interface — the mixin must be an {@code interface} too, not a class.
 *
 * @see SlotMixin Same cap logic for GUI slots; this fixes {@code limitSize} in {@code setItem} paths.
 */
@Mixin(Container.class)
public interface ContainerMixin {
    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    default void stacksizeconf$raiseContainerStackLimit(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        Container self = (Container) this;
        if (!StackSizeConfig.shouldApplyContainerStackOverride(self)) {
            return;
        }
        if (self.getMaxStackSize() != Item.ABSOLUTE_MAX_STACK_SIZE) {
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
