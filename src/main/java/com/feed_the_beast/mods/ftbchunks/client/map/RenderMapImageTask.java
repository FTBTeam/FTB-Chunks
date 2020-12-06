package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.client.FTBChunksClient;
import com.feed_the_beast.mods.ftbchunks.client.FTBChunksClientConfig;
import com.feed_the_beast.mods.ftbchunks.client.map.color.BlockColor;
import com.feed_the_beast.mods.ftbchunks.client.map.color.BlockColors;
import com.feed_the_beast.mods.ftbchunks.client.map.color.ColorUtils;
import com.feed_the_beast.mods.ftbchunks.client.map.color.CustomBlockColor;
import com.feed_the_beast.mods.ftbchunks.impl.XZ;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.icon.Icon;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.awt.*;
import java.util.Random;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class RenderMapImageTask implements MapTask
{
	public final MapRegion region;

	public RenderMapImageTask(MapRegion r)
	{
		region = r;
	}

	private static int getHeight(int data)
	{
		int h = (data >> 16) & 255;

		if (((data >> 15) & 1) != 0 && FTBChunksClientConfig.mapMode != MapMode.TOPOGRAPHY)
		{
			if (FTBChunksClientConfig.waterHeightFactor == 0)
			{
				return 62;
			}

			return (h / FTBChunksClientConfig.waterHeightFactor) * FTBChunksClientConfig.waterHeightFactor + FTBChunksClientConfig.waterHeightFactor - 1;
		}

		return h;
	}

	@Override
	public void runMapTask()
	{
		MapRegion.Images images = region.getImages();

		int[][] blockImgMap = new int[512][512];
		int[][] dataImgMap = new int[1024][1024];

		for (int z = 0; z < 512; z++)
		{
			for (int x = 0; x < 512; x++)
			{
				blockImgMap[x][z] = images.blocks.getPixelRGBA(x, z);
			}
		}

		for (int z = 0; z < 1024; z++)
		{
			for (int x = 0; x < 1024; x++)
			{
				dataImgMap[x][z] = images.data.getPixelRGBA(x, z);
			}
		}

		int[] dataImgMapW = new int[512];
		int[] dataImgMapN = new int[512];

		MapRegion rEast = region.dimension.getRegions().get(region.pos.offset(-1, 0));

		if (rEast != null)
		{
			NativeImage img = rEast.getImages().data;

			for (int i = 0; i < 512; i++)
			{
				dataImgMapW[i] = img.getPixelRGBA(511, i);
			}
		}
		else
		{
			for (int i = 0; i < 512; i++)
			{
				dataImgMapW[i] = dataImgMap[0][i];
			}
		}

		MapRegion rTop = region.dimension.getRegions().get(region.pos.offset(0, -1));

		if (rTop != null)
		{
			NativeImage img = rTop.getImages().data;

			for (int i = 0; i < 512; i++)
			{
				dataImgMapN[i] = img.getPixelRGBA(i, 511);
			}
		}
		else
		{
			for (int i = 0; i < 512; i++)
			{
				dataImgMapN[i] = dataImgMap[i][0];
			}
		}

		float[] hsb = new float[3];
		UUID ownId = Minecraft.getInstance().player.getUniqueID();
		World world = Minecraft.getInstance().world;
		BlockPos.Mutable blockPos = new BlockPos.Mutable();
		Int2ObjectOpenHashMap<Biome> biomeMap = new Int2ObjectOpenHashMap<>();

		for (int cz = 0; cz < 32; cz++)
		{
			for (int cx = 0; cx < 32; cx++)
			{
				MapChunk c = region.chunks.get(XZ.of(cx, cz));
				Random random = new Random(region.pos.asLong() ^ (c == null ? 0L : c.pos.asLong()));
				Color4I claimColor, fullClaimColor;
				boolean claimBarUp, claimBarDown, claimBarLeft, claimBarRight;

				if (c != null && c.claimedDate != null && (FTBChunksClient.alwaysRenderChunksOnMap || (ownId.equals(c.ownerId) ? FTBChunksClientConfig.ownClaimedChunksOnMap : FTBChunksClientConfig.claimedChunksOnMap)))
				{
					claimColor = Color4I.rgb(c.color).withAlpha(100);
					fullClaimColor = claimColor.withAlpha(255);
					claimBarUp = !c.connects(c.offset(0, -1));
					claimBarDown = !c.connects(c.offset(0, 1));
					claimBarLeft = !c.connects(c.offset(-1, 0));
					claimBarRight = !c.connects(c.offset(1, 0));
				}
				else
				{
					claimColor = Icon.EMPTY;
					fullClaimColor = Icon.EMPTY;
					claimBarUp = false;
					claimBarDown = false;
					claimBarLeft = false;
					claimBarRight = false;
				}

				for (int z = 0; z < 16; z++)
				{
					for (int x = 0; x < 16; x++)
					{
						int ax = cx * 16 + x;
						int az = cz * 16 + z;

						if (c == null)
						{
							region.renderedMapImage.setPixelRGBA(ax, az, 0);
						}
						else
						{
							BlockColor blockColor = region.dimension.manager.getBlockColor(blockImgMap[ax][az]);
							Color4I col;
							int data = dataImgMap[ax][az];
							int by = getHeight(data);
							boolean water = ((data >> 15) & 1) != 0;
							blockPos.setPos(region.pos.x * 512 + ax, by, region.pos.z * 512 + az);

							if (FTBChunksClientConfig.mapMode == MapMode.TOPOGRAPHY)
							{
								col = ColorUtils.getTopographyPalette()[by + (water ? 256 : 0)];
							}
							else if (FTBChunksClientConfig.mapMode == MapMode.BLOCKS)
							{
								col = Color4I.rgb(ColorUtils.convertFromNative(blockImgMap[ax][az]));
							}
							else if (FTBChunksClientConfig.mapMode == MapMode.BIOME_TEMPERATURE)
							{
								Biome biome = biomeMap.computeIfAbsent(data & 0b111_11111111, i -> region.dimension.manager.getBiome(world, i));
								float temp0 = biome.getTemperature(blockPos);

								float temp = (temp0 + 0.5F) / 2F;
								col = Color4I.hsb((float) (Math.PI - temp * Math.PI), 0.9F, 1F);
							}
							else if (FTBChunksClientConfig.mapMode == MapMode.LIGHT_SOURCES)
							{
								col = ColorUtils.getLightMapPalette()[15][(data >> 11) & 15];
							}
							else
							{
								if (blockColor instanceof CustomBlockColor)
								{
									col = ((CustomBlockColor) blockColor).color;
								}
								else if (blockColor == BlockColors.FOLIAGE)
								{
									col = Color4I.rgb(ColorUtils.convertFromNative(dataImgMap[ax + 512][az])).withAlpha(255).withTint(Color4I.BLACK.withAlpha(50));
								}
								else if (blockColor == BlockColors.GRASS)
								{
									col = Color4I.rgb(ColorUtils.convertFromNative(dataImgMap[ax][az + 512])).withAlpha(255).withTint(Color4I.BLACK.withAlpha(50));
								}
								else
								{
									col = blockColor.getBlockColor(world, blockPos).withAlpha(255);
								}

								if (FTBChunksClientConfig.mapMode == MapMode.NIGHT)
								{
									col = col.withTint(ColorUtils.getLightMapPalette()[(data >> 11) & 15][15].withAlpha(230));
								}

								if (water)
								{
									col = col.withTint(Color4I.rgb(ColorUtils.convertFromNative(dataImgMap[ax + 512][az + 512])).withAlpha(220));
								}

								if (FTBChunksClientConfig.reducedColorPalette)
								{
									col = ColorUtils.reduce(col);
								}
							}

							if (FTBChunksClientConfig.saturation < 1F)
							{
								Color.RGBtoHSB(col.redi(), col.greeni(), col.bluei(), hsb);
								hsb[1] *= FTBChunksClientConfig.saturation;
								col = Color4I.hsb(hsb[0], hsb[1], hsb[2]);
							}

							float addedBrightness = 0F;

							if (FTBChunksClientConfig.shadows > 0F)
							{
								int bn = getHeight(az == 0 ? dataImgMapN[ax] : dataImgMap[ax][az - 1]);
								int bw = getHeight(ax == 0 ? dataImgMapW[az] : dataImgMap[ax - 1][az]);

								if (by > bn || by > bw)
								{
									addedBrightness += FTBChunksClientConfig.shadows * (water ? 0.6F : 1F);
								}

								if (by < bn || by < bw)
								{
									addedBrightness -= FTBChunksClientConfig.shadows * (water ? 0.6F : 1F);
								}
							}

							if (FTBChunksClientConfig.noise > 0F)
							{
								addedBrightness += random.nextFloat() * FTBChunksClientConfig.noise - FTBChunksClientConfig.noise / 2F;
							}

							if (addedBrightness != 0F)
							{
								col = ColorUtils.addBrightness(col, addedBrightness);
							}

							if (FTBChunksClientConfig.chunkGrid && (x == 0 || z == 0))
							{
								col = col.withTint(MapRegion.GRID_COLOR);
							}

							if (!claimColor.isEmpty())
							{
								col = col.withTint(claimColor);
							}

							if ((claimBarUp && z == 0) || (claimBarDown && z == 15) || (claimBarLeft && x == 0) || (claimBarRight && x == 15))
							{
								col = fullClaimColor;
							}

							region.renderedMapImage.setPixelRGBA(ax, az, ColorUtils.convertToNative(0xFF000000 | col.rgb()));
						}
					}
				}
			}
		}

		region.updateMapTexture = true;
		FTBChunksClient.updateMinimap = true;
		region.renderingMapImage = false;
	}

	@Override
	public String toString()
	{
		return "RenderMapImageTask@" + region.pos;
	}
}
