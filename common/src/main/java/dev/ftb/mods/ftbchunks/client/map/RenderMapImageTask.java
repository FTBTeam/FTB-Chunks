package dev.ftb.mods.ftbchunks.client.map;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.map.color.BlockColor;
import dev.ftb.mods.ftbchunks.client.map.color.BlockColors;
import dev.ftb.mods.ftbchunks.client.map.color.ColorUtils;
import dev.ftb.mods.ftbchunks.client.map.color.CustomBlockColor;
import dev.ftb.mods.ftbchunks.net.LoadedChunkViewPacket;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Random;

/**
 * @author LatvianModder
 */
public class RenderMapImageTask implements MapTask {
	private static boolean alwaysRenderChunksOnMap = false;

	public final MapRegion region;
	private static final boolean exportImages = false;

	public RenderMapImageTask(MapRegion r) {
		region = r;
	}

	private static int getHeight(MapMode m, int whf, short data, short height) {
		if ((((data & 0xFFFF) >> 15) & 1) != 0 && m != MapMode.TOPOGRAPHY) {
			if (whf == 0) {
				return 62;
			}

			return ((int) height / whf) * whf + whf - 1;
		}

		return height;
	}

	public static void setAlwaysRenderChunksOnMap(boolean alwaysRenderChunksOnMap) {
		RenderMapImageTask.alwaysRenderChunksOnMap = alwaysRenderChunksOnMap;
	}

	private int[][] initColors(BiomeBlendMode blendMode, MapRegionData data, ColorsFromRegion getter) {
		int[] colors = getter.getColors(data);
		int blend = blendMode.getBlendRadius();
		int size = 512 + blend * 2;
		int[][] newColors = new int[size][size];

		int posX = region.pos.x();
		int posZ = region.pos.z();
		int[] rWW = blend == 0 ? null : region.dimension.getColors(posX - 1, posZ, getter);
		int[] rEE = blend == 0 ? null : region.dimension.getColors(posX + 1, posZ, getter);
		int[] rNN = blend == 0 ? null : region.dimension.getColors(posX, posZ - 1, getter);
		int[] rSS = blend == 0 ? null : region.dimension.getColors(posX, posZ + 1, getter);

		int[] rNW = blend == 0 ? null : region.dimension.getColors(posX - 1, posZ - 1, getter);
		int[] rSW = blend == 0 ? null : region.dimension.getColors(posX - 1, posZ + 1, getter);
		int[] rNE = blend == 0 ? null : region.dimension.getColors(posX + 1, posZ - 1, getter);
		int[] rSE = blend == 0 ? null : region.dimension.getColors(posX + 1, posZ + 1, getter);

		for (int i = 0; i < 512; i++) {
			for (int j = 0; j < 512; j++) {
				newColors[i + blend][j + blend] = colors[i + j * 512];
			}

			for (int j = 0; j < blend; j++) {
				if (rWW != null) {
					newColors[j][i + blend] = rWW[j + 512 - blend + i * 512];
				}

				if (rEE != null) {
					newColors[j + 512 + blend][i + blend] = rEE[j + i * 512];
				}

				if (rNN != null) {
					newColors[i + blend][j] = rNN[i + (j + 512 - blend) * 512];
				}

				if (rSS != null) {
					newColors[i + blend][j + blend + 512] = rSS[i + j * 512];
				}
			}
		}

		for (int i = 0; i < blend; i++) {
			for (int j = 0; j < blend; j++) {
				if (rNW != null) {
					newColors[i][j] = rNW[i + 512 - blend + (j + 512 - blend) * 512];
				}

				if (rNE != null) {
					newColors[i + 512 + blend][j] = rNE[i + (j + 512 - blend) * 512];
				}

				if (rSW != null) {
					newColors[i][j + 512 + blend] = rSW[i + 512 - blend + j * 512];
				}

				if (rSE != null) {
					newColors[i + 512 + blend][j + 512 + blend] = rSE[i + j * 512];
				}
			}
		}

		if (exportImages && FTBChunksClientConfig.DEBUG_INFO.get()) {
			BufferedImage export = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					export.setRGB(x, y, newColors[x][y]);
				}
			}

			try (OutputStream stream = Files.newOutputStream(Platform.getGameFolder().resolve("local/ftbchunks/debug/" + region + "-" + getter.getName() + ".png"))) {
				ImageIO.write(export, "PNG", stream);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return newColors;
	}

	private int[][] initFoliage(BiomeBlendMode blend, MapRegionData data) {
		return initColors(blend, data, ColorsFromRegion.FOLIAGE);
	}

	private int[][] initGrass(BiomeBlendMode blend, MapRegionData data) {
		return initColors(blend, data, ColorsFromRegion.GRASS);
	}

	private int[][] initWater(BiomeBlendMode blend, MapRegionData data) {
		return initColors(blend, data, ColorsFromRegion.WATER);
	}

	private Color4I getColor(BiomeBlendMode blendMode, int[][] colors, int ax, int az) {
		return blendMode.doBlending(colors, ax, az);
	}

