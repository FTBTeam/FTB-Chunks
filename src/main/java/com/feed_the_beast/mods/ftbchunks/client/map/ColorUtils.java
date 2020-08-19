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
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeColors;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.registries.ObjectHolder;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class ColorUtils
{
	public static final Map<Block, Color4I> COLOR_MAP = new HashMap<>();
	public static Color4I[] reducedColorPalette = null;
	private static final HashMap<Color4I, Color4I> reducedColorMap = new HashMap<>();

	@ObjectHolder("biomesoplenty:bush")
	public static Block BOP_BUSH = null;

	@ObjectHolder("biomesoplenty:rainbow_birch_leaves")
	public static Block BOP_RAINBOW_BIRCH_LEAVES = null;

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

	private static Color4I getColorRaw(BlockState state, IBlockDisplayReader world, BlockPos pos)
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

		Color4I colorOverride = COLOR_MAP.get(state.getBlock());

		if (colorOverride != null)
		{
			return colorOverride;
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
			RegistryKey<Biome> biome = world.func_242406_i(pos).orElse(null);

			if (biome == Biomes.LUKEWARM_OCEAN || biome == Biomes.WARM_OCEAN || biome == Biomes.DEEP_LUKEWARM_OCEAN || biome == Biomes.DEEP_WARM_OCEAN)
			{
				state1 = Blocks.SAND.getDefaultState();
			}

			return getColorRaw(state1, world, pos).withTint(Color4I.rgb(BiomeColors.getWaterColor(world, pos)).withAlpha(220));
		}
		else if (state.getBlock() == BOP_RAINBOW_BIRCH_LEAVES)
		{
			return Color4I.hsb((((float) pos.getX() + MathHelper.sin(((float) pos.getZ() + (float) pos.getX()) / 35) * 35) % 150) / 150, 0.6F, 0.5F);
		}
		else if (state.getBlock() instanceof GrassBlock)
		{
			return Color4I.rgb(BiomeColors.getGrassColor(world, pos)).withTint(Color4I.BLACK.withAlpha(50));
		}
		else if (state.getBlock() == BOP_BUSH || state.getBlock() instanceof LeavesBlock || state.getBlock() instanceof VineBlock)
		{
			return Color4I.rgb(BiomeColors.getFoliageColor(world, pos)).withTint(Color4I.BLACK.withAlpha(50));
		}
		else if (state.getBlock() instanceof RedstoneWireBlock)
		{
			return redstoneColor(state.get(RedstoneWireBlock.POWER));
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