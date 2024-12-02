package dev.ftb.mods.ftbchunks.client.map;

import com.google.common.collect.ImmutableList;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.ClientTaskQueue;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftblibrary.math.XZ;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.DeflaterOutputStream;

public class MapDimension implements MapTask {
	private static final Logger LOGGER = LogManager.getLogger();
	private static MapDimension currentDimension;

	private final MapManager manager;
	public final ResourceKey<Level> dimension;
	private final String safeDimensionId;
	public final Path directory;

	private Map<XZ, MapRegion> regions;
	private WaypointManagerImpl waypointManager;
	private boolean needsSave;
	private Long2IntMap loadedChunkView;

	MapDimension(MapManager manager, ResourceKey<Level> dimension, Path directory) {
		this.manager = manager;
		this.dimension = dimension;

		safeDimensionId = this.dimension.location().toString().replace(':', '_');
		this.directory = directory.resolve(safeDimensionId);
		needsSave = false;
		loadedChunkView = new Long2IntOpenHashMap();
	}

	public MapManager getManager() {
		return manager;
	}

	public static void clearCurrentDimension() {
		currentDimension = null;
	}

	public static Optional<MapDimension> getCurrent() {
		if (currentDimension == null) {
			if (MapManager.getInstance().isEmpty()) {
				LOGGER.warn("Attempted to access MapManager before it was setup!");
				return Optional.empty();
			}
			ClientLevel level = Minecraft.getInstance().level;
			if (level == null) {
				return Optional.empty();
			}
			currentDimension = MapManager.getInstance()
					.map(m -> m.getDimension(level.dimension()))
					.orElse(null);
		}

		return Optional.ofNullable(currentDimension);
	}

	@Override
	public String toString() {
		return safeDimensionId;
	}

	public Collection<MapRegion> getLoadedRegions() {
		return regions == null ? Collections.emptyList() : regions.values();
	}

	public int getLoadedView(MapRegion region, int cx, int cz) {
		return loadedChunkView.get(ChunkPos.asLong((region.pos.x() << 5) + cx, (region.pos.z() << 5) + cz));
	}

	public Map<XZ, MapRegion> getRegions() {
		synchronized (manager.lock) {
			if (regions == null) {
				regions = new HashMap<>();

				if (Files.notExists(directory)) {
					try {
						Files.createDirectories(directory);
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}

				if (!MapIOUtils.read(directory.resolve("dimension.regions"), stream -> {
					stream.readByte();
					int version = stream.readByte();
					int s = stream.readShort();

					for (int i = 0; i < s; i++) {
						int x = stream.readByte();
						int z = stream.readByte();

						MapRegion c = new MapRegion(this, XZ.of(x, z));
						regions.put(c.pos, c);
					}
				})) {
					needsSave = true;
				}
			}

			return regions;
		}
	}

	public int[] getColors(int x, int z, ColorsFromRegion colors) {
		MapRegion region = getRegions().get(XZ.of(x, z));
		if (region == null) {
			return null;
		} else {
			MapRegionData data = region.getDataBlocking();
			return colors.getColors(data);
		}
	}

	public MapRegion getRegion(XZ pos) {
		synchronized (manager.lock) {
			Map<XZ, MapRegion> map = getRegions();
			MapRegion region = map.get(pos);

			if (region == null) {
				region = new MapRegion(this, pos);
				region.created();
				map.put(pos, region);
			}

			return region;
		}
	}

	public WaypointManagerImpl getWaypointManager() {
		if (waypointManager == null) {
			waypointManager = WaypointManagerImpl.fromJson(this);
			FTBChunksAPI.clientApi().requestMinimapIconRefresh();
		}
		return waypointManager;
	}

	public void release() {
		for (MapRegion region : getLoadedRegions()) {
			region.release(true);
		}

		regions = null;
		waypointManager = null;
	}

	@Override
	public void runMapTask() throws Exception {
		List<WaypointImpl> waypoints = ImmutableList.copyOf(getWaypointManager());
		List<MapRegion> regionList = ImmutableList.copyOf(getRegions().values());

		if (!waypoints.isEmpty() || !regionList.isEmpty()) {
			FTBChunksClient.MAP_EXECUTOR.execute(() -> {
				try {
					writeData(waypoints, regionList);
				} catch (Exception ex) {
					FTBChunks.LOGGER.error("Failed to write map dimension " + this + ":");
					ex.printStackTrace();
				}
			});
		}
	}

	private void writeData(List<WaypointImpl> waypoints, List<MapRegion> regionList) throws IOException {
		WaypointManagerImpl.writeJson(this, waypoints);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(baos)))) {
			stream.writeByte(0);
			stream.writeByte(1);
			stream.writeShort(regionList.size());

			for (MapRegion region : regionList) {
				stream.writeByte(region.pos.x());
				stream.writeByte(region.pos.z());
			}
		}

		Files.write(directory.resolve("dimension.regions"), baos.toByteArray());
	}

	public void sync() {
		long now = System.currentTimeMillis();
		getRegions().values().stream().sorted(Comparator.comparingDouble(MapRegion::distToPlayer)).forEach(region -> ClientTaskQueue.queue(new SyncTXTask(region, now)));
	}

	public void releaseStaleRegionData(long now, long releaseIntervalMillis) {
		if (regions != null) {
			regions.values().forEach(region -> region.releaseIfStale(now, releaseIntervalMillis));
		}
	}

	public void saveIfChanged() {
		if (needsSave) {
			ClientTaskQueue.queue(this);
			needsSave = false;
		}
	}

	public void markDirty() {
		needsSave = true;
	}

	public void updateLoadedChunkView(Long2IntMap chunks) {
		loadedChunkView = chunks;
	}
}
