package com.feed_the_beast.mods.ftbchunks.impl.map;

import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.CubeCoordinateIterator;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.awt.*;

/**
 * @author LatvianModder
 */
public enum ColorBlend
{
	WATER((w, p) -> w.getBiome(p).getWaterColor()),
	GRASS((w, p) -> w.getBiome(p).getGrassColor(p.getX() + 0.5D, p.getZ() + 0.5D)),
	FOLIAGE((w, p) -> w.getBiome(p).getFoliageColor());

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

	public static int addBrightness(Color4I c, float f)
	{
		float[] hsb = new float[3];
		Color.RGBtoHSB(c.redi(), c.greeni(), c.bluei(), hsb);
		float b = hsb[2] + f;
		float s = hsb[1];

		if (b > 1F)
		{
			s += b - 1F;
		}

		b = MathHelper.clamp(b, 0F, 1F);
		s = MathHelper.clamp(s, 0F, 1F);

		return Color.HSBtoRGB(hsb[0], s, b) | 0xFF000000;
	}
}