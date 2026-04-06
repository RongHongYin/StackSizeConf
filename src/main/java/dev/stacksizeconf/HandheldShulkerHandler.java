package dev.stacksizeconf;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class HandheldShulkerHandler {
    private static final Set<UUID> OPENING_PLAYERS = ConcurrentHashMap.newKeySet();

    private HandheldShulkerHandler() {
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!StackSizeConfig.ENABLE_HANDHELD_SHULKER_OPEN.get()) {
            return;
        }
        InteractionHand hand = event.getHand();
        if (!StackSizeConfig.SHULKER_OPEN_ALLOW_OFFHAND.get() && hand == InteractionHand.OFF_HAND) {
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!isShulkerBoxItem(held)) {
            return;
        }
        if (held.getCount() > 1) {
            held = splitHeldStackForOpening(player, hand, held);
        }
        if (!passesOpenChecks(player)) {
            return;
        }

        openHandheldShulker(player, hand, held.copy());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static boolean passesOpenChecks(ServerPlayer player) {
        if (!StackSizeConfig.SHULKER_OPEN_SERVER_VALIDATION.get()) {
            return true;
        }
        if (StackSizeConfig.SHULKER_OPEN_REQUIRE_SNEAK.get() && !player.isShiftKeyDown()) {
            return false;
        }
        if (!StackSizeConfig.SHULKER_OPEN_ALLOW_RIDING_OR_FLYING.get()) {
            if (player.isPassenger() || player.getAbilities().flying || player.isFallFlying()) {
                return false;
            }
        }
        return true;
    }

    private static void openHandheldShulker(ServerPlayer player, InteractionHand hand, ItemStack snapshot) {
        HandheldShulkerContainer container = new HandheldShulkerContainer(player, hand, snapshot);
        var opened = player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new HandheldShulkerMenu(id, inv, container, player, hand),
                snapshot.getHoverName()
        ));
        if (opened.isEmpty()) {
            OPENING_PLAYERS.remove(player.getUUID());
            return;
        }
        OPENING_PLAYERS.add(player.getUUID());
        player.swing(hand, true);
        if (StackSizeConfig.SHULKER_OPEN_PLAY_SOUND.get()) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.SHULKER_BOX_OPEN, SoundSource.PLAYERS, 0.5F, 1.0F);
        }
    }

    public static boolean isShulkerBoxItem(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        return blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    private static ItemStack splitHeldStackForOpening(ServerPlayer player, InteractionHand hand, ItemStack held) {
        int keep = 1;
        int remainderCount = held.getCount() - keep;
        ItemStack single = held.copyWithCount(keep);
        player.setItemInHand(hand, single);
        if (remainderCount <= 0) {
            return single;
        }
        ItemStack remainder = held.copyWithCount(remainderCount);
        storeRemainderOutsideHand(player, hand, remainder);
        return player.getItemInHand(hand);
    }

    private static void storeRemainderOutsideHand(ServerPlayer player, InteractionHand hand, ItemStack remainder) {
        if (remainder.isEmpty()) {
            return;
        }
        Inventory inv = player.getInventory();
        int handSlot = hand == InteractionHand.MAIN_HAND ? inv.getSelectedSlot() : 40;
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            if (slot == handSlot) {
                continue;
            }
            ItemStack dst = inv.getItem(slot);
            if (dst.isEmpty()) {
                continue;
            }
            if (ItemStack.isSameItemSameComponents(dst, remainder) && dst.isStackable()) {
                int space = inv.getMaxStackSize(dst) - dst.getCount();
                if (space <= 0) {
                    continue;
                }
                int move = Math.min(space, remainder.getCount());
                dst.grow(move);
                remainder.shrink(move);
                if (remainder.isEmpty()) {
                    return;
                }
            }
        }
        while (!remainder.isEmpty()) {
            int free = inv.getFreeSlot();
            if (free < 0 || free == handSlot) {
                break;
            }
            int move = Math.min(remainder.getCount(), inv.getMaxStackSize(remainder));
            inv.setItem(free, remainder.split(move));
        }
        if (!remainder.isEmpty()) {
            if (player.level() instanceof ServerLevel serverLevel) {
                player.spawnAtLocation(serverLevel, remainder.copy());
            }
            remainder.setCount(0);
        }
    }

    public static boolean playerHasOpenLockedShulker(Player player) {
        return OPENING_PLAYERS.contains(player.getUUID());
    }

    private static final class HandheldShulkerMenu extends ShulkerBoxMenu {
        private final ServerPlayer owner;
        private final InteractionHand hand;
        private final HandheldShulkerContainer handheldContainer;

        HandheldShulkerMenu(int id, net.minecraft.world.entity.player.Inventory inv, SimpleContainer container, ServerPlayer owner, InteractionHand hand) {
            super(id, inv, container);
            this.owner = owner;
            this.hand = hand;
            this.handheldContainer = (HandheldShulkerContainer) container;
        }

        @Override
        public void removed(Player player) {
            handheldContainer.flushToHeldItem();
            super.removed(player);
            OPENING_PLAYERS.remove(owner.getUUID());
        }
    }

    private static final class HandheldShulkerContainer extends SimpleContainer {
        private final ServerPlayer player;
        private final InteractionHand hand;

        HandheldShulkerContainer(ServerPlayer player, InteractionHand hand, ItemStack source) {
            super(27);
            this.player = player;
            this.hand = hand;
            loadFrom(source);
        }

        private void loadFrom(ItemStack source) {
            ItemContainerContents contents = source.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            NonNullList<ItemStack> temp = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            contents.copyInto(temp);
            for (int i = 0; i < getContainerSize(); i++) {
                setItem(i, temp.get(i));
            }
        }

        @Override
        public boolean stillValid(Player player) {
            ItemStack current = this.player.getItemInHand(hand);
            return player == this.player && isShulkerBoxItem(current);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            saveBack();
        }

        void flushToHeldItem() {
            saveBack();
        }

        private void saveBack() {
            ItemStack current = this.player.getItemInHand(hand);
            if (!isShulkerBoxItem(current)) {
                return;
            }
            NonNullList<ItemStack> temp = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            boolean anyNonEmpty = false;
            for (int i = 0; i < getContainerSize(); i++) {
                ItemStack s = getItem(i).copy();
                temp.set(i, s);
                if (!s.isEmpty()) {
                    anyNonEmpty = true;
                }
            }
            if (!anyNonEmpty) {
                this.player.setItemInHand(hand, new ItemStack(current.getItem(), current.getCount()));
                return;
            }
            current.set(net.minecraft.core.component.DataComponents.CONTAINER, ItemContainerContents.fromItems(temp));
        }
    }
}
