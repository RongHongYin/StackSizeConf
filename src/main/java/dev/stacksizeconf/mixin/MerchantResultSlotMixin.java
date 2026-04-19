package dev.stacksizeconf.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.stacksizeconf.BetterTrading;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;

@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlotMixin {
    @Shadow @Final private Merchant merchant;

    @Inject(method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void stacksizeconf$capInfiniteTake(Player player, ItemStack stack, CallbackInfo ci) {
        BetterTrading.capTradeTakeStack(this.merchant, stack);
    }

    @Inject(method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
    private void stacksizeconf$infiniteTradeRestock(Player player, ItemStack stack, CallbackInfo ci) {
        if (!(player.containerMenu instanceof MerchantMenu menu)) {
            return;
        }
        BetterTrading.afterMerchantResultTake(this.merchant, ((MerchantMenuAccess) menu).stacksizeconf$getTradeContainer());
    }
}
