package dev.ftb.mods.ftbchunks.core.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PortalForcer.class)
public interface PortalForcerAccessor {
    @Accessor
    ServerLevel getLevel();
}
