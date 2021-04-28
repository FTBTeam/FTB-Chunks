package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.map.color.BlockColor;
import dev.ftb.mods.ftbchunks.client.map.color.BlockColors;
import dev.ftb.mods.ftbchunks.client.map.color.ColorUtils;
import dev.ftb.mods.ftbchunks.client.map.color.CustomBlockColor;
import dev.ftb.mods.ftbchunks.data.XZ;
import dev.ftb.mods.ftbguilibrary.icon.Color4I;
import dev.ftb.mods.ftbguilibrary.icon.Icon;
import dev.ftb.mods.ftbteams.data.ClientTeam;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.TeamBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.awt.Color;
import java.util.Random;

/**
 * @author LatvianModder
 */
public class RenderMapImageTask implements MapTask {
	public final MapRegion region;

	public RenderMapImageTask(MapRegion r) {
		region = r;
	}

	private static int getHeight(FTBChunksClientConfig c, short data, short height) {
		int h = height & 0xFFFF;

		if ((((data & 0xFFFF) >> 15) & 1) != 0 && c.mapMode != MapMode.TOPOGRAPHY) {
			if (c.waterHeightFactor == 0) {
				return 62;
			}

			return (h / c.waterHeightFactor) * c.waterHeightFactor + c.waterHeightFactor - 1;
		}

		return h;
	}

