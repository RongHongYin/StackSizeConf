package dev.stacksizeconf.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.stacksizeconf.BetterTrading;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V", at = @At("TAIL"))
    private void stacksizeconf$betterTradingQuickRestockClient(int syncId, Inventory playerInventory, CallbackInfo ci) {
        BetterTrading.applyQuickRestockOnMenuOpen((MerchantMenu) (Object) this, playerInventory);
    }

    @Inject(
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",
            at = @At("TAIL")
    )
    private void stacksizeconf$betterTradingQuickRestockServer(int syncId, Inventory playerInventory, Merchant merchant, CallbackInfo ci) {
        BetterTrading.applyQuickRestockOnMenuOpen((MerchantMenu) (Object) this, playerInventory);
    }
}
