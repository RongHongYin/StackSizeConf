package dev.stacksizeconf.aggregate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class AggregateInventory implements Container {
    public static final int VIRTUAL_SLOT_CAPACITY = 1000;

    private final AggregateChestBlockEntity owner;
    private final int size;
    private int viewOffset;
    private String filterQuery = "";
    private List<Integer> orderedSummaryIndices = List.of();
    private boolean useMappedOrder;
    private SortMode sortMode = SortMode.NONE;
    private Player boundPlayer;

    AggregateInventory(AggregateChestBlockEntity owner, int size) {
        this.owner = owner;
        this.size = size;
    }

    AggregateChestBlockEntity owner() {
        return this.owner;
    }

    void bindPlayer(Player player) {
        this.boundPlayer = player;
    }

    void resetViewOnOpen() {
        this.viewOffset = 0;
    }

    void refreshView() {
        this.owner.invalidateCache();
        rebuildFilteredIndices();
    }

    public int getViewOffset() {
        return this.viewOffset;
    }

    public int getVisibleSize() {
        return this.size;
    }

    public int getMaxOffset() {
        int entries = getLogicalEntryCount();
        return Math.max(0, entries - this.size);
    }

    public void scrollByRows(int rows) {
        if (rows == 0) {
            return;
        }
        int step = rows * 9;
        int maxOffset = getMaxOffset();
        int clamped = Math.max(0, Math.min(maxOffset, this.viewOffset + step));
        if (clamped == this.viewOffset) {
            return;
        }
        this.viewOffset = clamped;
        setChanged();
    }

    public void setViewOffset(int offset) {
        int maxOffset = getMaxOffset();
        int clamped = Math.max(0, Math.min(maxOffset, offset));
        if (clamped == this.viewOffset) {
            return;
        }
        this.viewOffset = clamped;
        setChanged();
    }

    private int mapVisibleToSummaryIndex(int visibleSlot) {
        int logical = this.viewOffset + Math.max(0, visibleSlot);
        if (logical < 0 || logical >= getLogicalEntryCount()) {
            return -1;
        }
        if (!this.useMappedOrder) {
            return logical;
        }
        if (logical >= this.orderedSummaryIndices.size()) {
            return -1;
        }
        return this.orderedSummaryIndices.get(logical);
    }

    public void setFilterQuery(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals(this.filterQuery)) {
            return;
        }
        this.filterQuery = normalized;
        rebuildFilteredIndices();
        this.viewOffset = 0;
        setChanged();
    }

    public String getFilterQuery() {
        return this.filterQuery;
    }

    public void setSortMode(SortMode mode) {
        if (mode == null || mode == this.sortMode) {
            return;
        }
        this.sortMode = mode;
        rebuildFilteredIndices();
        this.viewOffset = 0;
        setChanged();
    }

    public SortMode getSortMode() {
        return this.sortMode;
    }

    private int getLogicalEntryCount() {
        // UI contract: aggregate view is always a fixed 1000-slot window.
        return VIRTUAL_SLOT_CAPACITY;
    }

    private void rebuildFilteredIndices() {
        int summaryCount = this.owner.summaryEntryCount();
        List<Integer> matches = new ArrayList<>(summaryCount);
        for (int i = 0; i < summaryCount; i++) {
            ItemStack stack = this.owner.getVirtualStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!this.filterQuery.isEmpty()) {
                String name = stack.getHoverName().getString();
                if (name == null || !name.toLowerCase(Locale.ROOT).contains(this.filterQuery)) {
                    continue;
                }
            }
            matches.add(i);
        }
        if (this.sortMode == SortMode.COUNT_ASC || this.sortMode == SortMode.COUNT_DESC) {
            Comparator<Integer> comparator = Comparator
                    .comparingLong((Integer idx) -> this.owner.getVirtualTotalCount(idx))
                    .thenComparing(idx -> this.owner.getVirtualStack(idx).getHoverName().getString());
            if (this.sortMode == SortMode.COUNT_DESC) {
                comparator = comparator.reversed();
            }
            matches.sort(comparator);
        }
        this.orderedSummaryIndices = List.copyOf(matches);
        this.useMappedOrder = !this.filterQuery.isEmpty() || this.sortMode != SortMode.NONE;
    }

    @Override
    public int getContainerSize() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.owner.summaryEntryCount() == 0;
    }

    @Override
    public ItemStack getItem(int slot) {
        int summarySlot = mapVisibleToSummaryIndex(slot);
        if (summarySlot < 0) {
            return ItemStack.EMPTY;
        }
        return this.owner.getVirtualStack(summarySlot);
    }

    public long getVirtualTotalForSlot(int slot) {
        int summarySlot = mapVisibleToSummaryIndex(slot);
        if (summarySlot < 0) {
            return 0L;
        }
        return this.owner.getVirtualTotalCount(summarySlot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        // Extraction is handled by an explicit confirm flow to avoid accidental removals.
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    public ItemStack extractByRequest(int slot, int amount) {
        int summarySlot = mapVisibleToSummaryIndex(slot);
        if (summarySlot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = this.owner.extractVirtual(summarySlot, amount);
        if (!extracted.isEmpty()) {
            rebuildFilteredIndices();
            setChanged();
        }
        return extracted;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack before = getItem(slot);
        if (before.isEmpty()) {
            ItemStack remainder = this.owner.insertVirtual(stack);
            stack.setCount(remainder.getCount());
            setChanged();
            return;
        }
        if (stack.isEmpty()) {
            // Ignore empty-sync writes from menu internals; extraction uses explicit confirm flow.
            return;
        }
        if (ItemStack.isSameItemSameComponents(before, stack)) {
            // Quick-move and menu sync may write the full destination stack.
            // Only treat growth as insertion; never infer extraction from a smaller sync snapshot.
            if (stack.getCount() <= before.getCount()) {
                stack.setCount(before.getCount());
                return;
            }
            int delta = stack.getCount() - before.getCount();
            ItemStack toInsert = stack.copyWithCount(delta);
            ItemStack remainder = this.owner.insertVirtual(toInsert);
            stack.setCount(before.getCount() + (delta - remainder.getCount()));
            setChanged();
            return;
        }
        // Different-type replace is not a valid operation for aggregated virtual slots.
        stack.setCount(before.getCount());
    }

    @Override
    public int getMaxStackSize() {
        // Virtual aggregate slots can represent very large totals; keep quick-move merge path enabled.
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void setChanged() {
        this.owner.invalidateCache();
        rebuildFilteredIndices();
        this.owner.setChanged();
        if (this.boundPlayer != null && this.boundPlayer.containerMenu != null) {
            this.boundPlayer.containerMenu.broadcastChanges();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.owner.stillValid(player);
    }

    @Override
    public void clearContent() {
        // Intentionally no-op: aggregate chest never owns physical inventory.
    }

    public enum SortMode {
        NONE,
        COUNT_ASC,
        COUNT_DESC
    }
}