	@Override
	public void runMapTask() {
		FTBChunksClientConfig config = FTBChunksClientConfig.get();
		MapRegionData data = region.getDataBlocking();
		short[] heightW = new short[512];
		short[] heightN = new short[512];
		short[] waterLightAndBiomeW = new short[512];
		short[] waterLightAndBiomeN = new short[512];

		MapRegion rEast = region.dimension.getRegions().get(region.pos.offset(-1, 0));

		if (rEast != null) {
			MapRegionData d = rEast.getDataBlocking();

			for (int i = 0; i < 512; i++) {
				heightW[i] = d.height[511 + i * 512];
				waterLightAndBiomeW[i] = d.waterLightAndBiome[511 + i * 512];
			}
		} else {
			for (int i = 0; i < 512; i++) {
				heightW[i] = data.height[i * 512];
				waterLightAndBiomeW[i] = data.waterLightAndBiome[i * 512];
			}
		}

		MapRegion rTop = region.dimension.getRegions().get(region.pos.offset(0, -1));

		if (rTop != null) {
			MapRegionData d = rTop.getDataBlocking();
			System.arraycopy(d.height, 511 * 512, heightN, 0, 512);
			System.arraycopy(d.waterLightAndBiome, 511 * 512, waterLightAndBiomeN, 0, 512);
		} else {
			System.arraycopy(data.height, 0, heightN, 0, 512);
			System.arraycopy(data.waterLightAndBiome, 0, waterLightAndBiomeN, 0, 512);
		}

		float[] hsb = new float[3];
		ClientTeam ownTeam = ClientTeamManager.INSTANCE.selfTeam;
		Level world = Minecraft.getInstance().level;
		BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
		Int2ObjectOpenHashMap<Biome> biomeMap = new Int2ObjectOpenHashMap<>();

		for (int cz = 0; cz < 32; cz++) {
			for (int cx = 0; cx < 32; cx++) {
				MapChunk c = data.chunks.get(XZ.of(cx, cz));
				Random random = new Random(region.pos.asLong() ^ (c == null ? 0L : c.pos.asLong()));
				Color4I claimColor, fullClaimColor;
				boolean claimBarUp, claimBarDown, claimBarLeft, claimBarRight;

				if (c != null && c.claimedDate != null && (FTBChunksClient.alwaysRenderChunksOnMap || (ownTeam.equals(c.getTeam()) ? config.ownClaimedChunksOnMap : config.claimedChunksOnMap))) {
					claimColor = c.getTeam().getProperty(TeamBase.COLOR).withAlpha(100);
					fullClaimColor = claimColor.withAlpha(255);
					claimBarUp = !c.connects(c.offsetBlocking(0, -1));
					claimBarDown = !c.connects(c.offsetBlocking(0, 1));
					claimBarLeft = !c.connects(c.offsetBlocking(-1, 0));
					claimBarRight = !c.connects(c.offsetBlocking(1, 0));
				} else {
					claimColor = Icon.EMPTY;
					fullClaimColor = Icon.EMPTY;
					claimBarUp = false;
					claimBarDown = false;
					claimBarLeft = false;
					claimBarRight = false;
				}

				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						int ax = cx * 16 + x;
						int az = cz * 16 + z;
						int index = ax + az * 512;

						if (c == null) {
							region.renderedMapImage.setPixelRGBA(ax, az, 0);
						} else {
							BlockColor blockColor = region.dimension.manager.getBlockColor(data.getBlockIndex(index));
							Color4I col;
							int by = getHeight(config, data.waterLightAndBiome[index], data.height[index]);
							boolean water = ((data.waterLightAndBiome[index] >> 15) & 1) != 0;
							blockPos.set(region.pos.x * 512 + ax, by, region.pos.z * 512 + az);

							if (config.mapMode == MapMode.TOPOGRAPHY) {
								col = ColorUtils.getTopographyPalette()[by + (water ? 256 : 0)];
							} else if (config.mapMode == MapMode.BLOCKS) {
								col = Color4I.rgb(data.getBlockIndex(index));
							} else if (config.mapMode == MapMode.BIOME_TEMPERATURE) {
								Biome biome = biomeMap.computeIfAbsent(data.waterLightAndBiome[index] & 0b111_11111111, i -> region.dimension.manager.getBiome(world, i));
								float temp0 = biome.getTemperature(blockPos);

								float temp = (temp0 + 0.5F) / 2F;
								col = Color4I.hsb((float) (Math.PI - temp * Math.PI), 0.9F, 1F);
							} else if (config.mapMode == MapMode.LIGHT_SOURCES) {
								col = ColorUtils.getLightMapPalette()[15][(data.waterLightAndBiome[index] >> 11) & 15];
							} else {
								if (blockColor instanceof CustomBlockColor) {
									col = ((CustomBlockColor) blockColor).color;
								} else if (blockColor == BlockColors.FOLIAGE) {
									col = Color4I.rgb(data.foliage[index]).withAlpha(255).withTint(Color4I.BLACK.withAlpha(config.foliageDarkness));
								} else if (blockColor == BlockColors.GRASS) {
									col = Color4I.rgb(data.grass[index]).withAlpha(255).withTint(Color4I.BLACK.withAlpha(config.grassDarkness));
								} else {
									col = blockColor.getBlockColor(world, blockPos).withAlpha(255);
								}

								if (config.mapMode == MapMode.NIGHT) {
									col = col.withTint(ColorUtils.getLightMapPalette()[(data.waterLightAndBiome[index] >> 11) & 15][15].withAlpha(230));
								}

								if (water) {
									col = col.withTint(Color4I.rgb(data.water[index]).withAlpha(config.waterVisibility));
								}

								if (config.reducedColorPalette) {
									col = ColorUtils.reduce(col);
								}
							}

							if (config.saturation < 1F) {
								Color.RGBtoHSB(col.redi(), col.greeni(), col.bluei(), hsb);
								hsb[1] *= config.saturation;
								col = Color4I.hsb(hsb[0], hsb[1], hsb[2]);
							}

							float addedBrightness = 0F;

							if (config.shadows > 0F) {
								int bn = getHeight(config, az == 0 ? waterLightAndBiomeN[ax] : data.waterLightAndBiome[ax + (az - 1) * 512], az == 0 ? heightN[ax] : data.height[ax + (az - 1) * 512]);
								int bw = getHeight(config, ax == 0 ? waterLightAndBiomeW[az] : data.waterLightAndBiome[ax - 1 + az * 512], ax == 0 ? heightW[az] : data.height[ax - 1 + az * 512]);

								if (by > bn || by > bw) {
									addedBrightness += config.shadows * (water ? 0.6F : 1F);
								}

								if (by < bn || by < bw) {
									addedBrightness -= config.shadows * (water ? 0.6F : 1F);
								}
							}

							if (config.noise > 0F) {
								addedBrightness += random.nextFloat() * config.noise - config.noise / 2F;
							}

							if (addedBrightness != 0F) {
								col = ColorUtils.addBrightness(col, addedBrightness);
							}

							if (config.chunkGrid && (x == 0 || z == 0)) {
								col = col.withTint(MapRegion.GRID_COLOR);
							}

							if (!claimColor.isEmpty()) {
								col = col.withTint(claimColor);
							}

							if ((claimBarUp && z == 0) || (claimBarDown && z == 15) || (claimBarLeft && x == 0) || (claimBarRight && x == 15)) {
								col = fullClaimColor;
							}

							region.renderedMapImage.setPixelRGBA(ax, az, ColorUtils.convertToNative(0xFF000000 | col.rgb()));
						}
					}
				}
			}
		}

		region.updateRenderedMapTexture = true;
		FTBChunksClient.updateMinimap = true;
		region.renderingMapImage = false;
	}

	@Override
	public String toString() {
		return "RenderMapImageTask@" + region.pos;
	}
}
