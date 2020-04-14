package com.feed_the_beast.mods.ftbchunks.impl.map;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
import com.feed_the_beast.mods.ftbchunks.net.SendChunk;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class ReloadChunkTask implements Runnable
{
	private final World world;
	private final XZ chunkPosition;
	private final Predicate<ServerPlayerEntity> playerFilter;
	private final boolean sendIsOptional;

	public ReloadChunkTask(World w, XZ pos, Predicate<ServerPlayerEntity> p, boolean s)
	{
		world = w;
		chunkPosition = pos;
		playerFilter = p;
		sendIsOptional = s;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void run()
	{
		MapChunk data = FTBChunksAPIImpl.manager.map.getChunk(chunkPosition.dim(world.dimension.getType()));
		int cx = data.pos.x + (data.region.pos.x << 5);
		int cz = data.pos.z + (data.region.pos.z << 5);
		IChunk ichunk = world.getChunk(cx, cz, ChunkStatus.FULL, true);

		if (!(ichunk instanceof Chunk))
		{
			return;
		}

		Chunk chunk = (Chunk) ichunk;

		BlockPos.Mutable currentBlockPos = new BlockPos.Mutable(0, 0, 0);
		int x = cx << 4;
		int z = cz << 4;
		long now = System.currentTimeMillis();
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

		try
		{
			int topY = Math.max(world.getActualHeight(), chunk.getTopFilledSegment() + 16) - 1;

			for (int wi = 0; wi < 256; wi++)
			{
				int wx = wi % 16;
				int wz = wi / 16;
				int by = topY;
				BlockState state = null;

				for (; by > 0; by--)
				{
					currentBlockPos.setPos(x + wx, by, z + wz);
					state = chunk.getBlockState(currentBlockPos);

					if (by == topY || state.getBlock() == Blocks.BEDROCK)
					{
						for (; by > 0; by--)
						{
							currentBlockPos.setPos(x + wx, by, z + wz);
							state = chunk.getBlockState(currentBlockPos);

							if (state.getBlock().isAir(state, world, currentBlockPos))
							{
								break;
							}
						}
					}

					if (state.getBlock() != Blocks.GRASS && state.getBlock() != Blocks.TALL_GRASS && !state.getBlock().isAir(state, world, currentBlockPos))
					{
						if (data.setHeight(wx, wz, by))
						{
							data.lastUpdate = now;
						}

						break;
					}
				}

				if (state == null)
				{
					data.setRGB(wx, wz, 0);
					continue;
				}

				Color4I color = FTBChunksAPIImpl.COLOR_MAP.get(state.getBlock());

				if (color == null)
				{
					color = Color4I.rgb(state.getMaterialColor(world, currentBlockPos).colorValue);
				}

				float addedBrightness = 0F;

				if ((state.getBlock() instanceof FlowingFluidBlock && ((FlowingFluidBlock) state.getBlock()).getFluid().isEquivalentTo(Fluids.WATER)) || FTBChunksAPIImpl.MAP_IGNORE_IN_WATER_TAG.contains(state.getBlock()))
				{
					for (int depth = 1; depth < 50; depth++)
					{
						currentBlockPos.setPos(x + wx, by - depth, z + wz);
						BlockState state1 = chunk.getBlockState(currentBlockPos);

						if (state1.getBlock() instanceof FlowingFluidBlock || FTBChunksAPIImpl.MAP_IGNORE_IN_WATER_TAG.contains(state1.getBlock()))
						{
							continue;
						}

						Color4I fluidColor = Color4I.rgb(ColorBlend.WATER.blend(world, currentBlockPos, 2)).withAlpha(220);

						color = FTBChunksAPIImpl.COLOR_MAP.get(state1.getBlock());

						if (color == null)
						{
							color = Color4I.rgb(state1.getMaterialColor(world, currentBlockPos).colorValue);
						}

						color = color.withTint(fluidColor);
						break;
					}
				}
				else
				{
					if (state.getBlock() instanceof GrassBlock)
					{
						color = Color4I.rgb(ColorBlend.GRASS.blend(world, currentBlockPos, 2)).withTint(Color4I.BLACK.withAlpha(50));
					}
					else if (state.getBlock() instanceof LeavesBlock || state.getBlock() instanceof VineBlock)
					{
						color = Color4I.rgb(ColorBlend.FOLIAGE.blend(world, currentBlockPos, 2)).withTint(Color4I.BLACK.withAlpha(50));
					}
				}

				int c = ColorBlend.addBrightness(color, addedBrightness);

				if (data.setRGB(wx, wz, c))
				{
					data.lastUpdate = now;
				}

				image.setRGB(wx, wz, (c & 0x00FFFFFF) | (by << 24));
			}

			FTBChunks.LOGGER.debug("Reloaded chunk " + data.pos + " in " + data.region.pos + " (" + FTBChunksAPIImpl.manager.map.taskQueue.size() + " tasks left)");

			if (data.lastUpdate == now || !sendIsOptional)
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(image, "PNG", baos);
				byte[] imageData = baos.toByteArray();

				for (ServerPlayerEntity player : world.getServer().getPlayerList().getPlayers())
				{
					if (playerFilter.test(player))
					{
						FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> player), new SendChunk(cx, cz, imageData));
					}
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}