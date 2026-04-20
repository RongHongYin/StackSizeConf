package dev.stacksizeconf.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.stacksizeconf.BetterTrading;
import dev.stacksizeconf.BetterTradingMode;
import dev.stacksizeconf.StackSizeConfig;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.trading.Merchant;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {
    @Shadow @Final private Merchant trader;
    @Invoker("playTradeSound")
    protected abstract void stacksizeconf$invokePlayTradeSound();

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void stacksizeconf$limitInfiniteShiftTake(Player player, int slotIndex, CallbackInfoReturnable<net.minecraft.world.item.ItemStack> cir) {
        if (slotIndex != 2) {
            return;
        }
        if (StackSizeConfig.BETTER_TRADING_MODE.get() != BetterTradingMode.INFINITE) {
            return;
        }
        if (!(this.trader instanceof Villager villager) || villager.getVillagerData().level() < VillagerData.MAX_VILLAGER_LEVEL) {
            return;
        }
        int cap = StackSizeConfig.INFINITE_TRADE_MAX_PER_TAKE.get();
        if (cap <= 0) {
            cir.setReturnValue(net.minecraft.world.item.ItemStack.EMPTY);
            return;
        }
        MerchantMenu self = (MerchantMenu) (Object) this;
        Slot resultSlot = self.getSlot(slotIndex);
        if (resultSlot == null || !resultSlot.hasItem()) {
            cir.setReturnValue(net.minecraft.world.item.ItemStack.EMPTY);
            return;
        }

        int movedTotal = 0;
        while (movedTotal < cap && resultSlot.hasItem()) {
            net.minecraft.world.item.ItemStack tradeOutput = resultSlot.getItem();
            int perTrade = tradeOutput.getCount();
            if (perTrade <= 0 || movedTotal + perTrade > cap) {
                break;
            }
            net.minecraft.world.item.ItemStack toMove = tradeOutput.copy();
            if (!((AbstractContainerMenuAccess) self).stacksizeconf$invokeMoveItemStackTo(toMove, 3, 39, true) || !toMove.isEmpty()) {
                break;
            }
            resultSlot.onQuickCraft(tradeOutput, tradeOutput.copy());
            this.stacksizeconf$invokePlayTradeSound();
            resultSlot.onTake(player, tradeOutput);
            movedTotal += perTrade;
        }
        cir.setReturnValue(net.minecraft.world.item.ItemStack.EMPTY);
    }

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
