package dev.stacksizeconf.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Optional compatibility mixins (e.g. Lithium) are only applied when the target classes exist at runtime.
 */
public final class StackSizeMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (matchesLithiumHopperHelperMixin(mixinClassName)) {
            /*
             * Do not use Class.forName on Lithium classes: Fabric/Knot loads mods on a different loader than this
             * plugin's class loader, so HopperHelper can be missing even when Lithium is installed — the mixin would
             * stay disabled and the bug would persist.
             */
            return FabricLoader.getInstance().isModLoaded("lithium");
        }
        return true;
    }

    private static boolean matchesLithiumHopperHelperMixin(String mixinClassName) {
        if (mixinClassName == null) {
            return false;
        }
        String normalized = mixinClassName.replace('/', '.');
        return normalized.contains("compat.lithium.HopperHelperMixin");
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
