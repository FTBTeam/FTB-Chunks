package com.feed_the_beast.mods.ftbchunks.impl.map;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkImpl;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
import com.feed_the_beast.mods.ftbchunks.net.SendChunkPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class SendChunkTask implements MapTask
{
	private static class EitherChunk
	{
		private IChunk chunk;
		public MapChunk mapChunk;
	}

	private final World world;
	private final XZ chunkPosition;
	private final Predicate<ServerPlayerEntity> sendTo;

	public SendChunkTask(World w, XZ pos, Predicate<ServerPlayerEntity> p)
	{
		world = w;
		chunkPosition = pos;
		sendTo = p;
	}

	private static EitherChunk getChunkForHeight(World world, int x, int z)
	{
		EitherChunk e = new EitherChunk();
		MapRegion region = FTBChunksAPIImpl.manager.map.getDimension(world).getRegion(XZ.regionFromChunk(x, z));
		MapChunk mapChunk = region.chunks.get(XZ.of(x & 31, z & 31));

		if (mapChunk != null)
		{
			e.mapChunk = mapChunk;
		}
		else
		{
			e.chunk = world.getChunk(x, z, ChunkStatus.FULL, false);
		}

		return e;
	}

	private static int getHeight(EitherChunk e, BlockPos.Mutable currentBlockPos, int x, int z, int topY)
	{
		if (e.mapChunk != null)
		{
			return e.mapChunk.getHeight(x & 15, z & 15);
		}
		else if (e.chunk != null)
		{
			return MapChunk.getHeight(e.chunk, currentBlockPos, x, z, topY);
		}

		return -1;
	}

	@Override
	public void run()
	{
		List<ServerPlayerEntity> players = FTBChunksAPIImpl.manager.server.getPlayerList().getPlayers().stream().filter(sendTo).collect(Collectors.toList());

		if (players.isEmpty())
		{
			return;
		}

		String dimId = ChunkDimPos.getID(world);

		if (dimId.isEmpty())
		{
			return;
		}

		MapChunk c = FTBChunksAPIImpl.manager.map.getChunk(dimId, chunkPosition);
		EitherChunk cn = getChunkForHeight(world, chunkPosition.x, chunkPosition.z - 1);
		EitherChunk ce = getChunkForHeight(world, chunkPosition.x - 1, chunkPosition.z);
		EitherChunk cne = getChunkForHeight(world, chunkPosition.x - 1, chunkPosition.z - 1);

		int topY = world.func_234938_ad_() + 1;
		BlockPos.Mutable currentBlockPos = new BlockPos.Mutable();
		int blockX = chunkPosition.x << 4;
		int blockZ = chunkPosition.z << 4;

		int[][] heightMap = new int[17][17];

		for (int z = 0; z < 16; z++)
		{
			for (int x = 0; x < 16; x++)
			{
				heightMap[x + 1][z + 1] = c.getHeight(x, z);
			}
		}

		for (int i = 0; i < 16; i++)
		{
			heightMap[i + 1][0] = getHeight(cn, currentBlockPos, blockX + i, blockZ - 1, topY);
			heightMap[0][i + 1] = getHeight(ce, currentBlockPos, blockX - 1, blockZ + i, topY);
		}

		heightMap[0][0] = getHeight(cne, currentBlockPos, blockX - 1, blockZ - 1, topY);

		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

		for (int z = 0; z < 16; z++)
		{
			for (int x = 0; x < 16; x++)
			{
				int by = heightMap[x + 1][z + 1];
				int bn = heightMap[x + 1][z + 1 - 1];
				int bw = heightMap[x + 1 - 1][z + 1];
				float addedBrightness = 0F;

				if (bn != -1 || bw != -1)
				{
					if (by > bn || by > bw)
					{
						addedBrightness += 0.08F;
					}

					if (by < bn || by < bw)
					{
						addedBrightness -= 0.08F;
					}
				}

				int col = ColorBlend.addBrightness(c.getRGB(x, z), addedBrightness);

				if (col == 0xFF000000)
				{
					col = 0xFF010101;
				}

				image.setRGB(x, z, col);
			}
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try
		{
			ImageIO.write(image, "PNG", baos);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		SendChunkPacket packet = new SendChunkPacket();
		packet.dimension = dimId;
		packet.x = chunkPosition.x;
		packet.z = chunkPosition.z;
		packet.imageData = baos.toByteArray();

		ClaimedChunkImpl claimedChunk = FTBChunksAPIImpl.manager.getChunk(chunkPosition.dim(c.region.dimension.dimension));

		if (claimedChunk != null)
		{
			long now = new Date().getTime();
			packet.owner = claimedChunk.getDisplayName();
			packet.color = 0xFF000000 | claimedChunk.getColor();
			packet.relativeTimeClaimed = now - Date.from(claimedChunk.getTimeClaimed()).getTime();
			packet.forceLoaded = claimedChunk.isForceLoaded();

			if (packet.forceLoaded)
			{
				packet.relativeTimeForceLoaded = now - Date.from(claimedChunk.getForceLoadedTime()).getTime();
			}
		}

		for (ServerPlayerEntity player : players)
		{
			FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> player), packet);
		}
	}
}