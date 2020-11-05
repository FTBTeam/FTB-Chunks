package com.feed_the_beast.mods.ftbchunks.client.map.color;

import com.feed_the_beast.mods.ftbchunks.core.BlockFTBC;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockDisplayReader;

import java.io.InputStream;
import java.util.HashMap;

/**
 * @author LatvianModder
 */
public class ColorUtils
{
	public static Color4I[] reducedColorPalette = null;
	public static Color4I[] topographyPalette = null;
	public static Color4I[][] lightMapPalette = null;
	private static final HashMap<Color4I, Color4I> reducedColorMap = new HashMap<>();

	public static int convertToNative(int c)
	{
		return NativeImage.getCombined((c >> 24) & 0xFF, (c >> 0) & 0xFF, (c >> 8) & 0xFF, (c >> 16) & 0xFF);
	}

	public static int convertFromNative(int c)
	{
		return (NativeImage.getAlpha(c) << 24) | (NativeImage.getRed(c) << 16) | (NativeImage.getGreen(c) << 8) | NativeImage.getBlue(c);
	}

	public static Color4I addBrightness(Color4I c, float f)
	{
		float r = c.redf() + f;
		float g = c.greenf() + f;
		float b = c.bluef() + f;

		int ri = MathHelper.clamp((int) (r * 255F), 0, 255);
		int gi = MathHelper.clamp((int) (g * 255F), 0, 255);
		int bi = MathHelper.clamp((int) (b * 255F), 0, 255);

		return Color4I.rgb(ri, gi, bi);
	}

	public static Color4I reduce(Color4I c)
	{
		if (reducedColorPalette == null)
		{
			reducedColorPalette = new Color4I[0];

			try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation("ftbchunks:textures/reduced_color_palette.png")).getInputStream())
			{
				NativeImage image = NativeImage.read(stream);
				int w = image.getWidth();
				int h = image.getHeight();

				reducedColorPalette = new Color4I[w * h];

				for (int x = 0; x < w; x++)
				{
					for (int y = 0; y < h; y++)
					{
						int col = image.getPixelRGBA(x, y);
						reducedColorPalette[x + y * w] = Color4I.rgb((NativeImage.getRed(col) << 16) | (NativeImage.getGreen(col) << 8) | NativeImage.getBlue(col));
					}
				}

				image.close();
			}
			catch (Exception ex)
			{
			}
		}

		if (reducedColorPalette.length == 0)
		{
			return c;
		}

		return reducedColorMap.computeIfAbsent(c, col -> {
			int r = col.redi();
			int g = col.greeni();
			int b = col.bluei();
			long prevDist = Long.MAX_VALUE;
			Color4I colr = Color4I.BLACK;

			for (Color4I rcol : reducedColorPalette)
			{
				long dr = r - rcol.redi();
				long dg = g - rcol.greeni();
				long db = b - rcol.bluei();
				long d = dr * dr + dg * dg + db * db;

				if (d < prevDist)
				{
					prevDist = d;
					colr = rcol;
				}
			}

			return colr;
		});
	}

	public static Color4I[] getTopographyPalette()
	{
		if (topographyPalette == null)
		{
			topographyPalette = new Color4I[0];

			try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation("ftbchunks:textures/topography_palette.png")).getInputStream())
			{
				NativeImage image = NativeImage.read(stream);
				int w = image.getWidth();
				int h = image.getHeight();

				topographyPalette = new Color4I[w * h];

				for (int x = 0; x < w; x++)
				{
					for (int y = 0; y < h; y++)
					{
						int col = image.getPixelRGBA(x, y);
						topographyPalette[x + y * w] = Color4I.rgb(convertFromNative(col)).withAlpha(255);
					}
				}

				image.close();
			}
			catch (Exception ex)
			{
			}
		}

		return topographyPalette;
	}

	public static Color4I[][] getLightMapPalette()
	{
		if (lightMapPalette == null)
		{
			lightMapPalette = new Color4I[0][0];

			try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation("ftbchunks:textures/lightmap_palette.png")).getInputStream())
			{
				NativeImage image = NativeImage.read(stream);
				int w = image.getWidth();
				int h = image.getHeight();

				lightMapPalette = new Color4I[w][h];

				for (int x = 0; x < w; x++)
				{
					for (int y = 0; y < h; y++)
					{
						int col = image.getPixelRGBA(x, y);
						lightMapPalette[x][y] = Color4I.rgb(convertFromNative(col)).withAlpha(255);
					}
				}

				image.close();
			}
			catch (Exception ex)
			{
			}
		}

		return lightMapPalette;
	}

	public static Color4I getColor(Block block, IBlockDisplayReader world, BlockPos pos)
	{
		BlockColor blockColor = block instanceof BlockFTBC ? ((BlockFTBC) block).getFTBCBlockColor() : null;
		return (blockColor == null ? BlockColors.IGNORED : blockColor).getBlockColor(world, pos);
	}
}