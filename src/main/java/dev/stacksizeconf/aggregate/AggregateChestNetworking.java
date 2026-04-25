package dev.stacksizeconf.aggregate;

import dev.stacksizeconf.StackSizeMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class AggregateChestNetworking {
    public static final Identifier REQUEST_EXTRACT_ID = Identifier.fromNamespaceAndPath(StackSizeMod.MOD_ID, "aggregate_chest_extract");
    public static final Identifier REQUEST_SCROLL_ID = Identifier.fromNamespaceAndPath(StackSizeMod.MOD_ID, "aggregate_chest_scroll");
    public static final Identifier REQUEST_SCROLL_SET_ID = Identifier.fromNamespaceAndPath(StackSizeMod.MOD_ID, "aggregate_chest_scroll_set");
    public static final Identifier REQUEST_FILTER_ID = Identifier.fromNamespaceAndPath(StackSizeMod.MOD_ID, "aggregate_chest_filter");
    public static final Identifier REQUEST_SORT_ID = Identifier.fromNamespaceAndPath(StackSizeMod.MOD_ID, "aggregate_chest_sort");
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

    private AggregateChestNetworking() {}

    public static void initServer() {
        PayloadTypeRegistry.playC2S().register(REQUEST_EXTRACT_TYPE, REQUEST_EXTRACT_CODEC);
        PayloadTypeRegistry.playC2S().register(REQUEST_SCROLL_TYPE, REQUEST_SCROLL_CODEC);
        PayloadTypeRegistry.playC2S().register(REQUEST_SCROLL_SET_TYPE, REQUEST_SCROLL_SET_CODEC);
        PayloadTypeRegistry.playC2S().register(REQUEST_FILTER_TYPE, REQUEST_FILTER_CODEC);
        PayloadTypeRegistry.playC2S().register(REQUEST_SORT_TYPE, REQUEST_SORT_CODEC);
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
}
