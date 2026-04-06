package dev.stacksizeconf;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class ItemMagnetHandler {
    private ItemMagnetHandler() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!StackSizeConfig.ENABLE_ITEM_MAGNET.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isSpectator() || !player.isAlive()) {
            return;
        }

        double range = StackSizeConfig.ITEM_MAGNET_RANGE.get();
        if (range <= 0.0D) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(range, range, range);
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, area);
        if (items.isEmpty()) {
            return;
        }
        for (ItemEntity item : items) {
            if (!item.isAlive()) {
                continue;
            }
            if (item.hasPickUpDelay()) {
                continue;
            }
            if (player.containerMenu instanceof ShulkerBoxMenu
                    && HandheldShulkerHandler.playerHasOpenLockedShulker(player)
                    && HandheldShulkerHandler.isShulkerBoxItem(item.getItem())) {
                continue;
            }
            ItemStack remaining = item.getItem().copy();
            int before = remaining.getCount();
            addToInventoryExcludingHands(player, remaining);
            int moved = before - remaining.getCount();
            if (moved <= 0) {
                continue;
            }
            player.take(item, moved);
            if (remaining.isEmpty()) {
                item.discard();
            } else {
                item.setItem(remaining);
            }
        }
    }

    private static void addToInventoryExcludingHands(ServerPlayer player, ItemStack remaining) {
        if (remaining.isEmpty()) {
            return;
        }
        Inventory inv = player.getInventory();

        // First pass: merge into existing compatible stacks.
        // Do not merge into an "open locked" handheld shulker stack.
        for (int slot = 0; slot < inv.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack dest = inv.getItem(slot);
            if (dest.isEmpty() || !ItemStack.isSameItemSameComponents(dest, remaining) || !dest.isStackable()) {
                continue;
            }
            int space = inv.getMaxStackSize(dest) - dest.getCount();
            if (space <= 0) {
                continue;
            }
            int move = Math.min(space, remaining.getCount());
            dest.grow(move);
            remaining.shrink(move);
        }

        // Second pass: place into empty slots.
        for (int slot = 0; slot < inv.getContainerSize() && !remaining.isEmpty(); slot++) {
            if (!inv.getItem(slot).isEmpty()) {
                continue;
            }
            int move = Math.min(inv.getMaxStackSize(remaining), remaining.getCount());
            inv.setItem(slot, remaining.split(move));
        }
    }

}
