package dev.stacksizeconf.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

@Mixin(HopperBlockEntity.class)
public interface HopperBlockEntityAccess {
    @Invoker("tryMoveInItem")
    static ItemStack stacksizeconf$invokeTryMoveInItem(
            @Nullable Container source,
            Container destination,
            ItemStack movingStack,
            int slot,
            @Nullable Direction direction
    ) {
        throw new AssertionError("mixin invoker");
    }

    @Invoker("pushItemsTick")
    static void stacksizeconf$invokePushItemsTick(
            Level level,
            BlockPos pos,
            BlockState state,
            HopperBlockEntity hopper
    ) {
        throw new AssertionError("mixin invoker");
    }
}
