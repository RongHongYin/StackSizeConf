package dev.stacksizeconf.mixin;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;

import net.minecraft.util.ExtraCodecs;

/**
 * {@link ItemStack} (and a few other codecs) use {@code intRange(1, 99)} for stack-like counts. That lives in a
 * synthetic method, not {@code <clinit>}, so {@code @ModifyArg} on {@code ItemStack} fails. Widening every
 * {@code (1, 99)} range keeps save/load and network codecs consistent with larger stacks.
 */
@Mixin(ExtraCodecs.class)
public abstract class ExtraCodecsMixin {
    @WrapOperation(
            method = "intRange",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/ExtraCodecs;intRangeWithMessage(IILjava/util/function/Function;)Lcom/mojang/serialization/Codec;"
            )
    )
    private static Codec<Integer> stacksizeconf$widenLegacyStackCountRange(
            int min,
            int max,
            Function<Integer, String> errorMessage,
            Operation<Codec<Integer>> original
    ) {
        if (min == 1 && max == 99) {
            return original.call(min, Integer.MAX_VALUE, errorMessage);
        }
        return original.call(min, max, errorMessage);
    }
}
