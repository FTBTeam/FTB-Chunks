package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftblibrary.math.XZ;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;

/**
 * @author LatvianModder
 */
public class ReloadChunkFromLevelPacketTask implements MapTask {
	private static final ResourceLocation AIR = new ResourceLocation("minecraft:air");
	public static long debugLastTime = 0L;
	public static long debugTotalTime = 0L;
	public static long debugTotalCount = 0L;

	public final Level level;
	public final ChunkAccess chunkAccess;
	public final ClientboundLevelChunkPacket packet;

	public ReloadChunkFromLevelPacketTask(Level l, ChunkAccess ca, ClientboundLevelChunkPacket p) {
		level = l;
		chunkAccess = ca;
		packet = p;
	}

	@Override
	public void runMapTask(MapManager manager) throws Exception {
		long startTime = System.nanoTime();

		MapDimension dimension = manager.getDimension(level.dimension());
		MapChunk mapChunk = dimension.getRegion(XZ.regionFromChunk(packet.getX(), packet.getZ())).getDataBlocking().getChunk(XZ.of(packet.getX(), packet.getZ()));

		ChunkPos pos = new ChunkPos(packet.getX(), packet.getZ());
		MapRegionData data = mapChunk.region.getDataBlocking();

		WritableRegistry<Biome> biomes = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
		ChunkBiomeContainer biomeContainer = chunkAccess.getBiomes();

		int topY = level.getHeight() + 1;
		BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
		int blockX = pos.getMinBlockX();
		int blockZ = pos.getMinBlockZ();

		boolean changed = false;
		boolean[] flags = new boolean[1];

		for (int wi = 0; wi < 256; wi++) {
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
			int blockIndex = mapChunk.region.dimension.manager.getBlockColorIndex(id == null ? AIR : id);

			Biome biome = biomeContainer == null ? level.getBiome(blockPos) : biomeContainer.getNoiseBiome(blockPos.getX() >> 2, blockPos.getY() >> 2, blockPos.getZ() >> 2);
			waterLightAndBiome &= 0b11111000_00000000; // Clear biome bits
			waterLightAndBiome |= (mapChunk.region.dimension.manager.getBiomeColorIndex(biomes, biome, biome) & 0b111_11111111); // Biome

			if (height0 != height) {
				data.height[index] = (short) height;
				changed = true;
			}

			if (waterLightAndBiome0 != waterLightAndBiome) {
				data.waterLightAndBiome[index] = (short) waterLightAndBiome;

				if (biome != null && (waterLightAndBiome0 & 0b111_11111111) != (waterLightAndBiome & 0b111_11111111)) {
					data.foliage[index] = (data.foliage[index] & 0xFF000000) | (BiomeColors.FOLIAGE_COLOR_RESOLVER.getColor(biome, blockPos.getX(), blockPos.getZ()) & 0xFFFFFF);
					data.grass[index] = (data.grass[index] & 0xFF000000) | (BiomeColors.GRASS_COLOR_RESOLVER.getColor(biome, blockPos.getX(), blockPos.getZ()) & 0xFFFFFF);
					data.water[index] = (data.water[index] & 0xFF000000) | (BiomeColors.WATER_COLOR_RESOLVER.getColor(biome, blockPos.getX(), blockPos.getZ()) & 0xFFFFFF);
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
		debugTotalTime += debugLastTime;
		debugTotalCount++;
	}

	@Override
	public String toString() {
		return "ReloadChunkFromLevelPacketTask@" + packet.getX() + "," + packet.getZ();
	}
}