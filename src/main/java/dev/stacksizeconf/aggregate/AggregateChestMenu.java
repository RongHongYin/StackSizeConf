package dev.stacksizeconf.aggregate;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class AggregateChestMenu extends ChestMenu {
    private static final int AGGREGATE_SLOT_COUNT = 54;

    private final AggregateInventory aggregateInventory;

    public AggregateChestMenu(int containerId, Inventory playerInventory, AggregateInventory aggregateInventory) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, aggregateInventory, 6);
        this.aggregateInventory = aggregateInventory;
        replaceAggregateSlotsAsReadOnly();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        // Aggregate view slots require explicit confirm extraction flow.
        if (index < AGGREGATE_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        // Player inventory -> aggregate network insertion.
        ItemStack source = slot.getItem();
        ItemStack sourceCopy = source.copy();
        ItemStack remainder = this.aggregateInventory.owner().insertVirtual(source.copy());
        int moved = sourceCopy.getCount() - remainder.getCount();
        if (moved <= 0) {
            return ItemStack.EMPTY;
        }
        source.shrink(moved);
        slot.setChanged();
        this.broadcastChanges();
        return sourceCopy.copyWithCount(moved);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.aggregateInventory.stillValid(player);
    }

    private void replaceAggregateSlotsAsReadOnly() {
        for (int i = 0; i < AGGREGATE_SLOT_COUNT && i < this.slots.size(); i++) {
            Slot old = this.slots.get(i);
            Slot readOnly = new Slot(this.aggregateInventory, old.getContainerSlot(), old.x, old.y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            };
            this.slots.set(i, readOnly);
        }
    }
}
