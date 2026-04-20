package dev.stacksizeconf.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.stacksizeconf.ExperienceAbsorptionHelper;
import dev.stacksizeconf.StackSizeConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;

/**
 * Intercepts every experience orb entering the world (including paths that bypass
 * {@link ExperienceOrb#award} / {@code awardWithDirection}), so breeding, trading, fishing, etc. are covered.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void stacksizeconf$absorbExperienceOrb(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!StackSizeConfig.ENABLE_DIRECT_XP_ABSORPTION.get()) {
            return;
        }
        if (!(entity instanceof ExperienceOrb orb)) {
            return;
        }
        ServerLevel self = (ServerLevel) (Object) this;
        if (self.isClientSide()) {
            return;
        }
        int count = ((ExperienceOrbCountAccess) (Object) orb).stacksizeconf$getCount();
        int totalXp = ExperienceAbsorptionHelper.safeTotalOrbXp(orb.getValue(), count);
        if (totalXp <= 0) {
            return;
        }
        ServerPlayer player = ExperienceAbsorptionHelper.findNearestPlayer(
                self,
                orb.position(),
                StackSizeConfig.DIRECT_XP_ABSORPTION_RANGE.get()
        );
        if (player == null) {
            return;
        }
        ExperienceAbsorptionHelper.applyRepairWithXpThenGive(player, totalXp);
        ExperienceAbsorptionHelper.playXpPickupSoundOncePerTick(player, orb.position());
        entity.discard();
        cir.setReturnValue(true);
        cir.cancel();
    }
}
