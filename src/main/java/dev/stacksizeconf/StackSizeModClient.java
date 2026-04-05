package dev.stacksizeconf;

import com.mojang.datafixers.util.Function4;

import dev.stacksizeconf.client.LightLevelOverlay;
import dev.stacksizeconf.client.ToolboxConfigurationSectionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = StackSizeMod.MOD_ID, dist = Dist.CLIENT)
public final class StackSizeModClient {
    public StackSizeModClient(IEventBus modEventBus, ModContainer container) {
        LightLevelOverlay.register(modEventBus);
        Function4<ConfigurationScreen, ModConfig.Type, ModConfig, Component, Screen> openToolboxSection =
                (root, type, modConfig, title) -> new ToolboxConfigurationSectionScreen(root, type, modConfig, title);
        container.registerExtensionPoint(IConfigScreenFactory.class, (c, parent) -> new ConfigurationScreen(c, parent, openToolboxSection));
    }
}
