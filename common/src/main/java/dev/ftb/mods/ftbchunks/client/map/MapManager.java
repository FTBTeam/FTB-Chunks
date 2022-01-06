package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.ColorMapLoader;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.map.color.BlockColor;
import dev.ftb.mods.ftbchunks.client.map.color.BlockColors;
import dev.ftb.mods.ftbchunks.core.BiomeFTBC;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class MapManager implements MapTask {
	public static MapManager inst;

	public final Object lock = new Object();
	public boolean invalid;
	public final UUID serverId;
	public final Path directory;
	private final Map<ResourceKey<Level>, MapDimension> dimensions;
	public boolean saveData;

	private final Int2ObjectOpenHashMap<ResourceLocation> blockColorIndexMap;
	private final Object2IntOpenHashMap<ResourceLocation> blockColorIndexMapReverse;
	private final Int2ObjectOpenHashMap<ResourceKey<Biome>> biomeColorIndexMap;
	private final Int2ObjectOpenHashMap<BlockColor> blockIdToColCache;
	private final List<BiomeFTBC> biomesToRelease;

	public MapManager(UUID id, Path dir) {
		invalid = false;
		serverId = id;
		directory = dir;
		dimensions = new LinkedHashMap<>();
		saveData = false;

		blockColorIndexMap = new Int2ObjectOpenHashMap<>();
		blockColorIndexMap.defaultReturnValue(new ResourceLocation("minecraft:air"));
		blockColorIndexMapReverse = new Object2IntOpenHashMap<>();
		blockColorIndexMapReverse.defaultReturnValue(0);
		biomeColorIndexMap = new Int2ObjectOpenHashMap<>();
		biomeColorIndexMap.defaultReturnValue(Biomes.PLAINS);

		blockIdToColCache = new Int2ObjectOpenHashMap<>();

		biomesToRelease = new ArrayList<>();

		try {
			Path dimFile = directory.resolve("dimensions.txt");

			if (Files.exists(dimFile)) {
				for (String s : Files.readAllLines(dimFile)) {
					s = s.trim();

					if (s.length() >= 3) {
						ResourceKey<Level> key = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(s));
						dimensions.put(key, new MapDimension(this, key));
					}
				}
			} else {
				saveData = true;
			}

			Path blockFile = directory.resolve("block_map.txt");

			if (Files.exists(blockFile)) {
				for (String s : Files.readAllLines(blockFile)) {
					s = s.trim();

					if (!s.isEmpty()) {
						String[] s1 = s.split(" ", 2);
						int i = Integer.decode(s1[0]);
						ResourceLocation loc = new ResourceLocation(s1[1]);
						blockColorIndexMap.put(i, loc);
						blockColorIndexMapReverse.put(loc, i);
					}
				}
			} else {
				saveData = true;
			}

			Path biomeFile = directory.resolve("biome_map.txt");

			if (Files.exists(biomeFile)) {
				for (String s : Files.readAllLines(biomeFile)) {
					s = s.trim();

					if (!s.isEmpty()) {
						String[] s1 = s.split(" ", 2);
						int i = Integer.decode(s1[0]);
						ResourceLocation loc = new ResourceLocation(s1[1]);
						ResourceKey<Biome> key = ResourceKey.create(Registry.BIOME_REGISTRY, loc);
						biomeColorIndexMap.put(i, key);
					}
				}
			} else {
				saveData = true;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public Map<ResourceKey<Level>, MapDimension> getDimensions() {
		synchronized (lock) {
			return dimensions;
		}
	}

	public MapDimension getDimension(ResourceKey<Level> dim) {
		return getDimensions().computeIfAbsent(dim, d -> new MapDimension(this, d).created());
	}

	public void release() {
		for (MapDimension dimension : getDimensions().values()) {
			dimension.release();
		}

		for (BiomeFTBC b : biomesToRelease) {
			b.setFTBCBiomeColorIndex(-1);
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

		FTBChunksClient.updateMinimap = true;
	}

	@Override
	public void runMapTask() throws Exception {
		List<String> dimensionsList = dimensions
				.keySet()
				.stream()
				.map(key -> key.location().toString())
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
				.sorted(Comparator.comparing(o -> o.getValue().location()))
				.map(key -> String.format("#%03X %s", key.getIntKey(), key.getValue().location()))
				.collect(Collectors.toList());

		FTBChunks.EXECUTOR.execute(() -> {
			try {
				Files.write(directory.resolve("dimensions.txt"), dimensionsList);
				Files.write(directory.resolve("block_map.txt"), blockColorIndexMapList);
				Files.write(directory.resolve("biome_map.txt"), biomeColorIndexMapList);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
	}

	public int getBlockColorIndex(ResourceLocation id) {
		int i = blockColorIndexMapReverse.getInt(id);

		if (i == 0) {
			Random random = new Random((long) id.getNamespace().hashCode() & 4294967295L | ((long) id.getPath().hashCode() & 4294967295L) << 32);
			i = id.hashCode() & 0xFFFFFF;

			while (i == 0 || blockColorIndexMap.containsKey(i)) {
				i = random.nextInt() & 0xFFFFFF;
			}

			blockColorIndexMap.put(i, id);
			blockColorIndexMapReverse.put(id, i);
			saveData = true;
		}

		return i;
	}

	public int getBiomeColorIndex(Registry<Biome> biomes, Biome biome, Object b0) {
		BiomeFTBC b = b0 instanceof BiomeFTBC ? (BiomeFTBC) b0 : null;

		if (b == null) {
			return 0;
		}

		int i = b.getFTBCBiomeColorIndex();

		if (i == -1) {
			ResourceKey<Biome> key = biomes.getResourceKey(biome).orElse(null);

			if (key == null) {
				b.setFTBCBiomeColorIndex(0);
				return 0;
			}

			for (Int2ObjectOpenHashMap.Entry<ResourceKey<Biome>> entry : biomeColorIndexMap.int2ObjectEntrySet()) {
				if (entry.getValue() == key) {
					i = entry.getIntKey();
					b.setFTBCBiomeColorIndex(i);
					return i;
				}
			}

			Random random = new Random((long) key.location().getNamespace().hashCode() & 4294967295L | ((long) key.location().getPath().hashCode() & 4294967295L) << 32);
			i = key.location().hashCode() & 0b111_11111111;

			while (i == 0 || biomeColorIndexMap.containsKey(i)) {
				i = random.nextInt() & 0b111_11111111;
			}

			biomeColorIndexMap.put(i, key);
			b.setFTBCBiomeColorIndex(i);
			saveData = true;
			biomesToRelease.add(b);
		}

		return i;
	}

	public Block getBlock(int id) {
		ResourceLocation rl = blockColorIndexMap.get(id & 0xFFFFFF);
		Block block = rl == null ? null : FTBChunks.BLOCK_REGISTRY.get(rl);
		return block == null ? Blocks.AIR : block;
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

	public Biome getBiome(Level level, int id) {
		return level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).get(getBiomeKey(id));
	}
}