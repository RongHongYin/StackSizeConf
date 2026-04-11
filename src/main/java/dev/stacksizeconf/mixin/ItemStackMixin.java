package dev.stacksizeconf.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.stacksizeconf.StackSizeConfig;
import dev.stacksizeconf.HandheldShulkerHandler;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Unique
    private static final ThreadLocal<Boolean> stacksizeconf$HURT_RECURSE = ThreadLocal.withInitial(() -> false);

    /**
     * When a stack of {@code N > 1} damageable items takes durability loss, vanilla applies damage to the whole stack.
     * Split one item out, run vanilla {@link ItemStack#hurtAndBreak} on that copy, then merge the remainder.
     */
    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stacksizeconf$splitStackedDurabilityLoss(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {
        if (Boolean.TRUE.equals(stacksizeconf$HURT_RECURSE.get())) {
            return;
        }
        if (!StackSizeConfig.stackOverridesEnabled()) {
            return;
        }
        ItemStack self = (ItemStack) (Object) this;
        if (self.getCount() <= 1 || !self.isDamageableItem()) {
            return;
        }
        if (stacksizeconf$findHoldingSlot(entity, self) != slot) {
            return;
        }
        ci.cancel();
        ItemStack used = self.copyWithCount(1);
        self.shrink(1);
        entity.setItemSlot(slot, used);
        stacksizeconf$HURT_RECURSE.set(true);
        try {
            entity.getItemBySlot(slot).hurtAndBreak(amount, entity, slot);
        } finally {
            stacksizeconf$HURT_RECURSE.set(false);
        }
        if (!self.isEmpty()) {
            if (entity instanceof Player player) {
                stacksizeconf$mergeRemainderIntoPlayerInventory(player, self);
            } else if (entity.level() instanceof ServerLevel serverLevel) {
                entity.spawnAtLocation(serverLevel, self.copy());
            }
        }
    }

    /** Hotbar + storage (0–35) and offhand (40); same order idea as {@link Inventory#getSlotWithRemainingSpace}. */
    @Unique
    private static final int[] stacksizeconf$PLAYER_MERGE_SLOTS = buildPlayerMergeSlots();

    @Unique
    private static int[] buildPlayerMergeSlots() {
        int[] slots = new int[37];
        int i = 0;
        for (int s = 0; s < 36; s++) {
            slots[i++] = s;
        }
        slots[i] = 40;
        return slots;
    }

    /**
     * Merge like {@link Inventory#addResource} — grow matching slots — never {@link Inventory#add}'s damaged replace path.
     */
    @Unique
    private static void stacksizeconf$mergeRemainderIntoPlayerInventory(Player player, ItemStack remainder) {
        if (remainder.isEmpty()) {
            return;
        }
        Inventory inv = player.getInventory();
        while (!remainder.isEmpty()) {
            boolean progressed = false;
            for (int slot : stacksizeconf$PLAYER_MERGE_SLOTS) {
                ItemStack dest = inv.getItem(slot);
                if (dest == remainder) {
                    continue;
                }
                if (!dest.isEmpty()
                        && ItemStack.isSameItemSameComponents(remainder, dest)
                        && dest.isStackable()) {
                    int space = inv.getMaxStackSize(dest) - dest.getCount();
                    if (space > 0) {
                        int n = Math.min(space, remainder.getCount());
                        dest.grow(n);
                        remainder.shrink(n);
                        dest.setPopTime(5);
                        stacksizeconf$notifyInventorySlot(player, slot);
                        progressed = true;
                        break;
                    }
                }
            }
            if (remainder.isEmpty()) {
                break;
            }
            if (!progressed) {
                int free = inv.getFreeSlot();
                if (free == -1 && inv.getItem(40).isEmpty()) {
                    free = 40;
                }
                if (free == -1) {
                    player.drop(remainder, false);
                    remainder.setCount(0);
                    break;
                }
                int cap = inv.getMaxStackSize(remainder);
                int move = Math.min(remainder.getCount(), cap);
                inv.setItem(free, remainder.split(move));
                inv.getItem(free).setPopTime(5);
                stacksizeconf$notifyInventorySlot(player, free);
            }
        }
    }

    @Unique
    private static void stacksizeconf$notifyInventorySlot(Player player, int slot) {
        if (player instanceof ServerPlayer serverPlayer) {
            AbstractContainerMenu menu = serverPlayer.inventoryMenu;
            serverPlayer.connection.send(
                    new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), slot, serverPlayer.getInventory().getItem(slot)));
        }
    }

    @Unique
    private static @Nullable EquipmentSlot stacksizeconf$findHoldingSlot(@Nullable LivingEntity entity, ItemStack stack) {
        if (entity == null) {
            return null;
        }
        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            if (entity.getItemBySlot(equipmentSlot) == stack) {
                return equipmentSlot;
            }
        }
        return null;
    }

    /**
     * When {@code > 0}, {@link ItemStack#getMaxStackSize()} returns its vanilla value so we can read it
     * while computing {@link ItemStack#isStackable()} without recursion or double-applying the config.
     */
    @Unique
    private static final ThreadLocal<Integer> stacksizeconf$SKIP_MAX_MIXIN = ThreadLocal.withInitial(() -> 0);

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void stacksizeconf$modifyMaxStack(CallbackInfoReturnable<Integer> cir) {
        if (!StackSizeConfig.stackOverridesEnabled()) {
            return;
        }
        ItemStack self = (ItemStack) (Object) this;
        if (self.isEmpty() || stacksizeconf$SKIP_MAX_MIXIN.get() > 0) {
            return;
        }
        cir.setReturnValue(StackSizeConfig.applyToVanillaMax(cir.getReturnValue()));
    }

    /**
     * Vanilla {@code isStackable()} requires max stack {@code > 1} and (for damageable items) no damage,
     * which prevents tools and armor from ever stacking. After our max-stack change, use
     * {@code effectiveMax > 1} only; merging still requires identical components (same state).
     */
    @Inject(method = "isStackable", at = @At("HEAD"), cancellable = true)
    private void stacksizeconf$isStackable(CallbackInfoReturnable<Boolean> cir) {
        if (!StackSizeConfig.stackOverridesEnabled()) {
            return;
        }
        ItemStack self = (ItemStack) (Object) this;
        if (self.isEmpty()) {
            return;
        }
        if (stacksizeconf$isNonEmptyShulkerBox(self)) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }
        int depth = stacksizeconf$SKIP_MAX_MIXIN.get();
        stacksizeconf$SKIP_MAX_MIXIN.set(depth + 1);
        try {
            int vanillaMax = self.getMaxStackSize();
            int effectiveMax = StackSizeConfig.applyToVanillaMax(vanillaMax);
            cir.setReturnValue(effectiveMax > 1);
            cir.cancel();
        } finally {
            stacksizeconf$SKIP_MAX_MIXIN.set(depth);
        }
    }

    @Unique
    private static boolean stacksizeconf$isNonEmptyShulkerBox(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
            return false;
        }
        ItemContainerContents contents = stack.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        return !contents.equals(ItemContainerContents.EMPTY);
    }
}
