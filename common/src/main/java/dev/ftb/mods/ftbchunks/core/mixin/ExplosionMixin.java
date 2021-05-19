package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.core.ExplosionFTBC;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author LatvianModder
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin implements ExplosionFTBC {
	@Override
	@Nullable
	@Accessor("source")
	public abstract Entity getSourceFTBC();
}
