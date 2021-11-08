package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.ColorMapLoader;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.core.BlockStateFTBC;
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
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * @author LatvianModder
 */
public class ChunkUpdateTask implements MapTask, BiomeManager.NoiseBiomeSource {
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
	public ChunkBiomeContainer biomeContainer;
	public final long biomeZoomSeed;
	public final int[] blocksToUpdate;
	private final long taskStartTime;

	private int currentWaterY = Integer.MAX_VALUE;

	public ChunkUpdateTask(@Nullable MapManager m, Level w, ChunkAccess ca, ChunkPos p, @Nullable ChunkBiomeContainer b, long zs, int[] s) {
		manager = m;
		level = w;
		chunkAccess = ca;
		pos = p;
		biomeContainer = b;
		biomeZoomSeed = zs;
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

		if (biomeContainer == null) {
			biomeContainer = chunkAccess.getBiomes();
		}

		if (biomeContainer == null) {
			return;
		}

		// BiomeManager biomeManager = new BiomeManager(this, biomeZoomSeed, level.dimensionType().getBiomeZoomer());

		int topY = level.getHeight() + 1;
		BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
		int blockX = pos.getMinBlockX();
		int blockZ = pos.getMinBlockZ();

		boolean changed = false;
		boolean versionChange = mapChunk.version != MapChunk.VERSION;

		if (versionChange) {
			mapChunk.version = MapChunk.VERSION;
			changed = true;
		}

		for (int wi : blocksToUpdate) {
			int wx = wi % 16;
			int wz = wi / 16;
			blockPos.set(blockX + wx, topY, blockZ + wz);
			int height = Mth.clamp(getHeight(blockPos).getY(), Short.MIN_VALUE, Short.MAX_VALUE);
			blockPos.setY(height);
			BlockState state = chunkAccess.getBlockState(blockPos);

			int ax = mapChunk.pos.x * 16 + wx;
			int az = mapChunk.pos.z * 16 + wz;
			int index = ax + az * 512;

			int waterLightAndBiome0 = data.waterLightAndBiome[index] & 0xFFFF;
			int blockIndex0 = data.getBlockIndex(index);
			int height0 = data.height[index] & 0xFFFF; // Get old height

			blockPos.setY(currentWaterY == Integer.MAX_VALUE ? height : currentWaterY);

			int waterLightAndBiome = (waterLightAndBiome0 & 0b111_11111111); // Clear water and light bits
			waterLightAndBiome |= (currentWaterY != Integer.MAX_VALUE) ? (1 << 15) : 0; // Water
			waterLightAndBiome |= (level.getBrightness(LightLayer.BLOCK, blockPos) & 15) << 11; // Light

			ResourceLocation id = FTBChunks.BLOCK_REGISTRY.getId(state.getBlock());
			int blockIndex = manager.getBlockColorIndex(id == null ? AIR : id);

			// Biome biome = biomeManager.getBiome(blockPos);
			Biome biome = getNoiseBiome(blockPos.getX() >> 2, blockPos.getY() >> 2, blockPos.getZ() >> 2);
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

			currentWaterY = Integer.MAX_VALUE;
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

	private boolean isWater(BlockState state) {
		if (state.getBlock() == Blocks.WATER) {
			return true;
		}

		return state instanceof BlockStateFTBC ? ((BlockStateFTBC) state).getFTBCIsWater() : state.getFluidState().getType().isSame(Fluids.WATER);
	}

	private boolean skipBlock(BlockState state) {
		if (state.isAir()) {
			return true;
		}

		ResourceLocation id = FTBChunks.BLOCK_REGISTRY.getId(state.getBlock());
		return id == null || ColorMapLoader.getBlockColor(id).isIgnored();
	}

	private BlockPos.MutableBlockPos getHeight(BlockPos.MutableBlockPos pos) {
		int topY = pos.getY();

		if (topY == -1) {
			pos.setY(-1);
			return pos;
		}

		for (int by = topY; by > 0; by--) {
			pos.setY(by);
			BlockState state = chunkAccess.getBlockState(pos);

			if (by == topY || state.getBlock() == Blocks.BEDROCK) {
				for (; by > 0; by--) {
					pos.setY(by);
					state = chunkAccess.getBlockState(pos);

					if (state.isAir()) {
						break;
					}
				}
			}

			boolean water = isWater(state);

			if (water && currentWaterY == Integer.MAX_VALUE) {
				currentWaterY = by;
			}

			if (!water && !skipBlock(state)) {
				pos.setY(by);
				return pos;
			}
		}

		pos.setY(-1);
		return pos;
	}

	@Override
	public Biome getNoiseBiome(int x, int y, int z) {
		int cx = x >> 2;
		int cz = z >> 2;

		if (cx == pos.x && cz == pos.z) {
			return biomeContainer.getNoiseBiome(x, y, z);
		}

		return level.getNoiseBiome(x, y, z);
	}
}