package dev.ftb.mods.ftbchunks.client.map;

import com.google.common.collect.ImmutableList;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.integration.RefreshMinimapIconsEvent;
import dev.ftb.mods.ftblibrary.math.XZ;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.DeflaterOutputStream;

/**
 * @author LatvianModder
 */
public class MapDimension implements MapTask {
	private static final Logger LOGGER = LogManager.getLogger();
	private static MapDimension current;

	@Nullable
	public static MapDimension getCurrent() {
		if (current == null) {
			if (MapManager.inst == null) {
				LOGGER.warn("Attempted to access MapManger before it's setup");
				return null;
			}
			current = MapManager.inst.getDimension(Minecraft.getInstance().level.dimension());
		}

		return current;
	}

	public static void updateCurrent() {
		current = null;
	}

	public final MapManager manager;
	public final ResourceKey<Level> dimension;
	public final String safeDimensionId;
	public final Path directory;
	private Map<XZ, MapRegion> regions;
	private WaypointManager waypointManager;
	public boolean saveData;
	public Long2IntMap loadedChunkView;

	public MapDimension(MapManager m, ResourceKey<Level> id) {
		manager = m;
		dimension = id;
		safeDimensionId = dimension.location().toString().replace(':', '_');
		directory = manager.directory.resolve(safeDimensionId);
		saveData = false;
		loadedChunkView = new Long2IntOpenHashMap();
	}

	@Override
	public String toString() {
		return safeDimensionId;
	}

	public Collection<MapRegion> getLoadedRegions() {
		return regions == null ? Collections.emptyList() : regions.values();
	}

	public MapDimension created() {
		manager.saveData = true;
		return this;
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
					saveData = true;
				}
			}

			return regions;
		}
	}

	public int[] getColors(int x, int z, ColorsFromRegion colors) {
		MapRegion region = getRegions().get(XZ.of(x, z));
		return region == null ? null : colors.getColors(region.getDataBlocking());
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

	public WaypointManager getWaypointManager() {
		if (waypointManager == null) {
			waypointManager = WaypointManager.fromJson(this);
			RefreshMinimapIconsEvent.trigger();
		}
		return waypointManager;
	}

	public void release() {
		for (MapRegion region : getLoadedRegions()) {
			region.release();
		}

		regions = null;
		waypointManager = null;
	}

	@Override
	public void runMapTask() throws Exception {
		List<Waypoint> waypoints = ImmutableList.copyOf(getWaypointManager());
		List<MapRegion> regionList = ImmutableList.copyOf(getRegions().values());

		if (!waypoints.isEmpty() || !regionList.isEmpty()) {
			FTBChunks.EXECUTOR.execute(() -> {
				try {
					writeData(waypoints, regionList);
				} catch (Exception ex) {
					FTBChunks.LOGGER.error("Failed to write map dimension " + safeDimensionId + ":");
					ex.printStackTrace();
				}
			});
		}
	}

	private void writeData(List<Waypoint> waypoints, List<MapRegion> regionList) throws IOException {
		WaypointManager.writeJson(this, waypoints);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(baos)))) {
			stream.writeByte(0);
			stream.writeByte(1);
			stream.writeShort(regionList.size());

			for (MapRegion region : regionList) {
				stream.writeByte(region.pos.x);
				stream.writeByte(region.pos.z);
			}
		}

		Files.write(directory.resolve("dimension.regions"), baos.toByteArray());
	}

	public void sync() {
		long now = System.currentTimeMillis();
		getRegions().values().stream().sorted(Comparator.comparingDouble(MapRegion::distToPlayer)).forEach(region -> FTBChunksClient.queue(new SyncTXTask(region, now)));
	}
}
