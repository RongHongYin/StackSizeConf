package dev.stacksizeconf.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(method = "renderItemCount", at = @At("HEAD"), cancellable = true)
    private void stacksizeconf$scaledItemCount(Font font, ItemStack stack, int x, int y, @Nullable String text, CallbackInfo ci) {
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
        int left = x + 19 - 2 - w;
        int top = y + 6 + 3;
        self.pose().pushMatrix();
        self.pose().translate(left, top);
        self.pose().scale(scale, scale);
        self.drawString(font, s, 0, 0, 0xFFFFFFFF, true);
        self.pose().popMatrix();
    }
}
