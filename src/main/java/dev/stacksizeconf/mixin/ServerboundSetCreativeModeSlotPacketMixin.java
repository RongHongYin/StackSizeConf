package dev.stacksizeconf.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.item.ItemStack;

/**
 * {@link ServerboundSetCreativeModeSlotPacket} wraps items with {@link ItemStack#validatedStreamCodec},
 * which re-encodes through {@link ItemStack#CODEC} where {@code count} is limited to 1–99. Stacks above
 * 99 then fail decode and disconnect the client. Creative mode only; use the raw stream codec like vanilla
 * does for the on-wire payload, without that extra validation step.
 */
@Mixin(ServerboundSetCreativeModeSlotPacket.class)
public abstract class ServerboundSetCreativeModeSlotPacketMixin {
    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;validatedStreamCodec(Lnet/minecraft/network/codec/StreamCodec;)Lnet/minecraft/network/codec/StreamCodec;"
            )
    )
    private static StreamCodec<RegistryFriendlyByteBuf, ItemStack> stacksizeconf$creativeSlotNo99Cap(StreamCodec<RegistryFriendlyByteBuf, ItemStack> inner) {
        return inner;
    }
}
