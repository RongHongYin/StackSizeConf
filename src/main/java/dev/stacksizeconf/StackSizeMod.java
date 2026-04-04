package dev.stacksizeconf;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(StackSizeMod.MOD_ID)
public final class StackSizeMod {
    public static final String MOD_ID = "stacksizeconf";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StackSizeMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, StackSizeConfig.SPEC);
        LOGGER.info("Stack sizes: Main Menu -> Mods -> Stack Size Config -> Config, or config/{}-common.toml", MOD_ID);
    }
}
