package com.feed_the_beast.mods.ftbchunks.impl.map;

import com.feed_the_beast.mods.ftbchunks.FoliageColorLoader;
import com.feed_the_beast.mods.ftbchunks.GrassColorLoader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.CubeCoordinateIterator;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.DarkForestBiome;
import net.minecraft.world.biome.DarkForestHillsBiome;
import net.minecraft.world.biome.SwampBiome;
import net.minecraft.world.biome.SwampHillsBiome;

/**
 * @author LatvianModder
 */
public enum ColorBlend
{
	WATER((w, p) -> w.getBiome(p).getWaterColor()),
	GRASS((w, p) -> {
		if (GrassColorLoader.map.length != 65536)
		{
			return 0xFF7BB262;
		}

		Biome biome = w.getBiome(p);

		if (biome instanceof SwampBiome || biome instanceof SwampHillsBiome)
		{
			double d0 = Biome.INFO_NOISE.noiseAt((p.getX() + 0.5D) * 0.0225D, (p.getZ() + 0.5D) * 0.0225D, false);
			return d0 < -0.1D ? 5011004 : 6975545;
		}

		float temperature = MathHelper.clamp(biome.getDefaultTemperature(), 0F, 1F);
		float humidity = MathHelper.clamp(biome.getDownfall(), 0F, 1F) * temperature;
		int i = (int) ((1F - temperature) * 255F);
		int j = (int) ((1F - humidity) * 255F);
		int col = GrassColorLoader.map[j << 8 | i];

		if (biome instanceof DarkForestBiome || biome instanceof DarkForestHillsBiome)
		{
			return (col & 16711422) + 2634762 >> 1;
		}

		return col;
	}),
	FOLIAGE((w, p) -> {
		if (FoliageColorLoader.map.length != 65536)
		{
			return 0xFF559934;
		}

		Biome biome = w.getBiome(p);

		if (biome instanceof SwampBiome || biome instanceof SwampHillsBiome)
		{
			return 6975545;
		}

		float temperature = MathHelper.clamp(biome.getDefaultTemperature(), 0F, 1F);
		float humidity = MathHelper.clamp(biome.getDownfall(), 0F, 1F) * temperature;
		int i = (int) ((1F - temperature) * 255F);
		int j = (int) ((1F - humidity) * 255F);
		return FoliageColorLoader.map[j << 8 | i];
	});

	public final MapColorGetter colorGetter;

	ColorBlend(MapColorGetter c)
	{
		colorGetter = c;
	}

	public int blend(World world, BlockPos p, int radius)
	{
		return blend(world, p, radius, colorGetter);
	}

	public static int blend(World world, BlockPos p, int radius, MapColorGetter colorGetter)
	{
		int j = (radius * 2 + 1) * (radius * 2 + 1);
		int k = 0;
		int l = 0;
		int i1 = 0;
		CubeCoordinateIterator cci = new CubeCoordinateIterator(p.getX() - radius, p.getY(), p.getZ() - radius, p.getX() + radius, p.getY(), p.getZ() + radius);

		int j1;
		for (BlockPos.Mutable pm = new BlockPos.Mutable(); cci.hasNext(); i1 += j1 & 255)
		{
			pm.setPos(cci.getX(), cci.getY(), cci.getZ());
			j1 = colorGetter.getColor(world, pm);
			k += (j1 & 0xFF0000) >> 16;
			l += (j1 & 0x00FF00) >> 8;
		}

		return (k / j & 255) << 16 | (l / j & 255) << 8 | i1 / j & 255;
	}

	public static int addBrightness(int c, float f)
	{
		float r = ((c >> 16) & 0xFF) / 255F + f;
		float g = ((c >> 8) & 0xFF) / 255F + f;
		float b = ((c >> 0) & 0xFF) / 255F + f;

		int ri = MathHelper.clamp((int) (r * 255F), 0, 255);
		int gi = MathHelper.clamp((int) (g * 255F), 0, 255);
		int bi = MathHelper.clamp((int) (b * 255F), 0, 255);

		return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
	}
}