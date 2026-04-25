package dev.stacksizeconf.aggregate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public final class AggregateChestBlockEntity extends BlockEntity implements WorldlyContainer, net.minecraft.world.MenuProvider {
    public static final String AGGREGATE_TOTAL_TAG = "stacksizeconf:aggregate_total";
    private static final int MAX_SLOTS = 54;
    private static final int MAX_SEARCH_NODES = 1024;
    private static final int[] EXPOSED_SLOTS = buildExposedSlots();

    private final AggregateInventory aggregateInventory = new AggregateInventory(this, MAX_SLOTS);
    private boolean cacheValid;
    private long cachedGameTime = Long.MIN_VALUE;
    private boolean removing;
    private List<ContainerNode> cachedContainers = List.of();
    private List<AggregateSlotEntry> cachedSummary = List.of();

    public AggregateChestBlockEntity(BlockPos pos, BlockState blockState) {
        super(AggregateChestRegistry.AGGREGATE_CHEST_BLOCK_ENTITY, pos, blockState);
    }

    public void invalidateCache() {
        this.cacheValid = false;
        this.cachedGameTime = Long.MIN_VALUE;
    }

    public void prepareForRemoval() {
        this.removing = true;
        this.cachedSummary = List.of();
        this.cachedContainers = List.of();
        invalidateCache();
    }

    private void refreshCacheIfNeeded() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        long now = this.level.getGameTime();
        // Keep external edits (player interacting with real containers) visible quickly without
        // per-call full rescans: refresh at most once per game tick.
        if (this.cacheValid && this.cachedGameTime == now) {
            return;
        }
        this.cachedContainers = discoverConnectedContainers();
        this.cachedSummary = summarize(this.cachedContainers);
        this.cacheValid = true;
        this.cachedGameTime = now;
        setChanged();
    }

    private List<ContainerNode> discoverConnectedContainers() {
        if (this.level == null) {
            return List.of();
        }
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<ContainerNode> result = new ArrayList<>();
        Set<ContainerIdentity> dedup = new HashSet<>();

        for (Direction dir : Direction.values()) {
            queue.add(this.worldPosition.relative(dir));
        }

        int explored = 0;
        while (!queue.isEmpty() && explored < MAX_SEARCH_NODES) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) {
                continue;
            }
            explored++;
            Container container = resolveContainerAt(pos);
            if (container == null) {
                continue;
            }
            // Connectivity is graph-based: even if this container is already deduplicated for counting
            // (e.g. the second half of a double chest), we must still expand neighbors from this node.
            for (Direction dir : Direction.values()) {
                queue.add(pos.relative(dir));
            }
            ContainerIdentity identity = ContainerIdentity.of(this.level, pos, container);
            if (dedup.add(identity)) {
                int dist = manhattanDistance(this.worldPosition, pos);
                result.add(new ContainerNode(pos.immutable(), container, dist));
            }
        }
        result.sort(Comparator.comparingInt(ContainerNode::distance));
        return List.copyOf(result);
    }

    private Container resolveContainerAt(BlockPos pos) {
        if (this.level == null) {
            return null;
        }
        BlockEntity blockEntity = this.level.getBlockEntity(pos);
        if (blockEntity instanceof AggregateChestBlockEntity) {
            return null;
        }
        if (this.level.getBlockState(pos).getBlock() instanceof ChestBlock chestBlock) {
            Container chestContainer = ChestBlock.getContainer(chestBlock, this.level.getBlockState(pos), this.level, pos, true);
            if (chestContainer != null) {
                return chestContainer;
            }
        }
        if (blockEntity instanceof Container container) {
            return container;
        }
        return null;
    }

    private static int manhattanDistance(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }

    private static List<AggregateSlotEntry> summarize(List<ContainerNode> nodes) {
        List<AggregateSlotEntry> out = new ArrayList<>();
        for (ContainerNode node : nodes) {
            Container container = node.container();
            int size = container.getContainerSize();
            for (int i = 0; i < size; i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                AggregateSlotEntry existing = findByTemplate(out, stack);
                if (existing == null) {
                    out.add(new AggregateSlotEntry(stack.copyWithCount(1), stack.getCount()));
                } else {
                    existing.add(stack.getCount());
                }
            }
        }
        out.sort(Comparator.comparing(e -> e.template().getHoverName().getString()));
        return List.copyOf(out);
    }

    private static AggregateSlotEntry findByTemplate(List<AggregateSlotEntry> entries, ItemStack stack) {
        for (AggregateSlotEntry entry : entries) {
            if (ItemStack.isSameItemSameComponents(entry.template(), stack)) {
                return entry;
            }
        }
        return null;
    }

    public int summarySize() {
        if (this.removing) {
            return 0;
        }
        refreshCacheIfNeeded();
        return Math.min(MAX_SLOTS, this.cachedSummary.size());
    }

    public int summaryEntryCount() {
        if (this.removing) {
            return 0;
        }
        refreshCacheIfNeeded();
        return this.cachedSummary.size();
    }

    public ItemStack getVirtualStack(int slot) {
        if (this.removing) {
            return ItemStack.EMPTY;
        }
        refreshCacheIfNeeded();
        if (slot < 0 || slot >= this.cachedSummary.size()) {
            return ItemStack.EMPTY;
        }
        AggregateSlotEntry entry = this.cachedSummary.get(slot);
        return entry.toDisplayStack();
    }

    public long getVirtualTotalCount(int slot) {
        if (this.removing) {
            return 0L;
        }
        refreshCacheIfNeeded();
        if (slot < 0 || slot >= this.cachedSummary.size()) {
            return 0L;
        }
        return this.cachedSummary.get(slot).totalCount();
    }

    public ItemStack extractVirtual(int slot, int amount) {
        if (this.removing) {
            return ItemStack.EMPTY;
        }
        refreshCacheIfNeeded();
        if (slot < 0 || slot >= this.cachedSummary.size() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        AggregateSlotEntry entry = this.cachedSummary.get(slot);
        int request = (int) Math.min((long) amount, entry.totalCount());
        ItemStack extracted = extractFromContainers(entry.template(), request);
        if (!extracted.isEmpty()) {
            invalidateCache();
            refreshCacheIfNeeded();
        }
        return extracted;
    }

    public ItemStack insertVirtual(ItemStack stack) {
        if (this.removing) {
            return stack.copy();
        }
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        refreshCacheIfNeeded();
        ItemStack toInsert = stack.copy();
        ItemStack remainder = distributeIntoContainers(toInsert);
        if (remainder.getCount() != toInsert.getCount()) {
            invalidateCache();
            refreshCacheIfNeeded();
        }
        return remainder;
    }

    private ItemStack extractFromContainers(ItemStack template, int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        int remaining = count;
        ItemStack out = ItemStack.EMPTY;
        for (ContainerNode node : this.cachedContainers) {
            Container container = node.container();
            int size = container.getContainerSize();
            for (int slot = 0; slot < size && remaining > 0; slot++) {
                ItemStack inSlot = container.getItem(slot);
                if (inSlot.isEmpty() || !ItemStack.isSameItemSameComponents(template, inSlot)) {
                    continue;
                }
                int take = Math.min(remaining, inSlot.getCount());
                ItemStack removed = container.removeItem(slot, take);
                if (removed.isEmpty()) {
                    continue;
                }
                if (out.isEmpty()) {
                    out = removed.copy();
                } else {
                    out.grow(removed.getCount());
                }
                remaining -= removed.getCount();
            }
            container.setChanged();
            if (remaining <= 0) {
                break;
            }
        }
        return out;
    }

    private ItemStack distributeIntoContainers(ItemStack stack) {
        List<ContainerNode> ordered = new ArrayList<>(this.cachedContainers);
        ordered.sort(Comparator
                .comparingInt((ContainerNode n) -> containsItem(n.container(), stack) ? 0 : 1)
                .thenComparing((ContainerNode n) -> -emptySlots(n.container()))
                .thenComparingInt(ContainerNode::distance));

        ItemStack remainder = stack.copy();
        for (ContainerNode node : ordered) {
            if (remainder.isEmpty()) {
                break;
            }
            remainder = insertIntoContainer(node.container(), remainder);
            node.container().setChanged();
        }
        return remainder;
    }

    private static boolean containsItem(Container container, ItemStack needle) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, needle)) {
                return true;
            }
        }
        return false;
    }

    private static int emptySlots(Container container) {
        int empty = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty()) {
                empty++;
            }
        }
        return empty;
    }

    private static ItemStack insertIntoContainer(Container container, ItemStack stack) {
        ItemStack moving = stack.copy();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty() || !ItemStack.isSameItemSameComponents(slot, moving)) {
                continue;
            }
            int cap = Math.min(container.getMaxStackSize(moving), moving.getMaxStackSize());
            int space = cap - slot.getCount();
            if (space <= 0) {
                continue;
            }
            int move = Math.min(space, moving.getCount());
            slot.grow(move);
            moving.shrink(move);
            if (moving.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (!slot.isEmpty()) {
                continue;
            }
            int cap = Math.min(container.getMaxStackSize(moving), moving.getMaxStackSize());
            int move = Math.min(cap, moving.getCount());
            container.setItem(i, moving.split(move));
            if (moving.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return moving;
    }

    @Override
    public Component getDisplayName() {
        refreshCacheIfNeeded();
        int totalCapacity = getLinkedContainerSlotCapacity();
        int usedCapacity = getLinkedContainerUsedSlots();
        return Component.translatable("container.stacksizeconf.aggregate_chest")
                .append(Component.literal("  "))
                .append(Component.translatable("container.stacksizeconf.aggregate_capacity", usedCapacity, totalCapacity));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        this.aggregateInventory.bindPlayer(player);
        this.aggregateInventory.resetViewOnOpen();
        this.aggregateInventory.refreshView();
        return new AggregateChestMenu(containerId, inventory, this.aggregateInventory);
    }

    @Override
    public int getContainerSize() {
        return MAX_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        refreshCacheIfNeeded();
        return this.cachedSummary.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.aggregateInventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return this.aggregateInventory.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.aggregateInventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.aggregateInventory.setItem(slot, stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.aggregateInventory.clearContent();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return EXPOSED_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return true;
    }

    private static int[] buildExposedSlots() {
        int[] out = new int[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            out[i] = i;
        }
        return out;
    }

    private int getLinkedContainerSlotCapacity() {
        int total = 0;
        for (ContainerNode node : this.cachedContainers) {
            total += Math.max(0, node.container().getContainerSize());
        }
        return total;
    }

    private int getLinkedContainerUsedSlots() {
        int used = 0;
        for (ContainerNode node : this.cachedContainers) {
            Container container = node.container();
            int size = container.getContainerSize();
            for (int i = 0; i < size; i++) {
                if (!container.getItem(i).isEmpty()) {
                    used++;
                }
            }
        }
        return used;
    }

    private record ContainerNode(BlockPos pos, Container container, int distance) {}

    static final class AggregateSlotEntry {
        private final ItemStack template;
        private long totalCount;

        AggregateSlotEntry(ItemStack template, long totalCount) {
            this.template = template;
            this.totalCount = totalCount;
        }

        ItemStack template() {
            return template;
        }

        long totalCount() {
            return totalCount;
        }

        void add(long delta) {
            this.totalCount += delta;
        }

        ItemStack toDisplayStack() {
            ItemStack display = this.template.copy();
            // Keep slot non-empty while the real total is rendered by a custom overlay.
            display.setCount(1);
            CustomData customData = display.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();
            tag.putLong(AGGREGATE_TOTAL_TAG, this.totalCount);
            display.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return display;
        }
    }

    private record ContainerIdentity(BlockPos keyPos) {
        static ContainerIdentity of(Level level, BlockPos sourcePos, Container container) {
            if (level.getBlockState(sourcePos).getBlock() instanceof ChestBlock) {
                return new ContainerIdentity(canonicalChestPos(level, sourcePos));
            }
            if (container instanceof BlockEntity be) {
                return new ContainerIdentity(be.getBlockPos().immutable());
            }
            return new ContainerIdentity(sourcePos.immutable());
        }

        private static BlockPos canonicalChestPos(Level level, BlockPos sourcePos) {
            BlockState self = level.getBlockState(sourcePos);
            if (!(self.getBlock() instanceof ChestBlock)) {
                return sourcePos.immutable();
            }
            ChestType chestType = self.getValue(ChestBlock.TYPE);
            if (chestType == ChestType.SINGLE) {
                return sourcePos.immutable();
            }
            Direction connected = ChestBlock.getConnectedDirection(self);
            BlockPos otherPos = sourcePos.relative(connected);
            BlockState other = level.getBlockState(otherPos);
            if (!(other.getBlock() instanceof ChestBlock) || other.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
                return sourcePos.immutable();
            }
            return comparePos(sourcePos, otherPos) <= 0 ? sourcePos.immutable() : otherPos.immutable();
        }

        private static int comparePos(BlockPos a, BlockPos b) {
            if (a.getY() != b.getY()) {
                return Integer.compare(a.getY(), b.getY());
            }
            if (a.getZ() != b.getZ()) {
                return Integer.compare(a.getZ(), b.getZ());
            }
            return Integer.compare(a.getX(), b.getX());
        }
    }
}
