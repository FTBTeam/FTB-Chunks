package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.client.FTBChunksClient;
import com.feed_the_beast.mods.ftbchunks.client.FTBChunksClientConfig;
import com.feed_the_beast.mods.ftbchunks.impl.XZ;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureUtil;

import java.awt.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class MapRegion implements MapTask
{
	public static final Color4I GRID_COLOR = Color4I.rgba(70, 70, 70, 50);

	public final MapDimension dimension;
	public final XZ pos;
	private Map<XZ, MapChunk> chunks;
	private NativeImage dataImage;
	public boolean saveData;
	private NativeImage mapImage;
	private boolean updateMapImage;
	private int mapImageTextureId;
	public boolean mapImageLoaded;

	public MapRegion(MapDimension d, XZ p)
	{
		dimension = d;
		pos = p;
		dataImage = null;
		saveData = false;
		mapImage = null;
		updateMapImage = true;
		mapImageTextureId = -1;
		mapImageLoaded = false;
	}

	public MapRegion created()
	{
		dimension.saveData = true;
		return this;
	}

	public Map<XZ, MapChunk> getChunks()
	{
		if (chunks == null)
		{
			chunks = new HashMap<>();

			Path mapFile = dimension.directory.resolve(pos.x + "," + pos.z + ",map.png");

			if (Files.exists(mapFile) && Files.isReadable(mapFile))
			{
				if (!MapIOUtils.read(dimension.directory.resolve(pos.x + "," + pos.z + ",data.chunks"), stream -> {
					stream.readByte();
					int version = stream.readByte();
					int s = stream.readShort();

					for (int i = 0; i < s; i++)
					{
						int x = stream.readByte();
						int z = stream.readByte();
						long m = stream.readLong();

						MapChunk c = new MapChunk(this, XZ.of(x, z));
						c.modified = m;
						chunks.put(c.pos, c);
					}

					try (InputStream is = Files.newInputStream(mapFile))
					{
						dataImage = NativeImage.read(is);

						if (dataImage.getWidth() != 514 || dataImage.getHeight() != 514)
						{
							dataImage.close();
							dataImage = null;
						}
					}
				}))
				{
					update(true);
				}
			}

			if (dataImage == null)
			{
				dataImage = new NativeImage(NativeImage.PixelFormat.RGBA, 514, 514, true);
				dataImage.fillAreaRGBA(0, 0, 514, 514, NativeImage.getCombined(0, 0, 0, 0));
				update(false);
			}
		}

		return chunks;
	}

	public NativeImage getDataImage()
	{
		getChunks();
		return dataImage;
	}

	public void setPixelAndUpdate(int x, int z, int c)
	{
		getDataImage().setPixelRGBA(x, z, c);
		update(true);
	}

	public NativeImage getMapImage()
	{
		getChunks();

		if (mapImage == null)
		{
			mapImage = new NativeImage(NativeImage.PixelFormat.RGBA, 512, 512, true);
			update(false);
		}

		if (updateMapImage)
		{
			updateMapImage = false;

			FTBChunksClient.queue(() -> {
				NativeImage dataImg = getDataImage();

				int[][] dataImgMap = new int[514][514];

				for (int z = 0; z < 514; z++)
				{
					for (int x = 0; x < 514; x++)
					{
						dataImgMap[x][z] = dataImg.getPixelRGBA(x, z);
					}
				}

				float[] hsb = new float[3];

				for (int cz = 0; cz < 32; cz++)
				{
					for (int cx = 0; cx < 32; cx++)
					{
						MapChunk c = chunks.get(XZ.of(cx, cz));
						Random random = new Random(pos.asLong() ^ (c == null ? 0L : c.pos.asLong()));

						for (int z = 0; z < 16; z++)
						{
							for (int x = 0; x < 16; x++)
							{
								int ax = cx * 16 + x;
								int az = cz * 16 + z;

								if (c == null)
								{
									mapImage.setPixelRGBA(ax, az, 0);
								}
								else
								{
									int col = dataImgMap[ax + 1][az + 1];

									if (FTBChunksClientConfig.reducedColorPalette)
									{
										col = ColorUtils.reduce(col);
									}

									if (FTBChunksClientConfig.saturation < 1F)
									{
										int r = (col >> 16) & 0xFF;
										int g = (col >> 8) & 0xFF;
										int b = (col >> 0) & 0xFF;
										Color.RGBtoHSB(r, g, b, hsb);
										hsb[1] *= FTBChunksClientConfig.saturation;
										col = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
									}

									float addedBrightness = 0F;

									if (FTBChunksClientConfig.shadows > 0F)
									{
										int by = NativeImage.getAlpha(dataImgMap[ax + 1][az + 1]);
										int bn = NativeImage.getAlpha(dataImgMap[ax + 1][az + 1 - 1]);
										int bw = NativeImage.getAlpha(dataImgMap[ax + 1 - 1][az + 1]);

										if (bn != -1 || bw != -1)
										{
											if (by > bn || by > bw)
											{
												addedBrightness += FTBChunksClientConfig.shadows;
											}

											if (by < bn || by < bw)
											{
												addedBrightness -= FTBChunksClientConfig.shadows;
											}
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
										col = Color4I.rgb(col).withTint(GRID_COLOR).rgba();
									}

									if (col == 0xFF000000)
									{
										col = 0xFF010101;
									}

									mapImage.setPixelRGBA(ax, az, 0xFF000000 | col);
								}
							}
						}
					}
				}

				Minecraft.getInstance().runAsync(() -> {
					if (mapImageTextureId != -1 && mapImage != null)
					{
						TextureUtil.prepareImage(mapImageTextureId, 512, 512);
						mapImage.uploadTextureSub(0, 0, 0, false);
						mapImageLoaded = true;
					}
				});
			});
		}

		return mapImage;
	}

	public int getMapImageTextureId()
	{
		if (updateMapImage)
		{
			getMapImage();
		}

		if (mapImageTextureId == -1)
		{
			mapImageTextureId = TextureUtil.generateTextureId();
			mapImageLoaded = false;

			Minecraft.getInstance().runAsync(() -> {
				TextureUtil.prepareImage(mapImageTextureId, 512, 512);
				update(false);
				getMapImage().uploadTextureSub(0, 0, 0, false);
				mapImageLoaded = true;
			});
		}

		return mapImageTextureId;
	}

	public MapChunk getChunk(XZ pos)
	{
		if (pos.x != (pos.x & 31) || pos.z != (pos.z & 31))
		{
			pos = XZ.of(pos.x & 31, pos.z & 31);
		}

		return getChunks().computeIfAbsent(pos, p -> new MapChunk(this, p).created());
	}

	public void release()
	{
		if (dataImage != null)
		{
			dataImage.close();
			dataImage = null;
		}

		chunks = null;
		releaseMapImage();
	}

	public void releaseMapImage()
	{
		if (mapImage != null)
		{
			mapImage.close();
			mapImage = null;
		}

		if (mapImageTextureId != -1)
		{
			TextureUtil.releaseTextureId(mapImageTextureId);
			mapImageTextureId = -1;
		}

		mapImageLoaded = false;
	}

	@Override
	public void runMapTask()
	{
		if (getChunks().isEmpty())
		{
			return;
		}

		try
		{
			if (Files.notExists(dimension.directory))
			{
				Files.createDirectories(dimension.directory);
			}

			List<MapChunk> chunkList = getChunks().values().stream().filter(c -> c.modified > 0L).collect(Collectors.toList());

			if (chunkList.isEmpty())
			{
				return;
			}

			MapIOUtils.write(dimension.directory.resolve(pos.x + "," + pos.z + ",data.chunks"), stream -> {
				stream.writeByte(0);
				stream.writeByte(1);
				stream.writeShort(chunkList.size());

				for (MapChunk chunk : chunkList)
				{
					stream.writeByte(chunk.pos.x);
					stream.writeByte(chunk.pos.z);
					stream.writeLong(chunk.modified);
				}
			});

			getDataImage().write(dimension.directory.resolve(pos.x + "," + pos.z + ",map.png"));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void update(boolean save)
	{
		if (save)
		{
			saveData = true;
			dimension.saveData = true;
		}

		updateMapImage = true;
		FTBChunksClient.updateMinimap = true;
	}

	public MapRegion offset(int x, int z)
	{
		return dimension.getRegion(pos.offset(x, z));
	}

	public RegionSyncKey getSyncKey()
	{
		RegionSyncKey key = new RegionSyncKey();
		key.dim = dimension.dimension;
		key.x = pos.x;
		key.z = pos.z;
		key.random = MathUtils.RAND.nextInt();
		return key;
	}

	public double distToPlayer()
	{
		return MathUtils.distSq(pos.x * 512D + 256D, pos.z * 512D + 256D, Minecraft.getInstance().player.getPosX(), Minecraft.getInstance().player.getPosZ());
	}
}