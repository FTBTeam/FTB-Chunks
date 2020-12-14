package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.client.map.color.ColorUtils;
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

		MapChunk mapChunk = manager.getDimension(dimId).getRegion(XZ.regionFromChunk(pos)).getChunk(XZ.of(pos));
		MapRegion.Images images = mapChunk.region.getImages();

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
				int by = MathHelper.clamp(MapChunk.setHeight(ichunk, blockPos, flags).getY(), 0, 255);
				blockPos.setY(by);
				BlockState state = ichunk.getBlockState(blockPos);

				// A        B        G        R
				// FFFFFFFF HHHHHHHH WLLLLBBB BBBBBBBB
				// F - Filler
				// H - Height
				// W - Water
				// L - Light
				// B - Biome

				int ax = mapChunk.pos.x * 16 + wx;
				int az = mapChunk.pos.z * 16 + wz;
				int dataABGR0 = images.data.getPixelRGBA(ax, az) & 0xFFFFFF;
				int blockABGR0 = images.blocks.getPixelRGBA(ax, az) & 0xFFFFFF;
				int by0 = (dataABGR0 >> 16) & 255; // Get old height

				int dataABGR = (dataABGR0 & 0b00000000_00000111_11111111); // Clear height, water and light bits
				dataABGR |= by << 16; // Height
				dataABGR |= flags[0] ? (1 << 15) : 0; // Water
				dataABGR |= (world.getLightFor(LightType.BLOCK, blockPos.up()) & 15) << 11; // Light

				ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
				int blockABGR = manager.getBlockColorIndex(id == null ? ForgeRegistries.BLOCKS.getDefaultKey() : id);

				if (by != by0 || dataABGR0 == 0) // Only update biome, foliage, grass and water colors if its first visit or height changed
				{
					Biome biome = world.getBiome(blockPos);
					dataABGR &= 0b11111111_11111000_00000000; // Clear biome bits
					dataABGR |= (manager.getBiomeColorIndex(world, biome, biome) & 0b111_11111111); // Biome
					images.data.setPixelRGBA(ax + 512, az, ColorUtils.convertToNative(0xFF000000 | BiomeColors.getFoliageColor(world, blockPos)));
					images.data.setPixelRGBA(ax, az + 512, ColorUtils.convertToNative(0xFF000000 | BiomeColors.getGrassColor(world, blockPos)));
					images.data.setPixelRGBA(ax + 512, az + 512, ColorUtils.convertToNative(0xFF000000 | BiomeColors.getWaterColor(world, blockPos)));
					changed = true;
				}

				//100 00110010 01010011 10010010

				if (dataABGR0 != dataABGR)
				{
					images.data.setPixelRGBA(ax, az, 0xFF000000 | dataABGR);
					changed = true;
				}

				if (blockABGR0 != blockABGR)
				{
					images.blocks.setPixelRGBA(ax, az, 0xFF000000 | blockABGR);
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