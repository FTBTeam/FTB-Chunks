package dev.ftb.mods.ftbchunks.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

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

		try {
			data.read();
		} catch (IOException ex) {
			ex.printStackTrace();
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