	@Override
	public void runMapTask() {
		if (region.dimension.getManager().isInvalid()) {
			return;
		}

		BiomeBlendMode blend = FTBChunksClientConfig.BIOME_BLEND.get();

		MapRegionData data = region.getDataBlocking();
		short[] heightW = new short[512];
		short[] heightN = new short[512];
		short[] waterLightAndBiomeW = new short[512];
		short[] waterLightAndBiomeN = new short[512];
		int[][] foliage = null;
		int[][] grass = null;
		int[][] water = null;

		MapRegion rWest = region.dimension.getRegions().get(region.pos.offset(-1, 0));

		if (rWest != null) {
			MapRegionData d = rWest.getDataBlocking();

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

		MapRegion rNorth = region.dimension.getRegions().get(region.pos.offset(0, -1));

		if (rNorth != null) {
			MapRegionData d = rNorth.getDataBlocking();
			System.arraycopy(d.height, 511 * 512, heightN, 0, 512);
			System.arraycopy(d.waterLightAndBiome, 511 * 512, waterLightAndBiomeN, 0, 512);
		} else {
			System.arraycopy(data.height, 0, heightN, 0, 512);
			System.arraycopy(data.waterLightAndBiome, 0, waterLightAndBiomeN, 0, 512);
		}

		float[] hsb = new float[3];
		Team ownTeam = FTBTeamsAPI.api().getClientManager().selfTeam();
		Level world = Minecraft.getInstance().level;
		BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

		MapMode mapMode = FTBChunksClientConfig.MAP_MODE.get();
		int waterHeightFactor = FTBChunksClientConfig.WATER_HEIGHT_FACTOR.get();
		float noise = FTBChunksClientConfig.NOISE.get().floatValue();
		boolean ownClaimedChunksOnMap = FTBChunksClientConfig.OWN_CLAIMED_CHUNKS_ON_MAP.get();
		boolean claimedChunksOnMap = FTBChunksClientConfig.CLAIMED_CHUNKS_ON_MAP.get();
		float saturation = FTBChunksClientConfig.SATURATION.get().floatValue();
		float shadows = FTBChunksClientConfig.SHADOWS.get().floatValue();

		int foliageDarkness = FTBChunksClientConfig.FOLIAGE_DARKNESS.get();
		int grassDarkness = FTBChunksClientConfig.GRASS_DARKNESS.get();
		int waterVisibility = FTBChunksClientConfig.WATER_VISIBILITY.get();
		boolean reducedColorPalette = FTBChunksClientConfig.REDUCED_COLOR_PALETTE.get();

		boolean chunkGrid = FTBChunksClientConfig.CHUNK_GRID.get();
		Color4I weakLoadedTint = Color4I.rgba(255, 180, 0, 20);
		Color4I weakLoadedTint2 = Color4I.rgba(255, 180, 0, 100);
		Color4I strongLoadedTint = Color4I.RED.withAlpha(60);
		Color4I strongLoadedTint2 = Color4I.RED.withAlpha(140);

		for (int cz = 0; cz < 32; cz++) {
			for (int cx = 0; cx < 32; cx++) {
				int loadedView = region.dimension.getLoadedView(region, cx, cz);
				MapChunk c = region.getMapChunk(XZ.of(cx, cz));
				Random random = new Random(region.pos.toLong() ^ (c == null ? 0L : c.getPos().toLong()));
				Color4I claimColor;
				int fullClaimColor;
				boolean claimBarUp, claimBarDown, claimBarLeft, claimBarRight;
				boolean isOwnChunk = c != null && c.getTeam().isPresent() && ownTeam.getTeamId().equals(c.getTeam().get().getTeamId());

				if (c != null && c.getClaimedDate().isPresent() && (alwaysRenderChunksOnMap || (isOwnChunk ? ownClaimedChunksOnMap : claimedChunksOnMap))) {
					claimColor = c.getTeam().get().getProperty(TeamProperties.COLOR).withAlpha(100);
					fullClaimColor = ColorUtils.convertToNative(0xFF000000 | claimColor.rgb());
					claimBarUp = !c.connects(c.offsetBlocking(0, -1));
					claimBarDown = !c.connects(c.offsetBlocking(0, 1));
					claimBarLeft = !c.connects(c.offsetBlocking(-1, 0));
					claimBarRight = !c.connects(c.offsetBlocking(1, 0));
				} else {
					claimColor = Color4I.empty();
					fullClaimColor = 0;
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
						boolean tint2 = ax % 4 == (az % 2 == 0 ? 0 : 2);

						if (c == null) {
							if (loadedView == LoadedChunkViewPacket.LOADED) {
								region.setRenderedMapImageRGBA(ax, az, ColorUtils.convertToNative(0xFF000000 | Color4I.BLACK.withTint(tint2 ? weakLoadedTint2 : weakLoadedTint).rgb()));
							} else if (loadedView == LoadedChunkViewPacket.FORCE_LOADED) {
								region.setRenderedMapImageRGBA(ax, az, ColorUtils.convertToNative(0xFF000000 | Color4I.BLACK.withTint(tint2 ? strongLoadedTint2 : strongLoadedTint).rgb()));
							} else {
								region.setRenderedMapImageRGBA(ax, az, 0);
							}
						} else {
							BlockColor blockColor = region.dimension.getManager().getBlockColor(data.getBlockIndex(index));
							Color4I col;
							int by = getHeight(mapMode, waterHeightFactor, data.waterLightAndBiome[index], data.height[index]);
							boolean hasWater = ((data.waterLightAndBiome[index] >> 15) & 1) != 0;
							blockPos.set(region.pos.x() * 512 + ax, by, region.pos.z() * 512 + az);

							if (mapMode == MapMode.TOPOGRAPHY) {
								col = ColorUtils.getTopographyPalette()[by + (hasWater ? 256 : 0)];
							} else if (mapMode == MapMode.BLOCKS) {
								col = Color4I.rgb(data.getBlockIndex(index));
							} else if (mapMode == MapMode.LIGHT_SOURCES) {
								col = ColorUtils.getLightMapPalette()[15][(data.waterLightAndBiome[index] >> 11) & 15];
							} else if ((claimBarUp && z == 0) || (claimBarDown && z == 15) || (claimBarLeft && x == 0) || (claimBarRight && x == 15)) {
								region.setRenderedMapImageRGBA(ax, az, fullClaimColor);
								continue;
							} else {
								if (blockColor instanceof CustomBlockColor cbc) {
									col = cbc.getColor();
								} else if (blockColor == BlockColors.FOLIAGE) {
									if (foliage == null) {
										foliage = initFoliage(blend, data);
									}
									col = getColor(blend, foliage, ax, az).withAlpha(255).withTint(Color4I.BLACK.withAlpha(foliageDarkness));
								} else if (blockColor == BlockColors.GRASS) {
									if (grass == null) {
										grass = initGrass(blend, data);
									}

									col = getColor(blend, grass, ax, az).withAlpha(255).withTint(Color4I.BLACK.withAlpha(grassDarkness));
								} else {
									// This is unsafe but should be *mostly* fine
									col = blockColor.getBlockColor(world, blockPos).withAlpha(255);
								}

								if (mapMode == MapMode.NIGHT) {
									col = col.withTint(ColorUtils.getLightMapPalette()[(data.waterLightAndBiome[index] >> 11) & 15][15].withAlpha(230));
								}

								if (hasWater) {
									if (water == null) {
										water = initWater(blend, data);
									}

									col = col.withTint(getColor(blend, water, ax, az).withAlpha(waterVisibility));
								}

								if (reducedColorPalette) {
									col = ColorUtils.reduce(col);
								}
							}

							if (saturation < 1F) {
								Color.RGBtoHSB(col.redi(), col.greeni(), col.bluei(), hsb);
								hsb[1] *= saturation;
								col = Color4I.hsb(hsb[0], hsb[1], hsb[2]);
							}

							float addedBrightness = 0F;

							if (shadows > 0F) {
								int bn = getHeight(mapMode, waterHeightFactor, az == 0 ? waterLightAndBiomeN[ax] : data.waterLightAndBiome[ax + (az - 1) * 512], az == 0 ? heightN[ax] : data.height[ax + (az - 1) * 512]);
								int bw = getHeight(mapMode, waterHeightFactor, ax == 0 ? waterLightAndBiomeW[az] : data.waterLightAndBiome[ax - 1 + az * 512], ax == 0 ? heightW[az] : data.height[ax - 1 + az * 512]);

								if (by > bn || by > bw) {
									addedBrightness += shadows * (hasWater ? 0.6F : 1F);
								}

								if (by < bn || by < bw) {
									addedBrightness -= shadows * (hasWater ? 0.6F : 1F);
								}
							}

							if (noise > 0F) {
								addedBrightness += random.nextFloat() * noise - noise / 2F;
							}

							if (addedBrightness != 0F) {
								col = ColorUtils.addBrightness(col, addedBrightness);
							}

							if (chunkGrid && (x == 0 || z == 0)) {
								col = col.withTint(MapRegion.GRID_COLOR);
							}

							if (loadedView == 0 && !claimColor.isEmpty()) {
								col = col.withTint(claimColor);
							}

							if (loadedView == LoadedChunkViewPacket.LOADED) {
								col = col.withTint(tint2 ? weakLoadedTint2 : weakLoadedTint);
							} else if (loadedView == LoadedChunkViewPacket.FORCE_LOADED) {
								col = col.withTint(tint2 ? strongLoadedTint2 : strongLoadedTint);
							}

							region.setRenderedMapImageRGBA(ax, az, ColorUtils.convertToNative(0xFF000000 | col.rgb()));
						}
					}
				}
			}
		}

		region.afterImageRenderTask();
	}

	@Override
	public String toString() {
		return "RenderMapImageTask@" + region.pos;
	}
}
