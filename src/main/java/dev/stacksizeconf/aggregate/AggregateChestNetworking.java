package dev.stacksizeconf.aggregate;

import dev.stacksizeconf.StackSizeMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class AggregateChestNetworking {
    public static final Identifier REQUEST_EXTRACT_ID = Identifier.fromNamespaceAndPath(StackSizeMod.MOD_ID, "aggregate_chest_extract");
    public static final Identifier REQUEST_SCROLL_ID = Identifier.fromNamespaceAndPath(StackSizeMod.MOD_ID, "aggregate_chest_scroll");
    public static final Identifier REQUEST_SCROLL_SET_ID = Identifier.fromNamespaceAndPath(StackSizeMod.MOD_ID, "aggregate_chest_scroll_set");
    public static final Identifier REQUEST_FILTER_ID = Identifier.fromNamespaceAndPath(StackSizeMod.MOD_ID, "aggregate_chest_filter");
    public static final Identifier REQUEST_SORT_ID = Identifier.fromNamespaceAndPath(StackSizeMod.MOD_ID, "aggregate_chest_sort");
    public static final Identifier REQUEST_INSERT_MATCHING_ID = Identifier.fromNamespaceAndPath(StackSizeMod.MOD_ID, "aggregate_chest_insert_matching");
    public static final CustomPacketPayload.Type<AggregateExtractPayload> REQUEST_EXTRACT_TYPE =
            new CustomPacketPayload.Type<>(REQUEST_EXTRACT_ID);
    public static final CustomPacketPayload.Type<AggregateScrollPayload> REQUEST_SCROLL_TYPE =
            new CustomPacketPayload.Type<>(REQUEST_SCROLL_ID);
    public static final CustomPacketPayload.Type<AggregateScrollSetPayload> REQUEST_SCROLL_SET_TYPE =
            new CustomPacketPayload.Type<>(REQUEST_SCROLL_SET_ID);
    public static final CustomPacketPayload.Type<AggregateFilterPayload> REQUEST_FILTER_TYPE =
            new CustomPacketPayload.Type<>(REQUEST_FILTER_ID);
    public static final CustomPacketPayload.Type<AggregateSortPayload> REQUEST_SORT_TYPE =
            new CustomPacketPayload.Type<>(REQUEST_SORT_ID);
    public static final CustomPacketPayload.Type<AggregateInsertMatchingPayload> REQUEST_INSERT_MATCHING_TYPE =
            new CustomPacketPayload.Type<>(REQUEST_INSERT_MATCHING_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, AggregateExtractPayload> REQUEST_EXTRACT_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeVarInt(payload.menuSlot());
                        buf.writeVarInt(payload.amount());
                    },
                    buf -> new AggregateExtractPayload(buf.readVarInt(), buf.readVarInt()));
    public static final StreamCodec<RegistryFriendlyByteBuf, AggregateScrollPayload> REQUEST_SCROLL_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeVarInt(payload.rows()),
                    buf -> new AggregateScrollPayload(buf.readVarInt()));
    public static final StreamCodec<RegistryFriendlyByteBuf, AggregateScrollSetPayload> REQUEST_SCROLL_SET_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeVarInt(payload.offset()),
                    buf -> new AggregateScrollSetPayload(buf.readVarInt()));
    public static final StreamCodec<RegistryFriendlyByteBuf, AggregateFilterPayload> REQUEST_FILTER_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeUtf(payload.query(), 128),
                    buf -> new AggregateFilterPayload(buf.readUtf(128)));
    public static final StreamCodec<RegistryFriendlyByteBuf, AggregateSortPayload> REQUEST_SORT_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeVarInt(payload.modeOrdinal()),
                    buf -> new AggregateSortPayload(buf.readVarInt()));
    public static final StreamCodec<RegistryFriendlyByteBuf, AggregateInsertMatchingPayload> REQUEST_INSERT_MATCHING_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeVarInt(payload.menuSlot()),
                    buf -> new AggregateInsertMatchingPayload(buf.readVarInt()));

    private AggregateChestNetworking() {}

    public static void initServer() {
        PayloadTypeRegistry.playC2S().register(REQUEST_EXTRACT_TYPE, REQUEST_EXTRACT_CODEC);
        PayloadTypeRegistry.playC2S().register(REQUEST_SCROLL_TYPE, REQUEST_SCROLL_CODEC);
        PayloadTypeRegistry.playC2S().register(REQUEST_SCROLL_SET_TYPE, REQUEST_SCROLL_SET_CODEC);
        PayloadTypeRegistry.playC2S().register(REQUEST_FILTER_TYPE, REQUEST_FILTER_CODEC);
        PayloadTypeRegistry.playC2S().register(REQUEST_SORT_TYPE, REQUEST_SORT_CODEC);
        PayloadTypeRegistry.playC2S().register(REQUEST_INSERT_MATCHING_TYPE, REQUEST_INSERT_MATCHING_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_EXTRACT_TYPE, (payload, context) -> {
            context.server().execute(() -> handleExtractRequest(context.player(), payload.menuSlot(), payload.amount()));
        });
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_SCROLL_TYPE, (payload, context) -> {
            context.server().execute(() -> handleScrollRequest(context.player(), payload.rows()));
        });
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_SCROLL_SET_TYPE, (payload, context) -> {
            context.server().execute(() -> handleScrollSetRequest(context.player(), payload.offset()));
        });
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_FILTER_TYPE, (payload, context) -> {
            context.server().execute(() -> handleFilterRequest(context.player(), payload.query()));
        });
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_SORT_TYPE, (payload, context) -> {
            context.server().execute(() -> handleSortRequest(context.player(), payload.modeOrdinal()));
        });
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_INSERT_MATCHING_TYPE, (payload, context) -> {
            context.server().execute(() -> handleInsertMatchingRequest(context.player(), payload.menuSlot()));
        });
    }

    private static void handleExtractRequest(Player player, int menuSlot, int amount) {
        if (amount <= 0 || menuSlot < 0 || player.containerMenu == null || menuSlot >= player.containerMenu.slots.size()) {
            return;
        }
        Slot slot = player.containerMenu.getSlot(menuSlot);
        if (!(slot.container instanceof AggregateInventory aggregateInventory)) {
            return;
        }
        ItemStack extracted = aggregateInventory.extractByRequest(slot.getContainerSlot(), amount);
        if (extracted.isEmpty()) {
            return;
        }
        ItemStack remaining = extracted.copy();
        boolean allInserted = player.getInventory().add(remaining);
        if (!allInserted && !remaining.isEmpty()) {
            player.drop(remaining, false);
        }
        player.containerMenu.broadcastChanges();
    }

    private static void handleScrollRequest(Player player, int rows) {
        if (rows == 0 || player.containerMenu == null || player.containerMenu.slots.isEmpty()) {
            return;
        }
        Slot first = player.containerMenu.getSlot(0);
        if (!(first.container instanceof AggregateInventory aggregateInventory)) {
            return;
        }
        aggregateInventory.scrollByRows(rows > 0 ? 1 : -1);
        player.containerMenu.broadcastChanges();
    }

    private static void handleScrollSetRequest(Player player, int offset) {
        if (player.containerMenu == null || player.containerMenu.slots.isEmpty()) {
            return;
        }
        Slot first = player.containerMenu.getSlot(0);
        if (!(first.container instanceof AggregateInventory aggregateInventory)) {
            return;
        }
        aggregateInventory.setViewOffset(offset);
        player.containerMenu.broadcastChanges();
    }

    private static void handleFilterRequest(Player player, String query) {
        if (player.containerMenu == null || player.containerMenu.slots.isEmpty()) {
            return;
        }
        Slot first = player.containerMenu.getSlot(0);
        if (!(first.container instanceof AggregateInventory aggregateInventory)) {
            return;
        }
        aggregateInventory.setFilterQuery(query);
        player.containerMenu.broadcastChanges();
    }

    private static void handleSortRequest(Player player, int modeOrdinal) {
        if (player.containerMenu == null || player.containerMenu.slots.isEmpty()) {
            return;
        }
        Slot first = player.containerMenu.getSlot(0);
        if (!(first.container instanceof AggregateInventory aggregateInventory)) {
            return;
        }
        AggregateInventory.SortMode[] modes = AggregateInventory.SortMode.values();
        if (modeOrdinal < 0 || modeOrdinal >= modes.length) {
            return;
        }
        aggregateInventory.setSortMode(modes[modeOrdinal]);
        player.containerMenu.broadcastChanges();
    }

    private static void handleInsertMatchingRequest(Player player, int menuSlot) {
        if (player.containerMenu == null) {
            return;
        }
        if (menuSlot < 0) {
            handleInsertAllMatchingRequest(player);
            return;
        }
        if (menuSlot >= player.containerMenu.slots.size()) {
            return;
        }
        Slot targetSlot = player.containerMenu.getSlot(menuSlot);
        if (!(targetSlot.container instanceof AggregateInventory aggregateInventory)) {
            return;
        }
        ItemStack targetTemplate = normalizeAggregateDisplayTemplate(targetSlot.getItem());
        if (targetTemplate.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, targetTemplate)) {
                continue;
            }
            int originalCount = stack.getCount();
            ItemStack remainder = aggregateInventory.owner().insertVirtual(stack.copy());
            int moved = originalCount - remainder.getCount();
            if (moved <= 0) {
                continue;
            }
            stack.shrink(moved);
            changed = true;
        }
        if (changed) {
            player.containerMenu.broadcastChanges();
        }
    }

    private static void handleInsertAllMatchingRequest(Player player) {
        if (player.containerMenu.slots.isEmpty()) {
            return;
        }
        Slot first = player.containerMenu.getSlot(0);
        if (!(first.container instanceof AggregateInventory aggregateInventory)) {
            return;
        }
        boolean changed = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !isRepresentedInAggregate(aggregateInventory, stack)) {
                continue;
            }
            int originalCount = stack.getCount();
            ItemStack remainder = aggregateInventory.owner().insertVirtual(stack.copy());
            int moved = originalCount - remainder.getCount();
            if (moved <= 0) {
                continue;
            }
            stack.shrink(moved);
            changed = true;
        }
        if (changed) {
            player.containerMenu.broadcastChanges();
        }
    }

    private static boolean isRepresentedInAggregate(AggregateInventory aggregateInventory, ItemStack candidate) {
        int entries = aggregateInventory.owner().summaryEntryCount();
        for (int i = 0; i < entries; i++) {
            ItemStack template = normalizeAggregateDisplayTemplate(aggregateInventory.owner().getVirtualStack(i));
            if (!template.isEmpty() && ItemStack.isSameItemSameComponents(candidate, template)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack normalizeAggregateDisplayTemplate(ItemStack displayed) {
        if (displayed.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack normalized = displayed.copyWithCount(1);
        CustomData customData = normalized.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.isEmpty()) {
            return normalized;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(AggregateChestBlockEntity.AGGREGATE_TOTAL_TAG)) {
            return normalized;
        }
        tag.remove(AggregateChestBlockEntity.AGGREGATE_TOTAL_TAG);
        if (tag.isEmpty()) {
            normalized.remove(DataComponents.CUSTOM_DATA);
        } else {
            normalized.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return normalized;
    }

    public record AggregateExtractPayload(int menuSlot, int amount) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return REQUEST_EXTRACT_TYPE;
        }
    }

    public record AggregateScrollPayload(int rows) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return REQUEST_SCROLL_TYPE;
        }
    }

    public record AggregateScrollSetPayload(int offset) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return REQUEST_SCROLL_SET_TYPE;
        }
    }

    public record AggregateFilterPayload(String query) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return REQUEST_FILTER_TYPE;
        }
    }

    public record AggregateSortPayload(int modeOrdinal) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return REQUEST_SORT_TYPE;
        }
    }

    public record AggregateInsertMatchingPayload(int menuSlot) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return REQUEST_INSERT_MATCHING_TYPE;
        }
    }
}
