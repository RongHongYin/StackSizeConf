package dev.stacksizeconf;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public final class BetterTrading {
    private BetterTrading() {}

    public static void applyQuickRestockOnMenuOpen(MerchantMenu menu, Inventory playerInventory) {
        if (StackSizeConfig.BETTER_TRADING_MODE.get() != BetterTradingMode.QUICK_RESTOCK) {
            return;
        }
        if (playerInventory.player.level().isClientSide()) {
            return;
        }
        MerchantOffers offers = menu.getOffers();
        if (offers == null) {
            return;
        }
        for (MerchantOffer offer : offers) {
            if (offer.isOutOfStock()) {
                offer.resetUses();
            }
        }
    }

    public static void afterMerchantResultTake(Merchant merchant, MerchantContainer tradeContainer) {
        if (StackSizeConfig.BETTER_TRADING_MODE.get() != BetterTradingMode.INFINITE) {
            return;
        }
        if (merchant.isClientSide()) {
            return;
        }
        if (!(merchant instanceof Villager v)) {
            return;
        }
        if (v.getVillagerData().level() < VillagerData.MAX_VILLAGER_LEVEL) {
            return;
        }
        MerchantOffer offer = tradeContainer.getActiveOffer();
        if (offer != null) {
            offer.resetUses();
        }
    }

    public static void capTradeTakeStack(Merchant merchant, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (merchant.isClientSide()) {
            return;
        }
        if (StackSizeConfig.BETTER_TRADING_MODE.get() != BetterTradingMode.INFINITE) {
            return;
        }
        if (!(merchant instanceof Villager v)) {
            return;
        }
        if (v.getVillagerData().level() < VillagerData.MAX_VILLAGER_LEVEL) {
            return;
        }
        int cap = StackSizeConfig.INFINITE_TRADE_MAX_PER_TAKE.get();
        if (cap < 1) {
            return;
        }
        if (stack.getCount() > cap) {
            stack.setCount(cap);
        }
    }
}
