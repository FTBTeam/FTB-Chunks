package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.client.FTBChunksClient;
import com.feed_the_beast.mods.ftbchunks.impl.XZ;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureUtil;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class MapRegion implements MapTask
{
	public static final Color4I GRID_COLOR = Color4I.rgba(70, 70, 70, 50);

	public final MapDimension dimension;
	public final XZ pos;
	public Map<XZ, MapChunk> chunks;
	private NativeImage dataImage, blockImage;
	public boolean saveData;
	public NativeImage renderedMapImage;
	private boolean updateMapImage;
	public boolean updateMapTexture;
	private int mapImageTextureId;
	public boolean mapImageLoaded;
	public boolean renderingMapImage;

	public MapRegion(MapDimension d, XZ p)
	{
		dimension = d;
		pos = p;
		dataImage = null;
		blockImage = null;
		saveData = false;
		renderedMapImage = null;
		updateMapImage = true;
		updateMapTexture = true;
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
			dataImage = null;
			blockImage = null;

			Path chunksFile = dimension.directory.resolve(pos.toRegionString() + "-chunks.dat");

			if (Files.exists(chunksFile) && Files.isReadable(chunksFile))
			{
				if (!MapIOUtils.read(chunksFile, stream -> {
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
				}))
				{
					update(true);
				}
			}
		}

		return chunks;
	}

	public NativeImage getDataImage()
	{
		getChunks();

		if (dataImage == null)
		{
			Path file = dimension.directory.resolve(pos.toRegionString() + "-data.png");

			if (Files.exists(file) && Files.isReadable(file))
			{
				try (InputStream in = Files.newInputStream(file))
				{
					dataImage = NativeImage.read(in);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}

			if (dataImage == null)
			{
				dataImage = new NativeImage(NativeImage.PixelFormat.RGBA, 1024, 1024, true);
				dataImage.fillAreaRGBA(0, 0, 1024, 1024, NativeImage.getCombined(255, 0, 0, 0));
			}
		}

		return dataImage;
	}

	public NativeImage getBlockImage()
	{
		getChunks();

		if (blockImage == null)
		{
			Path file = dimension.directory.resolve(pos.toRegionString() + "-blocks.png");

			if (Files.exists(file) && Files.isReadable(file))
			{
				try (InputStream in = Files.newInputStream(file))
				{
					blockImage = NativeImage.read(in);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}

			if (blockImage == null)
			{
				blockImage = new NativeImage(NativeImage.PixelFormat.RGBA, 512, 512, true);
				blockImage.fillAreaRGBA(0, 0, 512, 512, NativeImage.getCombined(255, 0, 0, 0));
			}
		}

		return blockImage;
	}

	public NativeImage getRenderedMapImage()
	{
		getChunks();

		if (renderedMapImage == null)
		{
			renderedMapImage = new NativeImage(NativeImage.PixelFormat.RGBA, 512, 512, true);
			renderedMapImage.fillAreaRGBA(0, 0, 512, 512, 0);
			update(false);
		}

		if (updateMapImage && !renderingMapImage)
		{
			updateMapImage = false;
			mapImageLoaded = false;
			renderingMapImage = true;
			FTBChunksClient.queue(new RenderMapImageTask(this));
		}

		return renderedMapImage;
	}

	public int getMapImageTextureId()
	{
		if (mapImageTextureId == -1)
		{
			mapImageTextureId = TextureUtil.generateTextureId();
			TextureUtil.prepareImage(mapImageTextureId, 512, 512);
		}

		getRenderedMapImage();

		if (updateMapTexture)
		{
			mapImageLoaded = false;

			Minecraft.getInstance().runAsync(() -> {
				RenderSystem.bindTexture(mapImageTextureId);
				renderedMapImage.uploadTextureSub(0, 0, 0, false);
				mapImageLoaded = true;
				FTBChunksClient.updateMinimap = true;
			});

			updateMapTexture = false;
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

		if (blockImage != null)
		{
			blockImage.close();
			blockImage = null;
		}

		chunks = null;
		releaseMapImage();
	}

	public void releaseMapImage()
	{
		if (renderedMapImage != null)
		{
			renderedMapImage.close();
			renderedMapImage = null;
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

			MapIOUtils.write(dimension.directory.resolve(pos.toRegionString() + "-chunks.dat"), stream -> {
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

			getDataImage().write(dimension.directory.resolve(pos.toRegionString() + "-data.png"));
			getBlockImage().write(dimension.directory.resolve(pos.toRegionString() + "-blocks.png"));
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
		updateMapTexture = true;
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