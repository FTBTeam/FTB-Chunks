package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.ClientTaskQueue;
import dev.ftb.mods.ftbchunks.client.ColorMapLoader;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.map.color.BlockColor;
import dev.ftb.mods.ftbchunks.client.map.color.BlockColors;
import dev.ftb.mods.ftbchunks.config.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.core.BiomeFTBC;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.platform.Platform;
import dev.ftb.mods.ftbteams.api.Team;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.IdentifierException;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class MapManager implements MapTask {
	// 4MB per 512x512 region data:
	// - 0.5MB each for heightmap and water/light/biome map
	// - 1MB each for foliage, grass & water
	public static final long MEMORY_PER_REGION = 4L * 1024L * 1024L;

	@Nullable
	private static MapManager instance;

	public final Object lock = new Object();

	private boolean invalid;
	private final UUID serverId;
	private final Path directory;
	private final Map<ResourceKey<Level>, MapDimension> dimensions;

	private boolean needsSave;
	@Nullable
	private MapDimension pendingRegionPurge = null;

	private final Int2ObjectOpenHashMap<Identifier> blockColorIndexMap;
	private final Object2IntOpenHashMap<Identifier> blockColorIndexMapReverse;
	private final Int2ObjectOpenHashMap<ResourceKey<Biome>> biomeColorIndexMap;
	private final Int2ObjectOpenHashMap<BlockColor> blockIdToColCache;
	private final List<BiomeFTBC> biomesToRelease;
	private final PendingUpdateEvents pendingUpdateEvents;

	private MapManager(UUID serverId, Path directory) {
		this.serverId = serverId;
		this.directory = directory;

		invalid = false;
		dimensions = new LinkedHashMap<>();
		needsSave = false;

		blockColorIndexMap = new Int2ObjectOpenHashMap<>();
		blockColorIndexMap.defaultReturnValue(Identifier.fromNamespaceAndPath("minecraft", "air"));
		blockColorIndexMapReverse = new Object2IntOpenHashMap<>();
		blockColorIndexMapReverse.defaultReturnValue(0);
		biomeColorIndexMap = new Int2ObjectOpenHashMap<>();
		biomeColorIndexMap.defaultReturnValue(Biomes.PLAINS);

		blockIdToColCache = new Int2ObjectOpenHashMap<>();

		biomesToRelease = new ArrayList<>();

		pendingUpdateEvents = new PendingUpdateEvents();

		try {
			Path dimFile = this.directory.resolve("dimensions.txt");
			if (Files.exists(dimFile)) {
				for (String s : Files.readAllLines(dimFile)) {
					s = s.trim();
					if (s.length() >= 3) {
						try {
							Identifier dimId = Identifier.parse(s);
							ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimId);
							dimensions.put(key, new MapDimension(this, key, directory));
						} catch (IdentifierException e) {
							FTBChunks.LOGGER.warn("skipping invalid entry '{}' in dimensions.txt", s);
						}
                    }
				}
			} else {
				needsSave = true;
			}

			Path blockFile = this.directory.resolve("block_map.txt");
			if (Files.exists(blockFile)) {
				for (String s : Files.readAllLines(blockFile)) {
					s = s.trim();
					if (!s.isEmpty()) {
						String[] s1 = s.split(" ", 2);
						try {
							int i = Integer.decode(s1[0]);
							Identifier loc = Identifier.parse(s1[1]);
							blockColorIndexMap.put(i, loc);
							blockColorIndexMapReverse.put(loc, i);
						} catch (NumberFormatException | IdentifierException e) {
							FTBChunks.LOGGER.warn("skipping invalid entry '{}' in block_map.txt", s);
						}
					}
				}
			} else {
				needsSave = true;
			}

			Path biomeFile = this.directory.resolve("biome_map.txt");
			if (Files.exists(biomeFile)) {
				for (String s : Files.readAllLines(biomeFile)) {
					s = s.trim();
					if (!s.isEmpty()) {
						String[] s1 = s.split(" ", 2);
						try {
							int i = Integer.decode(s1[0]);
							Identifier loc = Identifier.parse(s1[1]);
							ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, loc);
							biomeColorIndexMap.put(i, key);
						} catch (NumberFormatException | IdentifierException e) {
							FTBChunks.LOGGER.warn("skipping invalid entry '{}' in biome_map.txt", s);
						}
                    }
				}
			} else {
				needsSave = true;
			}
		} catch (Exception ex) {
			FTBChunks.LOGGER.error("errors detecting loading map manager data from {}", directory);
			ex.printStackTrace();
		}
	}

	public static Optional<MapManager> getInstance() {
		return Optional.ofNullable(instance);
	}

	public static void startUp(UUID serverId) {
		// Ensure if existing manager instance is cleaned up first
		// Necessary if player is switching servers, e.g. with Velocity proxy or similar
		shutdown();

		Path dir = Platform.get().paths().gamePath().resolve("local/ftbchunks/data/" + serverId);
		if (Files.notExists(dir)) {
			try {
				Files.createDirectories(dir);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		instance = new MapManager(serverId, dir);
	}

	public static void shutdown() {
		if (instance != null) {
			instance.saveAllRegions();
			instance.release();
			ClientTaskQueue.flushTasks();
			MapDimension.clearCurrentDimension();
			instance = null;
		}
	}

	public UUID getServerId() {
		return serverId;
	}

	public void saveAllRegions() {
		if (MapManager.instance == null) {
			return;
		}

		for (MapDimension dimension : MapManager.instance.getDimensions().values()) {
			dimension.getLoadedRegions().forEach(MapRegion::saveIfChanged);
			dimension.saveIfChanged();
		}

		if (MapManager.instance.needsSave) {
			ClientTaskQueue.queue(MapManager.instance);
			MapManager.instance.needsSave = false;
		}
	}

	public Map<ResourceKey<Level>, MapDimension> getDimensions() {
		synchronized (lock) {
			return dimensions;
		}
	}

	public synchronized MapDimension getDimension(ResourceKey<Level> dim) {
		return getDimensions().computeIfAbsent(dim, dimKey -> {
			needsSave = true;
			return new MapDimension(this, dimKey, directory);
		});
	}

	public boolean isInvalid() {
		return invalid;
	}

	public void release() {
		for (MapDimension dimension : getDimensions().values()) {
			dimension.release();
		}

		for (BiomeFTBC b : biomesToRelease) {
			b.ftbc$setBiomeColorIndex(-1);
		}

		biomesToRelease.clear();
		blockIdToColCache.clear();
		invalid = true;
	}

	public void updateAllRegions(boolean save) {
		for (MapDimension dimension : getDimensions().values()) {
			for (MapRegion region : dimension.getRegions().values()) {
				region.update(save);
			}
		}

		FTBChunksClient.INSTANCE.getMinimapRenderer().requestTextureRefresh();
	}

	@Override
	public void runMapTask() throws Exception {
		List<String> dimensionsList = dimensions
				.keySet()
				.stream()
				.map(key -> key.identifier().toString())
				.collect(Collectors.toList());

		List<String> blockColorIndexMapList = blockColorIndexMap
				.int2ObjectEntrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue())
				.map(key -> String.format("#%06X %s", key.getIntKey(), key.getValue()))
				.collect(Collectors.toList());

		List<String> biomeColorIndexMapList = biomeColorIndexMap
				.int2ObjectEntrySet()
				.stream()
				.sorted(Comparator.comparing(o -> o.getValue().identifier()))
				.map(key -> String.format("#%03X %s", key.getIntKey(), key.getValue().identifier()))
				.collect(Collectors.toList());

		FTBChunksClient.MAP_EXECUTOR.execute(() -> {
			try {
				Files.write(directory.resolve("dimensions.txt"), dimensionsList);
				Files.write(directory.resolve("block_map.txt"), blockColorIndexMapList);
				Files.write(directory.resolve("biome_map.txt"), biomeColorIndexMapList);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
	}

	public int getBlockColorIndex(Identifier id) {
		int i = blockColorIndexMapReverse.getInt(id);

		if (i == 0) {
			Random random = new Random((long) id.getNamespace().hashCode() & 4294967295L | ((long) id.getPath().hashCode() & 4294967295L) << 32);
			i = id.hashCode() & 0xFFFFFF;

			while (i == 0 || blockColorIndexMap.containsKey(i)) {
				i = random.nextInt() & 0xFFFFFF;
			}

			blockColorIndexMap.put(i, id);
			blockColorIndexMapReverse.put(id, i);
			needsSave = true;
		}

		return i;
	}

	public int getBiomeColorIndex(Registry<Biome> biomes, Biome biome, Object b0) {
		BiomeFTBC b = b0 instanceof BiomeFTBC ? (BiomeFTBC) b0 : null;

		if (b == null) {
			return 0;
		}

		int i = b.ftbc$getBiomeColorIndex();

		if (i == -1) {
			ResourceKey<Biome> key = biomes.getResourceKey(biome).orElse(null);

			if (key == null) {
				b.ftbc$setBiomeColorIndex(0);
				return 0;
			}

			for (Int2ObjectOpenHashMap.Entry<ResourceKey<Biome>> entry : biomeColorIndexMap.int2ObjectEntrySet()) {
				if (entry.getValue() == key) {
					i = entry.getIntKey();
					b.ftbc$setBiomeColorIndex(i);
					return i;
				}
			}

			Random random = new Random((long) key.identifier().getNamespace().hashCode() & 4294967295L | ((long) key.identifier().getPath().hashCode() & 4294967295L) << 32);
			i = key.identifier().hashCode() & 0b111_11111111;

			while (i == 0 || biomeColorIndexMap.containsKey(i)) {
				i = random.nextInt() & 0b111_11111111;
			}

			biomeColorIndexMap.put(i, key);
			b.ftbc$setBiomeColorIndex(i);
			needsSave = true;
			biomesToRelease.add(b);
		}

		return i;
	}

	public Block getBlock(int id) {
		Identifier rl = blockColorIndexMap.get(id & 0xFFFFFF);
		return BuiltInRegistries.BLOCK.get(rl)
				.map(Holder.Reference::value)
				.orElse(Blocks.AIR);
	}

	public BlockColor getBlockColor(int id) {
		try {
			return blockIdToColCache.computeIfAbsent(id & 0xFFFFFF, i -> ColorMapLoader.getBlockColor(blockColorIndexMap.get(i)));
		} catch (Exception ex) {
			ex.printStackTrace();
			return BlockColors.ERROR;
		}
	}

	public ResourceKey<Biome> getBiomeKey(int id) {
		return biomeColorIndexMap.get(id & 0b111_11111111);
	}

	public void releaseStaleRegionData(long releaseIntervalMillis) {
		long now = System.currentTimeMillis();
		dimensions.values().forEach(dim -> dim.releaseStaleRegionData(now, releaseIntervalMillis));
	}

	public long estimateMemoryUsage() {
		long memory = 0L;

		for (MapDimension dim : dimensions.values()) {
			memory += dim.getLoadedRegions().stream().filter(MapRegion::isDataLoaded).mapToLong(region -> MEMORY_PER_REGION).sum();
		}

		return memory;
	}

	public void scheduleRegionPurge(MapDimension toPurge) {
		pendingRegionPurge = toPurge;
	}

	public void checkForRegionPurge() {
		if (pendingRegionPurge != null) {
			synchronized (lock) {
				int autoRelease = FTBChunksClientConfig.AUTORELEASE_ON_MAP_CLOSE.get();
				List<MapRegion> dataLoadedRegions = pendingRegionPurge.getLoadedRegions().stream().filter(MapRegion::isDataLoaded).toList();
				long nLoaded = dataLoadedRegions.size();
				autoRelease = Math.max(4, autoRelease);  // not useful to release regions which will be reloaded pretty much immediately
				if (nLoaded > autoRelease) {
					dataLoadedRegions.stream()
							.sorted(Comparator.comparingLong(MapRegion::getLastDataAccess))
							.limit(nLoaded - autoRelease)
							.forEach(r -> r.release(false));
				}
				pendingRegionPurge = null;
			}
		}
	}

	public void addPendingUpdateEvent(Team team, ChunkDimPos dim, MapChunk.DateInfo dateInfo) {
		pendingUpdateEvents.addPending(team, dim, dateInfo);
	}

	public void firePendingUpdateEvents() {
		pendingUpdateEvents.fireEvents();
	}
}
