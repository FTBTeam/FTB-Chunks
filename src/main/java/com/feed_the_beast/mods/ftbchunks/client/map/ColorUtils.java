package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeColors;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.chunk.IChunk;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class ColorUtils
{
	public static final Map<Block, Color4I> COLOR_MAP = new HashMap<>();
	public static int[] reducedColorPalette = null;

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

	public static int reduce(int c)
	{
		if (reducedColorPalette == null)
		{
			reducedColorPalette = new int[0];

			try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation("ftbchunks:textures/reduced_color_palette.png")).getInputStream())
			{
				NativeImage image = NativeImage.read(stream);
				int w = image.getWidth();
				int h = image.getHeight();

				reducedColorPalette = new int[w * h];

				for (int x = 0; x < w; x++)
				{
					for (int y = 0; y < h; y++)
					{
						int col = image.getPixelRGBA(x, y);
						reducedColorPalette[x + y * w] = (NativeImage.getRed(col) << 16) | (NativeImage.getGreen(col) << 8) | NativeImage.getBlue(col);
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

		int r = (c >> 16) & 0xFF;
		int g = (c >> 8) & 0xFF;
		int b = (c >> 0) & 0xFF;
		long prevDist = Long.MAX_VALUE;
		int colr = 0;

		for (int col : reducedColorPalette)
		{
			long dr = r - ((col >> 16) & 0xFF);
			long dg = g - ((col >> 8) & 0xFF);
			long db = b - ((col >> 0) & 0xFF);
			long d = dr * dr + dg * dg + db * db;

			if (d < prevDist)
			{
				prevDist = d;
				colr = col;
			}
		}

		return 0xFF000000 | colr;
	}

	public static Color4I getColorRaw(BlockState state, IBlockDisplayReader world, BlockPos pos)
	{
		Color4I color = COLOR_MAP.get(state.getBlock());

		if (color == null)
		{
			MaterialColor materialColor = state.getMaterialColor(world, pos);
			return materialColor == null ? Color4I.RED : Color4I.rgb(materialColor.colorValue);
		}

		return color;
	}

	public static Color4I getColor(BlockState state, IWorld world, IChunk chunk, BlockPos.Mutable pos)
	{
		if (state.getBlock() == Blocks.AIR)
		{
			return Color4I.BLACK;
		}

		int by = pos.getY();

		if ((state.getBlock() instanceof FlowingFluidBlock && ((FlowingFluidBlock) state.getBlock()).getFluid().isEquivalentTo(Fluids.WATER)) || FTBChunksAPIImpl.MAP_IGNORE_IN_WATER_TAG.contains(state.getBlock()))
		{
			for (int depth = 1; depth < 18; depth++)
			{
				pos.setY(by - depth);
				BlockState state1 = chunk.getBlockState(pos);

				if (state1.getBlock() instanceof FlowingFluidBlock || FTBChunksAPIImpl.MAP_IGNORE_IN_WATER_TAG.contains(state1.getBlock()))
				{
					continue;
				}

				return getColorRaw(state1, world, pos).withTint(Color4I.rgb(BiomeColors.getWaterColor(world, pos)).withAlpha(220));
			}

			BlockState state1 = Blocks.GRAVEL.getDefaultState();
			Biome biome = world.getBiome(pos);

			if (biome == Biomes.LUKEWARM_OCEAN || biome == Biomes.WARM_OCEAN || biome == Biomes.DEEP_LUKEWARM_OCEAN || biome == Biomes.DEEP_WARM_OCEAN)
			{
				state1 = Blocks.SAND.getDefaultState();
			}

			return getColorRaw(state1, world, pos).withTint(Color4I.rgb(BiomeColors.getWaterColor(world, pos)).withAlpha(220));
		}
		else
		{
			if (state.getBlock() instanceof GrassBlock)
			{
				return Color4I.rgb(BiomeColors.getGrassColor(world, pos)).withTint(Color4I.BLACK.withAlpha(50));
			}
			else if (state.getBlock() instanceof LeavesBlock || state.getBlock() instanceof VineBlock)
			{
				return Color4I.rgb(BiomeColors.getFoliageColor(world, pos)).withTint(Color4I.BLACK.withAlpha(50));
			}
			else if (state.getBlock() instanceof RedstoneWireBlock)
			{
				return redstoneColor(state.get(RedstoneWireBlock.POWER));
			}
		}

		return getColorRaw(state, world, pos);
	}

	private static Color4I redstoneColor(int power)
	{
		float f = power / 15F;
		float r0 = f * 0.6F + 0.4F;
		if (power == 0)
		{
			r0 = 0.3F;
		}

		float g0 = f * f * 0.7F - 0.5F;
		float b0 = f * f * 0.6F - 0.7F;

		if (g0 < 0F)
		{
			g0 = 0F;
		}

		if (b0 < 0F)
		{
			b0 = 0F;
		}

		int r = MathHelper.clamp((int) (r0 * 255F), 0, 255);
		int g = MathHelper.clamp((int) (g0 * 255F), 0, 255);
		int b = MathHelper.clamp((int) (b0 * 255F), 0, 255);
		return Color4I.rgb(r, g, b);
	}
}