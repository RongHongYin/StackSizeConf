package dev.stacksizeconf.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.stacksizeconf.StackSizeConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Scales down stack count text when it is wider than a slot so large counts (e.g. 1024) do not overlap neighbors.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    /** ~width of bottom-right count area inside a 16px slot */
    private static final int MAX_COUNT_TEXT_WIDTH = 15;
    private static final float MIN_SCALE = 0.38f;
    @Inject(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stacksizeconf$scaledItemCount(Font font, ItemStack stack, int x, int y, @Nullable String text, CallbackInfo ci) {
        if (!StackSizeConfig.stackOverridesEnabled()) {
            return;
        }
        if (stack.getCount() == 1 && text == null) {
            return;
        }
        String s = text == null ? String.valueOf(stack.getCount()) : text;
        int w = font.width(s);
        if (w <= MAX_COUNT_TEXT_WIDTH) {
            return;
        }
        float scale = Math.max(MIN_SCALE, Math.min(1.0f, MAX_COUNT_TEXT_WIDTH / (float) w));
        if (scale >= 0.999f) {
            return;
        }
        ci.cancel();
        GuiGraphics self = (GuiGraphics) (Object) this;
        // Anchor bottom-right like vanilla so scaling shrinks into the corner, not left into the item icon.
        float brX = x + 19 - 2f;
        float brY = y + 6 + 3f + font.lineHeight;
        // 1.21.11+ GuiGraphics uses a 2D pose stack (no Z); anchor and scale like vanilla bottom-right.
        self.pose().pushMatrix();
        self.pose().translate(brX, brY);
        self.pose().scale(scale, scale);
        self.drawString(font, s, -w, -font.lineHeight, 0xFFFFFFFF, true);
        self.pose().popMatrix();
    }
}
