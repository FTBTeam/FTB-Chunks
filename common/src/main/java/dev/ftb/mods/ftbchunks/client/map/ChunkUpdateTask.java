package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.util.HeightUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import net.minecraft.Util;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

public class ChunkUpdateTask implements MapTask, BiomeManager.NoiseBiomeSource {
	private static final int[] ALL_BLOCKS = Util.make(new int[256], array -> {
		for (int i = 0; i < 256; i++) {
			array[i] = i;
		}
	});
	private static final ResourceLocation AIR = ResourceLocation.fromNamespaceAndPath("minecraft", "air");
	private static long debugLastTime = 0L;

	private MapManager manager;
	private final Level level;
	private final ChunkAccess chunkAccess;
	private final ChunkPos chunkPos;
	private final int[] blocksToUpdate;
	private final long taskStartTime;

	public ChunkUpdateTask(@Nullable MapManager manager, Level level, ChunkAccess chunkAccess, ChunkPos chunkPos) {
		this(manager, level, chunkAccess, chunkPos, ALL_BLOCKS);
	}

	public ChunkUpdateTask(@Nullable MapManager manager, Level level, ChunkAccess chunkAccess, ChunkPos chunkPos, int[] blocksToUpdate) {
		this.manager = manager;
		this.level = level;
		this.chunkAccess = chunkAccess;
		this.chunkPos = chunkPos;
		this.blocksToUpdate = blocksToUpdate;

		taskStartTime = System.currentTimeMillis();
	}

	public static void init() {
		debugLastTime = 0L;
	}

	public static long getDebugLastTime() {
		return debugLastTime;
	}

	@Override
	public void runMapTask() throws Exception {
		while (manager == null) {
			manager = MapManager.getInstance().orElse(null);
			if (manager == null) {
				// Safety mechanic in case for some reason the map task hangs
				if ((System.currentTimeMillis() - taskStartTime) >= 30000L) {
					return;
				} else {
					Thread.sleep(1L);
				}
			}
		}

		if (manager.isInvalid()) {
			return;
		}

		long startTime = System.nanoTime();

		ResourceKey<Level> dimId = level.dimension();

		MapChunk mapChunk = manager.getDimension(dimId).getRegion(XZ.regionFromChunk(chunkPos)).getDataBlocking().getChunk(XZ.of(chunkPos));
		MapRegionData data = mapChunk.getRegionData();

		Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registries.BIOME);

		BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
		int blockX = chunkPos.getMinBlockX();
		int blockZ = chunkPos.getMinBlockZ();

		boolean changed = false;
		boolean versionChange = mapChunk.getVersion() != MapChunk.VERSION;

		if (versionChange) {
			mapChunk.setVersion(MapChunk.VERSION);
			changed = true;
		}

		for (int wi : blocksToUpdate) {
			int wx = wi % 16;
			int wz = wi / 16;
			blockPos.set(blockX + wx, chunkAccess.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX + wx, blockZ + wz) + 2, blockZ + wz);
			int waterY = Mth.clamp(HeightUtils.getHeight(level, chunkAccess, blockPos), Short.MIN_VALUE, Short.MAX_VALUE);
			int height = blockPos.getY();
			BlockState state = chunkAccess.getBlockState(blockPos);

			int ax = mapChunk.getPos().x() * 16 + wx;
			int az = mapChunk.getPos().z() * 16 + wz;
			int index = ax + az * 512;

			int waterLightAndBiome0 = data.waterLightAndBiome[index] & 0xFFFF;
			int blockIndex0 = data.getBlockIndex(index);
			int height0 = data.height[index]; // Get old height

			blockPos.setY(waterY == HeightUtils.UNKNOWN ? height : waterY);

			int waterLightAndBiome = (waterLightAndBiome0 & 0b111_11111111); // Clear water and light bits
			waterLightAndBiome |= (waterY != HeightUtils.UNKNOWN) ? (1 << 15) : 0; // Water
			waterLightAndBiome |= (level.getBrightness(LightLayer.BLOCK, blockPos) & 15) << 11; // Light

			// state shouldn't ever be null here, but yay threads
			// https://github.com/FTBTeam/FTB-Mods-Issues/issues/811
			@SuppressWarnings("ConstantValue") ResourceLocation id = state == null ? AIR : FTBChunks.BLOCK_REGISTRY.getId(state.getBlock());
			int blockIndex = manager.getBlockColorIndex(id == null ? AIR : id);

			// Biome biome = biomeManager.getBiome(blockPos);
			Biome biome = getNoiseBiome(blockPos.getX() >> 2, blockPos.getY() >> 2, blockPos.getZ() >> 2).value();
			waterLightAndBiome &= 0b11111000_00000000; // Clear biome bits
			waterLightAndBiome |= (manager.getBiomeColorIndex(biomes, biome, biome) & 0b111_11111111); // Biome

			if (versionChange || height0 != height) {
				data.height[index] = (short) height;
				changed = true;
			}

			if (versionChange || waterLightAndBiome0 != waterLightAndBiome) {
				data.waterLightAndBiome[index] = (short) waterLightAndBiome;

				if (biome != null && (versionChange || (waterLightAndBiome0 & 0b111_11111111) != (waterLightAndBiome & 0b111_11111111))) {
					double cx = blockPos.getX();
					double cz = blockPos.getZ();
					data.foliage[index] = (data.foliage[index] & 0xFF000000) | (BiomeColors.FOLIAGE_COLOR_RESOLVER.getColor(biome, cx, cz) & 0xFFFFFF);
					data.grass[index] = (data.grass[index] & 0xFF000000) | (BiomeColors.GRASS_COLOR_RESOLVER.getColor(biome, cx, cz) & 0xFFFFFF);
					data.water[index] = (data.water[index] & 0xFF000000) | (BiomeColors.WATER_COLOR_RESOLVER.getColor(biome, cx, cz) & 0xFFFFFF);
				}

				changed = true;
			}

			if (versionChange || blockIndex0 != blockIndex) {
				data.setBlockIndex(index, blockIndex);
				changed = true;
			}
		}

		if (changed) {
			mapChunk.forceUpdate();
		}

		debugLastTime = System.nanoTime() - startTime;
	}

	@Override
	public String toString() {
		return "ChunkUpdateTask@" + chunkPos;
	}

	@Override
	public Holder<Biome> getNoiseBiome(int x, int y, int z) {
		if ((x >> 2) == chunkPos.x && (z >> 2) == chunkPos.z) {
			return chunkAccess.getNoiseBiome(x, y, z);
			// return biomeContainer.getNoiseBiome(x, y, z);
		}

		return level.getNoiseBiome(x, y, z);
	}
}
