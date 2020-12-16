package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.impl.XZ;
import net.minecraft.block.BlockState;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeColors;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * @author LatvianModder
 */
public class ReloadChunkTask implements MapTask
{
	private static final ResourceLocation AIR = new ResourceLocation("minecraft:air");

	public final World world;
	public final ChunkPos pos;
	private final MapManager manager;

	public ReloadChunkTask(World w, ChunkPos p)
	{
		world = w;
		pos = p;
		manager = MapManager.inst;
	}

	@Override
	public void runMapTask()
	{
		if (MapManager.inst != manager)
		{
			return;
		}

		RegistryKey<World> dimId = world.getDimensionKey();

		IChunk ichunk = world.getChunk(pos.x, pos.z, ChunkStatus.FULL, false);

		if (ichunk == null)
		{
			return;
		}

		MapChunk mapChunk = manager.getDimension(dimId).getRegion(XZ.regionFromChunk(pos)).getDataBlocking().getChunk(XZ.of(pos));
		MapRegionData data = mapChunk.region.getDataBlocking();

		int topY = world.func_234938_ad_() + 1;
		BlockPos.Mutable blockPos = new BlockPos.Mutable();
		int blockX = pos.getXStart();
		int blockZ = pos.getZStart();

		boolean changed = false;
		boolean[] flags = new boolean[1];

		try
		{
			for (int wi = 0; wi < 256; wi++)
			{
				int wx = wi % 16;
				int wz = wi / 16;
				blockPos.setPos(blockX + wx, topY, blockZ + wz);
				int height = MathHelper.clamp(MapChunk.getHeight(ichunk, blockPos, flags).getY(), Short.MIN_VALUE, Short.MAX_VALUE);
				blockPos.setY(height);
				BlockState state = ichunk.getBlockState(blockPos);

				int ax = mapChunk.pos.x * 16 + wx;
				int az = mapChunk.pos.z * 16 + wz;
				int index = ax + az * 512;

				int waterLightAndBiome0 = data.waterLightAndBiome[index] & 0xFFFF;
				int blockIndex0 = data.getBlockIndex(index);
				int height0 = data.height[index] & 0xFFFF; // Get old height

				int waterLightAndBiome = (waterLightAndBiome0 & 0b111_11111111); // Clear water and light bits
				waterLightAndBiome |= flags[0] ? (1 << 15) : 0; // Water
				waterLightAndBiome |= (world.getLightFor(LightType.BLOCK, blockPos.up()) & 15) << 11; // Light

				ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
				int blockIndex = manager.getBlockColorIndex(id == null ? AIR : id);

				if (height0 != height || waterLightAndBiome0 == 0) // Only update biome, foliage, grass and water colors if its first visit or height changed
				{
					Biome biome = world.getBiome(blockPos);
					waterLightAndBiome &= 0b11111000_00000000; // Clear biome bits
					waterLightAndBiome |= (manager.getBiomeColorIndex(world, biome, biome) & 0b111_11111111); // Biome
					data.foliage[index] = (data.foliage[index] & 0xFF000000) | (BiomeColors.getFoliageColor(world, blockPos) & 0xFFFFFF);
					data.grass[index] = (data.grass[index] & 0xFF000000) | (BiomeColors.getGrassColor(world, blockPos) & 0xFFFFFF);
					data.water[index] = (data.water[index] & 0xFF000000) | (BiomeColors.getWaterColor(world, blockPos) & 0xFFFFFF);
					changed = true;
				}

				if (height0 != height)
				{
					data.height[index] = (short) height;
					changed = true;
				}

				if (waterLightAndBiome0 != waterLightAndBiome)
				{
					data.waterLightAndBiome[index] = (short) waterLightAndBiome;
					changed = true;
				}

				if (blockIndex0 != blockIndex)
				{
					data.setBlockIndex(index, blockIndex);
					changed = true;
				}

				flags[0] = false;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		if (changed)
		{
			mapChunk.modified = System.currentTimeMillis();
			mapChunk.region.update(true);
		}
	}

	@Override
	public String toString()
	{
		return "ReloadChunkTask@" + pos;
	}
}