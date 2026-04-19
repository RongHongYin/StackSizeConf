package dev.stacksizeconf.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;

@Mixin(MerchantMenu.class)
public interface MerchantMenuAccess {
    @Accessor("tradeContainer")
    MerchantContainer stacksizeconf$getTradeContainer();
}
