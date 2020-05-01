package com.feed_the_beast.mods.ftbchunks.impl.map;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkImpl;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
import com.feed_the_beast.mods.ftbchunks.net.SendChunk;
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
import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class SendChunkTask implements Runnable
{
	private final World world;
	private final XZ chunkPosition;
	private final Predicate<ServerPlayerEntity> sendTo;

	public SendChunkTask(World w, XZ pos, Predicate<ServerPlayerEntity> p)
	{
		world = w;
		chunkPosition = pos;
		sendTo = p;
	}

	@Override
	public void run()
	{
		ChunkDimPos chunkDimPos = chunkPosition.dim(world.dimension.getType());
		MapChunk c = FTBChunksAPIImpl.manager.map.getChunk(chunkDimPos);
		IChunk cn = world.getChunk(chunkDimPos.x, chunkDimPos.z - 1, ChunkStatus.FULL, true);
		IChunk ce = world.getChunk(chunkDimPos.x - 1, chunkDimPos.z, ChunkStatus.FULL, true);
		IChunk cne = world.getChunk(chunkDimPos.x - 1, chunkDimPos.z - 1, ChunkStatus.FULL, true);

		int topY = world.getActualHeight() + 1;
		BlockPos.Mutable currentBlockPos = new BlockPos.Mutable();
		int blockX = chunkPosition.x << 4;
		int blockZ = chunkPosition.z << 4;

		int[][] heightMap = new int[17][17];

		for (int z = 0; z < 16; z++)
		{
			for (int x = 0; x < 16; x++)
			{
				heightMap[x + 1][z + 1] = c.height[x + z * 16] & 0xFF;
			}
		}

		for (int i = 0; i < 16; i++)
		{
			heightMap[i + 1][0] = MapChunk.getHeight(cn, currentBlockPos, blockX + i, blockZ - 1, topY);
			heightMap[0][i + 1] = MapChunk.getHeight(ce, currentBlockPos, blockX - 1, blockZ + i, topY);
		}

		heightMap[0][0] = MapChunk.getHeight(cne, currentBlockPos, blockX - 1, blockZ - 1, topY);

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

		SendChunk packet = new SendChunk();
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

		for (ServerPlayerEntity player : FTBChunksAPIImpl.manager.server.getPlayerList().getPlayers())
		{
			if (sendTo.test(player))
			{
				FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> player), packet);
			}
		}
	}
}