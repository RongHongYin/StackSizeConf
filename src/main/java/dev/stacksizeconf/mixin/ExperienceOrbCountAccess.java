package dev.stacksizeconf.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.ExperienceOrb;

@Mixin(ExperienceOrb.class)
public interface ExperienceOrbCountAccess {
    @Accessor("count")
    int stacksizeconf$getCount();
}
