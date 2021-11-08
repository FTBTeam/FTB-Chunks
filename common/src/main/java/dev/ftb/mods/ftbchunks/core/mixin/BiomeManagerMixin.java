package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.core.BiomeManagerFTBC;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author LatvianModder
 */
@Mixin(BiomeManager.class)
public abstract class BiomeManagerMixin implements BiomeManagerFTBC {
	@Override
	@Accessor("biomeZoomSeed")
	public abstract long getBiomeZoomSeedFTBC();
}
