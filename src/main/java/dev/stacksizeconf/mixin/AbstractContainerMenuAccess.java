package dev.stacksizeconf.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccess {
    @Invoker("moveItemStackTo")
    boolean stacksizeconf$invokeMoveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean fromLast);
}
