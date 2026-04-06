package dev.stacksizeconf.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.stacksizeconf.HandheldShulkerHandler;
import dev.stacksizeconf.StackSizeConfig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ShulkerBoxMenu;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void stacksizeconf$blockShulkerPickupWhileEditing(Player player, CallbackInfo ci) {
        // Replace mode: when custom magnet is enabled, disable vanilla pickup path globally.
        if (StackSizeConfig.ENABLE_ITEM_MAGNET.get()) {
            ci.cancel();
            return;
        }
        if (!(player.containerMenu instanceof ShulkerBoxMenu)) {
            return;
        }
        if (!HandheldShulkerHandler.playerHasOpenLockedShulker(player)) {
            return;
        }
        ItemEntity self = (ItemEntity) (Object) this;
        if (HandheldShulkerHandler.isShulkerBoxItem(self.getItem())) {
            ci.cancel();
        }
    }
}
