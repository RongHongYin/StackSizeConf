package dev.stacksizeconf;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.platform.InputConstants;

import dev.stacksizeconf.client.ToolboxConfigScreen;

@Environment(EnvType.CLIENT)
public final class StackSizeModClient implements ClientModInitializer {
    private static KeyMapping openConfigKey;

    @Override
    public void onInitializeClient() {
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.stacksizeconf.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(ToolboxConfigScreen.create(mc.screen));
            }
        });
    }
}
