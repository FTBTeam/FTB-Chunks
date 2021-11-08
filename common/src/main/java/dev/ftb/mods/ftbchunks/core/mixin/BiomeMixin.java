package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.core.BiomeFTBC;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;

/**
 * @author LatvianModder
 */
@Mixin(Biome.class)
public abstract class BiomeMixin implements BiomeFTBC {
	private int ftbcBiomeColorIndex = -1;

	@Override
	public void setFTBCBiomeColorIndex(int c) {
		ftbcBiomeColorIndex = c;
	}

	@Override
	public int getFTBCBiomeColorIndex() {
		return ftbcBiomeColorIndex;
	}
}
