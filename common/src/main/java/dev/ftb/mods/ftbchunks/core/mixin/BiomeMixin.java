package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.core.BiomeFTBC;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Biome.class)
public abstract class BiomeMixin implements BiomeFTBC {
	@Unique
	private int ftbc$biomeColorIndex = -1;

	@Override
	public void ftbc$setBiomeColorIndex(int c) {
		ftbc$biomeColorIndex = c;
	}

	@Override
	public int ftbc$getBiomeColorIndex() {
		return ftbc$biomeColorIndex;
	}
}
