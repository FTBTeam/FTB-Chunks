package dev.ftb.mods.ftbchunks.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.map.color.ColorUtils;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.InflaterInputStream;

/**
 * @author LatvianModder
 */
public class MapRegion implements MapTask {
	public static final Color4I GRID_COLOR = Color4I.rgba(70, 70, 70, 50);

	public final MapDimension dimension;
	public final XZ pos;
	private MapRegionData data;
	private boolean isLoadingData;
	public boolean saveData;
	private NativeImage renderedMapImage;
	private boolean updateRenderedMapImage;
	public boolean updateRenderedMapTexture;
	private int renderedMapImageTextureId;
	public boolean mapImageLoaded;
	public boolean renderingMapImage;

	public MapRegion(MapDimension d, XZ p) {
		dimension = d;
		pos = p;
		data = null;
		isLoadingData = false;
		saveData = false;
		renderedMapImage = null;
		updateRenderedMapImage = true;
		updateRenderedMapTexture = true;
		renderedMapImageTextureId = -1;
		mapImageLoaded = false;
	}

	public MapRegion created() {
		dimension.saveData = true;
		return this;
	}

	public boolean isDataLoaded() {
		return data != null;
	}

	public MapRegionData getDataBlocking() {
		synchronized (dimension.manager.lock) {
			return getDataBlockingNoSync();
		}
	}

	public MapRegionData getDataBlockingNoSync() {
		if (data != null) {
			return data;
		}

		data = new MapRegionData(this);

		Path chunksFile = dimension.directory.resolve(pos.toRegionString() + "-chunks.dat");

		if (Files.exists(chunksFile) && Files.isReadable(chunksFile)) {
			FTBChunks.LOGGER.info("Found old map files, converting... [" + dimension.safeDimensionId + "/" + pos.toRegionString() + "]");

			try (DataInputStream stream = new DataInputStream(new BufferedInputStream(new InflaterInputStream(Files.newInputStream(chunksFile))))) {
				stream.readByte();
				stream.readByte();
				int s = stream.readShort();

				for (int i = 0; i < s; i++) {
					int x = stream.readByte();
					int z = stream.readByte();
					long m = stream.readLong();

					MapChunk c = new MapChunk(this, XZ.of(x, z));
					c.modified = m;
					data.chunks.put(c.pos, c);
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			try (InputStream stream = Files.newInputStream(dimension.directory.resolve(pos.toRegionString() + "-data.png"))) {
				BufferedImage img = ImageIO.read(stream);

				for (int y = 0; y < 512; y++) {
					for (int x = 0; x < 512; x++) {
						int index = x + y * 512;
						int d = ColorUtils.convertToNative(img.getRGB(x, y) & 0xFFFFFF);
						data.height[index] = (short) (d >> 16);
						data.waterLightAndBiome[index] = (short) d;
						data.foliage[index] = img.getRGB(x + 512, y) & 0xFFFFFF;
						data.grass[index] = img.getRGB(x, y + 512) & 0xFFFFFF;
						data.water[index] = img.getRGB(x + 512, y + 512) & 0xFFFFFF;
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			try (InputStream stream = Files.newInputStream(dimension.directory.resolve(pos.toRegionString() + "-blocks.png"))) {
				BufferedImage img = ImageIO.read(stream);

				for (int y = 0; y < 512; y++) {
					for (int x = 0; x < 512; x++) {
						data.setBlockIndex(x + y * 512, ColorUtils.convertToNative(img.getRGB(x, y)));
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			try {
				Files.deleteIfExists(chunksFile);
				Files.deleteIfExists(dimension.directory.resolve(pos.toRegionString() + "-data.png"));
				Files.deleteIfExists(dimension.directory.resolve(pos.toRegionString() + "-blocks.png"));
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			try {
				data.write();
				update(false);
			} catch (IOException ex) {
				update(true);
				ex.printStackTrace();
			}
		} else {
			try {
				data.read();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		return data;
	}

	@Nullable
	public MapRegionData getData() {
		if (data == null && !isLoadingData) {
			isLoadingData = true;
			FTBChunks.EXECUTOR.execute(this::getDataBlocking);
		}

		return data;
	}

	public NativeImage getRenderedMapImage() {
		synchronized (dimension.manager.lock) {
			if (renderedMapImage == null) {
				renderedMapImage = new NativeImage(NativeImage.Format.RGBA, 512, 512, true);
				renderedMapImage.fillRect(0, 0, 512, 512, 0);
				update(false);
			}
		}

		if (updateRenderedMapImage && !renderingMapImage) {
			updateRenderedMapImage = false;
			mapImageLoaded = false;
			renderingMapImage = true;
			// FTBChunksClient.queue(new RenderMapImageTask(this));
			FTBChunks.EXECUTOR.execute(new RenderMapImageTask(this));
		}

		return renderedMapImage;
	}

	public int getRenderedMapImageTextureId() {
		if (renderedMapImageTextureId == -1) {
			renderedMapImageTextureId = TextureUtil.generateTextureId();
			TextureUtil.prepareImage(renderedMapImageTextureId, 512, 512);
		}

		getRenderedMapImage();

		if (updateRenderedMapTexture) {
			mapImageLoaded = false;

			Minecraft.getInstance().submit(() -> {
				RenderSystem.bindTexture(renderedMapImageTextureId);
				uploadRenderedMapImage();
				mapImageLoaded = true;
				FTBChunksClient.updateMinimap = true;
			});

			updateRenderedMapTexture = false;
		}

		return renderedMapImageTextureId;
	}

	public void release() {
		if (saveData && data != null) {
			try {
				data.write();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		data = null;
		isLoadingData = false;
		releaseMapImage();
	}

	public void releaseMapImage() {
		synchronized (dimension.manager.lock) {
			if (renderedMapImage != null) {
				renderedMapImage.close();
				renderedMapImage = null;
			}
		}

		if (renderedMapImageTextureId != -1) {
			TextureUtil.releaseTextureId(renderedMapImageTextureId);
			renderedMapImageTextureId = -1;
		}

		mapImageLoaded = false;
	}

	@Override
	public void runMapTask() throws Exception {
		if (data != null) {
			data.write();
		}
	}

	public void setRenderedMapImageRGBA(int x, int z, int col) {
		synchronized (dimension.manager.lock) {
			renderedMapImage.setPixelRGBA(x, z, col);
		}
	}

	private void uploadRenderedMapImage() {
		synchronized (dimension.manager.lock) {
			renderedMapImage.upload(0, 0, 0, false);
		}
	}

	public void afterImageRenderTask() {
		synchronized (dimension.manager.lock) {
			updateRenderedMapTexture = true;
			FTBChunksClient.updateMinimap = true;
			renderingMapImage = false;
		}
	}

	public void update(boolean save) {
		if (save) {
			saveData = true;
			dimension.saveData = true;
		}

		updateRenderedMapImage = true;
		updateRenderedMapTexture = true;
		FTBChunksClient.updateMinimap = true;
	}

	public MapRegion offset(int x, int z) {
		return dimension.getRegion(pos.offset(x, z));
	}

	public RegionSyncKey getSyncKey() {
		RegionSyncKey key = new RegionSyncKey();
		key.dim = dimension.dimension;
		key.x = pos.x;
		key.z = pos.z;
		key.random = MathUtils.RAND.nextInt();
		return key;
	}

	public double distToPlayer() {
		return MathUtils.distSq(pos.x * 512D + 256D, pos.z * 512D + 256D, Minecraft.getInstance().player.getX(), Minecraft.getInstance().player.getZ());
	}

	@Override
	public String toString() {
		return pos.toRegionString();
	}
}