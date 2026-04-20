package dev.stacksizeconf;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

/**
 * Mirrors {@code ExperienceOrb.repairPlayerItems} + {@code Player.giveExperiencePoints} for the remainder,
 * matching vanilla orb pickup order (mending first, then XP bar).
 */
public final class ExperienceAbsorptionHelper {
    private static final Map<UUID, Long> LAST_XP_PICKUP_SOUND_TICK = new HashMap<>();

    private ExperienceAbsorptionHelper() {
    }

    public static @Nullable ServerPlayer findNearestPlayer(ServerLevel level, Vec3 pos, double rangeBlocks) {
        if (rangeBlocks <= 0.0D) {
            return null;
        }
        double r2 = rangeBlocks * rangeBlocks;
        ServerPlayer nearest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator()) {
                continue;
            }
            double d2 = p.distanceToSqr(pos);
            if (d2 <= r2 && d2 < best) {
                best = d2;
                nearest = p;
            }
        }
        return nearest;
    }

    /**
     * Same algorithm as {@code ExperienceOrb.repairPlayerItems(ServerPlayer, int)} (private in vanilla).
     */
    public static int repairWithXpLikeOrb(ServerPlayer player, int value) {
        Optional<EnchantedItemInUse> optional = EnchantmentHelper.getRandomItemWith(
                EnchantmentEffectComponents.REPAIR_WITH_XP,
                player,
                ItemStack::isDamaged
        );
        if (optional.isPresent()) {
            ItemStack itemstack = optional.get().itemStack();
            int i = EnchantmentHelper.modifyDurabilityToRepairFromXp(
                    player.level(),
                    itemstack,
                    (int) (value * itemstack.getXpRepairRatio())
            );
            int j = Math.min(i, itemstack.getDamageValue());
            itemstack.setDamageValue(itemstack.getDamageValue() - j);
            if (j > 0) {
                int k = value - j * value / i;
                if (k > 0) {
                    return repairWithXpLikeOrb(player, k);
                }
            }
            return 0;
        }
        return value;
    }

    public static void applyRepairWithXpThenGive(ServerPlayer player, int totalXp) {
        if (totalXp <= 0) {
            return;
        }
        int remainder = repairWithXpLikeOrb(player, totalXp);
        if (remainder > 0) {
            player.giveExperiencePoints(remainder);
        }
    }

    /**
     * Matches {@code ClientPacketListener#handleTakeItemEntity} XP orb pickup sound profile.
     * Throttled to once per player per game tick so burst absorption plays one crisp sound.
     */
    public static void playXpPickupSoundOncePerTick(ServerPlayer player, Vec3 pos) {
        ServerLevel level = (ServerLevel) player.level();
        long nowTick = level.getGameTime();
        UUID playerId = player.getUUID();
        Long lastTick = LAST_XP_PICKUP_SOUND_TICK.get(playerId);
        if (lastTick != null && lastTick == nowTick) {
            return;
        }
        LAST_XP_PICKUP_SOUND_TICK.put(playerId, nowTick);
        float pitch = (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.35F + 0.9F;
        level.playSound(
                null,
                pos.x(),
                pos.y(),
                pos.z(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.1F,
                pitch
        );
    }

    public static int safeTotalOrbXp(int value, int count) {
        if (value <= 0 || count <= 0) {
            return 0;
        }
        long total = (long) value * (long) count;
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }
}
