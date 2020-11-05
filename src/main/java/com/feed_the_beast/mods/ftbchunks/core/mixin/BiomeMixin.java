package com.feed_the_beast.mods.ftbchunks.core.mixin;

import com.feed_the_beast.mods.ftbchunks.core.BiomeFTBC;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;

/**
 * @author LatvianModder
 */
@Mixin(Biome.class)
public class BiomeMixin implements BiomeFTBC
{
	private int ftbcBiomeColorIndex = -1;

	@Override
	public void setFTBCBiomeColorIndex(int c)
	{
		ftbcBiomeColorIndex = c;
	}

	@Override
	public int getFTBCBiomeColorIndex()
	{
		return ftbcBiomeColorIndex;
	}
}
