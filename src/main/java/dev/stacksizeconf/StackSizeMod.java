package dev.stacksizeconf;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class StackSizeMod implements ModInitializer {
    public static final String MOD_ID = "stacksizeconf";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        StackSizeConfigPersistence.load();
        UseItemCallback.EVENT.register(HandheldShulkerHandler::onUseItem);
        ServerTickEvents.END_WORLD_TICK.register(StackSizeMod::onEndWorldTick);
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> HandheldShulkerHandler.clearOpeningState(oldPlayer));
        LOGGER.info("Initialized Tools ({}) (Fabric).", MOD_ID);
    }

    private static void onEndWorldTick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            ItemMagnetHandler.tickPlayer(player);
        }
    }
}
