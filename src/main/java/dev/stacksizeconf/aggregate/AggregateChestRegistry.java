package dev.stacksizeconf.aggregate;

import dev.stacksizeconf.StackSizeMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.Registry;

public final class AggregateChestRegistry {
    private static final Identifier AGGREGATE_CHEST_ID = id("aggregate_chest");

    public static final AggregateChestBlock AGGREGATE_CHEST_BLOCK = new AggregateChestBlock(
            BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, AGGREGATE_CHEST_ID))
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD));

    public static final BlockItem AGGREGATE_CHEST_ITEM = new BlockItem(
            AGGREGATE_CHEST_BLOCK,
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM, AGGREGATE_CHEST_ID)));

    public static final BlockEntityType<AggregateChestBlockEntity> AGGREGATE_CHEST_BLOCK_ENTITY =
            FabricBlockEntityTypeBuilder.create(AggregateChestBlockEntity::new, AGGREGATE_CHEST_BLOCK).build();

    private AggregateChestRegistry() {}

    public static void init() {
        Registry.register(BuiltInRegistries.BLOCK, AGGREGATE_CHEST_ID, AGGREGATE_CHEST_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, AGGREGATE_CHEST_ID, AGGREGATE_CHEST_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, AGGREGATE_CHEST_ID, AGGREGATE_CHEST_BLOCK_ENTITY);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS)
                .register(entries -> entries.accept(AGGREGATE_CHEST_ITEM));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(StackSizeMod.MOD_ID, path);
    }
}
