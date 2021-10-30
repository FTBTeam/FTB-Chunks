package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftblibrary.math.XZ;
import net.minecraft.Util;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import org.jetbrains.annotations.Nullable;

/**
 * @author LatvianModder
 */
public class ChunkUpdateTask implements MapTask {
	public static final int[] ALL_BLOCKS = Util.make(new int[256], array -> {
		for (int i = 0; i < 256; i++) {
			array[i] = i;
		}
	});

	private static final ResourceLocation AIR = new ResourceLocation("minecraft:air");
	public static long debugLastTime = 0L;

	public MapManager manager;
	public final Level level;
	public final ChunkAccess chunkAccess;
	public final ChunkPos pos;
	public final int[] blocksToUpdate;
	private final long taskStartTime;

	public ChunkUpdateTask(@Nullable MapManager m, Level w, ChunkAccess ca, ChunkPos p, int[] s) {
		manager = m;
		level = w;
		chunkAccess = ca;
		pos = p;
		blocksToUpdate = s;
		taskStartTime = System.currentTimeMillis();
	}

	@Override
	public void runMapTask() throws Exception {
		while (manager == null) {
			manager = MapManager.inst;

			if (manager == null) {
				// Safety mechanic in case for some reason the map task hangs
				if ((System.currentTimeMillis() - taskStartTime) >= 30000L) {
					return;
				} else {
					Thread.sleep(1L);
				}
			}
		}

		if (manager.invalid) {
			return;
		}

		long startTime = System.nanoTime();

		ResourceKey<Level> dimId = level.dimension();

		MapChunk mapChunk = manager.getDimension(dimId).getRegion(XZ.regionFromChunk(pos)).getDataBlocking().getChunk(XZ.of(pos));
		MapRegionData data = mapChunk.region.getDataBlocking();

		WritableRegistry<Biome> biomes = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
		ChunkBiomeContainer biomeContainer = chunkAccess.getBiomes();

		int topY = level.getHeight() + 1;
		BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
		int blockX = pos.getMinBlockX();
		int blockZ = pos.getMinBlockZ();

		boolean changed = false;
		boolean[] flags = new boolean[1];

		for (int wi : blocksToUpdate) {
			int wx = wi % 16;
			int wz = wi / 16;
			blockPos.set(blockX + wx, topY, blockZ + wz);
			int height = Mth.clamp(MapChunk.getHeight(chunkAccess, blockPos, flags).getY(), Short.MIN_VALUE, Short.MAX_VALUE);
			blockPos.setY(height);
			BlockState state = chunkAccess.getBlockState(blockPos);

			int ax = mapChunk.pos.x * 16 + wx;
			int az = mapChunk.pos.z * 16 + wz;
			int index = ax + az * 512;

			int waterLightAndBiome0 = data.waterLightAndBiome[index] & 0xFFFF;
			int blockIndex0 = data.getBlockIndex(index);
			int height0 = data.height[index] & 0xFFFF; // Get old height

			int waterLightAndBiome = (waterLightAndBiome0 & 0b111_11111111); // Clear water and light bits
			waterLightAndBiome |= flags[0] ? (1 << 15) : 0; // Water
			waterLightAndBiome |= (level.getBrightness(LightLayer.BLOCK, blockPos.above()) & 15) << 11; // Light

			ResourceLocation id = FTBChunks.BLOCK_REGISTRY.getId(state.getBlock());
			int blockIndex = manager.getBlockColorIndex(id == null ? AIR : id);

			Biome biome;

			// Only update biome, foliage, grass and water colors if its first visit or height changed
			if (height0 != height || waterLightAndBiome0 == 0) {
				biome = biomeContainer == null ? level.getBiome(blockPos) : biomeContainer.getNoiseBiome(blockPos.getX() >> 2, blockPos.getY() >> 2, blockPos.getZ() >> 2);
				waterLightAndBiome &= 0b11111000_00000000; // Clear biome bits
				waterLightAndBiome |= (manager.getBiomeColorIndex(biomes, biome, biome) & 0b111_11111111); // Biome
			} else {
				biome = null;
			}

			if (height0 != height) {
				data.height[index] = (short) height;
				changed = true;
			}

			if (waterLightAndBiome0 != waterLightAndBiome) {
				data.waterLightAndBiome[index] = (short) waterLightAndBiome;

				if (biome != null && (waterLightAndBiome0 & 0b111_11111111) != (waterLightAndBiome & 0b111_11111111)) {
					data.foliage[index] = (data.foliage[index] & 0xFF000000) | (BiomeColors.getAverageFoliageColor(level, blockPos) & 0xFFFFFF);
					data.grass[index] = (data.grass[index] & 0xFF000000) | (BiomeColors.getAverageGrassColor(level, blockPos) & 0xFFFFFF);
					data.water[index] = (data.water[index] & 0xFF000000) | (BiomeColors.getAverageWaterColor(level, blockPos) & 0xFFFFFF);
				}

				changed = true;
			}

			if (blockIndex0 != blockIndex) {
				data.setBlockIndex(index, blockIndex);
				changed = true;
			}

			flags[0] = false;
		}

		if (changed) {
			mapChunk.modified = System.currentTimeMillis();
			mapChunk.region.update(true);
		}

		debugLastTime = System.nanoTime() - startTime;
	}

	@Override
	public String toString() {
		return "ChunkUpdateTask@" + pos;
	}
}